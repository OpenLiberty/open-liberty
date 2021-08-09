package com.ibm.websphere.microprofile.faulttolerance_fat.validation;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
public class RetryDelayLongerThanDuration {

    @Retry(delay = 1, delayUnit = MINUTES, maxDuration = 30, durationUnit = SECONDS)
    public void badMethod() {}

}
