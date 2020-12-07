// IBM Confidential
//
// OCO Source Materials
//
// Copyright IBM Corp. 2013
//
// The source code for this program is not published or otherwise divested 
// of its trade secrets, irrespective of what has been deposited with the 
// U.S. Copyright Office.
//
// Change Log:
//  Date        pgmr        reason      Description
//  --------    -------     ------      ---------------------------------
//  05/27/04    swai        LIDB2110-67 create ActivationSpec for M8_ComplexMessage
//                                      Destination Processing test cases.
//                                      The setDestination takes Object as
//                                      the signature type.
//  --------------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.activationSpec;

import java.util.logging.Logger;

import javax.jms.Destination;
import javax.resource.ResourceException;
import javax.resource.spi.InvalidPropertyException;

/**
 * <p>This class implements the ActivationSpec interface. This ActivationSpec implementation
 * class only has 2 attributes, the testVariation and Destination.</p>
 */

public class ActivationSpecCompMsgDestObjectImpl extends ActivationSpecImpl {
    private final static String CLASSNAME = ActivationSpecCompMsgDestObjectImpl.class.getName();
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
        svLogger.entering(CLASSNAME, "ASObject.validate()");
        svLogger.info("testVariation is " + testVariation);

        // Make sure the destination object is null
        if (destination == null)
        {
            svLogger.info("Destination object is null");
            if (testVariation == 7)
            {
                // 05/26/04: swai
                // This means we are running testCompMsgDestWrongASDestType 
                // test case. The destinationJndiName is set to a correct 
                // admin object - "CompMsgDestAOGlobal" but the
                // setDestination signature is not using Destination as the
                // object type.
                // So the destination object should be null.
                svLogger.info("Destination object is null with testVariation = " + testVariation + ". Expected results.");
            }
            else
            {
                svLogger.info("Destination object is null with testVariation = " + testVariation + ". Unexpected results.");
                throw new InvalidPropertyException("The destination property should not be null.");
            }
        }
        else
        {
            svLogger.info("Destination object is not null");
            // swai: 05/27/04:
            // In this ActivationSpec, the Destination object has to be null, as
            // the Signature of setDestination is not of type Destination.
            throw new InvalidPropertyException("The dest property is not null");
        }
        svLogger.exiting(CLASSNAME, "ActivationSpecCompMsgDestObjectImpl.validate()");
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
    public Destination getDestination()
    {
        svLogger.entering(CLASSNAME, "ASObject.getDestination");
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
    public void setDestination(Object destination) throws ResourceException
    {
        svLogger.entering(CLASSNAME, "ASObject.setDestination", new Object[] { destination });
        // 05/18/04: swai
        // May need to add more code to add the verification mechanism
        this.destination = (Destination) destination;
        svLogger.exiting(CLASSNAME, "ASObject.setDestination");
    }
}