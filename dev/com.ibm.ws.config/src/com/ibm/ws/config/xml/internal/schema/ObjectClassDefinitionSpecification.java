/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal.schema;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.equinox.metatype.EquinoxAttributeDefinition;
import org.eclipse.equinox.metatype.EquinoxObjectClassDefinition;
import org.eclipse.equinox.metatype.impl.ExtendableHelper;

/**
 * Custom ObjectClassDefinition
 */
public class ObjectClassDefinitionSpecification implements EquinoxObjectClassDefinition {

    private String id;
    private String name;
    private String description;
    private Map<String, AttributeDefinitionSpecification> attributes;

    private ExtendableHelper extensionHelper;

    public ObjectClassDefinitionSpecification() {
        this.attributes = new LinkedHashMap<String, AttributeDefinitionSpecification>();
    }

    public void setExtensionAttributes(ExtendableHelper helper) {
        extensionHelper = helper;
    }

    public void setID(String id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * @return the attributes
     */
    public Map<String, AttributeDefinitionSpecification> getAttributes() {
        return attributes;
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(Map<String, AttributeDefinitionSpecification> attributes) {
        this.attributes = attributes;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    public void addAttribute(AttributeDefinitionSpecification ad) {
        attributes.put(ad.getID(), ad);
    }

    public AttributeDefinitionSpecification getAttribute(String id) {
        return attributes.get(id);
    }

    @Override
    public String toString() {
        return "ObjectClassDefinitionSpecification [id=" + id + ", name=" + name
               + ", attributes=" + attributes + ", description=" + description + "]";
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public EquinoxAttributeDefinition[] getAttributeDefinitions(int filter) {
        AttributeDefinitionSpecification[] retVal = null;

        switch (filter) {
            case REQUIRED:
                return getADs(true);
            case OPTIONAL:
                return getADs(false);
            case ALL:
            default:
                retVal = new AttributeDefinitionSpecification[attributes.values().size()];
                attributes.values().toArray(retVal);
                return retVal;
        }
    }

    /**
     * Helper method to filter between required and optional ADs
     * 
     * @param isRequired
     * @return
     */
    private AttributeDefinitionSpecification[] getADs(boolean isRequired) {
        AttributeDefinitionSpecification[] retVal = null;
        Vector<AttributeDefinitionSpecification> vector = new Vector<AttributeDefinitionSpecification>();
        for (Map.Entry<String, AttributeDefinitionSpecification> entry : attributes.entrySet()) {
            AttributeDefinitionSpecification ad = entry.getValue();
            if (isRequired == ad.isRequired()) {
                vector.add(ad);
            }
        }
        retVal = new AttributeDefinitionSpecification[vector.size()];
        vector.toArray(retVal);
        return retVal;
    }

    @Override
    public InputStream getIcon(int size) throws IOException {
        // not supported
        return null;
    }

    @Override
    public Map<String, String> getExtensionAttributes(String uri) {
        return extensionHelper.getExtensionAttributes(uri);
    }

    @Override
    public Set<String> getExtensionUris() {
        return extensionHelper.getExtensionUris();
    }

}