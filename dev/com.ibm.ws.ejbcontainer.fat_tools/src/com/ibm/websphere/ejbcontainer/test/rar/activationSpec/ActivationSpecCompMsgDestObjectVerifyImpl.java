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
//  Date       pgmr     reason      Description
//  --------   ------   ------      ---------------------------------
//  05/27/04   swai     LIDB2110-67 Create verification class for AS with 
//                                  setDestination method with Object type signature.
//  05/15/06   cjn      313344.1    Fix problems
//
//  --------------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.activationSpec;

import javax.jms.Destination;

/**
 * This class is for verifying if null complex message destination object
 * is being associated to the instance of ActivationSpecCompMsgDestObjectImpl.
 */
// 313344.1 extend with ActivationSpecImpl
public class ActivationSpecCompMsgDestObjectVerifyImpl extends ActivationSpecImpl {
    static Destination destinationValidDestWrongASDestType = null;

    /**
     * This method will set the destination object from which the complex message
     * destination object is reference globally with valid destinationJndiName specified
     * in the ActivationSpec is in resources.xml but the signature of setDestination method
     * in the AS is of type Object.
     * 
     * @param Destination
     */
    public static void setDestinationWrongASDestType(Destination dest) {
        destinationValidDestWrongASDestType = dest;
    }

    /**
     * This method will get the destination object from which the complex message
     * destination object is reference globally with valid destinationJndiName specified
     * in the ActivationSpec is in resources.xml but the signature of setDestination method
     * in the AS is of type Object.
     * 
     * @return Destination
     */
    public static Destination getDestinationWrongASDestType() {
        return destinationValidDestWrongASDestType;
    }
}