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
package com.ibm.ws.javaee.ddmetadata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be added to an interface for a root element to indicate
 * that the document has a corresponding XMI format. The relevant methods in
 * the interface should be annotated with one of:
 * <ul>
 * <li>{@link DDXMIAttribute}</li>
 * <li>{@link DDXMIElement}</li>
 * <li>{@link DDXMIRefElement}</li>
 * <li>{@link DDXMIFlatten}</li>
 * </ul>
 * 
 * This can only be used with {@link DDRootElement}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface DDXMIRootElement {
    /**
     * The name of the root XMI element (e.g., "WebAppBinding").
     */
    String name();

    /**
     * The namespace of the root element (e.g., "webappbnd.xmi").
     */
    String namespace();

    /**
     * The document version. The value is document specified, but it should not
     * conflict with {@link DDVersion#version}. Typically, this value is less
     * than all other {@link DDVersion#version} values.
     */
    int version();

    /**
     * The type of the primary deployment descriptor that is used to determine
     * whether or not the XMI parser should be used.
     */
    Class<?> primaryDDType();

    /**
     * The versions of the primary deployment descriptor that use the XMI format.
     */
    String[] primaryDDVersions();

    /**
     * The name of the element that describes the primary deployment descriptor.
     * Binding and extension files always refer to a deployment descriptor, and
     * the XMI format makes this reference explicit with a reference element.
     */
    String refElementName();
}
