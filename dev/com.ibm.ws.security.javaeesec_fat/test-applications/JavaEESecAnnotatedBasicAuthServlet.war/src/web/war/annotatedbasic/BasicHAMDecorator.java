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

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Decorator
@Priority(100)
public class BasicHAMDecorator implements HttpAuthenticationMechanism {
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(BasicHAMDecorator.class.getName());

    @Inject
    @Delegate
    private HttpAuthenticationMechanism delagateHAM;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response, HttpMessageContext httpMessageContext) throws AuthenticationException {
        HttpMessageContext httpContextWrapper = new BasicHAMMessageContextWrapper(response, httpMessageContext);
        response.addHeader("BasicHAMDecorator", "I have been decorated!");
        return delagateHAM.validateRequest(request, response, httpContextWrapper);
    }

}
