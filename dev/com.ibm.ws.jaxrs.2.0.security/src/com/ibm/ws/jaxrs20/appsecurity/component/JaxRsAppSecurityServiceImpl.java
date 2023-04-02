/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.appsecurity.component;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import javax.servlet.http.Cookie;
import javax.ws.rs.ProcessingException;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.security.web.WebSecurityHelper;
import com.ibm.ws.jaxrs20.api.JaxRsAppSecurityService;

@Component(name = "com.ibm.ws.jaxrs20.JaxRsAppSecurityServiceImpl", property = { "service.vendor=IBM" })
public class JaxRsAppSecurityServiceImpl implements JaxRsAppSecurityService {

    @Override
    public Cookie getSSOCookieFromSSOToken() {

        Cookie ssoCookie;
        try {
            ssoCookie = AccessController.doPrivileged(new PrivilegedExceptionAction<Cookie>() {

                @Override
                public Cookie run() throws Exception {
                    return WebSecurityHelper.getSSOCookieFromSSOToken();
                }
            });
        } catch (Exception e) {
            throw new ProcessingException(e);
        }

        return ssoCookie;
    }
}
