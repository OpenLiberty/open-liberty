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
package com.ibm.ws.javaee.ddmetadata.model;

/**
 * The model for an XML attribute, which typically correlate to a single field
 * in an implementation class.
 */
public class ModelAttribute extends ModelNode {
    /**
     * True if this attribute can be represented in an XMI documented with a
     * child element with an xsi:nil="true" attribute.
     */
    public boolean xmiNillable;

    public ModelAttribute(String name, ModelMethod method, boolean required) {
        super(name, method, required);
    }

    @Override
    public boolean hasXMIAttribute() {
        // An XML attribute that is obtained indirectly via an XMI reference
        // is an XMI element not an XMI attribute.
        return xmiName != null && method.xmiRefField == null;
    }
}
