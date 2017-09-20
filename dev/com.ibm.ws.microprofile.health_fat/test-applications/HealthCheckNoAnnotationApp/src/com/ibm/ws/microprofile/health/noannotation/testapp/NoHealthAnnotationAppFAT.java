package com.ibm.ws.microprofile.health.noannotation.testapp;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

@ApplicationScoped
public class NoHealthAnnotationAppFAT implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("testNoAnnotationPresent").up().build();
    }

}
