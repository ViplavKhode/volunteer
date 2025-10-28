package org.sfa.volunteer.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;


@Configuration
@RequiredArgsConstructor
//@ConditionalOnProperty(name = "minio.endpoint") // only active when env MINIO_ENDPOINT is set
public class S3Config {
    private final Environment env;

//    Local MinIO   //
//    private String get(String key, String def) {
//        String v = env.getProperty("minio." + key);
//        return (v != null && !v.isBlank()) ? v : def;
//    }
//
//    @Bean
//    public S3Client s3Client() {
//        return S3Client.builder()
//                .endpointOverride(URI.create(get("endpoint", "http://127.0.0.1:9000")))
//                .region(Region.US_EAST_1)
//                .forcePathStyle(true)
//                .credentialsProvider(StaticCredentialsProvider.create(
//                        AwsBasicCredentials.create(
//                                get("accessKey", "minioadmin"),
//                                get("secretKey", "minioadmin"))))
//                .build();
//    }
//
//    @Bean
//    public S3Presigner s3Presigner() {
//        return S3Presigner.builder()
//                .endpointOverride(URI.create(get("endpoint", "http://127.0.0.1:9000")))
//                .region(Region.US_EAST_1)
//                .credentialsProvider(StaticCredentialsProvider.create(
//                        AwsBasicCredentials.create(
//                                get("accessKey", "minioadmin"),
//                                get("secretKey", "minioadmin"))))
//                .build();
//    }

    // AWS
    @Bean
    @Primary
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(false)
                        .build())
                .build();
    }

    @Bean(name = "s3Presigner")
    @Primary
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    @Bean("s3ClientUs")
    public S3Client s3ClientUs() {
        return S3Client.builder()
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(false) // AWS default
                        .build())
                .build();
    }

    @Bean("s3ClientEu")
    public S3Client s3ClientEu() {
        return S3Client.builder()
                .region(Region.EU_WEST_1)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(false)
                        .build())
                .build();
    }

    @Bean("s3PresignerUs")
    public S3Presigner s3PresignerUs() {
        return S3Presigner.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    @Bean("s3PresignerEu")
    public S3Presigner s3PresignerEu() {
        return S3Presigner.builder()
                .region(Region.EU_WEST_1)
                .build();
    }
}