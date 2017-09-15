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
package com.ibm.websphere.metatype;

/**
 *
 */
public class AttributeDefinitionProperties {

    private String copyOf;
    private int cardinality;
    private String[] defaultValue;
    private String description;
    private final String id;
    private String name;
    private String[] optionLabels;
    private String[] optionValues;
    private int type;
    private String referencePid;
    private String service;
    private String serviceFilter;
    private boolean isFinal;
    private String variable;
    private String uniqueCategory;
    private boolean flat;

    /**
     * @param string
     */
    public AttributeDefinitionProperties(String id) {
        this.id = id;
    }

    /**
     * @return the copyOf
     */
    public String getCopyOf() {
        return copyOf;
    }

    /**
     * @param copyOf the copyOf to set
     */
    public void setCopyOf(String copyOf) {
        this.copyOf = copyOf;
    }

    /**
     * @return the cardinality
     */
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
     * @return the description
     */
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
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the name
     */
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
     * @return the optionLabels
     */
    public String[] getOptionLabels() {
        return optionLabels;
    }

    /**
     * @param optionLabels the optionLabels to set
     */
    public void setOptionLabels(String[] optionLabels) {
        this.optionLabels = optionLabels;
    }

    /**
     * @return the optionValues
     */
    public String[] getOptionValues() {
        return optionValues;
    }

    /**
     * @param optionValues the optionValues to set
     */
    public void setOptionValues(String[] optionValues) {
        this.optionValues = optionValues;
    }

    /**
     * @return the type
     */
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
     * @return the referencePid
     */
    public String getReferencePid() {
        return referencePid;
    }

    /**
     * @param referencePid the referencePid to set
     */
    public void setReferencePid(String referencePid) {
        this.referencePid = referencePid;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getServiceFilter() {
        return serviceFilter;
    }

    public void setServiceFilter(String serviceFilter) {
        this.serviceFilter = serviceFilter;
    }

    /**
     * @return the isFinal
     */
    public boolean isFinal() {
        return isFinal;
    }

    /**
     * @param isFinal the isFinal to set
     */
    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    /**
     * @return the variable
     */
    public String getVariable() {
        return variable;
    }

    /**
     * @param variable the variable to set
     */
    public void setVariable(String variable) {
        this.variable = variable;
    }

    /**
     * @return the unique
     */
    public boolean isUnique() {
        return uniqueCategory != null;
    }

    public String getUnique() {
        return this.uniqueCategory;
    }

    /**
     * @param unique the unique to set
     */
    public void setUnique(String unique) {
        this.uniqueCategory = unique;
    }

    /**
     * @return the flat
     */
    public boolean isFlat() {
        return flat;
    }

    /**
     * @param flat the flat to set
     */
    public void setFlat(boolean flat) {
        this.flat = flat;
    }

}
