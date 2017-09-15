/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import java.util.Dictionary;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

class MockServiceRegistration<S> implements ServiceRegistration<S> {

    @Override
    public ServiceReference<S> getReference() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProperties(Dictionary<String, ?> properties) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unregister() {
        //allow
    }

}
