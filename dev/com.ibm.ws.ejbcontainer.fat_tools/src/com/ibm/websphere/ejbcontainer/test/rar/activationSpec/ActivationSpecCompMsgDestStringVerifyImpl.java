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
//                                  setDestination method with String type signature.
//  05/15/06   cjn      313344.1    Fix problems
//
//  --------------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.activationSpec;

/**
 * This class is for verifying if null object or a String
 * is being associated to the instance of ActivationSpecCompMsgDestStringImpl.
 */
// 313344.1 extend with ActivationSpecImpl
public class ActivationSpecCompMsgDestStringVerifyImpl extends ActivationSpecImpl {
    static String destinationStringNullDest = null;
    static String destinationStringValidDest = null;

    /**
     * This method will set the destination object of type String which is referenced globally
     * by the destination property value in the ActivationSpec in resources.xml.
     * The signature of setDestination method in the AS is of type String.
     * 
     * @param String
     */
    public static void setDestinationNullDest(String destString) {
        destinationStringNullDest = destString;
    }

    /**
     * This method will get the destination object of type String which is referenced globally
     * by the destination property value in the ActivationSpec in resources.xml.
     * The signature of setDestination method in the AS is of type String.
     * 
     * @return String
     */
    public static String getDestinationNullDest() {
        return destinationStringNullDest;
    }

    /**
     * This method will set the destination object of type String which is referenced globally
     * by the destinationJndiName in the ActivationSpec in resources.xml.
     * The signature of setDestination method in the AS is of type String.
     * 
     * @param String
     */
    public static void setDestinationValidDest(String destString) {
        destinationStringValidDest = destString;
    }

    /**
     * This method will get the destination object of type String which is referenced globally
     * by the destinationJndiName in the ActivationSpec in resources.xml.
     * The signature of setDestination method in the AS is of type String.
     * 
     * @return String
     */
    public static String getDestinationValidDest() {
        return destinationStringValidDest;
    }
}