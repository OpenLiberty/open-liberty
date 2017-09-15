/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.common;

/**
 * Represents all java:comp-related subelements from the
 * jndiEnvironmentRefsGroup XSD type.
 */
public interface JNDIEnvironmentRef
{
    /**
     * @return the name relative to java:comp/env (&lt;env-entry-name>,
     *         &lt;ejb-ref-name>, etc.)
     */
    String getName();
}
