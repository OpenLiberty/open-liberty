/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tra.ann;

import javax.resource.ResourceException;
import javax.resource.spi.Activation;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;

import com.ibm.tra.inbound.impl.TRAMessageListener3;

@Activation(
            messageListeners = { TRAMessageListener3.class })
public class ConfigPropertyActivationAnn1 implements javax.resource.spi.ActivationSpec {

    public ConfigPropertyActivationAnn1() {
        super();
    }

    @ConfigProperty(
                    supportsDynamicUpdates = false, defaultValue = "TestPassword")
    private String password1;

    @ConfigProperty(
                    supportsDynamicUpdates = false)
    private String userName1;

    public String getPassword1() {
        return password1;
    }

    public void setPassword1(String password1) {
        this.password1 = password1;
    }

    public String getUserName1() {
        return userName1;
    }

    public void setUserName1(String userName) {
        this.userName1 = userName;
    }

/*
 * (non-Javadoc)
 *
 * @see javax.resource.spi.ResourceAdapterAssociation#getResourceAdapter()
 */
    @Override
    public ResourceAdapter getResourceAdapter() {
        return null;
    }

/*
 * (non-Javadoc)
 *
 * @see javax.resource.spi.ResourceAdapterAssociation#setResourceAdapter(javax.resource.spi.ResourceAdapter)
 */
    @Override
    public void setResourceAdapter(ResourceAdapter arg0) throws ResourceException {}

/*
 * (non-Javadoc)
 *
 * @see javax.resource.spi.ActivationSpec#validate()
 */
    @Override
    public void validate() throws InvalidPropertyException {}

}
