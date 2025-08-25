/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package com.ibm.inout.adapter;

import jakarta.resource.spi.Activation;
import jakarta.resource.spi.ConfigProperty;

/**
 * <p>
 * This class implements the ActivationSpec interface. This ActivationSpec
 * implementation class only has one attribute, the name of the endpoint
 * application.
 * </p>
 */

@Activation(messageListeners = { jakarta.jms.MessageListener.class })
public class ActivationSpecImpl extends com.ibm.adapter.ActivationSpecImpl {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Sets the name.
     *
     * @param name
     *                 The name to set
     */
    @Override
    @ConfigProperty(defaultValue = "InoutEndPoint")
    public void setName(String name) {
        this.name = name;
    }
}
