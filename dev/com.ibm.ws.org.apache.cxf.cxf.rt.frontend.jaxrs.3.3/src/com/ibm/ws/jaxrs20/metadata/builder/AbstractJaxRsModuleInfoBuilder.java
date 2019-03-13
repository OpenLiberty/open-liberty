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
package com.ibm.ws.jaxrs20.metadata.builder;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ws.jaxrs20.api.JaxRsModuleInfoBuilder;
import com.ibm.ws.jaxrs20.api.JaxRsModuleInfoBuilderExtension;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleType;

/**
 * The base impl of JaxWsModuleInfoBuilder, set builder type and EndpointInfoBuilder reference.
 */

public abstract class AbstractJaxRsModuleInfoBuilder implements JaxRsModuleInfoBuilder {

//    protected final AtomicServiceReference<EndpointInfoBuilder> endpointInfoBuilderSRRef = new AtomicServiceReference<EndpointInfoBuilder>("endpointInfoBuilder");

    protected final Set<JaxRsModuleInfoBuilderExtension> extensions = new CopyOnWriteArraySet<JaxRsModuleInfoBuilderExtension>();

    private final JaxRsModuleType supportType;

    public AbstractJaxRsModuleInfoBuilder(JaxRsModuleType supportType) {
        this.supportType = supportType;
    }

    @Override
    public JaxRsModuleType getSupportType() {
        return this.supportType;
    }

    @Reference(service = JaxRsModuleInfoBuilderExtension.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void setJaxRsModuleInfoBuilderExtension(JaxRsModuleInfoBuilderExtension extension) {
        if (extension.getSupportTypes().contains(this.supportType)) {
            extensions.add(extension);
        }
    }

    protected void unsetJaxRsModuleInfoBuilderExtension(JaxRsModuleInfoBuilderExtension extension) {
        extensions.remove(extension);
    }

//    protected void setEndpointInfoBuilder(ServiceReference<EndpointInfoBuilder> ref) {
//        endpointInfoBuilderSRRef.setReference(ref);
//    }

//    protected void unsetEndpointInfoBuilder(ServiceReference<EndpointInfoBuilder> ref) {
//        endpointInfoBuilderSRRef.unsetReference(ref);
//    }
    @Activate
    protected void activate(ComponentContext cc) {

    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {}

}
