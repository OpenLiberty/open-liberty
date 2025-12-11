/*******************************************************************************
 * Copyright (c) 2015, 2024 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.security.jacc;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditAuthResult;
import com.ibm.websphere.security.audit.AuditAuthenticationResult;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.security.audit.Audit;
import com.ibm.ws.security.audit.utils.AuditConstants;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.WebAppAuthorizationHelper;
import com.ibm.ws.webcontainer.security.WebJaccService;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.internal.DenyReply;
import com.ibm.ws.webcontainer.security.internal.PermitReply;
import com.ibm.ws.webcontainer.security.internal.WebReply;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

public class WebAppJaccAuthorizationHelper implements WebAppAuthorizationHelper {
    private static final TraceComponent tc = Tr.register(WebAppJaccAuthorizationHelper.class);

    private AtomicServiceReference<WebJaccService> jaccServiceRef = null;
    private static final WebReply DENY_AUTHZ_FAILED = new DenyReply("AuthorizationFailed");

    public WebAppJaccAuthorizationHelper(AtomicServiceReference<WebJaccService> ref) {
        this.jaccServiceRef = ref;
    }

    @Override
    public boolean isUserInRole(String role, IExtendedRequest req, Subject subject) {
        String servletName = null;
        RequestProcessor reqProc = req.getWebAppDispatcherContext().getCurrentServletReference();
        if (reqProc != null) {
            servletName = reqProc.getName();
        }
        return jaccServiceRef.getService().isSubjectInRole(getApplicationName(), getModuleName(), servletName, role, req, subject);
    }

    /**
     * Call the JACC authorization service to determine if the subject is authorized
     *
     * @param authResult the authentication result, containing the subject, user name and realm
     * @param uriName the uri being accessed
     * @param previousCaller the previous caller, used to restore the previous state if authorization fails
     * @return true if the subject is authorized, otherwise false
     */

    @Override
    public boolean authorize(AuthenticationResult authResult, WebRequest webRequest, String uriName) {
        HttpServletRequest req = webRequest.getHttpServletRequest();
        boolean isAuthorized = jaccServiceRef.getService().isAuthorized(getApplicationName(), getModuleName(), uriName, req.getMethod(), req, authResult.getSubject());
        //String[] methodNameArray = new String[] { req.getMethod() };
        //WebResourcePermission webPerm = new WebResourcePermission(uriName, methodNameArray);
        WebReply reply = isAuthorized ? new PermitReply() : DENY_AUTHZ_FAILED;

        if (isAuthorized) {
            AuditAuthenticationResult auditAuthResult = new AuditAuthenticationResult(AuditAuthResult.SUCCESS, authResult.getSubject(), AuditEvent.CRED_TYPE_BASIC, null, AuditEvent.OUTCOME_SUCCESS);
            Audit.audit(Audit.EventID.SECURITY_AUTHZ_02, webRequest, authResult, uriName, AuditConstants.WEB_CONTAINER, Integer.valueOf(reply.getStatusCode()));
        } else {
            AuditAuthenticationResult auditAuthResult = new AuditAuthenticationResult(AuditAuthResult.FAILURE, authResult.getSubject(), AuditEvent.CRED_TYPE_BASIC, null, AuditEvent.OUTCOME_FAILURE);
            Audit.audit(Audit.EventID.SECURITY_AUTHZ_02, webRequest, authResult, uriName, AuditConstants.WEB_CONTAINER, Integer.valueOf(reply.getStatusCode()));

        }

        if (!isAuthorized) {
            String authUserName = authResult.getUserName();
            String authRealm = authResult.getRealm();
            String appName = webRequest.getApplicationName();
            if (authRealm != null && authUserName != null) {
                Tr.audit(tc, "SEC_JACC_AUTHZ_FAILED", authUserName.concat(":").concat(authRealm), appName, uriName);
            } else {
                // We have a subject if we got this far, use it to determine the name
                authUserName = authResult.getSubject().getPrincipals(WSPrincipal.class).iterator().next().getName();
                //WebReply reply = isAuthorized ? new PermitReply() : DENY_AUTHZ_FAILED;
                Tr.audit(tc, "SEC_JACC_AUTHZ_FAILED", authUserName, appName, uriName);
            }
            //WebReply reply = isAuthorized ? new PermitReply() : DENY_AUTHZ_FAILED;
            //Audit.audit(Audit.EventID.SECURITY_AUTHZ_01, webRequest, authResult, uriName, Integer.valueOf(reply.getStatusCode()));

        }
        return isAuthorized;
    }

    @Override
    public boolean isSSLRequired(WebRequest webRequest, String uriName) {
        HttpServletRequest req = webRequest.getHttpServletRequest();
        boolean isSSLRequired = false;
        if (!req.isSecure()) {
            isSSLRequired = jaccServiceRef.getService().isSSLRequired(getApplicationName(), getModuleName(), uriName, req.getMethod(), req);
        }
        return isSSLRequired;
    }

    /**
     * Access precluded explicitly specified in the security constraints or
     * by the usage of the com.ibm.ws.webcontainer.security.checkdefaultmethod attribute.
     * This attribute is set by the web container in ServletWrapper.startRequest() with the
     * intent of blocking hackers from crafting an HTTP HEAD method to bypass security constraints.
     * See PK83258 for full details. <--- ToDo
     * This method also checks for uncovered HTTP methods
     */
    @Override
    public WebReply checkPrecludedAccess(WebRequest webRequest, String uriName) {
        WebReply webReply = null;
        /*
         * In order to check precluded access, WebUserDataPermission can be used,
         * since it is created for a excluded permission if there is no role assigned.
         * That can be used to do a check to return 403 right away instead of prompting
         * or switching protocol from tcp to ssl, and then throwing 403.
         */
        HttpServletRequest req = webRequest.getHttpServletRequest();
        boolean isExcluded = false;
        isExcluded = jaccServiceRef.getService().isAccessExcluded(getApplicationName(), getModuleName(), uriName, req.getMethod(), req);
        if (isExcluded) {
            webReply = new DenyReply("JACC provider denied the access.");
        }
        return webReply;
    }

    protected String getApplicationName() {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        WebModuleMetaData wmmd = (WebModuleMetaData) ((WebComponentMetaData) cmd).getModuleMetaData();
        return wmmd.getConfiguration().getApplicationName();
    }

    protected String getModuleName() {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        WebModuleMetaData wmmd = (WebModuleMetaData) ((WebComponentMetaData) cmd).getModuleMetaData();
        return wmmd.getConfiguration().getModuleName();
    }

}
