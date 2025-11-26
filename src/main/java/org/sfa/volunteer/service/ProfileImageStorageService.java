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

    @Value("${saayam.s3.keyPattern:users/%s/profile.jpg}")
    private String keyPattern;

    // Local memory cache
    private final Map<String, String> keyByUser = new ConcurrentHashMap<>();
    private final Map<String, String> etagByUser = new ConcurrentHashMap<>();

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

    /** Delete object + clear cache + clear DB field. */
    public void delete(String userId, String regionHint) {
        String key = keyByUser.get(userId);
        String bucket = pickBucket(regionHint);
        // Fallback to DB if key is not in memory
        if (key == null) {
            var uriOpt = userService.getProfilePicturePath(userId);
            if (uriOpt.isPresent()) {
                var uri = URI.create(uriOpt.get());
                String ssp = uri.getSchemeSpecificPart();
                // Remove leading "//" if present
                if (ssp.startsWith("//")) {
                    ssp = ssp.substring(2);
                }
                int slash = ssp.indexOf('/');
                if (slash > 0) {
                    String dbBucket = ssp.substring(0, slash);
                    key = ssp.substring(slash + 1);
                    bucket = dbBucket; // override bucket to real bucket
                }
            }
        }
        // Perform delete ONLY if key exists
        if (key != null) {
            pickClient(regionHint).deleteObject(
                        DeleteObjectRequest.builder()
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

    // AWS
    // helper
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
                        .serverSideEncryption("AES256")
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