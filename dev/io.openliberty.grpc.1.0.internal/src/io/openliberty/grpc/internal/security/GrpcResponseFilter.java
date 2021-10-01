/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.grpc.internal.security;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class GrpcResponseFilter implements Filter{

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        
        // We will need to override the response as 
        // com.ibm.ws.security.oauth20.util.OAuth20ProviderUtils.handleOAuthChallenge
        // gets a writer and commits the response
        GrpcSecurityServletResponseWrapper wrappedResponse = 
                new GrpcSecurityServletResponseWrapper((HttpServletResponse)res);
        
        
        chain.doFilter(req, wrappedResponse);
        
    }

}
