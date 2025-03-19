/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.request.probe.audit.rest.service.internal;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditConstants;
import io.openliberty.security.audit.source.rest.MemberManagementEvent;
import io.openliberty.security.audit.source.rest.RESTAuthorizationEvent;
import com.ibm.ws.security.audit.source.AuditServiceImpl;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.wsspi.security.audit.AuditService;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

import com.ibm.ws.request.probe.audit.servlet.AuditPERestService;

@Component(service = AuditPERestService.class, configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "service.vendor=IBM", immediate = true)

public class AuditPERestServiceImpl implements AuditPERestService {

    @Activate
    protected void activate(ComponentContext cc) {
    }

    @Override
    public void auditEventRESTAuthz(AuditService auditServiceRef, Object[] methodParams) {

        Object[] varargs = (Object[]) methodParams[1];
        Object req = varargs[0];
        Object response = varargs[1];
        int statusCode = (Integer) varargs[2];
        if (auditServiceRef != null && auditServiceRef
                .isAuditRequired(AuditConstants.SECURITY_REST_HANDLER_AUTHZ,
                        statusCode == 200 ? AuditConstants.SUCCESS : AuditConstants.FAILURE)) {
            RESTAuthorizationEvent av = new RESTAuthorizationEvent(req, response);
            auditServiceRef.sendEvent(av);
        }

    }

    @Override
    public void auditEventMemberMgmt01(AuditService auditServiceRef, Object[] methodParams) {
        Object[] varargs = (Object[]) methodParams[1];
        Object req = varargs[0];
        String action = (String) varargs[1];
        String repositoryId = (String) varargs[2];
        String uniqueName = (String) varargs[3];
        String realmName = (String) varargs[4];
        Object root = varargs[5];
        Integer statusCode = (Integer) varargs[6];
        String serviceType = null;
        if (varargs.length > 7) {
            serviceType = (String) varargs[7];
        }
        String outcome = statusCode.intValue() == 200 ? AuditConstants.SUCCESS : AuditConstants.FAILURE;

        if (auditServiceRef != null && auditServiceRef
                .isAuditRequired(AuditConstants.SECURITY_MEMBER_MGMT, outcome)) {
            MemberManagementEvent av;
            if (serviceType == null) {
                av = new MemberManagementEvent(req, action, repositoryId, uniqueName, realmName, root, statusCode);
            } else {
                av = new MemberManagementEvent(req, action, repositoryId, uniqueName, realmName, root, statusCode,
                        serviceType);
            }
            auditServiceRef.sendEvent(av);
        }
    }

}