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
package com.ibm.ws.javaee.dd.web.common;

import java.util.List;

/**
 *
 */
public interface CookieConfig {

    /**
     * @return &lt;name>, or null if unspecified
     */
    String getName();

    /**
     * @return &lt;domain>, or null if unspecified
     */
    String getDomain();

    /**
     * @return &lt;path>, or null if unspecified
     */
    String getPath();

    /**
     * @return &lt;comment>, or null if unspecified
     */
    String getComment();

    /**
     * @return true if &lt;http-only> is specified
     * @see #isHTTPOnly
     */
    boolean isSetHTTPOnly();

    /**
     * @return &lt;http-only> if specified
     * @see #isSetHTTPOnly
     */
    boolean isHTTPOnly();

    /**
     * @return true if &lt;secure> is specified
     * @see #isSecure
     */
    boolean isSetSecure();

    /**
     * @return &lt;secure> if specified
     * @see #isSetSecure
     */
    boolean isSecure();

    /**
     * @return true if &lt;max-age> is specified
     * @see #getMaxAge
     */
    boolean isSetMaxAge();

    /**
     * @return &lt;max-age> if specified
     * @see #isSetMaxAge
     */
    int getMaxAge();

    /**
     * @return &lt;attribute&gt; elements
     */
    List<AttributeValue> getAttributes();
}
