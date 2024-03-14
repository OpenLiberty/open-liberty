package com.ibm.ws.request.probe.audit.rest.service;

import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.ws.security.audit.rest.MemberManagementEvent;
import com.ibm.ws.security.audit.rest.RESTAuthorizationEvent;
import javax.servlet.http.HttpServletResponse;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.audit.AuditService;

public class AuditPERestServiceImpl implements AuditPERestService {
    @Override
    public void auditEventRESTAuthz(AtomicServiceReference<AuditService> auditServiceRef, Object[] methodParams) {

		Object[] varargs = (Object[]) methodParams[1];
		Object req = varargs[0];
		Object response = varargs[1];
		int statusCode = (Integer) varargs[2];
		if (auditServiceRef.getService() != null && auditServiceRef.getService()
				.isAuditRequired(AuditConstants.SECURITY_REST_HANDLER_AUTHZ,
						statusCode == HttpServletResponse.SC_OK ? AuditConstants.SUCCESS : AuditConstants.FAILURE)) {
			RESTAuthorizationEvent av = new RESTAuthorizationEvent(req, response);
			auditServiceRef.getService().sendEvent(av);
		}

	}

    @Override
	public void auditEventMemberMgmt01(AtomicServiceReference<AuditService> auditServiceRef, Object[] methodParams) {
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

		if (auditServiceRef.getService() != null && auditServiceRef.getService()
				.isAuditRequired(AuditConstants.SECURITY_MEMBER_MGMT, outcome)) {
			MemberManagementEvent av;
			if (serviceType == null) {
				av = new MemberManagementEvent(req, action, repositoryId, uniqueName, realmName, root, statusCode);
			} else {
				av = new MemberManagementEvent(req, action, repositoryId, uniqueName, realmName, root, statusCode,
						serviceType);
			}
			auditServiceRef.getService().sendEvent(av);
		}
	}

}