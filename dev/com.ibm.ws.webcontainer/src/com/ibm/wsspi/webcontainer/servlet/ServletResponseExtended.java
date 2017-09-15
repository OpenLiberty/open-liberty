/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.servlet;

import java.util.Vector;

import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;

import com.ibm.websphere.servlet.response.IResponse;

/**
 *  RTC 160610. Adding extra methods to ServletResponse
 *  in order to use this interface instead of IExtendedResponse.
 */
public interface ServletResponseExtended extends ServletResponse {

    public IResponse getIResponse();

    public Vector[] getHeaderTable();

    public void addSessionCookie(Cookie cookie);

    public void setHeader(String name, String s, boolean checkInclude);

    public int getStatusCode();

}