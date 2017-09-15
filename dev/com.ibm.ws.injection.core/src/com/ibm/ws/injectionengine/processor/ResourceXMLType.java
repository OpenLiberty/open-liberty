/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.processor;

/**
 * ResourceXMLType defines the various XML element types that correspond to the
 *
 * @Resource annotation. <p>
 *
 *           This enumeration is intended for use by the ResourceInjectionProcessor, to
 *           keep track of the original XML element type, as it converts XML to annotations.
 *           This will enable proper resolution of bindings, as well as error logging. <p>
 *
 *           The toString() method has been overriden, to return the XML syntax for the
 *           element. And, various xxx_element() methods have been provided for the
 *           common attributes, which may have different XML syntax. <p>
 **/
public enum ResourceXMLType
{
    UNKNOWN, // For use when not from XML (i.e. annotation)
    ENV_ENTRY, // env-entry
    RESOURCE_REF, // resource-ref
    RESOURCE_ENV_REF, // resource-env-ref
    MESSAGE_DESTINATION_REF; // message-destination-ref

    /**
     * Overriden so the exact XML syntax will appear in trace, exceptions,
     * and messages.
     **/
    public String toString()
    {
        if (this == ENV_ENTRY)
            return "env-entry";
        if (this == RESOURCE_REF)
            return "resource-ref";
        if (this == RESOURCE_ENV_REF)
            return "resource-env-ref";
        if (this == MESSAGE_DESTINATION_REF)
            return "message-destination-ref";

        return name(); // UNKNOWN
    }

    /**
     * Returns the XML element unique syntax for defining the name attribue.
     **/
    public String name_element()
    {
        if (this == RESOURCE_REF)
            return "res-ref-name";

        return this.toString() + "-name";
    }

    /**
     * Returns the XML element unique syntax for defining the type attribue.
     **/
    public String type_element()
    {
        if (this == RESOURCE_REF)
            return "res-type";
        if (this == MESSAGE_DESTINATION_REF)
            return "message-destination-type";

        return this.toString() + "-type";
    }
}
