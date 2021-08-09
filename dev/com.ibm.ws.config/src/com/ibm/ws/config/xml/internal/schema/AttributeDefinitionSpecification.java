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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.metatype.EquinoxAttributeDefinition;
import org.eclipse.equinox.metatype.impl.ExtendableHelper;

/**
 * Custom AttributeDefinition implementation
 */
public class AttributeDefinitionSpecification implements EquinoxAttributeDefinition {

    private String id;
    private String name;
    private String altName;
    private String description;
    private int type;
    private int cardinality = 0;
    private String[] defaultValue;
    // The default value for 'required' is true in the metatype spec. 
    private boolean required = true;
    private List<String[]> valueOptions;
    private String minValue;
    private String maxValue;

    private ExtendableHelper extensionHelper;

    public AttributeDefinitionSpecification() {
        this.valueOptions = new ArrayList<String[]>(5);
    }

    public void setExtendedAttributes(ExtendableHelper helper) {
        extensionHelper = helper;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setMinValue(String minValue) {
        this.minValue = minValue;
    }

    public void setMaxValue(String maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * @return the name
     */
    @Override
    public String getName() {
        return name;
    }

    public String getAttributeName() {
        return altName;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public void setAttributeName(String name) {
        this.altName = name;
    }

    /**
     * @return the description
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the type
     */
    @Override
    public int getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * @return the cardinality
     */
    @Override
    public int getCardinality() {
        return cardinality;
    }

    /**
     * @param cardinality the cardinality to set
     */
    public void setCardinality(int cardinality) {
        this.cardinality = cardinality;
    }

    /**
     * @return the defaultValue
     */
    @Override
    public String[] getDefaultValue() {
        return defaultValue;
    }

    /**
     * @param defaultValue the defaultValue to set
     */
    public void setDefaultValue(String[] defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * @return the required
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * @param required the required to set
     */
    public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     * @return the valueOptions
     */
    public List<String[]> getValueOptions() {
        return valueOptions;
    }

    /**
     * @param valueOptions the valueOptions to set
     */
    public void setValueOptions(List<String[]> valueOptions) {
        this.valueOptions = valueOptions;
    }

    /**
     * @param option[0] = value
     *            option[1] = label
     *            option[2] = description
     */
    public void addValueOption(String[] option) {
        valueOptions.add(option);
    }

    /** debug info */
    @Override
    public String toString() {
        return "AttributeDefinitionSpecification [id=" + id + ", name=" + name + ", type="
               + type + ", cardinality=" + cardinality + ", description=" + description
               + ", required=" + required + ", valueOptions=" + valueOptions + "]";
    }

    /** return id */
    @Override
    public String getID() {
        return id;
    }

    /**
     * return only values from option
     */
    @Override
    public String[] getOptionValues() {
        String[] values = new String[valueOptions.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = valueOptions.get(i)[0];
        }
        return values;
    }

    /**
     * return only labels from option
     */
    @Override
    public String[] getOptionLabels() {
        String[] labels = new String[valueOptions.size()];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = valueOptions.get(i)[1];
        }
        return labels;
    }

    @Override
    public String validate(String value) {
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

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.equinox.metatype.EquinoxAttributeDefinition#getMax()
     */
    @Override
    public String getMax() {
        return maxValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.equinox.metatype.EquinoxAttributeDefinition#getMin()
     */
    @Override
    public String getMin() {
        return minValue;
    }
}
