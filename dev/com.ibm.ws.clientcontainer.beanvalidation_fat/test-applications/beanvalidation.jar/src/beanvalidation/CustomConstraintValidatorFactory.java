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
package beanvalidation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;

/**
 * Simple implementation of a ConstraintValidatorFactory that tolerates a null
 * parameter for testing purposes.
 */
public class CustomConstraintValidatorFactory implements ConstraintValidatorFactory {

    @Override
    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> arg0) {
        if (arg0 != null) {
            try {
                return arg0.newInstance();
            } catch (IllegalAccessException e) {

            } catch (InstantiationException e) {

            }
        }
        return null;
    }

    @Override
    public void releaseInstance(ConstraintValidator<?, ?> arg0) {}

}
