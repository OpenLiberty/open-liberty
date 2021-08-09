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
 * Metadata for an XMI element that produces a value for the annotated method by
 * referring to an element in another document. The type of the other document
 * is specified by {@link DDXMIRootElement#refElementType}. This annotation can
 * only be used with {@link DDAttribute}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface DDXMIRefElement {
    /**
     * The element name.
     */
    String name();

    /**
     * The type of the element in the referenced document.
     */
    Class<?> referentType();

    /**
     * The method name on {@link #referentType} that produces the value.
     */
    String getter();
}
