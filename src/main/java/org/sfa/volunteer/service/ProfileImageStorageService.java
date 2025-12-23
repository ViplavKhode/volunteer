package org.sfa.volunteer.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProfileImageStorageService {

    private final UserService userService;
    private final S3Client s3ClientUs;
    private final S3Client s3ClientEu;
    private final S3Presigner s3PresignerUs;
    private final S3Presigner s3PresignerEu;

    public ProfileImageStorageService(
            UserService userService,
            @Qualifier("s3ClientUs") S3Client s3ClientUs,
            @Qualifier("s3ClientEu") S3Client s3ClientEu,
            @Qualifier("s3PresignerUs") S3Presigner s3PresignerUs,
            @Qualifier("s3PresignerEu") S3Presigner s3PresignerEu
    ) {
        this.userService = userService;
        this.s3ClientUs = s3ClientUs;
        this.s3ClientEu = s3ClientEu;
        this.s3PresignerUs = s3PresignerUs;
        this.s3PresignerEu = s3PresignerEu;
    }

    @Value("${saayam.s3.buckets.euPrivate}")
    private String euBucket;

    @Value("${saayam.s3.buckets.usPrivate}")
    private String usBucket;

    @Value("${saayam.s3.presign.getTtlSeconds:300}")
    private int getTtlSeconds;

    @Value("${saayam.s3.maxBytes:2097152}")
    private long maxBytes;

    @Value("${saayam.s3.allowedMime:image/jpeg,image/png,image/webp}")
    private String allowedMimeCsv;

    @Value("${saayam.s3.keyPattern:users/%s/profile.jpg}")
    private String keyPattern;

    @Value("${saayam.s3.presign.putTtlSeconds:300}")
    private int putTtlSeconds;

    // Local memory cache
    private final Map<String, String> keyByUser = new ConcurrentHashMap<>();

    /** Presign a GET; Returns a presigned GET URL if the user has an image; otherwise empty. */
    public Optional<URI> presignView(String userId, String regionHint) {
        if (!userService.userExists(userId)) {
            return Optional.empty();
        }
        String key = keyByUser.get(userId);
        String bucket = null;

        if (key == null) {
            // fallback: read S3 URI from DB
            var uriOpt = userService.getProfilePicturePath(userId);
            if (uriOpt.isEmpty()) return Optional.empty();

            var uri = URI.create(uriOpt.get());
            String ssp = uri.getSchemeSpecificPart();
            if (ssp.startsWith("//")) {
                ssp = ssp.substring(2);
            }
            int slash = ssp.indexOf('/');
            if (slash < 0) return Optional.empty();
            bucket = ssp.substring(0, slash);
            key    = ssp.substring(slash + 1);

            if (regionHint == null || regionHint.isBlank()) {
                if (bucket.equals(euBucket))       regionHint = "eu-west-1";
                else if (bucket.equals(usBucket))  regionHint = "us-east-1";
                else                               regionHint = "us-east-1"; // safe default
            }
            keyByUser.put(userId, key);
        }

        if (bucket == null) {
            bucket = pickBucket(regionHint);
        }

        var getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        var presigned = pickPresigner(regionHint).presignGetObject(g -> g
                .signatureDuration(Duration.ofSeconds(getTtlSeconds))
                .getObjectRequest(getReq));

        return Optional.of(URI.create(presigned.url().toString()));
    }

    /** Delete object from S3 + clear DB + clear cache. */
    public void delete(String userId, String regionHint) {
        // Ensure user exists
        if (!userService.userExists(userId)) {
            throw new org.sfa.volunteer.exception.UserNotFoundException(userId);
        }

        // Read current S3 URI from DB
        var uriOpt = userService.getProfilePicturePath(userId);
        if (uriOpt.isEmpty()) {
            // No profile picture stored → nothing to delete
            keyByUser.remove(userId);
            return;
        }

        var uri = java.net.URI.create(uriOpt.get());
        String ssp = uri.getSchemeSpecificPart();
        if (ssp.startsWith("//")) {
            ssp = ssp.substring(2);
        }

        int slash = ssp.indexOf('/');
        if (slash <= 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid profile picture URI for user " + userId
            );
        }

        String bucket = ssp.substring(0, slash);
        String key    = ssp.substring(slash + 1);

        // Resolve effective region
        String effectiveRegion = regionHint;
        if (effectiveRegion == null || effectiveRegion.isBlank()) {
            if (bucket.equals(euBucket)) {
                effectiveRegion = "eu-west-1";
            } else {
                effectiveRegion = "us-east-1";
            }
        }

        // Delete from S3
        try {
            pickClient(effectiveRegion).deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()
            );
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete profile image from S3",
                    e
            );
        }

        // Only after S3 delete ok → clear DB + cache
        keyByUser.remove(userId);
        userService.setProfilePicturePath(userId, null);
    }

    public Map<String, Object> presignUpload(String userId, String contentType, long contentLength, String regionHint) {

        // 0) Ensure user exists before touching S3
        if (!userService.userExists(userId)) {
            throw new org.sfa.volunteer.exception.UserNotFoundException(userId);
        }

        // 1) Validate (re-use existing validate)
        validate(contentType, contentLength);

        // 2) Bucket/key
        String bucket = pickBucket(regionHint);
        String key = buildKey(userId, contentType);

        // 3) Presign PUT
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .serverSideEncryption("AES256")
                .build();

        PresignedPutObjectRequest presigned = pickPresigner(regionHint)
                .presignPutObject(PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofSeconds(putTtlSeconds))
                        .putObjectRequest(putReq)
                        .build());

        // 4) Canonical S3 URI (what we store later in DB)
        String s3Uri = "s3://" + bucket + "/" + key;

        keyByUser.put(userId, key);

        return Map.of(
                "message", "Presigned upload URL generated",
                "userId", userId,
                "bucket", bucket,
                "key", key,
                "s3Uri", s3Uri,
                "putUrl", presigned.url().toString(),
                "expiresInSeconds", putTtlSeconds
        );
    }

    public void confirmUpload(String userId, String s3Uri) {
        if (!userService.userExists(userId)) {
            throw new org.sfa.volunteer.exception.UserNotFoundException(userId);
        }
        userService.setProfilePicturePath(userId, s3Uri);

        try {
            var uri = URI.create(s3Uri);
            String ssp = uri.getSchemeSpecificPart();
            if (ssp.startsWith("//")) ssp = ssp.substring(2);
            int slash = ssp.indexOf('/');
            if (slash > 0) {
                String key = ssp.substring(slash + 1);
                keyByUser.put(userId, key);
            }
        } catch (Exception ignored) { }
    }

    // helper
    private void validate(String mime, long size) {
        String m = Optional.ofNullable(mime).orElse("").trim();
        int semi = m.indexOf(';');
        if (semi > -1) m = m.substring(0, semi).trim();

        var allowed = Arrays.asList(allowedMimeCsv.split(","));
        if (!allowed.contains(m)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported image type. Allowed: " + String.join(", ", allowed) + ".");
        }
        if (size <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file.");
        }
        if (size > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Max upload size is 5 MB.");
        }
    }

    private String buildKey(String userId, String mime) {
        return String.format(keyPattern, userId);
    }

    private boolean isEu(String regionHint) {
        return regionHint != null && regionHint.trim().equalsIgnoreCase("eu-west-1");
    }

    private String pickBucket(String regionHint) {
        String bucket = isEu(regionHint) ? euBucket : usBucket;
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3 bucket config missing. Check saayam.s3.buckets.* in application.properties");
        }
        return bucket;
    }

    private S3Presigner pickPresigner(String regionHint) {
        return isEu(regionHint) ? s3PresignerEu : s3PresignerUs;
    }
    private S3Client pickClient(String regionHint) {
        return isEu(regionHint) ? s3ClientEu : s3ClientUs;
    }
}