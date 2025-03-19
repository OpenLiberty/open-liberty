/*******************************************************************************
 * Copyright (c) 2015,2024 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.security.internal.jacc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EnterpriseBean;
import javax.security.auth.Subject;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditAuthResult;
import com.ibm.websphere.security.audit.AuditAuthenticationResult;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.security.audit.context.AuditManager;
import com.ibm.ws.ejbcontainer.EJBComponentMetaData;
import com.ibm.ws.ejbcontainer.EJBMethodMetaData;
import com.ibm.ws.ejbcontainer.EJBRequestData;
import com.ibm.ws.ejbcontainer.security.internal.EJBAccessDeniedException;
import com.ibm.ws.ejbcontainer.security.internal.EJBAuthorizationHelper;
import com.ibm.ws.ejbcontainer.security.internal.TraceConstants;
import com.ibm.ws.ejbcontainer.security.jacc.EJBJaccService;
import com.ibm.ws.security.audit.Audit;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Encapsulate jacc related methods which are consumed by EJBSecurityCollaborator.
 */
public class EJBJaccAuthorizationHelper implements EJBAuthorizationHelper {
    private static final TraceComponent tc = Tr.register(EJBJaccAuthorizationHelper.class);

    private AtomicServiceReference<EJBJaccService> jaccServiceRef = null;

    public EJBJaccAuthorizationHelper(AtomicServiceReference<EJBJaccService> jaccServiceRef) {
        this.jaccServiceRef = jaccServiceRef;
    }

    protected AuditManager auditManager;

    public void populateAuditEJBHashMap(EJBRequestData request, Map<String, Object> ejbAuditHashMap) {
        EJBMethodMetaData methodMetaData = request.getEJBMethodMetaData();
        Object[] methodArguments = request.getMethodArguments();
        String applicationName = methodMetaData.getEJBComponentMetaData().getJ2EEName().getApplication();
        String moduleName = methodMetaData.getEJBComponentMetaData().getJ2EEName().getModule();
        String methodName = methodMetaData.getMethodName();
        String methodInterface = methodMetaData.getEJBMethodInterface().specName();
        String methodSignature = methodMetaData.getMethodSignature();
        String beanName = methodMetaData.getEJBComponentMetaData().getJ2EEName().getComponent();
        List<Object> methodParameters = null;
        if (methodArguments != null && methodArguments.length > 0) {
            methodParameters = Arrays.asList(methodArguments);
        }

        ejbAuditHashMap.put("methodArguments", methodArguments);
        ejbAuditHashMap.put("applicationName", applicationName);
        ejbAuditHashMap.put("moduleName", moduleName);
        ejbAuditHashMap.put("methodName", methodName);
        ejbAuditHashMap.put("methodInterface", methodInterface);
        ejbAuditHashMap.put("methodSignature", methodSignature);
        ejbAuditHashMap.put("beanName", beanName);
        ejbAuditHashMap.put("methodParameters", methodParameters);

    }

    /**
     * Authorizes the subject to call the given EJB by using JACC, based on the given method info.
     * If the subject is not authorized, an exception is thrown. The following checks are made:
     * <li>is the bean method excluded (denyAll)</li>
     * <li>are the required roles null or empty</li>
     * <li>is EVERYONE granted to any of the required roles</li>
     * <li>is the subject authorized to any of the required roles</li>
     *
     * @param methodMetaData the info on the EJB method to call
     * @param subject        the subject authorize
     * @throws EJBAccessDeniedException when the subject is not authorized to the EJB
     */
    @Override
    public void authorizeEJB(EJBRequestData request, Subject subject) throws EJBAccessDeniedException {
        auditManager = new AuditManager();
        Object req = auditManager.getHttpServletRequest();
        Object webRequest = auditManager.getWebRequest();
        String realm = auditManager.getRealm();
        EJBMethodMetaData methodMetaData = request.getEJBMethodMetaData();
        Object[] methodArguments = request.getMethodArguments();
        String applicationName = methodMetaData.getEJBComponentMetaData().getJ2EEName().getApplication();
        String moduleName = methodMetaData.getEJBComponentMetaData().getJ2EEName().getModule();
        String methodName = methodMetaData.getMethodName();
        String methodInterface = methodMetaData.getEJBMethodInterface().specName();
        String methodSignature = methodMetaData.getMethodSignature();
        String beanName = methodMetaData.getEJBComponentMetaData().getJ2EEName().getComponent();
        List<Object> methodParameters = null;

        HashMap<String, Object> ejbAuditHashMap = new HashMap<String, Object>();

        populateAuditEJBHashMap(request, ejbAuditHashMap);

        Object bean = request.getBeanInstance();
        EnterpriseBean ejb = null;
        if (bean instanceof EnterpriseBean) {
            ejb = (EnterpriseBean) bean;
        }

        if (methodArguments != null && methodArguments.length > 0) {
            methodParameters = Arrays.asList(methodArguments);
        }

        boolean isAuthorized = jaccServiceRef.getService().isAuthorized(applicationName, moduleName, beanName, methodName, methodInterface, methodSignature, methodParameters, ejb,
                                                                        subject);
        String authzUserName = subject.getPrincipals(WSPrincipal.class).iterator().next().getName();

        if (!isAuthorized) {
            Tr.audit(tc, "EJB_JACC_AUTHZ_FAILED", authzUserName, methodName, applicationName);
            AuditAuthenticationResult auditAuthResult = new AuditAuthenticationResult(AuditAuthResult.FAILURE, authzUserName, AuditEvent.CRED_TYPE_BASIC, null, AuditEvent.OUTCOME_FAILURE);
            Audit.audit(Audit.EventID.SECURITY_AUTHZ_03, auditAuthResult, ejbAuditHashMap, req, webRequest, realm, subject, Integer.valueOf("403"));

            throw new EJBAccessDeniedException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                            TraceConstants.MESSAGE_BUNDLE,
                                                                            "EJB_JACC_AUTHZ_FAILED",
                                                                            new Object[] { authzUserName, methodName, applicationName },
                                                                            "CWWKS9406A: Authorization by the JACC provider failed. The user is not granted access to any of the required roles."));
        } else {
            AuditAuthenticationResult auditAuthResult = new AuditAuthenticationResult(AuditAuthResult.SUCCESS, authzUserName, AuditEvent.CRED_TYPE_BASIC, null, AuditEvent.OUTCOME_SUCCESS);
            Audit.audit(Audit.EventID.SECURITY_AUTHZ_03, auditAuthResult, ejbAuditHashMap, req, webRequest, realm, subject, Integer.valueOf("200"));
        }

    }

    @Override
    public boolean isCallerInRole(EJBComponentMetaData cmd, EJBRequestData request, String roleName, String roleLink, Subject subject) {
        // roleLink is not used.
        String applicationName = cmd.getJ2EEName().getApplication();
        String moduleName = cmd.getJ2EEName().getModule();
        String beanName = cmd.getJ2EEName().getComponent();
        String methodName = request.getEJBMethodMetaData().getMethodName();
        Object[] methodArguments = request.getMethodArguments();
        List<Object> methodParameters = null;
        if (methodArguments != null && methodArguments.length > 0) {
            methodParameters = Arrays.asList(methodArguments);
        }
        EnterpriseBean bean = null;
        if (request.getBeanInstance() instanceof EnterpriseBean) {
            bean = (EnterpriseBean) request.getBeanInstance();
        }
        return jaccServiceRef.getService().isSubjectInRole(applicationName, moduleName, beanName, methodName, methodParameters, roleName, bean, subject);
    }

}
