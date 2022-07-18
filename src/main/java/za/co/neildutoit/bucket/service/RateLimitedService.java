package za.co.neildutoit.bucket.service;

import za.co.neildutoit.bucket.ratelimit.BucketRateLimit;
import org.springframework.stereotype.Service;

@Service
public class RateLimitedService {
    @BucketRateLimit(bucket = "A")
    public String executeCall1() {
        return "Success";
    }

    @BucketRateLimit(bucket = "B", consumeAmount = 2)
    public String executeCall2() {
        return "Success";
    }
}
