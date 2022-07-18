package za.co.neildutoit.bucket.ratelimit;

import io.github.bucket4j.*;
import io.github.bucket4j.grid.ProxyManager;
import io.github.bucket4j.grid.jcache.JCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import za.co.neildutoit.bucket.config.BucketConfig;
import za.co.neildutoit.bucket.config.BucketProperties;

import javax.annotation.PostConstruct;
import javax.cache.Cache;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.function.Supplier;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class BucketRateLimitService {

  private final Cache cache;

  private final BucketProperties bucketProperties;

  private ProxyManager<String> buckets;

  @PostConstruct
  public void init() {
    buckets = Bucket4j.extension(JCache.class).proxyManagerForCache(this.cache);
  }

  @Around("@annotation(za.co.neildutoit.bucket.ratelimit.BucketRateLimit)")
  public Object applyRateLimit(ProceedingJoinPoint point) throws Throwable {
    BucketRateLimit myAnnotation = getAnnotation(point);

    BucketConfig selectedBucket = getCorrectBucket(point, myAnnotation);
    BucketConfiguration bucketConfiguration = getBucketConfiguration(selectedBucket);

    // Prepare configuration supplier which will be called(on first interaction with proxy) if bucket was not saved yet previously.
    Supplier<BucketConfiguration> configurationLazySupplier = () -> bucketConfiguration;

    // Acquire proxy to bucket in the cache
    Bucket bucket = buckets.getProxy(selectedBucket.getName(), configurationLazySupplier);

    // Attempt to consume the number of tokens specified in the annotation (1 by default)
    log.debug("Attempting to consume {} tokens from bucket {}, available tokens: {}", myAnnotation.consumeAmount(), selectedBucket.getName(), bucket.getAvailableTokens());
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(myAnnotation.consumeAmount());

    Object object;
    if (probe.isConsumed()) {
      // the limit is not exceeded
      object = point.proceed();
      log.info("Consume success - Remaining {}", probe.getRemainingTokens());
    } else {
      // limit is exceeded
      log.warn("Consume failure - Refill in {}", probe.getNanosToWaitForRefill());
      throw new Exception("Limit exceeded");
    }

    return object;
  }

  /**
   * Builds the configuration of the bucket containing the capacity and refill rate
   *
   * @param selectedBucket The bucket selected by the annotation
   * @return BucketConfiguration
   */
  private BucketConfiguration getBucketConfiguration(BucketConfig selectedBucket) {
    if (selectedBucket.getCapacity() == 0) {
      log.error("No capacity specified for bucket '{}'", selectedBucket);
      throw new RuntimeException("Error - Check logs");
    }

    if (selectedBucket.getRefillSeconds() == 0 && selectedBucket.getRefillMinutes() == 0) {
      log.error("No refill specified for bucket '{}'", selectedBucket);
      throw new RuntimeException("Error - Check logs");
    }

    //Calculate everything in seconds
    long seconds = selectedBucket.getRefillSeconds() + (selectedBucket.getRefillMinutes() * 60);

    return Bucket4j.configurationBuilder()
            .addLimit(Bandwidth.simple(selectedBucket.getCapacity(), Duration.ofSeconds(seconds)))
            .buildConfiguration();
  }

  /**
   * Uses the bucket configured in the annotation to get the configuration for the bucket
   *
   * @param point - The Aspect interrupt point
   * @return BucketConfig if found, throws RuntimeException if not found
   */
  private BucketConfig getCorrectBucket(ProceedingJoinPoint point, BucketRateLimit myAnnotation) {
    String bucketToUse = myAnnotation.bucket();

    BucketConfig selectedBucket = null;

    if (bucketToUse != null) {
      for (BucketConfig bucketConfig : bucketProperties.getConfig()) {
        if (bucketToUse.equals(bucketConfig.getName())) {
          selectedBucket = bucketConfig;
        }
      }
    }

    if (selectedBucket == null) {
      log.error("No matching bucket configured for bucket '{}'", bucketToUse);
      throw new RuntimeException("No matching bucket configured for bucket '" + bucketToUse + "'");
    }
    return selectedBucket;
  }

  private BucketRateLimit getAnnotation(ProceedingJoinPoint point) {
    MethodSignature signature = (MethodSignature) point.getSignature();
    Method method = signature.getMethod();
    BucketRateLimit myAnnotation = method.getAnnotation(BucketRateLimit.class);
    return myAnnotation;
  }
}