/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.fat.jaxrs;

import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;

/**
 * Repeat rule to set a value in the FATSuite (for test classes to use) to indicate what instance of repeat is running
 */
public class OidcClientJaxrsRepeatAction extends SecurityTestRepeatAction {

    /**
     * @param inNameExtension
     */
    public OidcClientJaxrsRepeatAction(String inNameExtension) {
        super(inNameExtension);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void setup() throws Exception {

        FATSuite.repeatFlag = nameExtension;
    }

}