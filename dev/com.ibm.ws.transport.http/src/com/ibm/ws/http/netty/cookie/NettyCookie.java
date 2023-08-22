/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.cookie;

import com.ibm.wsspi.http.HttpCookie;

import io.netty.handler.codec.http.cookie.Cookie;

/**
 *
 */
public class NettyCookie extends HttpCookie implements Cookie {

    public NettyCookie(String name, String value) {
        super(name, value);
    }

    @Override
    public int compareTo(Cookie cookie) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String domain() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isHttpOnly() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSecure() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long maxAge() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String name() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String path() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setDomain(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setHttpOnly(boolean arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setMaxAge(long arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setPath(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setSecure(boolean arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setValue(String arg0) {

    }

    @Override
    public void setWrap(boolean arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public String value() {
        return getValue();
    }

    @Override
    public boolean wrap() {
        // TODO Auto-generated method stub
        return false;
    }

}
