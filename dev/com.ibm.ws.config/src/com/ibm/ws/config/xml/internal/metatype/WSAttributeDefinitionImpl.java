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
package com.ibm.ws.config.xml.internal.metatype;

import org.osgi.service.metatype.AttributeDefinition;

import com.ibm.websphere.metatype.AttributeDefinitionProperties;

/**
 *
 */
public class WSAttributeDefinitionImpl implements AttributeDefinition {

    private final AttributeDefinitionProperties properties;

    /**
     * @param props
     */
    public WSAttributeDefinitionImpl(AttributeDefinitionProperties props) {
        this.properties = props;
    }

    @Override
    public int getCardinality() {
        return properties.getCardinality();
    }

    @Override
    public String[] getDefaultValue() {
        return properties.getDefaultValue();
    }

    @Override
    public String getDescription() {
        return properties.getDescription();
    }

    @Override
    public String getID() {
        return properties.getId();
    }

    @Override
    public String getName() {
        return properties.getName();
    }

    @Override
    public String[] getOptionLabels() {
        return properties.getOptionLabels();
    }

    @Override
    public String[] getOptionValues() {
        return properties.getOptionValues();
    }

    @Override
    public int getType() {
        return properties.getType();
    }

    @Override
    public String validate(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public String getReferencePid() {
        return properties.getReferencePid();
    }

    public String getService() {
        return properties.getService();
    }

    public String getServiceFilter() {
        return properties.getServiceFilter();
    }

    public boolean isFinal() {
        return properties.isFinal();
    }

    public String getVariable() {
        return properties.getVariable();
    }

    public boolean isUnique() {
        return properties.isUnique();
    }

    public String getUnique() {
        return properties.getUnique();
    }

    public boolean isFlat() {
        return properties.isFlat();
    }

    public String getCopyOf() {
        return properties.getCopyOf();
    }

}
