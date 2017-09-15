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
package com.ibm.ws.javaee.ddmetadata.model;

/**
 * The modeled return type of an interface method.
 */
public interface ModelType {
    /**
     * The fully qualified method return type (e.g., "java.lang.String" or "int").
     */
    String getJavaTypeName();

    /**
     * The fully qualified implementation class name. This will be distinct
     * from {@link #getJavaTypeName} if this type represents an interface.
     */
    String getJavaImplTypeName();

    /**
     * The fully qualified implementation class name for a list.
     */
    String getJavaListImplTypeName();

    /**
     * The default value if the element or attribute is not specified.
     *
     * @param string the default value specified for the specific method
     */
    String getDefaultValue(String string);
}
