/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import com.ibm.ws.kernel.productinfo.ProductInfo;

/**
 * DS for custom CDI properties. The active instance can either be retrieved through DS or through a static getter method.
 */
@Component(name = "com.ibm.ws.cdi.config.liberty.CDI12ContainerConfig", service = CDI12ContainerConfig.class, configurationPid = "com.ibm.ws.cdi12.cdiContainer", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true, property = { "service.vendor=IBM" })
public class CDI12ContainerConfig {

    private static final TraceComponent tc = Tr.register(CDI12ContainerConfig.class);

    private Boolean enableImplicitBeanArchives = null;

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
        //TOOD the ProductInfo.getBetaEdition() check should be removed before GA
        if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled() && ProductInfo.getBetaEdition()) {
            //CWOWB1015W: The cdi12 configuration element is superseded by the cdi configuration element.
            Tr.warning(tc, "cdi12.element.type.superceded.CWOWB1015W");
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
            //we actually only care about one property so read it here
            this.enableImplicitBeanArchives = (Boolean) properties.get(AggregatedConfiguration.ENABLE_IMPLICIT_BEAN_ARCHIVES);
        } else {
            this.enableImplicitBeanArchives = null;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "cdi12." + AggregatedConfiguration.ENABLE_IMPLICIT_BEAN_ARCHIVES + ": " + this.enableImplicitBeanArchives);
        }
        this.aggregatedConfig.setCdi12Config(this.enableImplicitBeanArchives);
    }
}
