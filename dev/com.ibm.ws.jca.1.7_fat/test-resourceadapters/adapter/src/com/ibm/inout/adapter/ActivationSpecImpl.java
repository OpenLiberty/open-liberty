/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.inout.adapter;

import javax.resource.spi.Activation;
import javax.resource.spi.ConfigProperty;

/**
 * <p>
 * This class implements the ActivationSpec interface. This ActivationSpec
 * implementation class only has one attribute, the name of the endpoint
 * application.
 * </p>
 */

@Activation(messageListeners = { javax.jms.MessageListener.class })
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
