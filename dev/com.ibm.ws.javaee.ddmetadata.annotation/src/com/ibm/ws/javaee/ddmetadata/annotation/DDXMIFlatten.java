/*******************************************************************************
 * Copyright (c) 2014,2020 IBM Corporation and others.
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
 * This annotation is used on a method returning an interface to indicate that
 * the XMI document does not have an intermediate element as in the XML
 * document, and instead the attributes and elements of the interface should be
 * "flattened" into the interface containing this annotation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface DDXMIFlatten {
    // EMPTY
}
