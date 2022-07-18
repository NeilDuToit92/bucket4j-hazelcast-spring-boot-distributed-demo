package za.co.neildutoit.bucket.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "buckets")
@Getter
@Setter
public class BucketProperties {
  private List<BucketConfig> config;
}
