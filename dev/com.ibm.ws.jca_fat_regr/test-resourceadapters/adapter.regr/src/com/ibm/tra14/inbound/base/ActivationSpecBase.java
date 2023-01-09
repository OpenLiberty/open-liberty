/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

    public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
        if (_ra != null) {
            throw new ResourceException("Resource Adapter was already set.");
        }
        _ra = ra;
    }

    public ResourceAdapter getResourceAdapter() {
        return _ra;
    }

    public abstract void validate() throws InvalidPropertyException;
}