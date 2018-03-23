package com.ibm.ws.cdi.test.dependentscopedproducer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Stereotype;

@Stereotype
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApplicationScoped
public @interface AppScopedSterotype {

}
