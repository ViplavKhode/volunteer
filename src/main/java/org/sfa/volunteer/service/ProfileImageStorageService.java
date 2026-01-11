package org.sfa.volunteer.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProfileImageStorageService {

    private final UserService userService;
    private final S3Client s3ClientUs;
    private final S3Client s3ClientEu;

    @Value("${saayam.s3.buckets.euPrivate}")
    private String euBucket;

    @Value("${saayam.s3.buckets.usPrivate}")
    private String usBucket;

    @Value("${saayam.s3.maxBytes:2097152}")
    private long maxBytes;

    @Value("${saayam.s3.allowedMime:image/jpeg,image/png,image/webp}")
    private String allowedMimeCsv;

    @Value("${saayam.s3.keyPattern:users/%s/profile.jpg}")
    private String keyPattern;

    private final Map<String, String> keyByUser = new ConcurrentHashMap<>();

    public ProfileImageStorageService(
            UserService userService,
            @Qualifier("s3ClientUs") S3Client s3ClientUs,
            @Qualifier("s3ClientEu") S3Client s3ClientEu
    ) {
        this.userService = userService;
        this.s3ClientUs = s3ClientUs;
        this.s3ClientEu = s3ClientEu;
    }

    public Map<String, Object> uploadBase64(String userId, String contentType, String base64, String regionHint) {
        if (!userService.userExists(userId)) {
            throw new org.sfa.volunteer.exception.UserNotFoundException(userId);
        }
        if (contentType == null || contentType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentType is required");
        }
        if (base64 == null || base64.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "base64 is required");
        }

        String cleaned = stripDataUrlPrefix(base64);

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(cleaned);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid base64");
        }

        validate(contentType, bytes.length);

        String bucket = pickBucket(regionHint);
        String key = buildKey(userId);

        try {
            pickClient(regionHint).putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .serverSideEncryption("AES256")
                            .build(),
                    RequestBody.fromBytes(bytes)
            );
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload to S3", e);
        }

        String s3Uri = "s3://" + bucket + "/" + key;
        userService.setProfilePicturePath(userId, s3Uri);
        keyByUser.put(userId, key);

        return Map.of(
                "message", "Profile image uploaded",
                "userId", userId,
                "s3Uri", s3Uri,
                "bucket", bucket,
                "key", key
        );
    }

    public void delete(String userId, String regionHint) {
        if (!userService.userExists(userId)) {
            throw new org.sfa.volunteer.exception.UserNotFoundException(userId);
        }

        var uriOpt = userService.getProfilePicturePath(userId);
        if (uriOpt.isEmpty()) {
            keyByUser.remove(userId);
            return;
        }

        URI uri = URI.create(uriOpt.get());
        String ssp = uri.getSchemeSpecificPart();
        if (ssp.startsWith("//")) ssp = ssp.substring(2);

        int slash = ssp.indexOf('/');
        if (slash <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid profile picture URI");
        }

        String bucket = ssp.substring(0, slash);
        String key = ssp.substring(slash + 1);

        String effectiveRegion = regionHint;
        if (effectiveRegion == null || effectiveRegion.isBlank()) {
            effectiveRegion = bucket.equals(euBucket) ? "eu-west-1" : "us-east-1";
        }

        try {
            pickClient(effectiveRegion).deleteObject(
                    DeleteObjectRequest.builder().bucket(bucket).key(key).build()
            );
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete from S3", e);
        }

        keyByUser.remove(userId);
        userService.setProfilePicturePath(userId, null);
    }

    public Optional<DownloadedImage> download(String userId, String regionHint) {

        if (!userService.userExists(userId)) {
            throw new org.sfa.volunteer.exception.UserNotFoundException(userId);
        }

        var uriOpt = userService.getProfilePicturePath(userId);
        if (uriOpt.isEmpty()) return Optional.empty();

        URI uri = URI.create(uriOpt.get());
        String ssp = uri.getSchemeSpecificPart();
        if (ssp.startsWith("//")) ssp = ssp.substring(2);

        int slash = ssp.indexOf('/');
        if (slash <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid profile picture URI");
        }

        String bucket = ssp.substring(0, slash);
        String key = ssp.substring(slash + 1);

        String effectiveRegion = (regionHint == null || regionHint.isBlank())
                ? (bucket.equals(euBucket) ? "eu-west-1" : "us-east-1")
                : regionHint;

        try {
            ResponseBytes<GetObjectResponse> obj = pickClient(effectiveRegion)
                    .getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build());

            String contentType = obj.response().contentType();
            if (contentType == null || contentType.isBlank()) contentType = "application/octet-stream";

            return Optional.of(new DownloadedImage(contentType, obj.asByteArray()));

        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            return Optional.empty();
        }
    }

    public record DownloadedImage(String contentType, byte[] bytes) {}

    // ---------------- helpers ----------------

    private String stripDataUrlPrefix(String base64) {
        int comma = base64.indexOf(',');
        if (comma > 0 && base64.substring(0, comma).contains("base64")) {
            return base64.substring(comma + 1);
        }
        return base64;
    }

    private void validate(String mime, long size) {
        String m = Optional.ofNullable(mime).orElse("").trim();
        int semi = m.indexOf(';');
        if (semi > -1) m = m.substring(0, semi).trim();

        List<String> allowed = Arrays.asList(allowedMimeCsv.split(","));
        if (!allowed.contains(m)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported image type. Allowed: " + String.join(", ", allowed));
        }
        if (size <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
        }
        if (size > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Max upload size is 5 MB");
        }
    }

    private String buildKey(String userId) {
        return String.format(keyPattern, userId);
    }

    private boolean isEu(String regionHint) {
        return regionHint != null && regionHint.trim().equalsIgnoreCase("eu-west-1");
    }

    private String pickBucket(String regionHint) {
        String bucket = isEu(regionHint) ? euBucket : usBucket;
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3 bucket config missing: saayam.s3.buckets.*");
        }
        return bucket;
    }

    private S3Client pickClient(String regionHint) {
        return isEu(regionHint) ? s3ClientEu : s3ClientUs;
    }
}