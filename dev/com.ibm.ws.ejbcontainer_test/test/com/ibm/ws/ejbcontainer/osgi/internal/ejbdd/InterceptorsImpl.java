/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.ejbdd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.ejb.Interceptor;
import com.ibm.ws.javaee.dd.ejb.Interceptors;

class InterceptorsImpl implements Interceptors {

    private final List<Interceptor> interceptors = new ArrayList<Interceptor>();

    InterceptorsImpl(String... interceptorClassNames) {
        for (String interceptorClassName : interceptorClassNames) {
            interceptors.add(new InterceptorImpl(interceptorClassName));
        }
    }

    @Override
    public List<Description> getDescriptions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Interceptor> getInterceptorList() {
        return Collections.unmodifiableList(interceptors);
    }
}
