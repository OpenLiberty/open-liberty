/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.activationSpec;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.logging.Logger;

import javax.jms.Destination;
import javax.resource.ResourceException;
import javax.resource.spi.InvalidPropertyException;

/**
 * <p>This class implements the ActivationSpec interface. This ActivationSpec implementation
 * class only has 2 attributes, the testVariation and Destination. Destination is a required
 * attribute.</p>
 */
public class ActivationSpecCompMsgDestRequiredImpl extends ActivationSpecImpl {
    private final static String CLASSNAME = ActivationSpecCompMsgDestRequiredImpl.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** configured property - test variation number */
    private int testVariation;

    /** Destination instance */
    private Destination destination;

    /** class name of the Destination admin object */
    private final String classCompMsgDestAO = "fvt.adapter.adminobject.FVTCompMsgDestAOImpl";

    /**
     * This method may be called by a deployment tool to validate the overall activation configuration
     * information provided by the endpoint deployer. This helps to catch activation configuration
     * errors earlier on without having to wait until endpoint activation time for configuration
     * validation. The implementation of this self-validation check behavior is optional.
     *
     * This method will be called after setDestination() is called if setDestination doesn't
     * throw any exception.
     *
     * If validate() doesn't throw InvalidPropertyException, then endpointActivcation will
     * be called by J2C.
     */
    @Override
    public void validate() throws InvalidPropertyException {
        svLogger.entering(CLASSNAME, "ASRequired.validate()");
        svLogger.info("testVariation is **" + testVariation + "**, and the destination is **" + destination + "**");

        // Make sure the destination object is of type javax.jms.Destination
        if (destination == null) {
            if (testVariation == 12) {
                svLogger.info("testVariation = " + testVariation + ". AS JNDI is jndi/mdbCompMsgDestRequiredWrongAODestType. Validate() shouldn't be called by J2C.");
                // Need to indicate the AS instance is trying to load when CMPEXPEAR is trying
                // to start.
                ActivationSpecCompMsgDestRequiredVerifyImpl.setASRequiredWrongAODestTypeInit(true);
            } else if (testVariation == 13) {
                svLogger.info("testVariation = " + testVariation + ". AS JNDI is jndi/mdbCompMsgDestRequiredNullDest. Validate() shouldn't be called by J2C.");
                // Since no destinationJndiName is supplied, validate() shouldn't even get called.
                ActivationSpecCompMsgDestRequiredVerifyImpl.setASRequiredNullDestInit(true);

                // Do not throw the exception, else it will be confused that it is J2C not loading the
                // MDB due to null destJndiName.
                // throw new InvalidPropertyException("Missing the destinationJndiName but activationSpec is still loaded by J2C. ERROR!!!");
            } else if (testVariation == 14) {
                svLogger.info("testVariation = " + testVariation + ". AS JNDI is jndi/mdbCompMsgDestRequiredInvalidDest. Validate() shouldn't be called by J2C.");
                // Need to indicate the AS instance is trying to load when CMPEXPEAR is trying
                // to start.
                ActivationSpecCompMsgDestRequiredVerifyImpl.setASRequiredInvalidDestInit(true);

                // Do not throw the exception, else it will be confused that it is J2C not loading the
                // MDB due to invalid destJndiName.
                // throw new InvalidPropertyException("The destinationJndiName specified is invalid. Throw InvalidPropertyException in validate().");
            } else if (testVariation == 15) {
                // The point of test variation 15 is to verify that we actually call the .validate() method on the J2C-ActivationSpec object, and that we a failure
                // in the .validate() method prevents the Server from sending that J2C-ActivationSpec object into the ResourceAdapter.activateEndpoint() method.
                //
                // The wsadmin command that we used to create the J2C-ActivationSpec instance flagged as variation 15 intentionally omitted the 'destination' property, and
                // we did this on purpose so that it would fail the validation check we are about to do.
                //
                // When we intentionally fail the validation check, an exception should be thrown, which should prevent the Server from feeding this J2C-ActivationSpec
                // into the ResourceAdapter.activateEndpoint() method, which should stop it from getting activated...and we'll be able to verify this via the switch on
                // the static storage object that gets set in this method, and a second switch on the same static storage object which does not get set because we never
                // went through the .activateEndpoint() method due to the exception we are about to throw.
                svLogger.info("testVariation = " + testVariation + ", so we expect the 'destination' variable to be null, and so we expect to throw an exception here.");

                // Update the static variable to indicate that we actually did call the .validate() method on the J2C-ActivationSpec object.
                // Our test will check this variable at some point in the future to confirm we actually called the .validate() method.
                ActivationSpecCompMsgDestRequiredVerifyImpl.setASRequiredValidateThrowExInit(true);
            } else if (testVariation == 16) {
                svLogger.info("testVariation = " + testVariation + ". AS JNDI is jndi/mdbCompMsgDestRequiredValidGlobalDest. Destination object should not be null.");
                throw new InvalidPropertyException("The destinationJndiName specified is valid. Destination object should not be null.");
            } else {
                svLogger.info("Destination object is null with testVariation = " + testVariation + ". Unexpected results.");
                throw new InvalidPropertyException("The destination property should not be null.");
            }
        } else if (!(destination.getClass().getName().equals(classCompMsgDestAO))) {
            svLogger.info("Destination object is not of type " + classCompMsgDestAO);
            // This shouldn't happen at all since we will not have a test case which
            // assign an object to the destination property.
            throw new InvalidPropertyException("The dest property is not of type " + classCompMsgDestAO);
        } else {
            // This means the destination object is of type Destination.
            // This shouldn't happen on testVariation == 12, 13 and 14.
            svLogger.info("Destination object is of type Destination");
            if (testVariation == 12 || testVariation == 13 || testVariation == 14) {
                // This means we are either running testMsgDestASRequiredWrongAODestType or
                // testMsgDestASRequiredNullDest test case.
                // So the destination object should be null.
                svLogger.info("Destination object is of type Destination with testVariation = " + testVariation + ". Unexpected results.");
                throw new InvalidPropertyException("Destination object should be null.");
            } else if (testVariation == 15) {
                svLogger.info("Inside ActivationSpecCompMsgDestREquiredImpl.validate() for variation 15..");
                svLogger.info("Destination object is of type Destination with testVariation = " + testVariation + ". Expected results.");
                svLogger.info("AS JNDI is jndi/mdbCompMsgDestRequiredValidateThrowEx. About to throw exception at Validate().");

                // Set the global static variable to indicate that the the .validate() method was infact called.
                svLogger.info("Setting the 'called validate' switch to true...");
                ActivationSpecCompMsgDestRequiredVerifyImpl.setASRequiredValidateThrowExInit(true);

                // Set the global static variable to indicate to prove that the provider was correctly set into this
                // J2C-ActivationSpec object based on the fact that it was called out in the 'create' command that
                // instantiated the J2C-ActivationSpec object.
                svLogger.info("Setting the 'provider property' switch...");
                FVTCompMsgDestAOImpl complexMessageProvider = (FVTCompMsgDestAOImpl) destination;
                ActivationSpecCompMsgDestRequiredVerifyImpl.setVerifyStringGlobal(complexMessageProvider.getVerifyString());

                // Throw exception in validate() method. Set 2 properties to the PropertyDescriptor array first.
                PropertyDescriptor propDesc[] = new PropertyDescriptor[2];
                svLogger.info("Create PropertyDescriptors.");

                try {
                    svLogger.info("Create PropertyDescriptor #1.");
                    propDesc[0] = new PropertyDescriptor("destination", this.getClass());

                    svLogger.info("Create PropertyDescriptor #2.");
                    propDesc[1] = new PropertyDescriptor("name", this.getClass());
                } catch (IntrospectionException ie) {
                    svLogger.info("Caught exception when PropertyDescriptors are being created.");
                    throw new InvalidPropertyException(ie.getMessage());
                }

                InvalidPropertyException ipe = new InvalidPropertyException("Throw InvalidPropertyException in validate() for mdbCompMsgDestRequiredValidateThrowEx");
                svLogger.info("Set PropertyDescriptors to the InvalidPropertyException.");
                ipe.setInvalidPropertyDescriptors(propDesc);

                // 313344.1 start Put message in system log that it is ok to see this exception
                svLogger.info("!!!WARNING: It is expected to see exception J2CA0137E and J2CA0089E and WSVR0062E in the log shortly after seeing this message.");
                svLogger.info("!!!Do not investigate, these exceptions are being generated by the test case in preparation for a later test.");
                // 313344.1 end

                throw ipe;
            } else if (testVariation == 16) {
                // This is the valid destination jndi name with required dest property
                svLogger.info("Destination object is of type Destination with testVariation = " + testVariation + ". Expected results.");
                svLogger.info("AS JNDI is jndi/mdbCompMsgDestRequiredValidGlobal.");
            } else {
                svLogger.info("Should not come to here. ERROR!!!");
                throw new InvalidPropertyException("Should not come to here. ERROR!!!");
            }
        }
        svLogger.exiting(CLASSNAME, "ActivationSpecCompMsgDestRequiredImpl.validate()");
    }

    /**
     * Sets the testVariation
     *
     * @param var The test variation this spec will be used for
     * @return
     */
    public void setTestVariation(int testVar) {
        this.testVariation = testVar;
    }

    /**
     * Returns the test variation
     *
     * @return int
     */
    public int getTestVariation() {
        return testVariation;
    }

    /**
     * Get the associated Destination object (should be FVTComplexMsgDest object)
     *
     * @return destination the destination instance
     *
     */
    public Destination getDestination() {
        svLogger.entering(CLASSNAME, "ASRequired.getDestination");
        return destination;
    }

    /**
     * Set the destination
     *
     * @param object the destination instance
     *
     * @exception ResourceException
     *                ResourceExeception - generic exception.
     *                ResourceAdapterInternalException - resource adapter related error condition.
     */
    public void setDestination(Destination destination) throws ResourceException {
        svLogger.entering(CLASSNAME, "ASRequired.setDestination", new Object[] { destination });
        // May need to add more code to add the verification mechanism
        this.destination = destination;
        svLogger.exiting(CLASSNAME, "ASRequired.setDestination");
    }
}