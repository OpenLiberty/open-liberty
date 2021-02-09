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

/**
 * Base class for ActivationSpec, Already has the required functions, just need to implement validate
 * (if validation is required), and any other properties.
 */

package com.ibm.tra14.inbound.base;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;

public abstract class ActivationSpecBase implements ActivationSpec {

    protected ResourceAdapter _ra;

    public ActivationSpecBase() {
        _ra = null;
    }

    public ActivationSpecBase(ResourceAdapter ra) {
        _ra = ra;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
        if (_ra != null) {
            throw new ResourceException("Resource Adapter was already set.");
        }
        _ra = ra;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return _ra;
    }

    @Override
    public abstract void validate() throws InvalidPropertyException;
}