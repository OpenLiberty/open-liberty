/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.websphere.metatype.MetaTypeFactory;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.RegistryEntry;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;
import com.ibm.wsspi.logging.Introspector;

@Component(service = { Introspector.class },
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = {
                        Constants.SERVICE_VENDOR + "=" + "IBM"
           })
public class ConfigIntrospection implements Introspector {

    private final static String NAME = "ConfigIntrospection";
    private final static String DESC = "Introspect internal configuration store";
    private SystemConfiguration systemConfiguration;
    private MetaTypeRegistry metaTypeRegistry;

    @Activate
    protected void activate(BundleContext context) {

    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected void setSystemConfiguration(SystemConfiguration sc) {
        this.systemConfiguration = sc;
    }

    protected void unsetSystemConfiguration(SystemConfiguration sc) {
        if (sc == this.systemConfiguration) {
            this.systemConfiguration = null;
        }
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected void setMetaTypeRegistry(MetaTypeRegistry mtr) {
        this.metaTypeRegistry = mtr;
    }

    protected void unsetMetaTypeRegistry(MetaTypeRegistry mtr) {
        if (mtr == this.metaTypeRegistry) {
            this.metaTypeRegistry = null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.logging.IntrospectableService#introspect(java.io.OutputStream)
     */
    @Override
    public void introspect(PrintWriter ps) throws IOException {

        ServerConfiguration server = systemConfiguration.getServerConfiguration();

        for (String name : server.getConfigurationNames()) {
            ps.println("");
            ps.println("Configuration Element Information for top level PID: " + name);
            RegistryEntry re = metaTypeRegistry.getRegistryEntry(name);
            ps.println("Has Metatype: " + (re != null));
            ConfigurationList<SimpleElement> cl = server.getConfigurationList(name);
            ConfigurationList<SimpleElement> defaultCl = server.getDefaultConfiguration().getConfigurationList(name);
            List<SimpleElement> elements = new ArrayList<SimpleElement>();
            elements = cl.collectElements(elements);
            elements = defaultCl.collectElements(elements);
            ps.println("Number of Elements: " + elements.size());
            for (SimpleElement element : elements) {
                ps.println("");
                ps.println("\tSequence Number: " + element.getSequenceId());
                ps.println("\tOrigin: " + element.getDocumentLocation());
                ps.println("\tAttributes: " + filterMetatypeAttributes(re, element.getAttributes()));
                ps.println("\tMerge Behavior: " + element.mergeBehavior);
                printNestedConfiguration(1, ps, element);
            }

        }
    }

    private void printNestedElementInformation(int indent, PrintWriter ps, ConfigElement element, RegistryEntry re) {
        boolean hasMetatype = (re != null);
        print(indent, ps, "");
        print(indent, ps, "PID: " + (hasMetatype ? re.getPid() : element.getNodeDisplayName()));
        print(indent, ps, "Has Metatype: " + hasMetatype);
        print(indent, ps, "Sequence Number: " + element.getSequenceId());
        print(indent, ps, "Attributes: " + filterMetatypeAttributes(re, element.getAttributes()));
        printNestedConfiguration(indent + 1, ps, element);

    }

    private void print(int indent, PrintWriter ps, String text) {
        StringWriter writer = new StringWriter();
        writer.append('\t');
        for (int i = 0; i < indent; i++) {
            writer.append("   ");
        }
        writer.append(text);
        ps.println(writer.toString());
    }

    /**
     * @param element
     */
    private void printNestedConfiguration(int indent, PrintWriter ps, ConfigElement element) {
        if (!element.hasNestedElements())
            return;

        print(indent, ps, "");
        print(indent, ps, "Nested Elements for " + element.getNodeDisplayName());
        for (ConfigElement nested : element.getChildren()) {
            RegistryEntry re = metaTypeRegistry.getRegistryEntry(nested);
            if (re == null)
                re = metaTypeRegistry.getRegistryEntry(nested.getNodeName());
            printNestedElementInformation(indent, ps, nested, re);
        }
        print(indent, ps, "");

    }

    /**
     * Filter out password fields
     *
     * @param attributes
     * @return
     */
    private Map<String, Object> filterMetatypeAttributes(RegistryEntry re, Map<String, Object> attributes) {
        if (re == null)
            return attributes;

        Map<String, ExtendedAttributeDefinition> attributeMap = re.getObjectClassDefinition().getAttributeMap();
        Map<String, Object> retVal = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            ExtendedAttributeDefinition ad = attributeMap.get(entry.getKey());
            if (isObscured(ad)) {
                retVal.put(entry.getKey(), "*******");
            } else {
                retVal.put(entry.getKey(), entry.getValue());
            }
        }

        return retVal;

    }

    /**
     * @param ad
     * @return
     */
    private boolean isObscured(ExtendedAttributeDefinition ad) {
        if (ad == null)
            return false;

        if (ad.isObscured() || ad.getType() == MetaTypeFactory.PASSWORD_TYPE || ad.getType() == MetaTypeFactory.HASHED_PASSWORD_TYPE)
            return true;

        return false;
    }

    @Override
    public String getIntrospectorDescription() {
        return DESC;
    }

    @Override
    public String getIntrospectorName() {
        return NAME;
    }

}
