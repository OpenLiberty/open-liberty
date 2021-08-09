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
package com.ibm.ws.jaxws.metadata.builder;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.jaxws.metadata.JaxWsModuleType;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * The base impl of JaxWsModuleInfoBuilder, set builder type and EndpointInfoBuilder reference.
 */
public abstract class AbstractJaxWsModuleInfoBuilder implements JaxWsModuleInfoBuilder {

    protected final AtomicServiceReference<EndpointInfoBuilder> endpointInfoBuilderSRRef = new AtomicServiceReference<EndpointInfoBuilder>("endpointInfoBuilder");

    protected final Set<JaxWsModuleInfoBuilderExtension> extensions = new CopyOnWriteArraySet<JaxWsModuleInfoBuilderExtension>();

    private final JaxWsModuleType supportType;

    public AbstractJaxWsModuleInfoBuilder(JaxWsModuleType supportType) {
        this.supportType = supportType;
    }

    public JaxWsModuleType getSupportType() {
        return this.supportType;
    }

    protected void setJaxWsModuleInfoBuilderExtension(JaxWsModuleInfoBuilderExtension extension) {
        if (extension.getSupportTypes().contains(this.supportType)) {
            extensions.add(extension);
        }
    }

    protected void unsetJaxWsModuleInfoBuilderExtension(JaxWsModuleInfoBuilderExtension extension) {
        extensions.remove(extension);
    }

    protected void setEndpointInfoBuilder(ServiceReference<EndpointInfoBuilder> ref) {
        endpointInfoBuilderSRRef.setReference(ref);
    }

    protected void unsetEndpointInfoBuilder(ServiceReference<EndpointInfoBuilder> ref) {
        endpointInfoBuilderSRRef.unsetReference(ref);
    }

    protected void activate(ComponentContext cc) {
        endpointInfoBuilderSRRef.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        endpointInfoBuilderSRRef.deactivate(cc);
    }

}
