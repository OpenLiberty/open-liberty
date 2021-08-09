/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.activationspec;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;

import javax.jms.Destination;
import javax.resource.ResourceException;
import javax.resource.spi.InvalidPropertyException;

import com.ibm.adapter.ActivationSpecImpl;
import com.ibm.adapter.adminobject.FVTCompMsgDestAOImpl;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * <p>This class implements the ActivationSpec interface. This ActivationSpec implementation
 * class only has 2 attributes, the testVariation and Destination. Destination is a required
 * attribute.</p>
 */
public class ActivationSpecCompMsgDestRequiredImpl extends ActivationSpecImpl {

    /** configured property - test variation number */
    private int testVariation;

    /** Destination instance */
    private Destination destination;

    private static final TraceComponent tc = Tr.register(ActivationSpecCompMsgDestRequiredImpl.class);

    /** class name of the Destination admin object */
    private String classCompMsgDestAO = "fvt.adapter.adminobject.FVTCompMsgDestAOImpl";

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
        System.out.println("MHD: Inside of the ActivationSpecCompMsgDestRequiredImpl.validate() method, with destination of **" + destination + "** and variations of **"
                           + testVariation + "**");
        if (tc.isEntryEnabled())
            Tr.entry(tc, "ASRequired.validate()");

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "testVariation is **" + testVariation + "**, and the destination is **" + destination + "**");
        } //end if

        // Make sure the destination object is of type javax.jms.Destination
        if (destination == null) {
            if (testVariation == 12) {
                if (tc.isDebugEnabled()) {
                    // 06/02/04: 
                    //Tr.debug(tc, "Destination object is null with testVariation = " + testVariation + ". Expected results.");
                    Tr.debug(tc, "testVariation = " + testVariation +
                                 ". AS JNDI is jndi/mdbCompMsgDestRequiredWrongAODestType. Validate() shouldn't be called by J2C.");
                }

                // 05/31/04: 
                // Need to indicate the AS instance is trying to load when CMPEXPEAR is trying
                // to start.
                ActivationSpecCompMsgDestRequiredVerifyImpl.setASRequiredWrongAODestTypeInit(true);
            } else if (testVariation == 13) {
                if (tc.isDebugEnabled()) {
                    // 06/02/04: 
                    //Tr.debug(tc, "Destination object is null with testVariation = " + testVariation + ". Expected results.");
                    Tr.debug(tc, "testVariation = " + testVariation +
                                 ". AS JNDI is jndi/mdbCompMsgDestRequiredNullDest. Validate() shouldn't be called by J2C.");
                }
                // 05/31/04: 
                // Since no destinationJndiName is supplied, validate() shouldn't even get called.
                ActivationSpecCompMsgDestRequiredVerifyImpl.setASRequiredNullDestInit(true);

                // 06/02/04: 
                // Do not throw the exception, else it will be confused that it is J2C not loading the
                // MDB due to null destJndiName.
                //throw new InvalidPropertyException("Missing the destinationJndiName but activationSpec is still loaded by J2C. ERROR!!!");
            } else if (testVariation == 14) {
                if (tc.isDebugEnabled()) {
                    // 06/02/04: 
                    //Tr.debug(tc, "Destination object is null with testVariation = " + testVariation + ". Expected results.");
                    Tr.debug(tc, "testVariation = " + testVariation +
                                 ". AS JNDI is jndi/mdbCompMsgDestRequiredInvalidDest. Validate() shouldn't be called by J2C.");
                }
                // 05/31/04: 
                // Need to indicate the AS instance is trying to load when CMPEXPEAR is trying
                // to start.
                ActivationSpecCompMsgDestRequiredVerifyImpl.setASRequiredInvalidDestInit(true);

                // 06/02/04: 
                // Do not throw the exception, else it will be confused that it is J2C not loading the
                // MDB due to invalid destJndiName.
                //throw new InvalidPropertyException("The destinationJndiName specified is invalid. Throw InvalidPropertyException in validate().");
            } else if (testVariation == 15) {
                //The point of test variation 15 is to verify that we actually call the .validate() method on the J2C-ActivationSpec object, and that we a failure
                //in the .validate() method prevents the Server from sending that J2C-ActivationSpec object into the ResourceAdapter.activateEndpoint() method.
                //
                //The wsadmin command that we used to create the J2C-ActivationSpec instance flagged as variation 15 intentionally omitted the 'destination' property, and
                //we did this on purpose so that it would fail the validation check we are about to do.
                //
                //When we intentionally fail the validation check, an exception should be thrown, which should prevent the Server from feeding this J2C-ActivationSpec
                //into the ResourceAdapter.activateEndpoint() method, which should stop it from getting activated...and we'll be able to verify this via the switch on
                //the static storage object that gets set in this method, and a second switch on the same static storage object which does not get set because we never
                //went through the .activateEndpoint() method due to the exception we are about to throw.
                System.out.println("MHD: Inside ActivationSpecCompMsgDestRequiredImpl.validate() for option 15....");
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "testVariation = " + testVariation + ", so we expect the 'destination' variable to be null, and so we expect to throw an exception here.");
                } //end if

                //Update the static variable to indicate that we actually did call the .validate() method on the J2C-ActivationSpec object.
                //		Our test will check this variable at some point in the future to confirm we actually called the .validate() method.
                ActivationSpecCompMsgDestRequiredVerifyImpl.setASRequiredValidateThrowExInit(true);

                /*
                 * //Throw the error that should ultimately stop this J2C-ActivationSpec from getting fed into the RA.activateEndpoint() method
                 * if(destination == null)
                 * {
                 * throw new InvalidPropertyException("The destination is null.");
                 * }//end if
                 */
            } else if (testVariation == 16) {
                if (tc.isDebugEnabled()) {
                    // 06/02/04: 
                    //Tr.debug(tc, "Destination object is null with testVariation = " + testVariation + ". Expected results.");
                    Tr.debug(tc, "testVariation = " + testVariation +
                                 ". AS JNDI is jndi/mdbCompMsgDestRequiredValidGlobalDest. Destination object should not be null.");

                    throw new InvalidPropertyException("The destinationJndiName specified is valid. Destination object should not be null.");
                }
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Destination object is null with testVariation = " + testVariation + ". Unexpected results.");
                throw new InvalidPropertyException("The destination property should not be null.");
            }
        } else if (!(destination.getClass().getName().equals(classCompMsgDestAO))) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Destination object is not of type " + classCompMsgDestAO);
            // 05/30/04:
            // This shouldn't happen at all since we will not have a test case which
            // assign an object to the destination property.
            throw new InvalidPropertyException("The dest property is not of type " + classCompMsgDestAO);
        } else {
            // This means the destination object is of type Destination.
            // This shouldn't happen on testVariation == 12, 13 and 14.
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Destination object is of type Destination");
            if (testVariation == 12 ||
                testVariation == 13 ||
                testVariation == 14) {
                // 05/26/04: 
                // This means we are either running testMsgDestASRequiredWrongAODestType or
                // testMsgDestASRequiredNullDest test case.
                // So the destination object should be null.
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Destination object is of type Destination with testVariation = " + testVariation + ". Unexpected results.");
                    throw new InvalidPropertyException("Destination object should be null.");
                }
            }
            // 06/02/04: 
            else if (testVariation == 15) {

                System.out.println("Inside ActivationSpecCompMsgDestREquiredImpl.validate() for variation 15..");
                if (tc.isDebugEnabled()) {
                    // 06/02/04: 
                    Tr.debug(tc, "Destination object is of type Destination with testVariation = " + testVariation + ". Expected results.");
                    Tr.debug(tc, "AS JNDI is jndi/mdbCompMsgDestRequiredValidateThrowEx. About to throw exception at Validate().");
                }

                //Set the global static variable to indicate that the the .validate() method was infact called.
                System.out.println("Setting the 'called validate' switch to true...");
                ActivationSpecCompMsgDestRequiredVerifyImpl.setASRequiredValidateThrowExInit(true);

                //Set the global static variable to indicate to prove that the provider was correctly set into this
                //J2C-ActivationSpec object based on the fact that it was called out in the 'create' command that
                //instantiated the J2C-ActivationSpec object.
                System.out.println("Setting the 'provider property' switch...");
                FVTCompMsgDestAOImpl complexMessageProvider = (FVTCompMsgDestAOImpl) destination;
                ActivationSpecCompMsgDestRequiredVerifyImpl.setVerifyStringGlobal(complexMessageProvider.getVerifyString());

                // 06/02/04: 
                // Throw exception in validate() method. Set 2 properties to the PropertyDescriptor array first.
                PropertyDescriptor propDesc[] = new PropertyDescriptor[2];

                if (tc.isDebugEnabled()) {
                    // 06/02/04: 
                    Tr.debug(tc, "Create PropertyDescriptors.");
                }

                try {
                    if (tc.isDebugEnabled()) {
                        // 06/02/04: 
                        Tr.debug(tc, "Create PropertyDescriptor #1.");
                    }
                    propDesc[0] = new PropertyDescriptor("destination", this.getClass());
                    if (tc.isDebugEnabled()) {
                        // 06/02/04: 
                        Tr.debug(tc, "Create PropertyDescriptor #2.");
                    }
                    propDesc[1] = new PropertyDescriptor("name", this.getClass());
                } catch (IntrospectionException ie) {
                    if (tc.isDebugEnabled()) {
                        // 06/02/04: 
                        Tr.debug(tc, "Caught exception when PropertyDescriptors are being created.");
                    }
                    throw new InvalidPropertyException(ie.getMessage());
                }

                InvalidPropertyException ipe = new InvalidPropertyException("Throw InvalidPropertyException in validate() for mdbCompMsgDestRequiredValidateThrowEx");

                if (tc.isDebugEnabled()) {
                    // 06/02/04: 
                    Tr.debug(tc, "Set PropertyDescriptors to the InvalidPropertyException.");
                }

                ipe.setInvalidPropertyDescriptors(propDesc);

                // 313344.1 start Put message in system log that it is ok to see this exception
                System.out.println("!!!WARNING: It is expected to see exception J2CA0137E and J2CA0089E and WSVR0062E in the log shortly after seeing this message.");
                System.out.println("!!!Do not investigate, these exceptions are being generated by the test case in preparation for a later test.");
                // 313344.1 end

                throw ipe;
            } else if (testVariation == 16) {
                if (tc.isDebugEnabled()) {
                    // 06/02/04: 
                    // This is the valid destination jndi name with required dest property
                    Tr.debug(tc, "Destination object is of type Destination with testVariation = " + testVariation + ". Expected results.");
                    Tr.debug(tc, "AS JNDI is jndi/mdbCompMsgDestRequiredValidGlobal.");
                }
            } else {
                Tr.debug(tc, "Should not come to here. ERROR!!!");
                throw new InvalidPropertyException("Should not come to here. ERROR!!!");
            }

        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "ActivationSpecCompMsgDestRequiredImpl.validate()");
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
        if (tc.isEntryEnabled())
            Tr.entry(tc, "ASRequired.getDestination");
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
        if (tc.isEntryEnabled())
            Tr.entry(tc, "ASRequired.setDestination", new Object[] { destination });
        // 05/18/04: 
        // May need to add more code to add the verification mechanism
        this.destination = destination;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "ASRequired.setDestination");
    }

}
