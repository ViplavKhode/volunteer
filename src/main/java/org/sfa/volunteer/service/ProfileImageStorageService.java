package org.sfa.volunteer.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProfileImageStorageService {

    private final UserService userService;

    // Region-specific clients/presigners //
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

    @Value("${saayam.s3.presign.putTtlSeconds:300}")
    private int putTtlSeconds;

    @Value("${saayam.s3.maxBytes:2097152}")
    private long maxBytes;

    @Value("${saayam.s3.allowedMime:image/jpeg,image/png,image/webp}")
    private String allowedMimeCsv;

    @Value("${saayam.s3.keyPattern:users/%s/profile%s}")
    private String keyPattern;

    // Local memory cache
    private final Map<String, String> keyByUser = new ConcurrentHashMap<>();
    private final Map<String, String> etagByUser = new ConcurrentHashMap<>();

    // Public API //
    /** Presign a PUT for the caller’s region; validates MIME + size. */
    public PresignedUpload presignUpload(String userId, String mimeType, long sizeBytes, String regionHint) {
        validate(mimeType, sizeBytes);
        String bucket = pickBucket(regionHint);
        String key    = buildKey(userId, mimeType); // users/{id}/profile.jpg

        var putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(mimeType)
                .build();

        var presigned = pickPresigner(regionHint).presignPutObject(p -> p
                .signatureDuration(Duration.ofSeconds(putTtlSeconds))
                .putObjectRequest(putReq));

        return new PresignedUpload(
                URI.create(presigned.url().toString()),
                key,
                Map.of("Content-Type", mimeType)
        );
    }

    /** Presign a GET; if cache is empty, reads canonical S3 URI from DB. */
    public Optional<URI> presignView(String userId, String regionHint) {
        String key = keyByUser.get(userId);
        String bucket = null;

        if (key == null) {
            // fallback: read S3 URI from DB
            var uriOpt = userService.getProfilePicturePath(userId);
            if (uriOpt.isEmpty()) return Optional.empty();

            var uri = URI.create(uriOpt.get());
            String ssp = uri.getSchemeSpecificPart();
            int slash = ssp.indexOf('/');
            if (slash < 0) return Optional.empty();
            bucket = ssp.substring(0, slash);
            key    = ssp.substring(slash + 1);

            if (regionHint == null || regionHint.isBlank()) {
                if (bucket.equals(euBucket))       regionHint = "eu-west-1";
                else if (bucket.equals(usBucket))  regionHint = "us-east-1";
                else                               regionHint = "us-east-1"; // safe default
            }
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

    /** Confirm upload → cache + persist canonical S3 URI (s3://bucket/key). */
    public void confirmUpload(String userId, String key, String etag, String regionHint) {
        keyByUser.put(userId, key);
        etagByUser.put(userId, etag == null ? "" : etag);

        // persist stable S3 URI like s3://<bucket>/users/<id>/profile.jpg
        String bucket = pickBucket(regionHint);
        String s3Uri  = "s3://" + bucket + "/" + key;
        userService.setProfilePicturePath(userId, s3Uri);
    }

    /** Delete object + clear cache + clear DB field. */
    public void delete(String userId, String regionHint) {
        String key = keyByUser.remove(userId);
        etagByUser.remove(userId);
        if (key == null) return;

        String bucket = pickBucket(regionHint);
        pickClient(regionHint).deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
        userService.setProfilePicturePath(userId, null); //
    }

    //
    private void validate(String mime, long size) {
        var allowed = Arrays.asList(allowedMimeCsv.split(","));
        if (!allowed.contains(mime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only JPEG allowed (send image/jpeg).");
        }
        if (size > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Max upload size is 5 MB.");
        }
    }

    private String buildKey(String userId, String mime) {
        return String.format(keyPattern, userId);
    }

    // DTO
    public record PresignedUpload(URI url, String key, Map<String,String> headers) {}

    // AWS
    // helper
    private boolean isEu(String regionHint) {
        return regionHint != null && regionHint.trim().equalsIgnoreCase("eu-west-1");
    }
    private String pickBucket(String regionHint) {
        return isEu(regionHint) ? euBucket : usBucket;
    }
    private S3Presigner pickPresigner(String regionHint) {
        return isEu(regionHint) ? s3PresignerEu : s3PresignerUs;
    }
    private S3Client pickClient(String regionHint) {
        return isEu(regionHint) ? s3ClientEu : s3ClientUs;
    }
}