/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.appsecurity.component;

import java.io.IOException;
import java.security.Principal;

import javax.annotation.Priority;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.osgi.collaborator.CollaboratorHelperImpl;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppSecurityCollaborator;

/**
 *
 */
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {
    private static final TraceComponent tc = Tr.register(AuthenticationFilter.class);

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.container.ContainerRequestFilter#filter(javax.ws.rs.container.ContainerRequestContext)
     */
    @Override
    public void filter(ContainerRequestContext crCtx) throws IOException {
        SecurityContext secCtx = crCtx.getSecurityContext();
        Principal p = secCtx.getUserPrincipal();
        if (p == null || "UNAUTHENTICATED".equals(p.getName())) {

            Message msg = JAXRSUtils.getCurrentMessage();
            HttpServletRequest req = (HttpServletRequest) msg.get(AbstractHTTPDestination.HTTP_REQUEST);
            HttpServletResponse resp = (HttpServletResponse) msg.get(AbstractHTTPDestination.HTTP_RESPONSE);
            IWebAppSecurityCollaborator collaborator = CollaboratorHelperImpl.getCurrentSecurityCollaborator();
            if (collaborator != null) {
                try {
                    boolean auth = collaborator.authenticate(req, resp);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "filter auth = " + auth);
                    }
                } catch (ServletException e) {
                    throw new IOException(e);
                }
            }
        }

    }

}
