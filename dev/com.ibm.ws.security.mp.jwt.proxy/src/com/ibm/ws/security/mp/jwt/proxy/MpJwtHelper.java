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
package com.ibm.ws.security.mp.jwt.proxy;

import java.security.Principal;
import java.util.Hashtable;
import java.util.Map;

import javax.security.auth.Subject;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/*
 * This is a utility service for MicroProfile JsonWebToken in a subject
 */
@Component(service = MpJwtHelper.class, name = "MpJwtHelper", immediate = true, property = "service.vendor=IBM")
public class MpJwtHelper {
    private static final TraceComponent tc = Tr.register(MpJwtHelper.class);
    static final String JSON_WEB_TOKEN_UTIL_REF = "JsonWebTokenUtil";
    protected final static AtomicServiceReference<JsonWebTokenUtil> JsonWebTokenUtilRef = new AtomicServiceReference<JsonWebTokenUtil>(
            JSON_WEB_TOKEN_UTIL_REF);

    public MpJwtHelper() {
    }

    public static Principal getJsonWebTokenPricipal(Subject subject) {
        JsonWebTokenUtil jsonWebTokenUtil = getJsonWebTokenUtil();
        if (subject != null && jsonWebTokenUtil != null) {
            return jsonWebTokenUtil.getJsonWebTokenPrincipal(subject);
        }
        return null;
    }

    public static void addJsonWebToken(Subject subject, Hashtable<String, ?> customProperties, String key) {
        JsonWebTokenUtil jsonWebTokenUtil = getJsonWebTokenUtil();
        if (jsonWebTokenUtil != null && customProperties != null && subject != null) {
            jsonWebTokenUtil.addJsonWebToken(subject, customProperties, key);
        }
    }

    public static Principal cloneJsonWebToken(Subject subject) {
        JsonWebTokenUtil jsonWebTokenUtil = getJsonWebTokenUtil();
        if (subject != null && jsonWebTokenUtil != null) {
            return jsonWebTokenUtil.cloneJsonWebToken(subject);
        } else
            return null;
    }

    public static Principal getJsonWebToken(String jwt, String type, String username) {
        JsonWebTokenUtil jsonWebTokenUtil = getJsonWebTokenUtil();
        if (jsonWebTokenUtil != null) {
            return jsonWebTokenUtil.getJsonWebToken(jwt, type, username);
        }
        return null;
    }

    public static void addLoggedOutJwtToList(Principal p) {
        JsonWebTokenUtil jsonWebTokenUtil = getJsonWebTokenUtil();
        if (jsonWebTokenUtil != null) {
            jsonWebTokenUtil.addLoggedOutJwtToList(p);
        }
    }

    private static JsonWebTokenUtil getJsonWebTokenUtil() {
        return JsonWebTokenUtilRef.getService();
    }

    /**
     * answer if the mp-jwt feature behind this proxy is active or not. Since JsonWebTokens are part of that, if we can
     * get it, feature is active.
     */
    public static boolean isMpJwtFeatureActive() {
        return (JsonWebTokenUtilRef.getService() != null);
    }

    @Reference(service = JsonWebTokenUtil.class, name = JSON_WEB_TOKEN_UTIL_REF, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setJsonWebTokenUtil(ServiceReference<JsonWebTokenUtil> ref) {
        JsonWebTokenUtilRef.setReference(ref);
    }

    protected void unsetJsonWebTokenUtil(ServiceReference<JsonWebTokenUtil> ref) {
        JsonWebTokenUtilRef.unsetReference(ref);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        JsonWebTokenUtilRef.activate(cc);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "MpJwtHelper service is activated");
        }
    }

    @Modified
    protected void modified(Map<String, Object> props) {
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        JsonWebTokenUtilRef.deactivate(cc);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "MpJwtHelper service is activated");
        }
    }
}
