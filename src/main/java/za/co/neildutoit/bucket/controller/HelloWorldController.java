package za.co.neildutoit.bucket.controller;

import za.co.neildutoit.bucket.service.MyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
class HelloWorldController {

    private final MyService myService;

    @GetMapping("1")
    String sayHello1() {
        return myService.doSomething1();
    }

    @GetMapping("2")
    String sayHello2() {
        return myService.doSomething2();
    }
}
