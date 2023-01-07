/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
package com.ibm.ws.security.oauth20.filter;

public interface IRequestInfo {

    /**
     * 
     */
    public String getHeader(String name);

    public StringBuffer getRequestURL();

    public String getQueryString();

    public String getRemoteAddr();

    public String getApplicationName();

    public String getReferer();

    public String getRequestURI();
}
