/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal;

import org.eclipse.equinox.metatype.EquinoxAttributeDefinition;

import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;

public class MockAttributeDefinition extends BaseDefinition implements EquinoxAttributeDefinition {

    private final int type;
    private int cardinality;
    private String[] defaultValue;
    private String[] optionValues;
    private String[] optionLabels;
    private String min;
    private String max;
    private String[] validate;
    private int validatePos;

    public MockAttributeDefinition(String id, int type) {
        this(id, type, 0, null);
    }

    public MockAttributeDefinition(String id, int type, int cardinality) {
        this(id, type, cardinality, null);
    }

    public MockAttributeDefinition(String id, int type, int cardinality, String[] defaultValue) {
        super(id);
        this.type = type;
        this.cardinality = cardinality;
        this.defaultValue = defaultValue;
    }

    @Override
    public int getType() {
        return type;
    }

    public void setCardinality(int cardinality) {
        this.cardinality = cardinality;
    }

    @Override
    public int getCardinality() {
        return cardinality;
    }

    public void setDefaultValue(String[] defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String[] getDefaultValue() {
        return defaultValue;
    }

    public void setOptions(String[] values, String[] labels) {
        this.optionValues = values;
        this.optionLabels = labels;
    }

    @Override
    public String[] getOptionLabels() {
        return optionLabels;
    }

    @Override
    public String[] getOptionValues() {
        return optionValues;
    }

    public void setValidate(String[] validate) {
        this.validate = validate;
        this.validatePos = 0;
    }

    @Override
    public String validate(String arg0) {
        if (validate != null) {
            int pos = validatePos++;
            String expected = validate[pos];
            String message = "validate(" + getID() + "[" + pos + "]): expected [" + expected + "], actual [" + arg0 + "]";

            System.out.println(message);
            if (!expected.equals(arg0)) {
                return message;
            }
        }
        return null;
    }

    public void setExtensionType(String type) {
        setExtensionAttribute(XMLConfigConstants.METATYPE_EXTENSION_URI, ExtendedAttributeDefinition.ATTRIBUTE_TYPE_NAME, type);
    }

    public void setReferencePid(String referencePid) {
        setExtensionType("pid");
        setExtensionAttribute(XMLConfigConstants.METATYPE_EXTENSION_URI, ExtendedAttributeDefinition.ATTRIBUTE_REFERENCE_NAME, referencePid);
    }

    public void setRequires(String id, boolean value) {
        String attr = (value) ? ExtendedAttributeDefinition.REQUIRES_TRUE_ATTR_NAME : ExtendedAttributeDefinition.REQUIRES_FALSE_ATTR_NAME;
        setExtensionAttribute(XMLConfigConstants.METATYPE_UI_EXTENSION_URI, attr, id);
    }

    public void setGroup(String groupName) {
        setExtensionAttribute(XMLConfigConstants.METATYPE_UI_EXTENSION_URI, ExtendedAttributeDefinition.GROUP_ATTR_NAME, groupName);
    }

    public void setVariable(String variableName) {
        setExtensionAttribute(XMLConfigConstants.METATYPE_EXTENSION_URI, ExtendedAttributeDefinition.VARIABLE_ATTR_NAME, variableName);
    }

    public void setObscured(String value) {
        setExtensionAttribute(XMLConfigConstants.METATYPE_EXTENSION_URI, ExtendedAttributeDefinition.OBSCURE_NAME, value);
    }

    public void setMax(String max) {
        this.max = max;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.equinox.metatype.EquinoxAttributeDefinition#getMax()
     */
    @Override
    public String getMax() {
        return max;
    }

    public void setMin(String min) {
        this.min = min;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.equinox.metatype.EquinoxAttributeDefinition#getMin()
     */
    @Override
    public String getMin() {
        return min;
    }

    /**
     * @param string
     */
    public void setService(String service) {
        setExtensionType("pid");
        setExtensionAttribute(XMLConfigConstants.METATYPE_EXTENSION_URI, ExtendedAttributeDefinition.SERVICE, service);
    }

    /**
     * @param string
     */
    public void setSubstitution(String value) {
        setExtensionAttribute(XMLConfigConstants.METATYPE_EXTENSION_URI, ExtendedAttributeDefinition.VARIABLE_SUBSTITUTION_NAME, value);
    }

}
