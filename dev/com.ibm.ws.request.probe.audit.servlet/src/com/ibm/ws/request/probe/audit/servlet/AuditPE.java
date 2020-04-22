/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.probe.audit.servlet;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.NotificationFilter;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditAuthenticationResult;
import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.security.audit.context.AuditManager;
import com.ibm.ws.security.audit.Audit;
import com.ibm.ws.security.audit.event.ApiAuthnEvent;
import com.ibm.ws.security.audit.event.ApiAuthnTerminateEvent;
import com.ibm.ws.security.audit.event.ApplicationPasswordTokenEvent;
import com.ibm.ws.security.audit.event.AuditMgmtEvent;
import com.ibm.ws.security.audit.event.AuthenticationDelegationEvent;
import com.ibm.ws.security.audit.event.AuthenticationEvent;
import com.ibm.ws.security.audit.event.AuthenticationFailoverEvent;
import com.ibm.ws.security.audit.event.AuthenticationTerminateEvent;
import com.ibm.ws.security.audit.event.AuthorizationDelegationEvent;
import com.ibm.ws.security.audit.event.AuthorizationEvent;
import com.ibm.ws.security.audit.event.EJBAuthorizationEvent;
import com.ibm.ws.security.audit.event.JACCEJBAuthorizationEvent;
import com.ibm.ws.security.audit.event.JACCWebAuthorizationEvent;
import com.ibm.ws.security.audit.event.JMSAuthenticationEvent;
import com.ibm.ws.security.audit.event.JMSAuthenticationTerminateEvent;
import com.ibm.ws.security.audit.event.JMSAuthorizationEvent;
import com.ibm.ws.security.audit.event.JMXMBeanAttributeEvent;
import com.ibm.ws.security.audit.event.JMXMBeanEvent;
import com.ibm.ws.security.audit.event.JMXMBeanRegisterEvent;
import com.ibm.ws.security.audit.event.JMXNotificationEvent;
import com.ibm.ws.security.audit.event.MemberManagementEvent;
import com.ibm.ws.security.audit.event.RESTAuthorizationEvent;
import com.ibm.ws.security.audit.event.SAFAuthorizationDetailsEvent;
import com.ibm.ws.security.audit.event.SAFAuthorizationEvent;
//import com.ibm.ws.security.audit.utils.AuditConstants;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.probeExtension.ProbeExtension;
import com.ibm.wsspi.requestContext.Event;
import com.ibm.wsspi.requestContext.RequestContext;
import com.ibm.wsspi.security.audit.AuditService;

/**
 *
 */
@Component(service = { ProbeExtension.class }, 
           name = "com.ibm.ws.security.audit.pe", 
           configurationPolicy = ConfigurationPolicy.IGNORE, 
           property = "service.vendor=IBM", 
           immediate = true)
public class AuditPE implements ProbeExtension {

	private static final TraceComponent tc = Tr.register(AuditPE.class, "requestProbe",
			"com.ibm.ws.request.probe.internal.resources.LoggingMessages");

	private static final String requestProbeType = "websphere.security.audit.test";
	private static final String KEY_AUDIT_SERVICE = "auditService";
	protected final AtomicServiceReference<AuditService> auditServiceRef = new AtomicServiceReference<AuditService>(
			KEY_AUDIT_SERVICE);

	@Activate
	protected void activate(ComponentContext cc) {
		auditServiceRef.activate(cc);
	}

	@Deactivate
	protected void deactivate(ComponentContext cc) {
		auditServiceRef.deactivate(cc);
	}

	@Reference(service = AuditService.class, 
			   name = KEY_AUDIT_SERVICE)
	protected void setAuditService(ServiceReference<AuditService> reference) {
		auditServiceRef.setReference(reference);
	}

	protected void unsetAuditService(ServiceReference<AuditService> reference) {
		auditServiceRef.unsetReference(reference);
	}

	static ArrayList<String> events = new ArrayList<String>();
	static {
		events.add(requestProbeType);
	}

	/** {@inheritDoc} */
	@Override
	public int getRequestSampleRate() {
		return 1;
	}

	/** {@inheritDoc} */
	@Override
	public boolean invokeForRootEventsOnly() {
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public boolean invokeForEventEntry() {
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public boolean invokeForEventExit() {
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public boolean invokeForCounter() {
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public List<String> invokeForEventTypes() {
		return events;
	}

	/** {@inheritDoc} */
	@Override
	public int getContextInfoRequirement() {
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	public void processEntryEvent(Event event, RequestContext rc) {	}

	/** {@inheritDoc} */
	@Override
	public void processExitEvent(Event event, RequestContext rc) {
	}

	/** {@inheritDoc} */
	@Override
	// @FFDCIgnore(ClassCastException.class)
	public void processCounter(Event event) {
        Object[] methodParams = (Object[]) event.getContextInfo();
        if (methodParams != null && methodParams.length > 0) {

			if ((methodParams[0].toString()).equals("JMX_NOTIFICATION_01")) {
				auditEventJMXNotification01(methodParams);
			} else if ((methodParams[0].toString()).equals("JMX_MBEAN_01")) {
				auditEventJMXMBean01(methodParams);
			} else if ((methodParams[0].toString()).equals("JMX_MBEAN_ATTRIBUTES_01")) {
				auditEventJMXMBeanAttributes01(methodParams);
			} else if ((methodParams[0].toString()).equals("JMX_MBEAN_REGISTER_01")) {
				auditEventJMXMBeanRegister01(methodParams);
			} else {
				switch ((Audit.EventID) methodParams[0]) {
				case SECURITY_AUTHN_01:
					auditEventAuthn01(methodParams);
					break;
				case SECURITY_AUTHZ_01:
					auditEventAuthz01(methodParams);
					break;
				case SECURITY_AUTHZ_02:
					auditEventAuthz02(methodParams);
					break;
				case SECURITY_AUTHZ_03:
					auditEventAuthz03(methodParams);
					break;
				case SECURITY_AUTHZ_04:
					auditEventAuthz04(methodParams);
					break;
				case SECURITY_AUDIT_MGMT_01:
					auditEventAuditMgmt01(methodParams);
					break;
				case SECURITY_AUDIT_MGMT_02:
					auditEventAuditMgmt02(methodParams);
					break;
				case SECURITY_AUTHN_DELEGATION_01:
					auditEventAuthnDelegation01(methodParams);
					break;
				case SECURITY_AUTHZ_DELEGATION_01:
					auditEventAuthzDelegation01(methodParams);
					break;
				case SECURITY_API_AUTHN_01:
					auditEventApiAuthn01(methodParams);
					break;
				case SECURITY_API_AUTHN_TERMINATE_01:
					auditEventApiAuthnTerminate01(methodParams);
					break;
				case SECURITY_AUTHN_TERMINATE_01:
					auditEventAuthnTerminate01(methodParams);
					break;
				case SECURITY_AUTHN_FAILOVER_01:
					auditEventAuthnFailover01(methodParams);
					break;
				case SECURITY_MEMBER_MGMT_01:
					auditEventMemberMgmt01(methodParams);
					break;
				case SECURITY_JMS_AUTHN_01:
					auditEventJMSAuthn01(methodParams);
					break;
				case SECURITY_JMS_AUTHZ_01:
					auditEventJMSAuthz01(methodParams);
					break;
				case SECURITY_JMS_AUTHN_TERMINATE_01:
					auditEventJMSAuthnTerm01(methodParams);
					break;
				case SECURITY_SAF_AUTHZ_DETAILS:
					auditEventSafAuthDetails(methodParams);
					break;
				case APPLICATION_PASSWORD_TOKEN_01:
					auditEventApplicationPasswordToken(methodParams);
					break;
				case SECURITY_SAF_AUTHZ:
					auditEventSafAuth(methodParams);
					break;
				case SECURITY_REST_HANDLER_AUTHZ:
					auditEventRESTAuthz(methodParams);
					break;
				default:
					// TODO: emit error message
					break;
				}
			}
        }
	}

	private void auditEventAuthn01(Object[] methodParams) {
		Object[] varargs = (Object[]) methodParams[1];
		WebRequest webRequest = (WebRequest) varargs[0];
		AuthenticationResult authResult = (AuthenticationResult) varargs[1];
		Integer statusCode = (Integer) varargs[2];
		if (auditServiceRef.getService() != null
				&& auditServiceRef.getService()
						.isAuditRequired(
						AuditConstants.SECURITY_AUTHN,
						authResult.getAuditOutcome())) {
			AuthenticationEvent av =
					new AuthenticationEvent(webRequest, authResult, statusCode);
			auditServiceRef.getService().sendEvent(av);
		}
		
		try {
			/*
			 * Store user and webrequest information for possible VMM related
			 * audits down the thread
			 */
			if (auditServiceRef.getService() != null && (auditServiceRef.getService()
					.isAuditRequired(AuditConstants.SECURITY_MEMBER_MGMT, AuditConstants.SUCCESS)
					|| auditServiceRef.getService().isAuditRequired(AuditConstants.SECURITY_MEMBER_MGMT,
							AuditConstants.FAILURE))) {
				AuditManager auditManager = new AuditManager();
				if (auditManager.getWebRequest() == null) {
					auditManager.setWebRequest(webRequest);
				}
				if (auditManager.getCredentialType() == null && authResult != null) {
					auditManager.setCredentialType(authResult.getAuditCredType());
				}

				boolean userSet = false;
				if (auditManager.getCredentialUser() == null && authResult != null
						&& authResult.getAuditCredValue() != null) {
					auditManager.setCredentialUser(authResult.getAuditCredValue());
					userSet = true;
				}

				if (webRequest != null) {
					HttpServletRequest req = webRequest.getHttpServletRequest();
					if (req != null) {
						if (!userSet && req.getUserPrincipal() != null && req.getUserPrincipal().getName() != null) {
							auditManager.setCredentialUser(req.getUserPrincipal().getName());
						}
						auditManager.setRemoteAddr(req.getRemoteAddr());
						auditManager.setAgent(req.getHeader("User-Agent"));
						auditManager.setLocalAddr(req.getLocalAddr());
						auditManager.setLocalPort(String.valueOf(req.getLocalPort()));
						String sessionID = null;
						final HttpServletRequest f_req = req;
						sessionID = AccessController.doPrivileged(new PrivilegedAction<String>() {
							@Override
							public String run() {
								HttpSession session = f_req.getSession();
								if (session != null) {
									return session.getId();
								} else {
									return null;
								}
							}
						});
						if (sessionID != null) {
							auditManager.setSessionId(sessionID);
						}
						auditManager.setHttpType(
								req.getScheme() != null ? req.getScheme().toUpperCase() : AuditEvent.REASON_TYPE_HTTP);
					}

				}
			}
		} catch (Exception e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Exception occurred trying to save off information for VMM audits", e);
			}
		}
	}

	private void auditEventAuthnTerminate01(Object[] methodParams) {
		Object[] varargs = (Object[]) methodParams[1];
		HttpServletRequest req = (HttpServletRequest) varargs[0];
		AuthenticationResult authResult = (AuthenticationResult) varargs[1];
		Integer statusCode = (Integer) varargs[2];
		if (auditServiceRef.getService() != null && auditServiceRef.getService()
				.isAuditRequired(AuditConstants.SECURITY_AUTHN_TERMINATE, authResult.getAuditOutcome())) {
			AuthenticationTerminateEvent av = new AuthenticationTerminateEvent(req, authResult, statusCode);
			auditServiceRef.getService().sendEvent(av);
		}
	}

	private void auditEventApiAuthn01(Object[] methodParams) {
		Object[] varargs = (Object[]) methodParams[1];
		HttpServletRequest req = (HttpServletRequest) varargs[0];
		AuthenticationResult authResult = (AuthenticationResult) varargs[1];
		Integer statusCode = (Integer) varargs[2];
		if (auditServiceRef.getService() != null && auditServiceRef.getService()
				.isAuditRequired(AuditConstants.SECURITY_API_AUTHN, authResult.getAuditOutcome())) {
			ApiAuthnEvent av = new ApiAuthnEvent(req, authResult, statusCode);
			auditServiceRef.getService().sendEvent(av);
		}
	}

	private void auditEventApiAuthnTerminate01(Object[] methodParams) {
		Object[] varargs = (Object[]) methodParams[1];
		HttpServletRequest req = (HttpServletRequest) varargs[0];
		AuthenticationResult authResult = (AuthenticationResult) varargs[1];
		Integer statusCode = (Integer) varargs[2];
		if (auditServiceRef.getService() != null && auditServiceRef.getService()
				.isAuditRequired(AuditConstants.SECURITY_API_AUTHN_TERMINATE, authResult.getAuditOutcome())) {
			ApiAuthnTerminateEvent av = new ApiAuthnTerminateEvent(req, authResult, statusCode);
			auditServiceRef.getService().sendEvent(av);
		}
	}



	private void auditEventAuthnDelegation01(Object[] methodParams) {
		Object[] varargs = (Object[]) methodParams[1];
		HashMap<String, Object> extraAuditData = (HashMap) varargs[0];
		String auditOutcome = (String) varargs[1];
		Integer statusCode = (Integer) varargs[2];
		if (auditServiceRef.getService() != null && auditServiceRef.getService()
				.isAuditRequired(AuditConstants.SECURITY_AUTHN_DELEGATION, auditOutcome)) {
			if (extraAuditData.get("HTTP_SERVLET_REQUEST") != null) {
				AuthenticationDelegationEvent av = new AuthenticationDelegationEvent(extraAuditData, statusCode);
				auditServiceRef.getService().sendEvent(av);
			}

		}
	}

	private void auditEventAuthzDelegation01(Object[] methodParams) {
		Object[] varargs = (Object[]) methodParams[1];
		HashMap<String, Object> extraAuditData = (HashMap) varargs[0];
		String auditOutcome = (String) varargs[1];
		Integer statusCode = (Integer) varargs[2];
		if (auditServiceRef.getService() != null && auditServiceRef.getService()
				.isAuditRequired(AuditConstants.SECURITY_AUTHZ_DELEGATION, auditOutcome)) {
			if (extraAuditData.get("HTTP_SERVLET_REQUEST") != null) {
				AuthorizationDelegationEvent av = new AuthorizationDelegationEvent(extraAuditData, statusCode);
				auditServiceRef.getService().sendEvent(av);
			}

		}
	}

	private void auditEventAuthnFailover01(Object[] methodParams) {
		Object[] varargs = (Object[]) methodParams[1];
		WebRequest webRequest = (WebRequest) varargs[0];
		AuthenticationResult authResult = (AuthenticationResult) varargs[1];
		HashMap<String, Object> extraAuditData = (HashMap) varargs[2];
		Integer statusCode = (Integer) varargs[3];
		if (auditServiceRef.getService() != null && auditServiceRef.getService()
				.isAuditRequired(AuditConstants.SECURITY_AUTHN_FAILOVER, authResult.getAuditOutcome())) {
			AuthenticationFailoverEvent av = new AuthenticationFailoverEvent(webRequest, authResult, extraAuditData,
					statusCode);
			auditServiceRef.getService().sendEvent(av);
		}
	}

	private void auditEventAuthz01(Object[] methodParams) {
		Object[] varargs = (Object[]) methodParams[1];
		WebRequest webRequest = (WebRequest) varargs[0];
		AuthenticationResult authResult = (AuthenticationResult) varargs[1];
		String uriname = (String) varargs[2];
		Integer statusCode = (Integer) varargs[3];
		if (auditServiceRef.getService() != null
				&& auditServiceRef
						.getService()
						.isAuditRequired(
								AuditConstants.SECURITY_AUTHZ,
						statusCode == HttpServletResponse.SC_OK ? AuditConstants.SUCCESS : AuditConstants.FAILURE)) {
			AuthorizationEvent av =
					new AuthorizationEvent(webRequest, authResult, uriname, statusCode);
			auditServiceRef.getService().sendEvent(av);
		}
	}

	private void auditEventAuthz02(Object[] methodParams) {
		Object[] varargs = (Object[]) methodParams[1];
		WebRequest webRequest = (WebRequest) varargs[0];
		AuthenticationResult authResult = (AuthenticationResult) varargs[1];
		String uriname = (String) varargs[2];
		String containerType = (String) varargs[3];
		Integer statusCode = (Integer) varargs[4];
		if (auditServiceRef.getService() != null
				&& auditServiceRef.getService().isAuditRequired(AuditConstants.SECURITY_AUTHZ,
						statusCode == HttpServletResponse.SC_OK ? AuditConstants.SUCCESS : AuditConstants.FAILURE)) {
			JACCWebAuthorizationEvent av = new JACCWebAuthorizationEvent(webRequest, authResult, uriname, containerType,
					statusCode);
			auditServiceRef.getService().sendEvent(av);
		}
	}

	private void auditEventAuthz03(Object[] methodParams) {
		Object[] varargs = (Object[]) methodParams[1];
		AuditAuthenticationResult authResult = (AuditAuthenticationResult) varargs[0];
		HashMap request = (HashMap) varargs[1];
		Object req = varargs[2];
		Object webReq = varargs[3];
		String realm = (String) varargs[4];
		Subject subject = (Subject) varargs[5];
		Integer statusCode = (Integer) varargs[6];
		if (auditServiceRef.getService() != null
				&& auditServiceRef.getService().isAuditRequired(AuditConstants.SECURITY_AUTHZ,
						statusCode == HttpServletResponse.SC_OK ? AuditConstants.SUCCESS : AuditConstants.FAILURE)) {
			JACCEJBAuthorizationEvent av = new JACCEJBAuthorizationEvent(authResult, request, req, webReq, realm,
					subject, statusCode);
			auditServiceRef.getService().sendEvent(av);
		}
	}

	private void auditEventAuthz04(Object[] methodParams) {
		Object[] varargs = (Object[]) methodParams[1];
		AuditAuthenticationResult authResult = (AuditAuthenticationResult) varargs[0];
		HashMap request = (HashMap) varargs[1];
		Object req = varargs[2];
		Object webReq = varargs[3];
		String realm = (String) varargs[4];
		Subject subject = (Subject) varargs[5];
		Collection<String> roles = (Collection<String>) varargs[6];
		Integer statusCode = (Integer) varargs[7];
		if (auditServiceRef.getService() != null
				&& auditServiceRef.getService().isAuditRequired(AuditConstants.SECURITY_AUTHZ,
						statusCode == HttpServletResponse.SC_OK ? AuditConstants.SUCCESS : AuditConstants.FAILURE)) {
			EJBAuthorizationEvent av = new EJBAuthorizationEvent(authResult, request, req, webReq, realm, subject,
					roles,
					statusCode);
			auditServiceRef.getService().sendEvent(av);
		}
	}

	private void auditEventAuditMgmt01(Object[] methodParams) {
		Object[] varargs = (Object[]) methodParams[1];
		Map<String, Object> configuration = (Map<String, Object>) varargs[0];
		if (auditServiceRef.getService() != null
				&& auditServiceRef.getService().isAuditRequired(
						AuditConstants.SECURITY_AUDIT_MGMT,
						AuditConstants.SUCCESS)) {
			AuditMgmtEvent av = new AuditMgmtEvent(configuration,
					"AuditService", "start");
			auditServiceRef.getService().sendEvent(av);
		}
	}

	private void auditEventAuditMgmt02(Object[] methodParams) {
		Object[] varargs = (Object[]) methodParams[1];
		Map<String, Object> configuration = (Map<String, Object>) varargs[0];
		if (auditServiceRef.getService() != null
				&& auditServiceRef.getService().isAuditRequired(
						AuditConstants.SECURITY_AUDIT_MGMT,
						AuditConstants.SUCCESS)) {
			AuditMgmtEvent av = new AuditMgmtEvent(configuration,
					"AuditService", "stop");
			auditServiceRef.getService().sendEvent(av);
		}
	}

	private void auditEventMemberMgmt01(Object[] methodParams) {
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

	private void auditEventJMSAuthn01(Object[] methodParams) {
		Object[] varargs = (Object[]) methodParams[1];
		String userName = (String) varargs[0];
		String hostAddress = (String) varargs[1];
		String port = (String) varargs[2];
		String chainName = (String) varargs[3];
		String busName = (String) varargs[4];
		String messagingEngine = (String) varargs[5];
		String credentialType = (String) varargs[6];
		Integer statusCode = (Integer) varargs[7];
		if (auditServiceRef.getService() != null
				&& auditServiceRef.getService().isAuditRequired(AuditConstants.SECURITY_JMS_AUTHN,
						statusCode == HttpServletResponse.SC_OK ? AuditConstants.SUCCESS : AuditConstants.FAILURE)) {
			JMSAuthenticationEvent av = new JMSAuthenticationEvent(userName, hostAddress, port, chainName, busName,
					messagingEngine, credentialType, statusCode);
			auditServiceRef.getService().sendEvent(av);
		}
	}

	private void auditEventJMSAuthnTerm01(Object[] methodParams) {
		Object[] varargs = (Object[]) methodParams[1];
		String userName = (String) varargs[0];
		String hostAddress = (String) varargs[1];
		String port = (String) varargs[2];
		String chainName = (String) varargs[3];
		String busName = (String) varargs[4];
		String messagingEngine = (String) varargs[5];
		String credentialType = (String) varargs[6];
		Integer statusCode = (Integer) varargs[7];
		if (auditServiceRef.getService() != null
				&& auditServiceRef.getService().isAuditRequired(AuditConstants.SECURITY_JMS_AUTHN_TERMINATE,
						statusCode == HttpServletResponse.SC_OK ? AuditConstants.SUCCESS : AuditConstants.FAILURE)) {
			JMSAuthenticationTerminateEvent av = new JMSAuthenticationTerminateEvent(userName, hostAddress, port,
					chainName, busName, messagingEngine, credentialType, statusCode);
			auditServiceRef.getService().sendEvent(av);
		}
	}

	private void auditEventJMSAuthz01(Object[] methodParams) {
		Object[] varargs = (Object[]) methodParams[1];
		String userName = (String) varargs[0];
		String hostAddress = (String) varargs[1];
		String port = (String) varargs[2];
		String chainName = (String) varargs[3];
		String busName = (String) varargs[4];
		String messagingEngine = (String) varargs[5];
		String destination = (String) varargs[6];
		String operationType = (String) varargs[7];
		String[] roles = (String[]) varargs[8];
		String resource = (String) varargs[9];
		Integer statusCode = (Integer) varargs[10];
		if (auditServiceRef.getService() != null
				&& auditServiceRef.getService().isAuditRequired(AuditConstants.SECURITY_JMS_AUTHZ,
						statusCode == HttpServletResponse.SC_OK ? AuditConstants.SUCCESS : AuditConstants.FAILURE)) {
			JMSAuthorizationEvent av = new JMSAuthorizationEvent(userName, hostAddress, port, chainName, busName,
					messagingEngine,
					destination, operationType, roles, resource, statusCode);
			auditServiceRef.getService().sendEvent(av);
		}
	}

	private void auditEventJMXNotification01(Object[] methodParams) {

		Object[] varargs = (Object[]) methodParams[1];
		ObjectName name = (ObjectName) varargs[0];
		Object listener = varargs[1];
		NotificationFilter filter = (NotificationFilter) varargs[2];
		Object handback = varargs[3];
		String action = (String) varargs[4];
		String outcome = (String) varargs[5];
		String outcomeReason = (String) varargs[6];
		if (auditServiceRef.getService() != null
				&& auditServiceRef.getService().isAuditRequired(AuditConstants.JMX_NOTIFICATION, outcome)) {
			JMXNotificationEvent je = new JMXNotificationEvent(name, listener, filter, handback, action, outcome,
					outcomeReason);
			auditServiceRef.getService().sendEvent(je);
		}
	}

	private void auditEventJMXMBeanAttributes01(Object[] methodParams) {

		Object[] varargs = (Object[]) methodParams[1];
		ObjectName name = (ObjectName) varargs[0];
		Object attrs = varargs[1];
		String action = (String) varargs[2];
		String outcome = (String) varargs[3];
		String outcomeReason = (String) varargs[4];
		if (auditServiceRef.getService() != null
				&& auditServiceRef.getService().isAuditRequired(AuditConstants.JMX_MBEAN_ATTRIBUTES, outcome)) {
			JMXMBeanAttributeEvent je = new JMXMBeanAttributeEvent(name, attrs, action, outcome, outcomeReason);
			auditServiceRef.getService().sendEvent(je);
		}
	}

	private void auditEventJMXMBeanRegister01(Object[] methodParams) {
		Object[] varargs = (Object[]) methodParams[1];
		ObjectName name = (ObjectName) varargs[0];
		Object object = varargs[1];
		String action = (String) varargs[2];
		String outcome = (String) varargs[3];
		String outcomeReason = (String) varargs[4];
		if (auditServiceRef.getService() != null
				&& auditServiceRef.getService().isAuditRequired(AuditConstants.JMX_MBEAN_REGISTER, outcome)) {
			JMXMBeanRegisterEvent je = new JMXMBeanRegisterEvent(name, object, action, outcome, outcomeReason);
			auditServiceRef.getService().sendEvent(je);
		}
	}

	private void auditEventJMXMBean01(Object[] methodParams) {

		Object[] varargs = (Object[]) methodParams[1];
		ObjectName name = (ObjectName) varargs[0];
		String className = (String) varargs[1];
		ObjectName loader = (ObjectName) varargs[2];
		String operationName = (String) varargs[3];
		Object[] params = (Object[]) varargs[4];
		String[] signature = (String[]) varargs[5];
		QueryExp query = (QueryExp) varargs[6];
		String action = (String) varargs[7];
		String outcome = (String) varargs[8];
		String outcomeReason = (String) varargs[9];
		if (auditServiceRef.getService() != null
				&& auditServiceRef.getService().isAuditRequired(AuditConstants.JMX_MBEAN, outcome)) {
			JMXMBeanEvent je = new JMXMBeanEvent(name, className, loader, operationName, params, signature, query,
					action, outcome, outcomeReason);
			auditServiceRef.getService().sendEvent(je);
		}
	}


	private void auditEventApplicationPasswordToken(Object[] methodParams) {
		Object[] varargs = (Object[]) methodParams[1];
		Map<String, Object> m = (Map<String, Object>) varargs[0];
		// HttpServletRequest webRequest = (HttpServletRequest) varargs[0];
		// AuthenticationResult authResult = (AuthenticationResult) varargs[1];
		// Integer statusCode = (Integer) varargs[2];
		if (auditServiceRef.getService() != null && auditServiceRef.getService()
				.isAuditRequired(AuditConstants.APPLICATION_TOKEN_MANAGEMENT, (String) m.get("auditOutcome"))) {
			ApplicationPasswordTokenEvent ae = new ApplicationPasswordTokenEvent(m);
			auditServiceRef.getService().sendEvent(ae);
		}

	}

	/**
	 * Handles audit event for SECURITY_SAF_AUTH_DETAILS
	 *
	 * @param methodParams
	 */
	private void auditEventSafAuthDetails(Object[] methodParams) {

		// Getting object array which have audit fields
		Object[] varargs = (Object[]) methodParams[1];

		// Get audit fields and convert them to respective type
		int safReturnCode = (Integer) varargs[0];
		int racfReturnCode = (Integer) varargs[1];
		int racfReasonCode = (Integer) varargs[2];
		String userSecurityName = (String) varargs[3];
		String safProfile = (String) varargs[4];
		String safClass = (String) varargs[5];
		Boolean authDecision = (Boolean) varargs[6];
		String principalName = (String) varargs[7];
		String applid = (String) varargs[8];

		if (auditServiceRef.getService() != null
				&& auditServiceRef.getService().isAuditRequired(AuditConstants.SECURITY_SAF_AUTHZ_DETAILS,
						AuditConstants.SUCCESS)) {
			// Create audit event
			SAFAuthorizationDetailsEvent safAuthDetails = new SAFAuthorizationDetailsEvent(safReturnCode,
					racfReturnCode, racfReasonCode, userSecurityName, applid, safProfile, safClass, authDecision,
					principalName);
			auditServiceRef.getService().sendEvent(safAuthDetails);
		}
	}

	private void auditEventRESTAuthz(Object[] methodParams) {

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

    private void auditEventSafAuth(Object[] methodParams) {
        Object[] varargs = (Object[]) methodParams[1];

        int safReturnCode = (Integer) varargs[0];
        int racfReturnCode = (Integer) varargs[1];
        int racfReasonCode = (Integer) varargs[2];
        String userSecurityName = (String) varargs[3];
        String safProfile = (String) varargs[4];
        String safClass = (String) varargs[5];
        Boolean authDecision = (Boolean) varargs[6];
        String principleName = (String) varargs[7];
        String applid = (String) varargs[8];
        String accessLevel = (String) varargs[9];
        String errorMessage = (String) varargs[10];
		// Case where WS-CD may have this field but OL may not. This check will make
		// sure there is no IndexOutOfBoundsException. The size of varargs should be
		// at least 12 for us to get the value of the methodName
		String methodName = null;
		if (varargs.length >= 12) {
			methodName = (String) varargs[11];
		}
		// Case where WS-CD may have these fields but OL may not. This check will make
		// sure there is no IndexOutOfBoundsException. varargs needs to be greater than
		// 12 to have the volser and vsam arguments in it.
		String volser = null;
		String vsam = null;
		if (varargs.length > 12) {
			volser = (String) varargs[12];
			vsam = (String) varargs[13];
		}

        if (auditServiceRef.getService() != null && auditServiceRef.getService().isAuditRequired(AuditConstants.SECURITY_SAF_AUTHZ, AuditConstants.SUCCESS)) {
			SAFAuthorizationEvent safAuth = new SAFAuthorizationEvent(safReturnCode, racfReturnCode, racfReasonCode,
					userSecurityName, applid, safProfile, safClass, authDecision, principleName, accessLevel,
					errorMessage, methodName, volser, vsam);
            auditServiceRef.getService().sendEvent(safAuth);
        }
    }
}
