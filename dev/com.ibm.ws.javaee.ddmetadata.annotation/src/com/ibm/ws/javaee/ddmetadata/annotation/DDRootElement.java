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
 * This annotation can be added to an interface to describe the root element
 * for an EE deployment descriptor, IBM XML binding file, or IBM XML extension
 * file. All methods in the interface must be annotated with one of:
 * <ul>
 * <li>{@link DDAttribute}</li>
 * <li>{@link DDElement}</li>
 * </ul>
 * 
 * A processor can use this metadata to generate implementation classes for the
 * interface and all directly or indirectly referenced interfaces.
 * Additionally, a process can generate a parser class for the document.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface DDRootElement {
    /**
     * The name of the root XML element (e.g., "web-app").
     */
    String name();

    /**
     * The versions of the document supported.
     */
    DDVersion[] versions();
}
