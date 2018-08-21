package com.ibm.microprofile.illegal.optional.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@RequestScoped
public class BeanHolder {
	
    @Inject
    @ConfigProperty(name = "com.ibm.nonexistant.property")
    Optional optionalProperty;
    
    public Optional getProp() {
    	return optionalProperty;
    }
}
