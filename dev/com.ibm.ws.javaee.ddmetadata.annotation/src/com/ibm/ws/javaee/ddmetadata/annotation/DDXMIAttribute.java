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
 * Metadata for an XMI attribute. This can only be used with {@link DDAttribute}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface DDXMIAttribute {
    /**
     * The attribute name.
     */
    String name();

    /**
     * True if an attribute can be represented as a nested element with the
     * xsi:nil="true" attribute.
     */
    boolean nillable() default false;

    /**
     * The intermediate element name if the attribute exists on a nested element
     * that is not mirrored in the interfaces. As an implementation restriction,
     * this can only be used if the corresponding {@link DDAttribute#elementName} also
     * specifies an intermediate element name.
     */
    String elementName() default "";

    /**
     * The type name in the xmi:type attribute that must be present on the
     * intermediate element. This must be used with {@link #elementXMITypeNamespace}.
     * As an implementation restriction, only one xmi:type can be specified.
     */
    String elementXMIType() default "";

    /**
     * The naamespace of the prefix in the xmi:type that must be present on the
     * intermediate element. This can only be used with {@link #elementXMIType}.
     */
    String elementXMITypeNamespace() default "";
}
