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
package apachebvalconfig.test;

import static org.junit.Assert.assertEquals;

import javax.validation.MessageInterpolator;
import javax.validation.ValidatorFactory;

public class BeanValidation {

    /**
     * Test that this web module that has a validaion.xml specified with all
     * classes available in the apache bval 1.1 implementation can properly load the
     * classes and build the validator factory accordingly.
     */
    public void testBuildApacheConfiguredValidatorFactory(ValidatorFactory validatorFactory) throws Exception {
        if (validatorFactory == null) {
            throw new IllegalStateException("Injection of ValidatorFactory never occurred.");
        }

        MessageInterpolator mi = validatorFactory.getMessageInterpolator();
        String expectedInterpolator = "org.apache.bval.jsr.DefaultMessageInterpolator";
        String actualInterpolator = mi.getClass().getName();
        assertEquals("the correct message interpolator wasn't provided: " + actualInterpolator,
                     expectedInterpolator, actualInterpolator);
    }

    /**
     * Ensure that the apache bval impl classes have the proper/expected visibility
     * to the application.
     */
    public void testApacheBvalImplClassVisibility() throws Exception {
        String providerImpl = "org.apache.bval.jsr.ApacheValidationProvider";
        try {
            Class.forName(providerImpl);
            throw new Exception("application should not be able to load provider class with default classloader");
        } catch (ClassNotFoundException e) {
            // expected
        }

        // Thread context classloader should, however have visibility
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Class.forName(providerImpl, false, tccl);
    }
}
