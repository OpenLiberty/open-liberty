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
package com.ibm.ws.microprofile.openapi.impl.core.jackson;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.media.XML;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.ibm.ws.microprofile.openapi.impl.model.media.SchemaImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.XMLImpl;

/**
 * The <code>JAXBAnnotationsHelper</code> class defines helper methods for
 * applying JAXB annotations to property definitions.
 */
class JAXBAnnotationsHelper {
    private static final String JAXB_DEFAULT = "##default";

    private JAXBAnnotationsHelper() {}

    /**
     * Applies annotations to property's {@link XML} definition.
     *
     * @param member annotations provider
     * @param property property instance to be updated
     */
    public static void apply(Annotated member, Schema property) {
        if (member.hasAnnotation(XmlElementWrapper.class) || member.hasAnnotation(XmlElement.class)) {
            applyElement(member, property);
        } else if (member.hasAnnotation(XmlAttribute.class) && isAttributeAllowed(property)) {
            applyAttribute(member, property);
        }
    }

    /**
     * Puts definitions for XML element.
     *
     * @param member annotations provider
     * @param property property instance to be updated
     */
    private static void applyElement(Annotated member, Schema property) {
        final XmlElementWrapper wrapper = member.getAnnotation(XmlElementWrapper.class);
        if (wrapper != null) {
            final XML xml = getXml(property);
            xml.setWrapped(true);
            // No need to set the xml name if the name provided by xmlelementwrapper annotation is ##default or equal to the property name | https://github.com/swagger-api/swagger-core/pull/2050
            if (!"##default".equals(wrapper.name()) && !wrapper.name().isEmpty() && !wrapper.name().equals(((SchemaImpl) property).getName())) {
                xml.setName(wrapper.name());
            }
        } else {
            final XmlElement element = member.getAnnotation(XmlElement.class);
            if (element != null) {
                setName(element.namespace(), element.name(), property);
            }
        }
    }

    /**
     * Puts definitions for XML attribute.
     *
     * @param member annotations provider
     * @param property property instance to be updated
     */
    private static void applyAttribute(Annotated member, Schema property) {
        final XmlAttribute attribute = member.getAnnotation(XmlAttribute.class);
        if (attribute != null) {
            final XML xml = getXml(property);
            xml.setAttribute(true);
            setName(attribute.namespace(), attribute.name(), property);
        }
    }

    private static XML getXml(Schema property) {
        final XML existing = property.getXml();
        if (existing != null) {
            return existing;
        }
        final XML created = new XMLImpl();
        property.setXml(created);
        return created;
    }

    /**
     * Puts name space and name for XML node or attribute.
     *
     * @param ns name space
     * @param name name
     * @param property property instance to be updated
     * @return <code>true</code> if name space and name have been set
     */
    private static boolean setName(String ns, String name, Schema property) {
        boolean apply = false;
        final String cleanName = StringUtils.trimToNull(name);
        final String useName;
        if (!isEmpty(cleanName) && !cleanName.equals(((SchemaImpl) property).getName())) {
            useName = cleanName;
            apply = true;
        } else {
            useName = null;
        }
        final String cleanNS = StringUtils.trimToNull(ns);
        final String useNS;
        if (!isEmpty(cleanNS)) {
            useNS = cleanNS;
            apply = true;
        } else {
            useNS = null;
        }
        // Set everything or nothing
        if (apply) {
            getXml(property).name(useName).namespace(useNS);
        }
        return apply;
    }

    /**
     * Checks whether the passed property can be represented as node attribute.
     *
     * @param property property instance to be checked
     * @return <code>true</code> if the passed property can be represented as
     *         node attribute
     */
    private static boolean isAttributeAllowed(Schema property) {

        if (property.getType() == SchemaType.ARRAY || property.getType() == SchemaType.OBJECT) {
            return false;
        }

        if (!StringUtils.isBlank(property.getRef())) {
            return false;
        }
        return true;
    }

    private static boolean isEmpty(String name) {
        return StringUtils.isEmpty(name) || JAXB_DEFAULT.equals(name);
    }
}
