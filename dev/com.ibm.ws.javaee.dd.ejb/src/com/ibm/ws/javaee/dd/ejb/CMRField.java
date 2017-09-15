/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.ejb;

import com.ibm.ws.javaee.dd.common.Describable;

/**
 * Represents &lt;cmr-field>.
 */
public interface CMRField
                extends Describable
{
    /**
     * Represents an unspecified value for {@link #getTypeValue}.
     */
    int TYPE_UNSPECIFIED = -1;

    /**
     * Represents "java.util.Collection" for {@link #getTypeValue}.
     */
    int TYPE_JAVA_UTIL_COLLECTION = 0;

    /**
     * Represents "java.util.Set" for {@link #getTypeValue}.
     */
    int TYPE_JAVA_UTIL_SET = 1;

    /**
     * @return &lt;cmr-field-name>
     */
    String getName();

    /**
     * @return &lt;cmr-field-type>
     *         <ul>
     *         <li>{@link #TYPE_UNSPECIFIED} if unspecified
     *         <li>{@link #TYPE_JAVA_UTIL_COLLECTION} - java.util.Collection
     *         <li>{@link #TYPE_JAVA_UTIL_SET} - java.util.Set
     *         </ul>
     */
    int getTypeValue();
}
