/*******************************************************************************
 * Copyright (c) 2013,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.test.jca.enterprisera;

import javax.jms.Destination;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;

public class JCAEnterpriseAppActivationSpec implements ActivationSpec {
    private Destination destination;

    public Destination getDestination() {
        return destination;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return null;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {}

    @Override
    public void validate() throws InvalidPropertyException {}
}
