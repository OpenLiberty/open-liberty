package com.ibm.websphere.microprofile.faulttolerance_fat.validation;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.SECONDS;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
public class RetryJitterLongerThanDelay {

    @Retry(jitter = 1001, jitterDelayUnit = MILLIS, delay = 1, delayUnit = SECONDS)
    public void badMethod() {}

}
