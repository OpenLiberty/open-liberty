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
package com.ibm.ws.javaee.dd.web.common;

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

}
