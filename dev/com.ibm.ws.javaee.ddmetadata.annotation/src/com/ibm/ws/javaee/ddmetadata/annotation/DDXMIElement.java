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
 * Metadata for an XMI element. This can only be used with {@link DDElement}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface DDXMIElement {
    /**
     * The element name.
     */
    String name();

    /**
     * The list of allowed types that can be specified via xmi:type. The target
     * types must have the {@link DDXMIType} annotation. If xmi:type is not
     * specified, then {@link #defaultType} will be used, so this list is not
     * a complete list of the types that can be returned from the method.
     */
    Class<?>[] types() default {};

    /**
     * The subtype for this element if no xmi:type is specified and the default
     * type is different from the return type of the annotated method.
     */
    Class<?> defaultType() default Object.class;
}
