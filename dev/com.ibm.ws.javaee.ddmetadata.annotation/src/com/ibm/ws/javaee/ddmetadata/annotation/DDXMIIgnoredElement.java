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
 * Metadata for an ignored element in an XMI document.
 * 
 * @see DDXMIElement
 */
@Target({})
public @interface DDXMIIgnoredElement {
    /**
     * The element name
     */
    String name();

    /**
     * True if the element can occur multiple times.
     */
    boolean list() default false;

    /**
     * The child attributes of the ignored element.
     */
    DDXMIIgnoredAttribute[] attributes() default {};

    /**
     * The child reference elements of the ignored element.
     */
    DDXMIIgnoredRefElement[] refElements() default {};
}
