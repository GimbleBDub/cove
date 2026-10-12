package bw.co.gimble.cove.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String endpoint;

    private String accessKey;
    private String secretKey;

    private String kycBucket;
    private String profileBucket;
    private String organisationsBucket;

    private String propertyMediaBucket;

    private String publicBaseUrl;

    private long presignedUrlTtlMinutes = 15;
}