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
package com.ibm.ws.microprofile.openapi.test.utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

public class TestValidationContextHelper implements Context {

    private final OpenAPIImpl openAPI;
    private final Deque<String> pathSegments = new ArrayDeque<>();

    public TestValidationContextHelper(OpenAPIImpl openAPI) {
        this.openAPI = openAPI;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.openapi.internal.utils.OpenAPIModelWalker.Context#getModel()
     */
    @Override
    public OpenAPI getModel() {
        return openAPI;
    }

    @Override
    public String getLocation() {
        return getLocation(null);
    }

    @Override
    public String getLocation(String suffix) {
        final Iterator<String> i = pathSegments.descendingIterator();
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        while (i.hasNext()) {
            if (!first) {
                sb.append('/');
            }
            sb.append(i.next());
            first = false;
        }
        if (suffix != null && !suffix.isEmpty()) {
            sb.append('/');
            sb.append(suffix);
        }
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.openapi.internal.utils.OpenAPIModelWalker.Context#getParent()
     */
    @Override
    public Object getParent() {
        // TODO Auto-generated method stub
        return null;
    }
}
