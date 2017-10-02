package com.ibm.ws.microprofile.health.testapp;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

@ApplicationScoped
@Health
public class HealthAppFAT implements HealthCheck {

    private int count = 1;

    @Override
    public HealthCheckResponse call() {

        if (count == 1) {
            count++;
            return HealthCheckResponse.named("testJsonReceived").up().build();
        } else if (count == 2) {
            count++;
            return HealthCheckResponse.named("testSingleOutcomeUP").up().build();
        } else if (count == 3) {
            count++;
            return HealthCheckResponse.named("testSingleOutcomeDOWN").down().build();
        } else if (count == 4) {
            count++;
            return HealthCheckResponse.named("testCheckUPWithData").withData("CPU", "online").withData("Fan", "functional").up().build();

        } else if (count == 5) {
            count++;
            return HealthCheckResponse.named("testCheckDOWNWithData").withData("CPU", "offline").withData("Fan", "failed").down().build();
        } else {
            return HealthCheckResponse.named("something is not right in the app").up().withData("count", count).build();
        }
    }

    // TODO Auto-generated method stub
    //testResponseBuilder.name("myFirstCheck");
    //testResponseBuilder.up();
    //return testResponseBuilder.build();
}