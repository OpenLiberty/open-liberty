package com.ibm.websphere.microprofile.faulttolerance_fat.validation;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

@ApplicationScoped
public class FallbackMethodNotExist {

    @Fallback(fallbackMethod = "noSuchMethod")
    public void badMethod() {}

}
