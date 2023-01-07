/*******************************************************************************
 * Copyright (c) 2011,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.common;

/**
 * Represents all java:comp-related subelements from the
 * jndiEnvironmentRefsGroup XSD type.
 */
public interface JNDIEnvironmentRef {
    /**
     * @return the name relative to java:comp/env (&lt;env-entry-name>,
     *         &lt;ejb-ref-name&gt;, etc.)
     */
    String getName();
}
