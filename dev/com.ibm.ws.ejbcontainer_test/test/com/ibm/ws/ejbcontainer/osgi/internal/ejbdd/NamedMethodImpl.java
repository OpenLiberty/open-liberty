/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.ejbdd;

import java.util.Arrays;
import java.util.List;

import com.ibm.ws.javaee.dd.ejb.NamedMethod;

class NamedMethodImpl implements NamedMethod {
    private final String name;
    private final List<String> params;

    NamedMethodImpl(String name, String... params) {
        this.name = name;
        this.params = Arrays.asList(params);
    }

    @Override
    public String getMethodName() {
        return name;
    }

    @Override
    public List<String> getMethodParamList() {
        return params;
    }
}
