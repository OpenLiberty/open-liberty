/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.propagation;

import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.jaxrs20.client.MpJwtPropagation;

/*
 * This is a utility service to retrieve MicroProfile JsonWebToken in a subject
 */
@Component(service = MpJwtPropagation.class, name = "MpJwtPropagation", immediate = true, property = "service.vendor=IBM")
public class MpJwtPropagationImpl implements MpJwtPropagation {
    private static final TraceComponent tc = Tr.register(MpJwtPropagationImpl.class);

    @Activate
    protected void activate(ComponentContext cc) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "MpJwtPropagation service is being activated!!");
        }
    }

    @Modified
    protected void modified(Map<String, Object> props) {
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "MpJwtPropagation service is being deactivated!!");
        }
    }

    @Override
    public String getJsonWebTokenPrincipal(@Sensitive Subject subject) {
        if (subject != null) {
            Set<JsonWebToken> jsonWebTokenPrincipal = subject.getPrincipals(JsonWebToken.class);

            if (!jsonWebTokenPrincipal.isEmpty()) {
                JsonWebToken jwtPrincipal = jsonWebTokenPrincipal.iterator().next();
                if (jwtPrincipal != null) {
                    return jwtPrincipal.getRawToken();
                }
            }
        }

        return null;
    }

}
