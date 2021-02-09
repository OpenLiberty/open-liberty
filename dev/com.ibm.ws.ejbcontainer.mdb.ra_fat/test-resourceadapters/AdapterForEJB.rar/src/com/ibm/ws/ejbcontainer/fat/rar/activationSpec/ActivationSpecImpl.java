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

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;

/**
 * <p>This class implements the ActivationSpec interface. This ActivationSpec implementation
 * class only has one attribute, the name of the endpoint application.</p>
 */
public class ActivationSpecImpl implements ActivationSpec, Serializable {
    /** configured property - endpoint name */
    protected String name;

    /** resoure adpater instance */
    protected ResourceAdapter resourceAdapter;

    /**
     * This method may be called by a deployment tool to validate the overall activation configuration
     * information provided by the endpoint deployer. This helps to catch activation configuration
     * errors earlier on without having to wait until endpoint activation time for configuration
     * validation. The implementation of this self-validation check behavior is optional.
     */
    @Override
    public void validate() throws InvalidPropertyException {
        // make sure the name is not null or empty.

        if (name == null || name.equals("") || name.trim().equals("")) {
            throw new InvalidPropertyException("The name property cannot be null or an empty string.");
        }
    }

    /**
     * Get the associated ResourceAdapter JavaBean.
     *
     * @return adater the resource adpater instance
     */
    @Override
    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    /**
     * Associate this ActivationSpec JavaBean with a ResourceAdapter JavaBean. Note,
     * this method must be called exactly once; that is, the association must not change
     * during the lifetime of this ActivationSpec JavaBean.
     *
     * @param adapter the resource adapter instance
     *
     * @exception ResourceException
     *                ResourceExeception - generic exception.
     *                ResourceAdapterInternalException - resource adapter related error condition.
     */
    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.resourceAdapter = adapter;
    }

    /**
     * Returns the name.
     *
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public String introspectSelf() {
        return "ActivationSpecImpl - name: " + name;
    }
}