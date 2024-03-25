/**
 *
 */
package com.ibm.ws.jaxws.beanvalidation;

import static org.junit.Assert.assertNotNull;

import com.ibm.ws.jaxws.cdi.beanvalidation.stubs.BeanValidationWebService;
import com.ibm.ws.jaxws.cdi.beanvalidation.stubs.OneWayWithValidation;

import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

/**
 * Basic Abstract Class that holds all the methods for testing which can be
 * repeated by the extending test class.
 * The extending test need to set the LibertyServer instance for the asserts, and pass the specific proxy that the test is calling.
 */
public abstract class AbstractBeanValidationTest {

    private static LibertyServer server;
    private static boolean isCDIEnabled = false;

    public static void setLibertyServer(LibertyServer testServer) {
        server = testServer;
    }

    // Because CDI's integration with Bean Validation slightly
    // Changes the Validation failure message, we need to
    // change asserts for a handfull of tests
    public static void enableCDI() {
        isCDIEnabled = true;
    }

    /*
     * Tests basic validation passes with the String being set to test
     */
    public void testOneWayRequestWithBeanValidation(BeanValidationWebService proxy) throws Exception {

        proxy.oneWayWithValidation("test");

        assertNotNull("testOneWayRequestWithBeanValidation did not pass validation.",
                      server.waitForStringInLog("Validation passed validation"));

    }

    /*
     * Tests basic validation fails with the String being set to null
     */
    public void testOneWayRequestWithBeanValidationWithNullFailure(BeanValidationWebService proxy) throws Exception {

        proxy.oneWayWithValidation(null);
        if (isCDIEnabled == true && !JakartaEEAction.isEE9OrLaterActive()) { // Messages also change depending of if its CDI and EE8 features

            assertNotNull("testOneWayRequestWithBeanValidationWithNullFailure didn't find failure message in logs.",
                          server.waitForStringInLog("validated object cannot be null"));
        } else {

            assertNotNull("testOneWayRequestWithBeanValidationWithNullFailure didn't find failure message in logs.",
                          server.waitForStringInLog("The object to be validated must not be null."));
        }

    }

    /*
     * Tests basic validation passes with bean validation set on the method parameters
     * Always passes since code does nothing.
     * TODO: implement support for validating request method parameters
     */
    public void testOneWayRequestWithBeanValidationOnMethodParameters(BeanValidationWebService proxy) throws Exception {

        proxy.oneWayWithMethodValidation("test");

        assertNotNull("testOneWayRequestWithBeanValidation did not pass validation.",
                      server.waitForStringInLog("Validation passed method validation"));

    }

    /*
     * Tests basic validation passes with bean validation set on the method parameters
     * Always passes since code does nothing.
     * TODO: implement support for validating request method parameters
     */
    public void testOneWayRequestWithBeanValidationOnMethodParametersNullFailure(BeanValidationWebService proxy) throws Exception {

        proxy.oneWayWithMethodValidation(null);

        //TODO: Make this a negative test once Method Parameter validation is enabled.
        assertNotNull("testOneWayRequestWithBeanValidation did not pass validation.",
                      server.waitForStringInLog("Validation passed method validation"));

    }

    /*
     * This tests ensures validation will pass when both arg0, and arg1 are valid
     *
     * @Size(min = 4, max = 10)
     *
     */
    public void testOneWayRequestWithBeanValidationJAXB(BeanValidationWebService proxy) throws Exception {

        OneWayWithValidation onwv = new OneWayWithValidation();
        onwv.setArg0("test");
        onwv.setArg1("test");

        proxy.oneWayWithJAXBAnnotatedValidation(onwv);
        assertNotNull("testOneWayRequestWithBeanValidationMaxFailure did not fail vaildation",
                      server.waitForStringInLog("Validation passed validation"));

    }

    /*
     * This tests ensures validation will fail with the arg0 parameter passed from the
     * request parameter, OneWayWithValidation, is set to nul
     *
     * @Size(min = 4, max = 10)
     *
     */
    public void testOneWayRequestWithBeanValidationJAXBNullArg0NullFailure(BeanValidationWebService proxy) throws Exception {
        OneWayWithValidation onwv = new OneWayWithValidation();
        onwv.setArg1("test");

        proxy.oneWayWithJAXBAnnotatedValidation(onwv);
        assertNotNull("testOneWayRequestWithBeanValidationMaxFailure did not fail vaildation",
                      server.waitForStringInLog("arg0 cannot be null"));

    }

    /*
     * This tests ensures validation will pass with the arg1 parameter passed from the
     * request parameter, OneWayWithValidation, set to a valid size (4).
     *
     * @Size(min = 4, max = 10)
     * String arg1
     *
     */
    public void testOneWayRequestWithBeanValidationJAXBArg1(BeanValidationWebService proxy) throws Exception {

        OneWayWithValidation onwv = new OneWayWithValidation();
        onwv.setArg0("test");
        onwv.setArg1("test");

        proxy.oneWayWithJAXBAnnotatedValidation(onwv);
        assertNotNull("testOneWayRequestWithBeanValidationMaxFailure did not fail vaildation",
                      server.waitForStringInLog("Validation passed validation "));

    }

    /*
     * This tests ensures validation will fail with the arg1 parameter passed from the
     * request parameter, OneWayWithValidation, fails when the min is set on the @Size annotation (3).
     *
     * @Size(min = 4, max = 10)
     * String arg1
     *
     */
    public void testOneWayRequestWithBeanValidationJAXBArg1MinFailure(BeanValidationWebService proxy) throws Exception {

        OneWayWithValidation onwv = new OneWayWithValidation();
        onwv.setArg0("test");
        onwv.setArg1("tes");

        proxy.oneWayWithJAXBAnnotatedValidation(onwv);
        assertNotNull("testOneWayRequestWithBeanValidationMaxFailure did not fail vaildation",
                      server.waitForStringInLog("arg1 cannot less that 4 or greater than 10 in length"));

    }

    /*
     * This tests ensures validation will fail with the Sarg1 parameter passed from the
     * request parameter, OneWayWithValidation, when the max is set on the @Size annotation.
     *
     * @Size(min = 4, max = 10)
     * String arg1
     */
    public void testOneWayRequestWithBeanValidationJAXBArg1MaxFailure(BeanValidationWebService proxy) throws Exception {

        OneWayWithValidation onwv = new OneWayWithValidation();
        onwv.setArg0("test");
        onwv.setArg1("testOneWayRequestWithBeanValidationJAXBArg1MaxFailure");

        proxy.oneWayWithJAXBAnnotatedValidation(onwv);
        assertNotNull("testOneWayRequestWithBeanValidationMaxFailure did not fail vaildation",
                      server.waitForStringInLog("arg1 cannot less that 4 or greater than 10 in length"));

    }

}
