/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package web.war.annotatedbasic;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContextWrapper;
import javax.servlet.http.HttpServletResponse;

@Default
@ApplicationScoped
public class BasicHAMMessageContextWrapper extends HttpMessageContextWrapper {
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(BasicHAMMessageContextWrapper.class.getName());

    private final HttpServletResponse response;

    public BasicHAMMessageContextWrapper(HttpServletResponse response, HttpMessageContext httpMessageContext) {
        super(httpMessageContext);
        this.response = response;
    }

    @Override
    public AuthenticationStatus responseUnauthorized() {
        response.addHeader("BasicHAMMessageContextWrapper", "I have been wrapped!");
        return super.responseUnauthorized();
    }

}
