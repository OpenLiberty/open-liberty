/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import com.ibm.websphere.config.ConfigEvaluatorException;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;

/**
 *
 */
class AttributeValidationException extends ConfigEvaluatorException {

    /**  */
    private static final long serialVersionUID = -8873485148740653410L;

    private final String validateResult;
    private final ExtendedAttributeDefinition attributeDefintion;
    private final String value;

    /**
     * @param attrDef
     * @param validateResult
     * @param validateResult2
     */
    public AttributeValidationException(ExtendedAttributeDefinition inAttrDef, String inValue, String inValidateResult) {
        super(inValidateResult);
        this.value = inValue;
        this.attributeDefintion = inAttrDef;
        this.validateResult = inValidateResult;
    }

    /**
     * @return the validateResult
     */
    public String getValidateResult() {
        return validateResult;
    }

    /**
     * @return the attributeDefintion
     */
    public ExtendedAttributeDefinition getAttributeDefintion() {
        return attributeDefintion;
    }

    /**
     * @return
     */
    public Object getValue() {
        return this.value;
    }

}
