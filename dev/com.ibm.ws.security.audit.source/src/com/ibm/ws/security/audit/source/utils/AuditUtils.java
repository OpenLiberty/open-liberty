/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
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
package com.ibm.ws.security.audit.source.utils;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.audit.AuditService;
import com.ibm.wsspi.security.registry.RegistryHelper;


/**
 * Various and sundry utility methods for auditing.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "service.vendor=IBM",
           immediate = true)
public class AuditUtils {

    private static final String AUDIT_SERVICE = "auditService";
    private static AtomicServiceReference<AuditService> auditServiceRef = new AtomicServiceReference<AuditService>(AUDIT_SERVICE);

    @Reference(name = AUDIT_SERVICE, service = AuditService.class)
    protected void setAuditService(ServiceReference<AuditService> ref) {
        auditServiceRef.setReference(ref);
    }

    protected void unsetAuditService(ServiceReference<AuditService> ref) {
        auditServiceRef.unsetReference(ref);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        auditServiceRef.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        auditServiceRef.deactivate(cc);
    }

    /**
     * Return the session id if the request has an HttpSession,
     * otherwise return null.
     *
     * @param req
     * @return session id or null
     */
    public static String getSessionID(HttpServletRequest req) {
        String sessionID = null;
        final HttpServletRequest f_req = req;

        try {
            sessionID = getSessionIDPrivileged(f_req);
        } catch (PrivilegedActionException | com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException e) {
            sessionID = handleUnauthorizedSessionRequest(f_req);
        }
        return sessionID;
    }

    /**
     * A helper method called from getSessionID(HttpServletRequest req) 
     * Calls getSession() with access control handling. 
     * 
     * @param req
     * @return sessionid or null 
     * @throws PrivilegedActionException
     */
    private static String getSessionIDPrivileged(HttpServletRequest req) throws PrivilegedActionException {
        return AccessController.doPrivileged((PrivilegedExceptionAction<String>) () -> {
            HttpSession session = req.getSession(false); // Ensure the audit code does not create a new session
            return (session != null) ? session.getId() : null;
        });
    }
    /**
     * 
     * A helper method called from getSessionID(HttpServletRequest req)
     * Handles exceptions 
     * 
     * @param req
     * @return sessionid, null or "UnauthorizedSessionRequest"
     */
    private static String handleUnauthorizedSessionRequest(HttpServletRequest req) {
        try {
            if (!req.isRequestedSessionIdFromCookie()) {
                return getPrivilegedSessionID(req, false); // Ensure the audit function does not create a new session
            } else {
                return getPrivilegedRequestedSessionID(req);
            }
        } catch (NullPointerException | com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException e) {
            return "UnauthorizedSessionRequest";
        }
    }

    /**
     * A helper method called from getSessionID(HttpServletRequest req)
     * Returns requested session id or null if a cookie includes requested session id 
     * 
     * @param req
     * @param createNew
     * @return requested session id or null 
     */
    private static String getPrivilegedSessionID(HttpServletRequest req, boolean createNew) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> {
            HttpSession session = req.getSession(createNew);
            return (session != null) ? session.getId() : null;
        });
    }

    /**
     * 
     * @param req
     * @return session id or null
     */
    private static String getPrivilegedRequestedSessionID(HttpServletRequest req) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> req.getRequestedSessionId());
    }

    /**
     * Get the scheme from the request - generally "HTTP" or HTTPS"
     *
     * @param req
     * @return the scheme
     */
    public static String getRequestScheme(HttpServletRequest req) {
        String scheme;
        if (req.getScheme() != null)
            scheme = req.getScheme().toUpperCase();
        else
            scheme = AuditEvent.REASON_TYPE_HTTP;
        return scheme;
    }

    /**
     * Get the unique identifier String of this server. The format is:
     * "websphere: hostName:userDir:serverName"
     *
     * @return the unique identifier of this server
     */
    public static String getServerID() {
        return auditServiceRef.getService().getServerID();
    }

    /**
     * Get the method from the request - generally "GET" or "POST"
     *
     * @param req
     * @return the method
     */
    public static String getRequestMethod(HttpServletRequest req) {
        String method;
        if (req.getMethod() != null)
            method = req.getMethod().toUpperCase();
        else
            method = AuditEvent.TARGET_METHOD_GET;
        return method;
    }

    /**
     * Hide the password
     *
     * @param string
     * @return string with any passwords obfuscated
     */
    public static String hidePassword(@Sensitive String s) {
        if ((s.indexOf("password") == -1) && (s.indexOf("PASSWORD") == -1)) {
            return s;
        } else {
            String ss = "";
            int indexLowerCase = s.indexOf("password");
            int indexUpperCase = s.indexOf("PASSWORD");

            if (indexLowerCase != -1) {
                ss = s.substring(0, indexLowerCase + 9);
                for (int index = (indexLowerCase + 10); index < s.length(); index++) {
                    ss = ss.concat("*");
                }
            } else if (indexUpperCase != -1) {
                ss = s.substring(0, indexUpperCase + 9);
                for (int index = (indexUpperCase + 10); index < s.length(); index++) {
                    ss = ss.concat("*");
                }

            }
            return ss;
        }
    }

    /*
     * Get the J2EE component name
     *
     */
    public static String getJ2EEComponentName() {
        ComponentMetaData wcmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (wcmd != null) {
            J2EEName j2eename = wcmd.getJ2EEName();
            return j2eename.getComponent();
        } else {
            return null;
        }
    }

    public static String getRealmName() {
        String realm = "defaultRealm";
        try {
            UserRegistry ur = RegistryHelper.getUserRegistry(null);
            if (ur != null) {
                String r = ur.getRealm();
                if (r != null) {
                    realm = r;
                }
            }
        } catch (Exception ex) {
        }
        return realm;
    }

}
