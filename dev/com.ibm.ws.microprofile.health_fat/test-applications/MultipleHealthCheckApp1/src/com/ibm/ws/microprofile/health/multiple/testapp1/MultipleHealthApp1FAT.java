package com.ibm.ws.microprofile.health.multiple.testapp1;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

@ApplicationScoped
@Health
public class MultipleHealthApp1FAT implements HealthCheck {

    private int count = 1;

    @Override
    public HealthCheckResponse call() {
        if (count == 1) {
            count++;
            return HealthCheckResponse.named("testMultipeUPChecks1").up().withData("CPU", "online").withData("Fan", "functional").build();
        } else if (count == 2) {
            count++;
            return HealthCheckResponse.named("testMultipeDOWNChecks1").down().withData("CPU", "offline").withData("Fan", "failed").build();

        } else {
            count++;
            return HealthCheckResponse.named("testMultipeUPChecks1 - something wrong with the app").up().build();
        }
    }
}
