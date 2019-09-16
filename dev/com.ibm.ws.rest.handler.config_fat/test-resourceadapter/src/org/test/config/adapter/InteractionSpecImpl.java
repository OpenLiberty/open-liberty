/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.test.config.adapter;

import javax.resource.cci.InteractionSpec;
import javax.resource.spi.AdministeredObject;
import javax.resource.spi.ConfigProperty;

@AdministeredObject
public class InteractionSpecImpl implements InteractionSpec {
    private static final long serialVersionUID = 1L;

    @ConfigProperty
    private String functionName;

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }
}
