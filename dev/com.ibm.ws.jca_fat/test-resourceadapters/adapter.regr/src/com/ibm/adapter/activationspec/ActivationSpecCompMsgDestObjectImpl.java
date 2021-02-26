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

import javax.jms.Destination;
import javax.resource.ResourceException;
import javax.resource.spi.InvalidPropertyException;

import com.ibm.adapter.ActivationSpecImpl;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * <p>
 * This class implements the ActivationSpec interface. This ActivationSpec
 * implementation class only has 2 attributes, the testVariation and
 * Destination.
 * </p>
 */

public class ActivationSpecCompMsgDestObjectImpl extends ActivationSpecImpl {

    /** configured property - test variation number */
    private int testVariation;

    /** Destination instance */
    private Destination destination;

    private static final TraceComponent tc = Tr
                    .register(ActivationSpecCompMsgDestObjectImpl.class);

    /** class name of the Destination admin object */
    private final String classCompMsgDestAO = "fvt.adapter.adminobject.FVTCompMsgDestAOImpl";

    /**
     * This method may be called by a deployment tool to validate the overall
     * activation configuration information provided by the endpoint deployer.
     * This helps to catch activation configuration errors earlier on without
     * having to wait until endpoint activation time for configuration
     * validation. The implementation of this self-validation check behavior is
     * optional.
     *
     * This method will be called after setDestination() is called if
     * setDestination doesn't throw any exception.
     *
     * If validate() doesn't throw InvalidPropertyException, then
     * endpointActivcation will be called by J2C.
     */
    @Override
    public void validate() throws InvalidPropertyException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "ASObject.validate()");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "testVariation is " + testVariation);

        // Make sure the destination object is null

        if (destination == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Destination object is null");
            if (testVariation == 7) {
                // 05/26/04: 
                // This means we are running testCompMsgDestWrongASDestType
                // test case. The destinationJndiName is set to a correct
                // admin object - "CompMsgDestAOGlobal" but the
                // setDestination signature is not using Destination as the
                // object type.
                // So the destination object should be null.
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc,
                             "Destination object is null with testVariation = "
                                 + testVariation + ". Expected results.");
                }
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc,
                             "Destination object is null with testVariation = "
                                 + testVariation + ". Unexpected results.");
                throw new InvalidPropertyException("The destination property should not be null.");
            }
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Destination object is not null");
            // 05/27/04:
            // In this ActivationSpec, the Destination object has to be null, as
            // the Signature of setDestination is not of type Destination.
            throw new InvalidPropertyException("The dest property is not null");
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "ActivationSpecCompMsgDestObjectImpl.validate()");
    }

    /**
     * Sets the testVariation
     *
     * @param var
     *            The test variation this spec will be used for
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
     * Get the associated Destination object (should be FVTComplexMsgDest
     * object)
     *
     * @return destination the destination instance
     *
     */
    public Destination getDestination() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "ASObject.getDestination");
        return destination;
    }

    /**
     * Set the destination
     *
     * @param object
     *            the destination instance
     *
     * @exception ResourceException
     *                ResourceExeception - generic exception.
     *                ResourceAdapterInternalException - resource adapter
     *                related error condition.
     */
    public void setDestination(Object destination) throws ResourceException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "ASObject.setDestination",
                     new Object[] { destination });
        // 05/18/04: 
        // May need to add more code to add the verification mechanism
        this.destination = (Destination) destination;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "ASObject.setDestination");
    }
}
