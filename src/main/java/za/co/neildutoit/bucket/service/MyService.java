package za.co.neildutoit.bucket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyService {

    private final RateLimitedService rateLimitedService;

    public String doSomething1() {
        return rateLimitedService.executeCall1();
    }

    public String doSomething2() {
        return rateLimitedService.executeCall2();
    }
}
