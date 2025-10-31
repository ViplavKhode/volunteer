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
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Value("${saayam.s3.keyPattern:users/%s/profile.jpg}")
    private String keyPattern;

    // Local memory cache
    private final Map<String, String> keyByUser = new ConcurrentHashMap<>();
    private final Map<String, String> etagByUser = new ConcurrentHashMap<>();
//    private static final Logger log = LoggerFactory.getLogger(ProfileImageStorageService.class);
//    @PostConstruct
//    void logS3Config() {
//        log.info("S3 config -> euBucket='{}', usBucket='{}', keyPattern='{}', maxBytes={}",
//                euBucket, usBucket, keyPattern, maxBytes);
//    }

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
        String key = keyByUser.get(userId);
        String bucket = pickBucket(regionHint);

        if (key == null) {
            // fall back to DB
            var uriOpt = userService.getProfilePicturePath(userId);
            if (uriOpt.isPresent()) {
                var uri = URI.create(uriOpt.get());
                String ssp = uri.getSchemeSpecificPart();
                int slash = ssp.indexOf('/');
                if (slash > 0) {
                    String dbBucket = ssp.substring(0, slash);
                    key = ssp.substring(slash + 1);
                    bucket = dbBucket;
                }
            }
        }

        if (key != null) {
            pickClient(regionHint).deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        }

        keyByUser.remove(userId);
        etagByUser.remove(userId);
        userService.setProfilePicturePath(userId, null);
    }

    //
    private void validate(String mime, long size) {
        var allowed = Arrays.asList(allowedMimeCsv.split(","));
        if (!allowed.contains(mime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported image type. Allowed: " + String.join(", ", allowed) + ".");
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
//    private String pickBucket(String regionHint) {
//        return isEu(regionHint) ? euBucket : usBucket;
//    }
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

    // new API
//    public record UploadResult(String s3Uri, URI viewUrl) {}

//    public UploadResult uploadAndPersist(String userId, MultipartFile file, String regionHint) {
//        // 1) Validate
//        String mime = Optional.ofNullable(file.getContentType()).orElse("");
//        long size  = file.getSize();
//        validate(mime, size); // you already have validate(...)
//
//        // 2) Build target (bucket/key) and upload via AWS SDK
//        String bucket = pickBucket(regionHint);
//        String key    = buildKey(userId, mime); // users/{id}/profile.jpg
//
//        var put = PutObjectRequest.builder()
//                .bucket(bucket)
//                .key(key)
//                .contentType(mime)
//                .serverSideEncryption("AES256")
//                .build();
//
//        try {
//            pickClient(regionHint).putObject(put, software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));
//        } catch (IOException e) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file bytes");
//        }
//
//        // 3) Persist canonical S3 URI to RDS
//        String s3Uri = "s3://" + bucket + "/" + key;
//        userService.setProfilePicturePath(userId, s3Uri);
//
//        // 4) Return a fresh presigned VIEW URL for instant use in FE
//        var viewUrl = presignView(userId, regionHint).orElseThrow(
//                () -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to presign view"));
//
//        // Update in-memory caches so GET works immediately
//        keyByUser.put(userId, key);
//        etagByUser.put(userId, "");
//
//        return new UploadResult(s3Uri, viewUrl);
//    }

    public Map<String, Object> uploadMultipart(String userId,
                                               org.springframework.web.multipart.MultipartFile file,
                                               String regionHint) throws java.io.IOException {
        // 1) Validate content-type + size
        validate(file.getContentType(), file.getSize());

        // 2) Determine bucket/key/client
        String bucket = pickBucket(regionHint);           // <-- throws if blank
        String key    = buildKey(userId, file.getContentType());
        S3Client s3   = pickClient(regionHint);

        // 3) Put to S3
        software.amazon.awssdk.services.s3.model.PutObjectRequest putReq =
                software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .build();

        s3.putObject(putReq, software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));

        // 4) Save canonical S3 URI to DB + cache key (so presignView can reuse)
        String s3Uri = "s3://" + bucket + "/" + key;
        userService.setProfilePicturePath(userId, s3Uri);
        keyByUser.put(userId, key);

        // 5) Return a presigned GET now for immediate preview
        software.amazon.awssdk.services.s3.model.GetObjectRequest getReq =
                software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build();

        java.net.URI viewUrl = java.net.URI.create(
                pickPresigner(regionHint)
                        .presignGetObject(g -> g
                                .signatureDuration(java.time.Duration.ofSeconds(getTtlSeconds))
                                .getObjectRequest(getReq))
                        .url().toString()
        );

        return java.util.Map.of(
                "message", "Uploaded",
                "userId", userId,
                "key", key,
                "s3Uri", s3Uri,
                "viewUrl", viewUrl.toString()
        );
    }
}