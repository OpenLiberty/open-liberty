package com.ibm.ws.microprofile.health.noapi.testapp;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.Health;

@ApplicationScoped
@Health
public class NoHealthAPIAppFAT {

    public void call() {
        System.out.println("This is not a HealthCheckApp");
    }

}
