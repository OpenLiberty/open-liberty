package com.ibm.ws.microprofile.health.multiple.testapp2;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

@ApplicationScoped
@Health
public class MultipleHealthApp2FAT implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("testMultipeUPChecks2").up().build();
    }
}
