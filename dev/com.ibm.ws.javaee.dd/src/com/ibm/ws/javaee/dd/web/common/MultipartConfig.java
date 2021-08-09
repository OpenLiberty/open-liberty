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
public interface MultipartConfig {

    /**
     * @return &lt;location>, or null if unspecified
     */
    String getLocation();

    /**
     * @return true if &lt;max-file-size> is specified
     * @see #getMaxFileSize
     */
    boolean isSetMaxFileSize();

    /**
     * @return &lt;max-file-size> if specified
     * @see #isSetMaxFileSize
     */
    long getMaxFileSize();

    /**
     * @return true if &lt;max-request-size> is specified
     * @see #getMaxRequestSize
     */
    boolean isSetMaxRequestSize();

    /**
     * @return &lt;max-request-size> if specified
     * @see #isSetMaxRequestSize
     */
    long getMaxRequestSize();

    /**
     * @return true if &lt;file-size-threshold> is specified
     * @see #getFileSizeThreshold
     */
    boolean isSetFileSizeThreshold();

    /**
     * @return &lt;file-size-threshold> if specified
     * @see #isSetFileSizeThreshold
     */
    int getFileSizeThreshold();

}
