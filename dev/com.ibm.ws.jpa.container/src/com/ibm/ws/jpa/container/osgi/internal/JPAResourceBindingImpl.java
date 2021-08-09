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
package com.ibm.ws.jpa.container.osgi.internal;

import java.util.Collection;
import java.util.Map;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jpa.management.JPAConstants;
import com.ibm.wsspi.kernel.service.utils.ServiceAndServiceReferencePair;
import com.ibm.wsspi.resource.ResourceBinding;
import com.ibm.wsspi.resource.ResourceBindingListener;

@Trivial
public class JPAResourceBindingImpl implements ResourceBinding {
    private static final TraceComponent tc = Tr.register(JPAResourceBindingImpl.class, JPAConstants.JPA_TRACE_GROUP, JPAConstants.JPA_RESOURCE_BUNDLE_NAME);

    String bindingName;
    boolean bindingNameSet;
    private final Map<String, Object> properties;

    JPAResourceBindingImpl(String bindingName, Map<String, Object> properties) {
        this.bindingName = bindingName;
        this.properties = properties;
    }

    @Override
    public String toString() {
        return super.toString() + '[' +
               "bindingName=" + bindingName +
               ", properties=" + properties +
               ']';
    }

    @Override
    public String getReferenceName() {
        return null;
    }

    @Override
    public String getTypeName() {
        return "javax.sql.DataSource";
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

    void notify(ServiceAndServiceReferencePair<ResourceBindingListener> listener) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "notify", listener.getServiceReference(), Util.identity(listener.getService()) );

        listener.getService().binding(this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "notify");
    }

    @Override
    public void setBindingName(String bindingName) {
        if (bindingName == null) {
            throw new IllegalArgumentException("bindingName");
        }

        this.bindingName = bindingName;
        this.bindingNameSet = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setBindingName", bindingName);
    }
}
