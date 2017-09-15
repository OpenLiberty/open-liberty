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

public enum DDAttributeType {
    /**
     * This type can only be used with methods that return {@link Enum}. If a
     * value is not specified in XML, null is the default value.
     */
    Enum,

    /**
     * This type can only be used with methods that return boolean. false is
     * the default value.
     */
    Boolean,

    /**
     * This type can only be used with methods that return int. 0 is the
     * default value.
     */
    Int,

    /**
     * This type can only be used with methods that return long. 0 is the
     * default value.
     */
    Long,

    /**
     * This type can only be used with methods that return {@link String},
     * null is the default value.
     */
    String,

    /**
     * This type can only be used with methods that return {@link String}.
     * null is the default value.
     */
    ProtectedString,
}
