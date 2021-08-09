package com.ibm.websphere.microprofile.faulttolerance_fat.validation;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Timeout;

@ApplicationScoped
public class TimeoutNegative {

    @Timeout(-1)
    public void badMethod() {}

}
