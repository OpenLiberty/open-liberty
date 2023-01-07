/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
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

package com.ibm.ws.security.authentication.filter.internal;

public interface IRequestInfo {

    /**
     *
     */
    public String getHeader(String name);

    public String getRequestURL();

    public String getRemoteAddr();

    public String getApplicationName();

    public String getCookieName(String name);

}
