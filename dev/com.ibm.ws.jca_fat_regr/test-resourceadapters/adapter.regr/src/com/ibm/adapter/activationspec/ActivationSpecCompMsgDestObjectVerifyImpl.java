/*******************************************************************************
 * Copyright (c) 2006, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.activationspec;

import javax.jms.Destination;

import com.ibm.adapter.ActivationSpecImpl; // 313344.1

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
