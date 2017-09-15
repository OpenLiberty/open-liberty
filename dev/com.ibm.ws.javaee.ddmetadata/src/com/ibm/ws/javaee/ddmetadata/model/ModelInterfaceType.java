/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmetadata.model;

import java.util.List;

/**
 * Models a Java interface type, which must annotate all its methods with
 * DDAttribute or DDElement.
 */
public class ModelInterfaceType implements ModelType {
    /**
     * The fully qualified class name of the interface this type represents, or
     * null if this is an anonymous type that is used only used internally by
     * its outer type.
     */
    public final String interfaceName;

    /**
     * The fully qualified implementation class name for this type.
     */
    public final String implClassName;

    /**
     * True if this type needs to support XMI.
     */
    public final boolean xmi;

    /**
     * The type names in xmi:type for the XMI element, or null if none.
     */
    public List<String> xmiTypes;

    /**
     * The namespace for the prefix in xmi:type for the XMI element.
     */
    public String xmiTypeNamespace;

    /**
     * The list of supertypes of the modeled interface.
     */
    public final List<ModelInterfaceType> supertypes;

    /**
     * True if this type implements DeploymentDescriptor.
     */
    public final boolean ddSupertype;

    /**
     * The fields for this type.
     */
    public final List<ModelField> fields;

    /**
     * The methods for this type.
     */
    public final List<ModelMethod> methods;

    /**
     * The attributes supported by elements of this type.
     */
    public final List<ModelAttribute> attributes;

    /**
     * True if this type supports the id attribute.
     */
    public final boolean idAttribute;

    /**
     * The child elements supported by elements of this type.
     */
    public final List<ModelElement> elements;

    /**
     * List of anonymous types created for this type.
     */
    public final List<ModelInterfaceType> anonymousTypes;

    /**
     * True if is DDXMIIgnoredElement
     */
    public boolean xmiIgnored = false;

    /**
     * The model if this type represents the root element. If non-null, then
     * this type object is the same as {@code rootElementModel.root.type}.
     */
    public Model rootElementModel;

    private boolean libertyNotInUse = false;

    private boolean libertyModule = false;

    public ModelInterfaceType(String interfaceName,
                              String implClassName,
                              List<ModelInterfaceType> supertypes,
                              boolean ddSupertype,
                              List<ModelField> fields,
                              List<ModelMethod> methods,
                              List<ModelAttribute> attributes,
                              boolean idAttribute,
                              List<ModelElement> elements,
                              List<ModelInterfaceType> anonymousTypes,
                              boolean xmi) {
        this.interfaceName = interfaceName;
        this.implClassName = implClassName;
        this.supertypes = supertypes;
        this.ddSupertype = ddSupertype;
        this.fields = fields;
        this.methods = methods;
        this.attributes = attributes;
        this.idAttribute = idAttribute;
        this.elements = elements;
        this.anonymousTypes = anonymousTypes;
        this.xmi = xmi;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + (interfaceName != null ? interfaceName : implClassName) + ']';
    }

    public ModelInterfaceType getExtendsSupertype() {
        return supertypes.isEmpty() ? null : supertypes.get(0);
    }

    public boolean isIdAllowed() {
        ModelInterfaceType extendsSupertype = getExtendsSupertype();
        return idAttribute || (extendsSupertype != null && extendsSupertype.isIdAllowed());
    }

    public boolean hasAttributes() {
        for (ModelAttribute attribute : attributes) {
            if (attribute.name != null || attribute.hasXMIAttribute()) {
                return true;
            }
        }

        for (ModelElement element : elements) {
            if (element.xmiFlattenType != null) {
                for (ModelAttribute attribute : element.xmiFlattenType.attributes) {
                    if (attribute.xmiName != null) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean hasRequiredNodes() {
        for (int whichNodes = 0; whichNodes < 2; whichNodes++) {
            for (ModelNode node : whichNodes == 0 ? attributes : elements) {
                if (node.required) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getJavaTypeName() {
        return interfaceName != null ? interfaceName : implClassName;
    }

    @Override
    public String getJavaImplTypeName() {
        return implClassName;
    }

    @Override
    public String getJavaListImplTypeName() {
        return "DDParser.ParsableListImplements<" + getJavaImplTypeName() + ", " + getJavaTypeName() + '>';
    }

    @Override
    public String getDefaultValue(String string) {
        if (string != null) {
            throw new IllegalArgumentException();
        }
        return null;
    }

    /**
     * @return
     */
    public boolean isLibertyNotInUse() {
        return this.libertyNotInUse;
    }

    public void setLibertyNotInUse(boolean b) {
        this.libertyNotInUse = b;
    }

    /**
     * Returns true if this type represents a module inside an application
     *
     * @return
     */
    public boolean isLibertyModule() {
        return this.libertyModule;
    }

    public void setLibertyModule(boolean b) {
        this.libertyModule = b;

    }
}
