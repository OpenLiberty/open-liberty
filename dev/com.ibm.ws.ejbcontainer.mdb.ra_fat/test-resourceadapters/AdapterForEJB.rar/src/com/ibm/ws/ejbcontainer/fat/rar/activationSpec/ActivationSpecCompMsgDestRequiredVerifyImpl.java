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

// This class has a misleading name and a misleading extension.
//
// This classes only reason for existance is to be a storage mechanism for data...it has no other purpose.
// In other words, this class could be replaced by writing to a flat file, or writing to a database, or any
// other mechanism that we can use to persist data.  The only reason we chose this class is that is the easier
// way to persist data...its simply a collection of static variables, with getter() and setter() methods for dealing
// with those variables.
//
// The class contains the phrase 'ActiationSpec' in its name, but its not used as an activation spec because
// its not called out the ra.xml file.
//
// It extends something with extends the Java ActivationSpec interface, so technically it is an activationSpec,
// but since its not being *used* as one, there is no reason to make it extend this interface, and it could be
// changed to stop extending this interface without causing any harm.

public class ActivationSpecCompMsgDestRequiredVerifyImpl extends ActivationSpecImpl {

    static Destination destinationRequiredWrongAODestType = null;
    static Destination destinationRequiredNullDest = null;
    static Destination destinationRequiredInvalidDest = null;

    static Destination destinationRequiredValidateThrowEx = null;

    static boolean isASRequiredWrongAODestTypeInit = false;
    static boolean isASRequiredNullDestInit = false;
    static boolean isASRequiredInvalidDestInit = false;

    static boolean isASRequiredValidateThrowExInit = false;
    static boolean isASRequiredValidateThrowExActivated = false;
    static String verifyStringGlobal = null;

    /**
     * This method will set the destination object from which the complex message
     * destination object is reference globally with destinationJndiName refers to
     * an admin object not of type Destination, specified
     * in the ActivationSpec is in resources.xml.
     *
     * @param Destination
     */
    public static void setDestinationRequiredWrongAODestType(Destination dest) {
        destinationRequiredWrongAODestType = dest;
    }

    /**
     * This method will get the destination object from which the complex message
     * destination object is reference globally with destinationJndiName refers to
     * an admin object not of type Destination, specified
     * in the ActivationSpec is in resources.xml.
     *
     * @return Destination
     */
    public static Destination getDestinationRequiredWrongAODestType() {
        return destinationRequiredWrongAODestType;
    }

    /**
     * This method will set the boolean variable to true. This means the
     * AS instances which has destination as required property and the destJndiName
     * set to an admin object of wrong type was loaded.
     */
    public static void setASRequiredWrongAODestTypeInit(boolean init) {
        isASRequiredWrongAODestTypeInit = init;
    }

    /**
     * This method will get the boolean variable to true. This means the
     * AS instances which has destination as required property and the destJndiName
     * set to an admin object of wrong type was loaded.
     */
    public static boolean getASRequiredWrongAODestTypeInit() {
        return isASRequiredWrongAODestTypeInit;
    }

    /**
     * This method will set the destination object from which the complex message
     * destination object is reference globally without destinationJndiName specified
     * in the ActivationSpec is in resources.xml.
     *
     * @param Destination
     */
    public static void setDestinationRequiredNullDest(Destination dest) {
        destinationRequiredNullDest = dest;
    }

    /**
     * This method will get the destination object from which the complex message
     * destination object is reference globally without destinationJndiName specified
     * in the ActivationSpec is in resources.xml.
     *
     * @return Destination
     */
    public static Destination getDestinationRequiredNullDest() {
        return destinationRequiredNullDest;
    }

    /**
     * This method will set the boolean variable to true. This means the
     * AS instances which has destination as required property and no destJndiName
     * specified was loaded.
     *
     */
    public static void setASRequiredNullDestInit(boolean init) {
        isASRequiredNullDestInit = init;
    }

    /**
     * This method will get the boolean variable to true. This means the
     * AS instances which has destination as required property and no destJndiName
     * specified was loaded.
     */
    public static boolean getASRequiredNullDestInit() {
        return isASRequiredNullDestInit;
    }

    /**
     * This method will set the destination object from which the complex message
     * destination object is reference globally with invalid destinationJndiName specified
     * in the ActivationSpec is in resources.xml.
     *
     * @param Destination
     */
    public static void setDestinationRequiredInvalidDest(Destination dest) {
        destinationRequiredInvalidDest = dest;
    }

    /**
     * This method will get the destination object from which the complex message
     * destination object is reference globally with invalid destinationJndiName specified
     * in the ActivationSpec is in resources.xml.
     *
     * @return Destination
     */
    public static Destination getDestinationRequiredInvalidDest() {
        return destinationRequiredInvalidDest;
    }

    /**
     * This method will set the boolean variable to true. This means the
     * AS instances which has destination as required property and invalid destJndiName
     * specified was loaded.
     *
     */
    public static void setASRequiredInvalidDestInit(boolean init) {
        isASRequiredInvalidDestInit = init;
    }

    /**
     * This method will get the boolean variable to true. This means the
     * AS instances which has destination as required property and invalid destJndiName
     * specified was loaded.
     */
    public static boolean getASRequiredInvalidDestInit() {
        return isASRequiredInvalidDestInit;
    }

    /**
     * This method will set the destination object from which the complex message
     * destination object is reference globally with destinationJndiName specified
     * in the ActivationSpec is in resources.xml.
     *
     * @param Destination
     */
    public static void setDestinationRequiredValidateThrowEx(Destination dest) {
        destinationRequiredValidateThrowEx = dest;
    }

    /**
     * This method will get the destination object from which the complex message
     * destination object is reference globally with destinationJndiName specified
     * in the ActivationSpec is in resources.xml.
     *
     * @return Destination
     */
    public static Destination getDestinationRequiredValidateThrowEx() {
        return destinationRequiredValidateThrowEx;
    }

    /**
     * This method will set the boolean variable to true. This means the
     * AS instances which has destination as required property and valid destJndiName
     * specified was loaded.
     *
     */
    public static void setASRequiredValidateThrowExInit(boolean init) {
        isASRequiredValidateThrowExInit = init;
    }

    /**
     * This method will get the boolean variable to true. This means the
     * AS instances which has destination as required property and valid destJndiName
     * specified was loaded.
     */
    public static boolean getASRequiredValidateThrowExInit() {
        return isASRequiredValidateThrowExInit;
    }

    /**
     * This method will set the boolean variable to true. This means the
     * AS instances which has destination as required property and invalid destJndiName
     * specified was activated.
     *
     */
    public static void setASRequiredValidateThrowExActivated(boolean act) {
        isASRequiredValidateThrowExActivated = act;
    }

    /**
     * This method will get the boolean variable to true. This means the
     * AS instances which has destination as required property and invalid destJndiName
     * specified was activated.
     */
    public static boolean getASRequiredValidateThrowExActivated() {
        return isASRequiredValidateThrowExActivated;
    }

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
}