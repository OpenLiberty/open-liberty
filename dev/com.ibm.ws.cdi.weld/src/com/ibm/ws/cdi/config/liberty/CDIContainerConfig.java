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

/**
 * DS for custom CDI properties. The active instance can either be retrieved through DS or through a static getter method.
 */
@Component(name = "com.ibm.ws.cdi.config.liberty.CDIContainerConfig", service = CDIContainerConfig.class, configurationPid = "io.openliberty.cdi.configuration", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true, property = { "service.vendor=IBM" })
public class CDIContainerConfig {

    /*-
     * This example demonstrates how to add a new property.
     *
     * Steps:
     *   1) Create a public getter method that returns a primitive type:
     *
     *      public boolean getBeAwesome() {
     *          return (Boolean) this.properties.get("beAwesome");
     *      }
     *
     *   2) Add an attribute definition to metatype.xml with a default value:
     *
     *      <AD id="beAwesome"
     *          name="%beAwesome"
     *          description="%beAwesome.desc"
     *          required="false" type="Boolean" default="true" />
     *
     *   3) Add documentation to metatype.properties for the new property.
     *      These properties are used by the InfoCenter and Eclipse tooling.
     *      See the wiki on "Metatype Authoring Guidelines" for rules:
     *
     *      beAwesome=Be awesome
     *      beAwesome.desc=Disable this property if you want the CDI Container to be average.
     *
     *   4) Add a FAT that verifies the behavior of the new property with
     *      various values. Here's an example of setting the property in server.xml:
     *
     *      <cdiContainer beAwesome="true"/>
     *
     *
     * How this process simplifies property maintenance:
     *   1) this.properties is never null, so you never need to check for null
     *   2) if we provide getter methods for each property we support,
     *      then the property names only need to be defined in two places:
     *      this class and metatype.xml.
     *   3) if metatype.xml defines default values for each property we support,
     *      then DS will always give us non-null values with appropriate type.
     *      As a result, we can always safely cast without additional checks.
     *   4) if our getter methods return primitive types, then the caller
     *      won't need to check for null either.
     */

    private static final TraceComponent tc = Tr.register(CDIContainerConfig.class);

    private Boolean enableImplicitBeanArchives = null;
    private Boolean emptyBeansXMLExplicitArchive = null;

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
            this.emptyBeansXMLExplicitArchive = (Boolean) properties.get(AggregatedConfiguration.EMPTY_BEANS_XML_EXPLICIT_BEAN_ARCHIVE);
        } else {
            this.enableImplicitBeanArchives = null;
            this.emptyBeansXMLExplicitArchive = null;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "cdi." + AggregatedConfiguration.ENABLE_IMPLICIT_BEAN_ARCHIVES + ": " + this.enableImplicitBeanArchives);
            Tr.debug(tc, "cdi." + AggregatedConfiguration.EMPTY_BEANS_XML_EXPLICIT_BEAN_ARCHIVE + ": " + this.emptyBeansXMLExplicitArchive);
        }
        this.aggregatedConfig.setCdiConfig(this.enableImplicitBeanArchives, this.emptyBeansXMLExplicitArchive);
    }
}
