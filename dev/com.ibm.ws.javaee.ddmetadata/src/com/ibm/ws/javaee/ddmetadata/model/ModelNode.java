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
 * A child element or attribute of an interface type.
 */
public abstract class ModelNode {
    /**
     * The method for this element or attribute. A method can be shared by
     * several nodes (for example, the session and message-driven elements both
     * share the same getEnterpriseBeans method).
     */
    public final ModelMethod method;

    /**
     * The element or attribute name, or null if this element or attribute only
     * exists in an XMI document.
     */
    public final String name;

    /**
     * True if the parser should fail if this element or attribute is not specified.
     */
    public final boolean required;

    /**
     * The element or attribute name in an XMI document. If {@link #xmiRefReferentTypeName} is
     * non-null, then this is an element name in an XMI document even if this is
     * a {@link ModelAttribute} (see {@link ModelAttribute#hasXMIAttribute}).
     */
    public String xmiName;
    
    /**
     * Represents whether this element or attribute is in use by the Liberty runtime or not
     */
    private boolean libertyNotInUse = false;

    ModelNode(String name, ModelMethod method, boolean required) {
        this.method = method;
        this.name = name;
        this.required = required;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(getClass().getSimpleName()).append('[');
        if (name != null) {
            b.append(name);
            if (xmiName != null) {
                b.append(' ');
            }
        }
        if (xmiName != null) {
            b.append("(XMI ");
            if (this instanceof ModelAttribute && !hasXMIAttribute()) {
                b.append("element ");
            }
            b.append(xmiName);
            b.append(')');
        }

        b.append(", ");
        if (method.isList()) {
            b.append("List ");
        }
        b.append(method.getType());
        if (method.name != null) {
            b.append(' ').append(method.name);
        }

        return b.append(']').toString();
    }

    /**
     * True if this node is represented as an attribute in an XMI document.
     */
    public abstract boolean hasXMIAttribute();
    
    /**
     * Returns true if the value of this element is not used by the Liberty runtime
     */
    public void setLibertyNotInUse(boolean b) {
        this.libertyNotInUse = b;
    }

    public boolean isLibertyNotInUse() {
        return this.libertyNotInUse;
    }
}
