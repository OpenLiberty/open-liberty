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
 * Metadata for an XML attribute.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface DDAttribute {
    /**
     * The attribute name.
     */
    String name();

    /**
     * The intermediate element name if the attribute exists on a nested element
     * that is not mirrored in the interfaces.
     */
    String elementName() default "";

    /**
     * The attribute type.
     */
    DDAttributeType type();

    /**
     * True if the parser should fail if an attribute is not specified.
     */
    boolean required() default false;

    /**
     * The default value. If not specified, a type-specific default (typically
     * null) will be used.
     */
    String defaultValue() default "";
}
