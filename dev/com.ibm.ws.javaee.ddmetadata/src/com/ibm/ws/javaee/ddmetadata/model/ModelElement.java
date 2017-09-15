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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The model for an XML element, which typically correlate to a single field
 * in an implementation class.
 */
public class ModelElement extends ModelNode {
    /**
     * If this element shares a method with other elements that share a common
     * supertype, this is the specific subtype for this element.
     */
    private final ModelInterfaceType choiceSubtype;

    /**
     * The non-null attribute if the field for the single attribute on this
     * element should be inclined into the outer class. This will only be
     * non-null if the element has no other attributes or child elements.
     */
    public ModelAttribute inlineAttribute;

    /**
     * True if the parent element in an XMI document does not contain this
     * element directly, but instead contains all the attributes and elements
     * of this element's type. The {@link #type} of this element must be a
     * ModelInterfaceType, and for convenience it is included here.
     */
    public ModelInterfaceType xmiFlattenType;

    /**
     * The set of types that can be used via {@code xmi:type}. If empty, the
     * return type of the method will be used.
     */
    public final List<ModelInterfaceType> xmiTypes = new ArrayList<ModelInterfaceType>();

    /**
     * If {@link #xmiTypes} is non-empty, the type to use instead of the return
     * type if {@code xmi:type} is not specified.
     */
    public ModelInterfaceType xmiDefaultType;

    public ModelElement(String name, ModelMethod method, boolean required, ModelInterfaceType choiceType) {
        super(name, method, required);
        this.choiceSubtype = choiceType;
    }

    public ModelType getType() {
        return choiceSubtype != null ? choiceSubtype : method.getType();
    }

    @Override
    public boolean hasXMIAttribute() {
        return false;
    }

    public void dump(PrintStream out, StringBuilder indent, Set<ModelInterfaceType> dumped) {
        ModelType type = getType();
        if (type instanceof ModelInterfaceType) {
            ModelInterfaceType interfaceType = (ModelInterfaceType) type;

            out.append(indent).print(this);
            if (!dumped.add(interfaceType)) {
                out.println(" ...");
                return;
            }

            out.println();
            indent.append(' ');

            for (ModelInterfaceType supertype : interfaceType.supertypes) {
                dumpSupertype(out, indent, supertype, dumped);
            }
            for (ModelAttribute attr : interfaceType.attributes) {
                out.append(indent).println(attr);
            }
            for (ModelElement element : interfaceType.elements) {
                element.dump(out, indent, dumped);
            }

            indent.setLength(indent.length() - 1);
        } else {
            out.append(indent).println(this);
        }
    }

    private void dumpSupertype(PrintStream out, StringBuilder indent, ModelInterfaceType type, Set<ModelInterfaceType> dumped) {
        out.append(indent).append("extends ").print(type.interfaceName);
        if (!dumped.add(type)) {
            out.println(" ...");
            return;
        }
        out.println();
        indent.append(' ');

        for (ModelInterfaceType supertype : type.supertypes) {
            dumpSupertype(out, indent, supertype, dumped);
        }
        for (ModelAttribute attr : type.attributes) {
            out.append(indent).println(attr);
        }
        for (ModelElement element : type.elements) {
            element.dump(out, indent, dumped);
        }

        indent.setLength(indent.length() - 1);
    }
}
