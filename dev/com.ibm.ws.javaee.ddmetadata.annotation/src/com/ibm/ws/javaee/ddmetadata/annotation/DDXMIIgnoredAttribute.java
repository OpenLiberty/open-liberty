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

import java.lang.annotation.Target;

/**
 * Metadata for an ignored attribute in an XMI document.
 *
 * @see DDXMIAttribute
 */
@Target({})
public @interface DDXMIIgnoredAttribute {
    /**
     * The attribute name.
     */
    String name();

    /**
     * The attribute type.
     */
    DDAttributeType type();

    /**
     * The enumeration constants if type is {@link DDAttributeType#Enum}.
     */
    String[] enumConstants() default {};

    /**
     * True if an attribute can be represented as a nested element with the
     * xsi:nil="true" attribute.
     */
    boolean nillable() default false;
}
