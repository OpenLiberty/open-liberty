/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.test.errorpathadapter;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;

public class EPActivationSpec implements ActivationSpec {
    private String requiredProp1, requiredProp2, requiredProp3, optionalPropA;

    public String getOptionalPropA() {
        return optionalPropA;
    }

    public String getRequiredProp1() {
        return requiredProp1;
    }

    public String getRequiredProp2() {
        return requiredProp2;
    }

    public String getRequiredProp3() {
        return requiredProp3;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return null;
    }

    public void setOptionalPropA(String value) {
        optionalPropA = value;
    }

    public void setRequiredProp1(String value) {
        requiredProp1 = value;
    }

    public void setRequiredProp2(String value) {
        requiredProp2 = value;
    }

    public void setRequiredProp3(String value) {
        requiredProp3 = value;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {}

    @Override
    public void validate() throws InvalidPropertyException {}
}
