/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package beanvalidationcdi.validation;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ValidationException;

import beanvalidationcdi.beans.TestBean;
import beanvalidationcdi.client.BeanValidationClient;

/**
 * Simple implementation of a ConstraintValidatorFactory that tolerates a null
 * parameter for testing purposes and also checks to see if a CDI bean was
 * injected.
 */
public class CustomConstraintValidatorFactory implements ConstraintValidatorFactory {

    @Inject
    public TestBean bean;
    
    @Override
    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> arg0) {
        if (bean == null) {
            throw new ValidationException("bean shouldn't be null");
        }

        try {
            if (arg0 != null) {
                return arg0.newInstance();
            }
        } catch (Throwable e) {
            throw new ValidationException("couldn't create new instance of " + arg0.getName(), e);
        }

        return null;
    }

    @Override
    public void releaseInstance(ConstraintValidator<?, ?> arg0) {

    }

    @PreDestroy
    public void preDestroy() {
        System.out.println(CustomConstraintValidatorFactory.class.getSimpleName() + " is getting destroyed.");
    }

}
