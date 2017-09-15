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
package com.ibm.ws.security.mp.jwt.impl.utils;

import java.security.Principal;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.mp.jwt.impl.DefaultJsonWebTokenImpl;
import com.ibm.ws.security.mp.jwt.proxy.JsonWebTokenUtil;

/**
 * The JsonWebTokenUtilImpl service only activate when running with JDK 8 and it's handler the JsonWebToken in subject
 */

@org.osgi.service.component.annotations.Component(service = JsonWebTokenUtil.class, name = "JsonWebTokenUtil", immediate = true, property = "service.vendor=IBM")
public class JsonWebTokenUtilImpl implements JsonWebTokenUtil {

    public static final TraceComponent tc = Tr.register(JsonWebTokenUtilImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    @Override
    public Principal getJsonWebTokenPrincipal(Subject subject) {
        String methodName = "getJsonWebTokenPrincipal";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, subject);
        }

        Set<JsonWebToken> jsonWebTokenPrincipal = subject.getPrincipals(JsonWebToken.class);

        if (jsonWebTokenPrincipal.size() > 1) {
            multipleJsonWebTokenPrincipalsError(jsonWebTokenPrincipal);
        }

        if (!jsonWebTokenPrincipal.isEmpty()) {
            Principal principal = jsonWebTokenPrincipal.iterator().next();
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, principal);
            }
            return principal;
        }

        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, null);
        }
        return null;
    }

    @Override
    public void addJsonWebToken(Subject subject, Hashtable<String, ?> customProperties, String key) {
        String methodName = "addJsonWebToken";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, subject, customProperties, key);
        }
        if (customProperties != null) {
            JsonWebToken jsonWebToken = (JsonWebToken) customProperties.get(key);
            if (jsonWebToken != null) {
                subject.getPrincipals().add(jsonWebToken);
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName);
        }
    }

    @Override
    public Principal cloneJsonWebToken(Subject subject) {
        String methodName = "cloneJsonWebToken";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, subject);
        }
        if (getJsonWebTokenPrincipal(subject) != null) {
            Principal p = ((DefaultJsonWebTokenImpl) getJsonWebTokenPrincipal(subject)).clone();
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, p);
            }
            return p;
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, null);
        }
        return null;
    }

    private void multipleJsonWebTokenPrincipalsError(Set<JsonWebToken> principals) {
        String principalNames = null;
        for (JsonWebToken principal : principals) {
            if (principalNames == null) {
                principalNames = principal.getName();
            } else {
                principalNames = principalNames + ", " + principal.getName();
            }
        }
        Tr.warning(tc, "TOO_MANY_JWT_PRINCIPALS", new Object[] { principalNames });
    }

    @Activate
    protected void activate(ComponentContext cc) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "JsonWebTokenUtil service is activated");
        }
    }

    @Modified
    protected void modified(Map<String, Object> props) {
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "JsonWebTokenUtil service is deactivated");
        }
    }
}
