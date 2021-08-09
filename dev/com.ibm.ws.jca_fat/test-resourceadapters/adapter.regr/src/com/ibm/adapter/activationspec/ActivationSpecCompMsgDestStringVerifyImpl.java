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

import com.ibm.adapter.ActivationSpecImpl;

/**
 * This class is for verifying if null object or a String
 * is being associated to the instance of ActivationSpecCompMsgDestStringImpl.
 */
//313344.1 extend with ActivationSpecImpl
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
