package com.ibm.websphere.microprofile.faulttolerance_fat.validation;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

@ApplicationScoped
public class FallbackMethodWrongParameters {

    @Fallback(fallbackMethod = "fallbackMethod")
    public void badMethod(String a) {}

    public void fallbackMethod(int b) {}

}
