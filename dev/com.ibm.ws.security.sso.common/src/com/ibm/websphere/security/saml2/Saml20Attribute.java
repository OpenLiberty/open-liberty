/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.saml2;

import java.io.Serializable;
import java.util.List;

import javax.xml.namespace.QName;

/**
 *
 */
public interface Saml20Attribute extends Serializable {
    /*
     * return a list of Attribute namespace
     */
    public List<QName> getNameSpaces();

    /*
     * return attribute name
     */
    public String getName();

    /*
     * return attribute name format
     */
    public String getNameFormat();

    /*
     * return attribute's friendly name
     */
    public String getFriendlyName();

    /*
     * return attribute value schema type
     */
    public QName getSchemaType();

    /*
     * return a list of attribute values
     */
    public List<String> getValuesAsString();

    /*
     * return a list of serialized attribute values
     */
    public List<String> getSerializedValues();

    /*
     * return serialized attribute
     */
    public String getSerializedAttribute();

}
