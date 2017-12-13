/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2011
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package beanvalidationcdi.test;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ValidatorFactory;

/**
 * Client class that obtains a ValidatorFactory through using the CDI @Inject annotation.
 */
@ApplicationScoped
public class BValAtInject {

    @Inject
    ValidatorFactory injectValidatorFactory;

    /**
     * Test that a ValidatorFactory obtained with @Inject has the custom message
     * interpolator set and that the custom message interpolator was able to inject
     * another bean in this application.
     */
    public void testCDIInjectionInInterpolatorAtInject() throws Exception {
        String interpolatedValue = injectValidatorFactory.getMessageInterpolator().interpolate(null, null);

        if (!interpolatedValue.equals("something")) {
            throw new Exception("custom interpolator should have returned the value " +
                                "'something', but returned : " + interpolatedValue);
        }

    }

}