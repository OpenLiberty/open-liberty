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

import javax.jms.Destination;

/**
 * This class is for verifying if the correct complex message destination object
 * is being referenced by the destinationJndiName specified in the instance of
 * ActivationSpecCompMsgDestValidImpl.
 */
//313344.1 extend with ActivationSpecImpl
public class ActivationSpecCompMsgDestValidVerifyImpl extends ActivationSpecImpl {
    static String verifyStringLocal = null;
    static String verifyStringGlobal = null;
    static Destination destinationInvalidDest = null;
    static Destination destinationNullDest = null;
    static Destination destinationValidDestWrongAODestType = null;
    static Destination destinationBlankDest = null;

    /**
     * This method will set the verifyString value from which the complex message
     * destination object is reference globally (destinationJndiName specified
     * in the ActivationSpec is in resources.xml)
     *
     * @param string
     */
    public static void setVerifyStringGlobal(String verifyString) {
        verifyStringGlobal = verifyString;
    }

    /**
     * This method will get the verifyString value from which the complex message
     * destination object is reference globally (destinationJndiName specified
     * in the ActivationSpec is in resources.xml)
     *
     * @return string
     */
    public static String getVerifyStringGlobal() {
        return verifyStringGlobal;
    }

    /**
     * This method will set the verifyString value from which the complex message
     * destination object is reference locally (destinationJndiName specified
     * in the ActivationSpec is in ejb-jar.xml)
     *
     * @param string
     */
    public static void setVerifyStringLocal(String verifyString) {
        verifyStringLocal = verifyString;
    }

    /**
     * This method will get the verifyString value from which the complex message
     * destination object is reference locally (destinationJndiName specified
     * in the ActivationSpec is in ejb-jar.xml)
     *
     * @return string
     */
    public static String getVerifyStringLocal() {
        return verifyStringLocal;
    }

    /**
     * This method will set the destination object from which the complex message
     * destination object is reference globally with non-exist destinationJndiName specified
     * in the ActivationSpec is in resources.xml
     *
     * @param Destination
     */
    public static void setDestinationInvalidDest(Destination dest) {
        destinationInvalidDest = dest;
    }

    /**
     * This method will get the destination object from which the complex message
     * destination object is reference globally with non-exist destinationJndiName specified
     * in the ActivationSpec is in resources.xml
     *
     * @return Destination
     */
    public static Destination getDestinationInvalidDest() {
        return destinationInvalidDest;
    }

    /**
     * This method will set the destination object from which the complex message
     * destination object is reference globally with null destinationJndiName specified
     * in the ActivationSpec is in resources.xml
     *
     * @param Destination
     */
    public static void setDestinationNullDest(Destination dest) {
        destinationNullDest = dest;
    }

    /**
     * This method will get the destination object from which the complex message
     * destination object is reference globally with null destinationJndiName specified
     * in the ActivationSpec is in resources.xml
     *
     * @return Destination
     */
    public static Destination getDestinationNullDest() {
        return destinationNullDest;
    }

    /**
     * This method will set the destination object from which the complex message
     * destination object is reference globally with valid destinationJndiName specified
     * in the ActivationSpec is in resources.xml but the AO is not of type Destination
     *
     * @param Destination
     */
    public static void setDestinationValidDestWrongAODestType(Destination dest) {
        destinationValidDestWrongAODestType = dest;
    }

    /**
     * This method will get the destination object from which the complex message
     * destination object is reference globally with valid destinationJndiName specified
     * in the ActivationSpec is in resources.xml but the AO is not of type Destination
     *
     * @return Destination
     */
    public static Destination getDestinationValidDestWrongAODestType() {
        return destinationValidDestWrongAODestType;
    }

    /**
     * This method will set the destination object from which the complex message
     * destination object is reference globally with blank destinationJndiName specified
     * in the ActivationSpec is in resources.xml
     *
     * @param Destination
     */
    public static void setDestinationBlankDest(Destination dest) {
        destinationBlankDest = dest;
    }

    /**
     * This method will get the destination object from which the complex message
     * destination object is reference globally with blank destinationJndiName specified
     * in the ActivationSpec is in resources.xml
     *
     * @return Destination
     */
    public static Destination getDestinationBlankDest() {
        return destinationBlankDest;
    }
}