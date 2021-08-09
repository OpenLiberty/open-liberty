/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.simplicity.config.wim;

import javax.xml.bind.annotation.XmlAttribute;

import com.ibm.websphere.simplicity.config.ConfigElement;

/**
 * Configuration for the following nested elements:
 *
 * <ul>
 * <li>federatedRepository --> primaryRealm --> groupDisplayNameMapping</li>
 * <li>federatedRepository --> primaryRealm --> groupSecurityNameMapping</li>
 * <li>federatedRepository --> primaryRealm --> uniqueGroupIdMapping</li>
 * <li>federatedRepository --> primaryRealm --> uniqueUserIdMapping</li>
 * <li>federatedRepository --> primaryRealm --> userSecurityNameMapping</li>
 * <li>federatedRepository --> primaryRealm --> userDisplayNameMapping</li>
 * <li>federatedRepository --> realm --> groupDisplayNameMapping</li>
 * <li>federatedRepository --> realm --> groupSecurityNameMapping</li>
 * <li>federatedRepository --> realm --> uniqueGroupIdMapping</li>
 * <li>federatedRepository --> realm --> uniqueUserIdMapping</li>
 * <li>federatedRepository --> realm --> userSecurityNameMapping</li>
 * <li>federatedRepository --> realm --> userDisplayNameMapping</li>
 * </ul>
 */
public class RealmPropertyMapping extends ConfigElement {

    private String inputProperty;
    private String outputProperty;

    public RealmPropertyMapping() {}

    public RealmPropertyMapping(String inputProperty, String outputProperty) {
        this.inputProperty = inputProperty;
        this.outputProperty = outputProperty;
    }

    /**
     * @return the inputProperty
     */
    public String getInputProperty() {
        return inputProperty;
    }

    /**
     * @return the outputProperty
     */
    public String getOutputProperty() {
        return outputProperty;
    }

    /**
     * @param inputProperty the inputProperty to set
     */
    @XmlAttribute(name = "inputProperty")
    public void setInputProperty(String inputProperty) {
        this.inputProperty = inputProperty;
    }

    /**
     * @param outputProperty the outputProperty to set
     */
    @XmlAttribute(name = "outputProperty")
    public void setOutputProperty(String outputProperty) {
        this.outputProperty = outputProperty;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (inputProperty != null) {
            sb.append("inputProperty=\"").append(inputProperty).append("\" ");;
        }
        if (outputProperty != null) {
            sb.append("outputProperty=\"").append(outputProperty).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}