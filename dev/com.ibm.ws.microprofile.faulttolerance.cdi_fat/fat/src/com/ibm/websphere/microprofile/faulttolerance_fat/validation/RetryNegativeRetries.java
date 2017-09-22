package com.ibm.websphere.microprofile.faulttolerance_fat.validation;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
public class RetryNegativeRetries {

    @Retry(maxRetries = -2)
    public void badMethod() {}

}
