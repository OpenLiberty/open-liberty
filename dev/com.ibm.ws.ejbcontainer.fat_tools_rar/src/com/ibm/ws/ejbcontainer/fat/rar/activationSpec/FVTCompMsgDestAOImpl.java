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

import java.io.Serializable;

import javax.jms.Destination;

/**
 * <p>This class implements the Destination interface. This Destination implementation
 * class has a few attributes, which are verifyString, .</p>
 */
public class FVTCompMsgDestAOImpl implements Destination, Serializable {

    /** configured property - verifyString */
    private String verifyString;

    /**
     * Returns the verifyString.
     *
     * @return String
     */
    public String getVerifyString() {
        return verifyString;
    }

    /**
     * Sets the name.
     *
     * @param name The name to set
     */
    public void setVerifyString(String verifyString) {
        this.verifyString = verifyString;
    }

    public String introspectSelf() {
        return "ComplexMsgDest - verifyString: " + verifyString;
    }
}