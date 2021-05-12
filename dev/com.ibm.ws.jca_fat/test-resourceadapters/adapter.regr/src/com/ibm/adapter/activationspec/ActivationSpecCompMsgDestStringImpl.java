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

import javax.resource.ResourceException;
import javax.resource.spi.InvalidPropertyException;

import com.ibm.adapter.ActivationSpecImpl;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * <p>This class implements the ActivationSpec interface. This ActivationSpec implementation
 * class only has 2 attributes, the testVariation and Destination.</p>
 */

public class ActivationSpecCompMsgDestStringImpl extends ActivationSpecImpl {

    /** configured property - test variation number */
    private int testVariation;

    /** Destination instance of type String */
    private String destination;

    private static final TraceComponent tc = Tr.register(ActivationSpecCompMsgDestObjectImpl.class);

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
        if (tc.isEntryEnabled())
            Tr.entry(tc, "ASString.validate()");

        if (tc.isDebugEnabled())
            Tr.debug(tc, "testVariation is " + testVariation);

        // Make sure the destination object is of type javax.jms.Destination

        if (destination == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Destination object is null");
            // 05/18/04:
            // May need to add more code to add the verification mechanism
            if (testVariation == 9) {
                // 05/26/04: 
                // This means we are either running testMsgDestStringASDestTypeValidDestJndi
                // test case. The destinationJndiName is set to a correct
                // admin object - "CompMsgDestAOGlobal". Since this AS expects
                // destination is of type String, there will be a type mismatch
                // when setDestination is called.
                // So the destination object should be null.
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Destination object is null with testVariation = " + testVariation + ". Expected results.");
                }
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Destination object is null with testVariation = " + testVariation + ". Unexpected results.");
                throw new InvalidPropertyException("The destination property should not be null.");
            }
        } else if (!(destination.getClass().getName().equals("java.lang.String"))) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Destination object is not of type String");
            // 05/18/04:
            // May need to add more code to add the verification mechanism
            throw new InvalidPropertyException("The dest property is not of type String");
        } else {
            // This means the destination object is of type String.
            // This should happen on testVariation == 8 only.
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Destination object is of type String");
            if (testVariation == 8) {
                // 05/26/04: 
                // This means we are either running testMsgDestStringASDestTypeNullDestJndi
                // test case. The destinationJndiName is not set but the value of
                // destination property is set to a String value - "destinationString".
                // So the destination object should be of type String and has value
                // destinationString.
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Destination object is of type String with testVariation = " + testVariation + ". Expected results.");
                }
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Destination object is of type String with testVariation = " + testVariation + ". Unexpected results.");
                throw new InvalidPropertyException("Destination object should not be of type String.");
            }

        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "ActivationSpecCompMsgDestStringImpl.validate()");
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
    public String getDestination() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "ASString.getDestination");
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
    public void setDestination(String destination) throws ResourceException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "ASString.setDestination", new Object[] { destination });
        // 05/18/04: 
        // May need to add more code to add the verification mechanism
        this.destination = destination;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "ASString.setDestination");
    }
}
