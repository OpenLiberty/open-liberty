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
package com.ibm.ws.security.notifications;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;

/**
 * Base class implementation of the SecurityChangeNotifier interface.
 * It will send a change notification to all registered listeners.
 * If a component wants to send notifications it needs to create a
 * component that has a class that extends this one and its bnd
 * file must declare the listeners it wants to accept. For example,
 * <pre>
 * Service-Component: \
 * com.ibm.ws.security.MyComponent; \
 * implementation:=com.ibm.ws.security.MyComponentImpl; \
 * activate:=activate; \
 * deactivate:=deactivate; \
 * modified:=modified; \
 * configuration-policy:=optional; \
 * immediate:=true; \
 * myChangeNotifier=com.ibm.ws.security.MyChangeNotifier; \
 * dynamic:='myChangeNotifier'; \
 * properties:='service.vendor=IBM', \
 * com.ibm.ws.security.MyChangeNotifier; \
 * implementation:=com.ibm.ws.security.MyChangeNotifier; \
 * provide:='com.ibm.ws.security.MyChangeNotifier'; \
 * activate:=activate; \
 * deactivate:=deactivate; \
 * configuration-policy:=ignore; \
 * immediate:=true; \
 * changeListener=com.ibm.ws.security.notifications.SecurityChangeListener; \
 * optional:='changeListener'; \
 * multiple:='changeListener'; \
 * dynamic:='changeListener'; \
 * properties:='service.vendor=IBM'
 * </pre>
 * 
 */
public class BaseSecurityChangeNotifier implements SecurityChangeNotifier {

    protected static final String KEY_LISTENER = "changeListener";
    private final ConcurrentServiceReferenceSet<SecurityChangeListener> listeners = new ConcurrentServiceReferenceSet<SecurityChangeListener>(KEY_LISTENER);

    protected void activate(ComponentContext componentContext) {
        listeners.activate(componentContext);
    }

    protected void deactivate(ComponentContext componentContext) {
        listeners.deactivate(componentContext);
    }

    /**
     * @param reference the SecurityChangeListener service reference to add to the set of listeners.
     */
    protected synchronized void setChangeListener(ServiceReference<SecurityChangeListener> reference) {
        listeners.addReference(reference);
    }

    /**
     * @param reference the SecurityChangeListener service reference to remove from the set of listeners.
     */
    protected synchronized void unsetChangeListener(ServiceReference<SecurityChangeListener> reference) {
        listeners.removeReference(reference);
    }

    /** {@inheritDoc} */
    public synchronized void notifyListeners() {
        for (SecurityChangeListener listener : listeners.services()) {
            listener.notifyChange();
        }
    }

}
