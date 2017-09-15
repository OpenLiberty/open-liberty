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
public interface ErrorPage {

    /**
     * @return true if &lt;error-code> is specified
     * @see #getErrorCode
     */
    boolean isSetErrorCode();

    /**
     * @return &lt;error-code> if specified
     * @see #isSetErrorCode
     */
    int getErrorCode();

    /**
     * @return &lt;exception-type>, or null if unspecified
     */
    String getExceptionType();

    /**
     * @return &lt;location>
     */
    String getLocation();

}
