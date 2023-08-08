/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.config.liberty;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.internal.config.AggregatedConfiguration;

/**
 * DS for custom CDI properties. The active instance can either be retrieved through DS or through a static getter method.
 */
@Component(name = "com.ibm.ws.cdi.config.liberty.CDIContainerConfig", service = CDIContainerConfig.class, configurationPid = "io.openliberty.cdi.configuration", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
public class CDIContainerConfig {

    private static final TraceComponent tc = Tr.register(CDIContainerConfig.class);

    private Boolean enableImplicitBeanArchives = null;
    private Boolean emptyBeansXmlCDI3Compatibility = null;

    @Reference
    private AggregatedConfiguration aggregatedConfig;

    /**
     * DS method to activate this component
     *
     * @param compcontext the context of this component
     * @param properties  the new configuration properties
     */
    protected void activate(ComponentContext compcontext, Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Activating " + this);
        }
        this.updateConfiguration(properties);
    }

    /**
     * DS method to deactivate this component
     *
     * @param compcontext the context of this component
     */
    protected void deactivate(ComponentContext compcontext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Deactivating " + this);
        }
        this.updateConfiguration(null);
    }

    /**
     * DS method to modify the configuration of this component
     *
     * @param compcontext the context of this component
     * @param properties  the updated configuration properties
     */
    @Modified
    protected void modified(ComponentContext compcontext, Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Modifying " + this);
        }
        this.updateConfiguration(properties);
    }

    /**
     * Updates the current configuration properties
     *
     * @param properties the updated configuration properties
     */
    protected void updateConfiguration(Map<String, Object> properties) {
        if (properties != null) {
            //we actually only care about two properties so read them here
            this.enableImplicitBeanArchives = (Boolean) properties.get(AggregatedConfiguration.ENABLE_IMPLICIT_BEAN_ARCHIVES);
            this.emptyBeansXmlCDI3Compatibility = (Boolean) properties.get(AggregatedConfiguration.EMPTY_BEANS_XML_CDI3_COMPATIBILITY);
        } else {
            this.enableImplicitBeanArchives = null;
            this.emptyBeansXmlCDI3Compatibility = null;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "cdi." + AggregatedConfiguration.ENABLE_IMPLICIT_BEAN_ARCHIVES + ": " + this.enableImplicitBeanArchives);
            Tr.debug(tc, "cdi." + AggregatedConfiguration.EMPTY_BEANS_XML_CDI3_COMPATIBILITY + ": " + this.emptyBeansXmlCDI3Compatibility);
        }
        this.aggregatedConfig.setCdiConfig(this.enableImplicitBeanArchives, this.emptyBeansXmlCDI3Compatibility);
    }
}
