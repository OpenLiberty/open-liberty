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
package com.ibm.ws.injectionengine.osgi.internal;

import java.util.Collection;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.utils.ServiceAndServiceReferencePair;
import com.ibm.wsspi.resource.ResourceBinding;
import com.ibm.wsspi.resource.ResourceBindingListener;

@Trivial
public class ResourceBindingImpl implements ResourceBinding {
    private static final TraceComponent tc = Tr.register(ResourceBindingImpl.class);

    private final String refName;
    private String bindingName;
    private final String type;
    private final Map<String, Object> properties;
    private ServiceAndServiceReferencePair<ResourceBindingListener> currentListener;
    ServiceAndServiceReferencePair<ResourceBindingListener> bindingListener;

    ResourceBindingImpl(String refName, String bindingName, String type, Map<String, Object> properties) {
        this.refName = refName;
        this.bindingName = bindingName;
        this.type = type;
        this.properties = properties;
    }

    @Override
    public String toString() {
        return super.toString() + '[' +
               "referenceName=" + refName +
               ", type=" + type +
               ", properties=" + properties +
               ", bindingName=" + bindingName +
               ", bindingListener=" + (bindingListener == null ? null : bindingListener.getServiceReference()) +
               ']';
    }

    @Override
    public String getReferenceName() {
        return refName;
    }

    @Override
    public String getTypeName() {
        return type;
    }

    @Override
    public String getBindingName() {
        return bindingName;
    }

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return properties.keySet();
    }

    public void notify(ServiceAndServiceReferencePair<ResourceBindingListener> listener) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "notify",
                     listener.getServiceReference(),
                     Util.identity(listener.getService()));

        this.currentListener = listener;
        listener.getService().binding(this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "notify");
    }

    @Override
    public void setBindingName(String bindingName) {
        if (bindingName == null) {
            throw new IllegalArgumentException("bindingName");
        }

        this.bindingName = bindingName;
        this.bindingListener = currentListener;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setBindingName",
                     bindingName,
                     bindingListener.getServiceReference());
    }

    /**
     * Returns the binding listener name. This method can only be called
     * if {@link #bindingListener} is non-null.
     */
    public String getBindingListenerName() {
        ServiceReference<?> ref = bindingListener.getServiceReference();
        Object serviceDescription = ref.getProperty(Constants.SERVICE_DESCRIPTION);
        if (serviceDescription instanceof String) {
            return (String) serviceDescription;
        }

        return bindingListener.getService().getClass().getName() + " (" + ref.getBundle().getSymbolicName() + ')';
    }
}
