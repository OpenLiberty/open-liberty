/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.audit.utils;

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
            sessionID = AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws Exception {
                    HttpSession session = f_req.getSession();
                    if (session != null) {
                        return session.getId();
                    } else {
                        return null;
                    }
                }
            });
        } catch (PrivilegedActionException e) {
            if ((e.getException()) instanceof com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException) {
                if (!req.isRequestedSessionIdFromCookie()) {
                    sessionID = AccessController.doPrivileged(new PrivilegedAction<String>() {
                        @Override
                        public String run() {
                            return f_req.getSession().getId();
                        }
                    });
                } else {
                    sessionID = AccessController.doPrivileged(new PrivilegedAction<String>() {
                        @Override
                        public String run() {
                            return f_req.getRequestedSessionId();
                        }
                    });
                }

            }
        } catch (com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException e) {
            try {
                if (!req.isRequestedSessionIdFromCookie()) {
                    sessionID = AccessController.doPrivileged(new PrivilegedAction<String>() {
                        @Override
                        public String run() {
                            return f_req.getSession().getId();
                        }
                    });
                } else {
                    sessionID = AccessController.doPrivileged(new PrivilegedAction<String>() {
                        @Override
                        public String run() {
                            return f_req.getRequestedSessionId();
                        }
                    });
                }

            } catch (java.lang.NullPointerException ee) {
                sessionID = "UnauthorizedSessionRequest";
            } catch (com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException ue) {
                sessionID = "UnauthorizedSessionRequest";
            }

        }
        return sessionID;
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
