package com.ibm.websphere.microprofile.faulttolerance_fat.validation;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
public class RetryNegativeDelay {

    @Retry(delay = -1)
    public void badMethod() {}

}
