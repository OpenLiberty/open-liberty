/*******************************************************************************
 * Copyright (c) 2011, 2024 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.security.internal;

import java.security.AccessController;
import java.security.Identity;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialExpiredException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditAuthResult;
import com.ibm.websphere.security.audit.AuditAuthenticationResult;
import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.security.audit.context.AuditManager;
import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.container.service.metadata.ComponentMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.ejbcontainer.EJBComponentMetaData;
import com.ibm.ws.ejbcontainer.EJBMethodInterface;
import com.ibm.ws.ejbcontainer.EJBMethodMetaData;
import com.ibm.ws.ejbcontainer.EJBRequestData;
import com.ibm.ws.ejbcontainer.EJBSecurityCollaborator;
import com.ibm.ws.ejbcontainer.security.internal.jacc.EJBJaccAuthorizationHelper;
import com.ibm.ws.ejbcontainer.security.jacc.EJBJaccService;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.audit.Audit;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.authentication.principals.WSIdentity;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authorization.AuthorizationService;
import com.ibm.ws.security.collaborator.CollaboratorUtils;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.ready.SecurityReadyService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * This security collaborator is called by the EJB container for authorization checks and API calls.
 */

@SuppressWarnings("deprecation")
public class EJBSecurityCollaboratorImpl implements EJBSecurityCollaborator<SecurityCookieImpl>, EJBAuthorizationHelper, ComponentMetaDataListener {
    private static final TraceComponent tc = Tr.register(EJBSecurityCollaboratorImpl.class);
    protected static final String KEY_SECURITY_SERVICE = "securityService";
    protected static final String KEY_CREDENTIAL_SERVICE = "credentialsService";
    protected static final String KEY_UNAUTHENTICATED_SUBJECT_SERVICE = "unauthenticatedSubjectService";
    protected static final String KEY_EJB_JACC_SERVICE = "eJBJaccService";
    protected static final String KEY_SECURITY_READY_SERVICE = "securityReadyService";
    private SecurityReadyService securityReadyService;
    protected final AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);
    private final AtomicServiceReference<CredentialsService> credServiceRef = new AtomicServiceReference<CredentialsService>(KEY_CREDENTIAL_SERVICE);
    private final AtomicServiceReference<UnauthenticatedSubjectService> unauthenticatedSubjectServiceRef = new AtomicServiceReference<UnauthenticatedSubjectService>(KEY_UNAUTHENTICATED_SUBJECT_SERVICE);
    private final AtomicServiceReference<EJBJaccService> ejbJaccService = new AtomicServiceReference<EJBJaccService>(KEY_EJB_JACC_SERVICE);

    protected SubjectManager subjectManager;
    protected CollaboratorUtils collabUtils;

    protected AuditManager auditManager;

    protected volatile EJBSecurityConfig ejbSecConfig = null;
    private EJBAuthorizationHelper eah = this;

    private boolean waitedForSecurity = false;

    private static final String securityWaitTimeProperty = "io.openliberty.ejb.security.startWaitTime";

    // wait time in seconds, default 0
    private static final int securityWaitTime = AccessController.doPrivileged(new PrivilegedAction<Integer>() {
        @Override
        public Integer run() {
            int waitTime = Integer.getInteger(securityWaitTimeProperty, 0);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "EJBSecurityCollaborator securityWaitTime set to " + waitTime + " seconds");
            }
            return waitTime;
        }
    });

    /**
     * Zero length constructor required by DS.
     */
    public EJBSecurityCollaboratorImpl() {
        this(new SubjectManager());
        this.auditManager = new AuditManager();
    }

    public EJBSecurityCollaboratorImpl(SubjectManager subjectManager) {
        this.subjectManager = subjectManager;
        this.collabUtils = new CollaboratorUtils(subjectManager);
    }

    protected void setCredentialService(ServiceReference<CredentialsService> ref) {
        credServiceRef.setReference(ref);
    }

    protected void unsetCredentialService(ServiceReference<CredentialsService> ref) {
        credServiceRef.unsetReference(ref);
    }

    protected void setSecurityService(ServiceReference<SecurityService> ref) {
        securityServiceRef.setReference(ref);
    }

    protected void unsetSecurityService(ServiceReference<SecurityService> ref) {
        securityServiceRef.unsetReference(ref);
    }

    @Reference
    protected void setSecurityReadyService(SecurityReadyService ref) {
        this.securityReadyService = ref;
    }

    protected void unsetSecurityReadyService(SecurityReadyService ref) {
    }

    protected void setUnauthenticatedSubjectService(ServiceReference<UnauthenticatedSubjectService> ref) {
        unauthenticatedSubjectServiceRef.setReference(ref);
    }

    protected void unsetUnauthenticatedSubjectService(ServiceReference<UnauthenticatedSubjectService> ref) {
        unauthenticatedSubjectServiceRef.unsetReference(ref);
    }

    protected void setEJBJaccService(ServiceReference<EJBJaccService> reference) {
        ejbJaccService.setReference(reference);
        eah = new EJBJaccAuthorizationHelper(ejbJaccService);
    }

    protected void unsetEJBJaccService(ServiceReference<EJBJaccService> reference) {
        ejbJaccService.unsetReference(reference);
        eah = this;
    }

    protected void activate(ComponentContext cc, Map<String, Object> props) {
        securityServiceRef.activate(cc);
        credServiceRef.activate(cc);
        unauthenticatedSubjectServiceRef.activate(cc);
        ejbJaccService.activate(cc);
        ejbSecConfig = new EJBSecurityConfigImpl(props);
    }

    protected void modified(Map<String, Object> newProperties) {
        EJBSecurityConfig newEjbSecConfig = new EJBSecurityConfigImpl(newProperties);
        // Capture the properties that were changed for our audit record
        String deltaString = newEjbSecConfig.getChangedProperties(ejbSecConfig);
        ejbSecConfig = newEjbSecConfig;
        Tr.audit(tc, "EJB_SECURITY_CONFIGURATION_UPDATED", deltaString);
    }

    protected void deactivate(ComponentContext cc) {
        securityServiceRef.deactivate(cc);
        credServiceRef.deactivate(cc);
        unauthenticatedSubjectServiceRef.deactivate(cc);
        ejbJaccService.deactivate(cc);
    }

    /**
     * This method is called by the EJB container prior to executing a method
     * on a bean. Called after bean is activated and loaded, but before EJB
     * method is dispatched. The preInvoke will authorize the request and then
     * delegate to the run-as user, if specified. {@inheritDoc}
     *
     * @throws EJBAccessDeniedException when the caller is not authorized to invoke
     *                                      the given request
     */
    /** {@inheritDoc} */
    @Override
    public SecurityCookieImpl preInvoke(EJBRequestData request) throws EJBAccessDeniedException {
        Subject invokedSubject = subjectManager.getInvocationSubject();
        Subject callerSubject = subjectManager.getCallerSubject();

        EJBMethodMetaData methodMetaData = request.getEJBMethodMetaData();

        if (ejbSecConfig.getUseUnauthenticatedForExpiredCredentials()) {
            invokedSubject = setNullSubjectWhenExpired(invokedSubject);
            callerSubject = setNullSubjectWhenExpired(callerSubject);
        }
        Subject originalInvokedSubject = invokedSubject;
        Subject originalCallerSubject = callerSubject;

//        SecurityCookieImpl securityCookie = new SecurityCookieImpl(invokedSubject, callerSubject);
        if (setUnauthenticatedSubjectIfNeeded(invokedSubject, callerSubject)) {
            invokedSubject = subjectManager.getInvocationSubject();
            callerSubject = subjectManager.getCallerSubject();
        }
        Subject subjectToAuthorize = (invokedSubject == null) ? callerSubject : invokedSubject;

        if (!isInternalUnprotectedMethod(methodMetaData)) {
            eah.authorizeEJB(request, subjectToAuthorize);
        }

        performDelegation(methodMetaData, subjectToAuthorize);
        subjectManager.setCallerSubject(subjectToAuthorize);
        SecurityCookieImpl securityCookie = new SecurityCookieImpl(originalInvokedSubject, originalCallerSubject, subjectManager.getInvocationSubject(), subjectToAuthorize);
        return securityCookie;
    }

    /**
     * Restore the caller's security context before returning to them.
     */
    /** {@inheritDoc} */
    @Override
    public void postInvoke(EJBRequestData request, SecurityCookieImpl preInvokeResult) throws EJBAccessDeniedException {
        if (preInvokeResult != null) {
            EJBJaccService js = ejbJaccService.getService();
            if (js != null) {
                js.resetPolicyContextHandlerInfo();
            }
            SecurityCookieImpl securityCookie = preInvokeResult;
            Subject invocationSubject = subjectManager.getInvocationSubject();
            Subject callerSubject = subjectManager.getCallerSubject();
            // A unit test might set either invocationSubject or callerSubject as null.
            if ((invocationSubject == null || invocationSubject.equals(securityCookie.getAdjustedInvokedSubject())) &&
                (callerSubject == null || callerSubject.equals(securityCookie.getAdjustedReceivedSubject()))) {
                // if invocation and caller subject are unchanged, this means that a programmatic authentication
                // was not carried out, thus put the original subject back.
                // otherwise, keep the current subjects in order to preserve the subjects from the programmatic login.
                Subject invokedSubject = securityCookie.getInvokedSubject();
                Subject receivedSubject = securityCookie.getReceivedSubject();

                subjectManager.setCallerSubject(receivedSubject);
                subjectManager.setInvocationSubject(invokedSubject);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Subjects have been changed, preserving the current Subjects.");
                }
            }
        }
    }

    /**
     * This method is called by the EJB container after preInvoke occurs and
     * one or more AroundInvoke interceptors has modified at least one
     * of the parameters of the EJB method to be invoked. Allows the
     * SecurityCollaborator to pass the parameter changes to a JACC provider
     * to determine if authorized to modify the parameters. This call
     * will not occur if there are no AroundInvoke interceptors or if
     * none of the AroundInvoke interceptors called the setParameters
     * method on the javax.interceptors.InvocationContext interface.
     */
    /** {@inheritDoc} */
    @Override
    public void argumentsUpdated(EJBRequestData request, SecurityCookieImpl preInvokeData) throws Exception {
        // Do nothing
    }

    /** {@inheritDoc} */
    @Override
    public Identity getCallerIdentity(EJBComponentMetaData cmd, EJBRequestData request, SecurityCookieImpl preInvokeData) {
        Principal callerPrincipal = getCallerPrincipal(cmd, request, preInvokeData);
        if (callerPrincipal != null) {
            return new WSIdentity(callerPrincipal.getName());
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Principal getCallerPrincipal(EJBComponentMetaData cmd, EJBRequestData request, SecurityCookieImpl preInvokeData) {
        String realm = null;
        boolean useRealmQualifiedUserNames = ejbSecConfig.getUseRealmQualifiedUserNames();
        if (useRealmQualifiedUserNames) {
            realm = collabUtils.getUserRegistryRealm(securityServiceRef);
        }
        return collabUtils.getCallerPrincipal(ejbSecConfig.getUseRealmQualifiedUserNames(), realm, false, false);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCallerInRole(EJBComponentMetaData cmd, EJBRequestData request, SecurityCookieImpl preInvokeData, String roleName, String roleLink) {
        if (request == null) {
//            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                Tr.info(tc, "REQUEST_IS_NULL", "Request value passed into isCallerInRole is null. Returning false");
//            }
            return false;
        }
        Subject callerSubject = subjectManager.getCallerSubject();
        if (ejbSecConfig.getUseUnauthenticatedForExpiredCredentials()) {
            //create unauth subject if caller subjcect is expired
            CredentialsService credService = credServiceRef.getService();
            if (credService != null && !credService.isSubjectValid(callerSubject)) {
                callerSubject = unauthenticatedSubjectServiceRef.getService().getUnauthenticatedSubject();
            }
        }

        return eah.isCallerInRole(cmd, request, roleName, roleLink, callerSubject);

    }

    @Override
    public boolean isCallerInRole(EJBComponentMetaData cmd, EJBRequestData request, String roleName, String roleLink, Subject subject) {
        String role = roleLink == null ? roleName : roleLink;
        String appName = getApplicationName(request.getEJBMethodMetaData());
        waitForSecurity();
        SecurityService securityService = securityServiceRef.getService();
        AuthorizationService authzService = securityService.getAuthorizationService();
        if (authzService == null) {
            // If we can not get the authorization service, fail securely
            String authzUserName = subject.getPrincipals(WSPrincipal.class).iterator().next().getName();
            Tr.error(tc, "EJB_AUTHZ_SERVICE_NOTFOUND", authzUserName, "isCallerInRole", appName);
            return false;
        } else {
            final List<String> requiredRoles = new ArrayList<String>();
            requiredRoles.add(role);
            return authzService.isAuthorized(appName, requiredRoles, subject);
        }
    }

    /**
     * @param subject
     * @return
     */
    private Subject setNullSubjectWhenExpired(Subject subject) {
        if (subject != null) {
            CredentialsService credService = credServiceRef.getService();
            if (credService != null && !credService.isSubjectValid(subject)) {
                subject = null;
            }
        }
        return subject;
    }

    /**
     * Check if the methodMetaData interface is internal and supposed to be unprotected as per
     * spec.
     *
     * @param methodMetaData methodMetaData to get the interface type
     * @return true if it should be unprotected, otherwise false
     */
    private boolean isInternalUnprotectedMethod(EJBMethodMetaData methodMetaData) {
        EJBMethodInterface interfaceType = methodMetaData.getEJBMethodInterface();
        /***
         * For TIMED_OBJECT, the code references EJB 2.1 spec section 22.2.2 and matches a
         * method signature, which is necessary but not sufficient. As of EJB 3.0, the
         * TimedObject interface is not necessary and arbitrary methods can be designated
         * as timeout callback methods using the @Timeout annotation. Further, the EJB
         * 3.1 spec adds the ability to specify timeout methods without a Timer argument,
         * and it also adds new timeout callback methods via the @Schedule annotation. In
         * all of these cases, the MethodInterface will be TIMED_OBJECT.
         *
         * For LIFECYCLE_INTERCEPTOR, this type is used only for lifecycle interceptors of
         * EJB 3.1 singleton session beans. This type should have been added to the EJB 3.1
         * spec, but it was overlooked by the EG. However, EJB container needs to classify
         * methods in this way, so an internal type was added.
         *
         * For background on why returning true from internalUnprotected is correct for
         * LIFECYCLE_INTERCEPTOR, a singleton method invocation has two steps, and the
         * container invokes the security collaborator for each.
         *
         * 1. Obtain the bean instance if it does not already exist.
         * If the bean does not exist, the container calls the security collaborator
         * with LifecycleInterceptor to establish a RunAs security context. However,
         * authorization checks are not needed because the bean is performing
         * initialization, not business logic. Further, attempting to pass the
         * internal-only LifecycleInterceptor type to JACC causes an
         * IllegalArgumentException.
         *
         * If a singleton session bean is annotated @Startup, then the container
         * performs this step as part of application start rather than when a method
         * is first invoked on the bean. This is the scenario for this defect.
         *
         * 2. Invoke the business method.
         * The container calls the security collaborator "as normal" with Local,
         * Remote, or ServiceEndpoint to both authorize the caller security context
         * and to establish a RunAs security context as needed.
         *
         * Per EJB spec section 22.2.2:
         *
         * Since the ejbTimeout method is an internal method of the bean class,
         * it has no client security context. When getCallerPrincipal is called
         * from within the ejbTimeout method, it returns the container
         * representation of the unauthenticated identity.
         * Since the ejbTimeout method is an internal method of the bean class,
         * it has no client security context. The Bean Provider should use the
         * run-as deployment descriptor element to specify a security identity to
         * be used for the invocation of methods from within the ejbTimeout method.
         *
         * Because of the above spec requirements, we still need to establish the
         * runasSpecified identity when this method is called.
         **/
        if (EJBMethodInterface.LIFECYCLE_INTERCEPTOR.value() == (interfaceType.value()) ||
            EJBMethodInterface.TIMER.value() == (interfaceType.value())) {
            return true;
            //TODO: should this logic go into ejb container?
        }
        return false;
    }

    public void populateAuditEJBHashMap(EJBRequestData request, Map<String, Object> ejbAuditHashMap) {
        EJBMethodMetaData methodMetaData = request.getEJBMethodMetaData();
        Object[] methodArguments = request.getMethodArguments();
        String applicationName = methodMetaData.getEJBComponentMetaData().getJ2EEName().getApplication();
        String moduleName = methodMetaData.getEJBComponentMetaData().getJ2EEName().getModule();
        String methodName = methodMetaData.getMethodName();
        String methodInterface = methodMetaData.getEJBMethodInterface().specName();
        String methodSignature = methodMetaData.getMethodSignature();
        String beanName = methodMetaData.getEJBComponentMetaData().getJ2EEName().getComponent();

        ejbAuditHashMap.put("applicationName", applicationName);
        ejbAuditHashMap.put("moduleName", moduleName);
        ejbAuditHashMap.put("methodName", methodName);
        ejbAuditHashMap.put("methodInterface", methodInterface);
        ejbAuditHashMap.put("methodSignature", methodSignature);
        ejbAuditHashMap.put("beanName", beanName);
        ejbAuditHashMap.put("methodParameters", methodArguments);
    }

    /**
     * Authorizes the subject to call the given EJB, based on the given method info.
     * If the subject is not authorized, an exception is thrown. The following checks are made:
     * <li>is the bean method excluded (denyAll)</li>
     * <li>are the required roles null or empty</li>
     * <li>is EVERYONE granted to any of the required roles</li>
     * <li>is the subject authorized to any of the required roles</li>
     *
     * @param EJBRequestData the info on the EJB method to call
     * @param subject        the subject authorize
     * @throws EJBAccessDeniedException when the subject is not authorized to the EJB
     */
    @Override
    public void authorizeEJB(EJBRequestData request, Subject subject) throws EJBAccessDeniedException {
        EJBMethodMetaData methodMetaData = request.getEJBMethodMetaData();
        String authzUserName = subject.getPrincipals(WSPrincipal.class).iterator().next().getName();
        String applicationName = getApplicationName(methodMetaData);
        String methodName = methodMetaData.getMethodName();//TODO: which API to call? methodInfo.getMethodSignature()+":"+methodInfo.getInterfaceType().getValue();

        Object req = (auditManager != null) ? auditManager.getHttpServletRequest() : null;
        Object webRequest = (auditManager != null) ? auditManager.getWebRequest() : null;
        String realm = (auditManager != null) ? auditManager.getRealm() : null;

        HashMap<String, Object> ejbAuditHashMap = new HashMap<String, Object>();

        populateAuditEJBHashMap(request, ejbAuditHashMap);

        Collection<String> roles = getRequiredRoles(methodMetaData);

        //check if bean method is excluded
        if (methodMetaData.isDenyAll()) {
            ejbAuditHashMap.put(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_EJB_DENYALL);
            Tr.audit(tc, "EJB_AUTHZ_EXCLUDED", authzUserName, methodName, applicationName);
            AuditAuthenticationResult auditAuthResult = new AuditAuthenticationResult(AuditAuthResult.FAILURE, authzUserName, AuditEvent.CRED_TYPE_BASIC, null, AuditEvent.OUTCOME_FAILURE);
            Audit.audit(Audit.EventID.SECURITY_AUTHZ_04, auditAuthResult, ejbAuditHashMap, req, webRequest, realm, subject, roles, Integer.valueOf("403"));

            throw new EJBAccessDeniedException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                            TraceConstants.MESSAGE_BUNDLE,
                                                                            "EJB_AUTHZ_EXCLUDED",
                                                                            new Object[] { authzUserName, methodName, applicationName },
                                                                            "CWWKS9402A: Authorization failed for user {0} while invoking {1} on {2} because the method is explicitly excluded."));
        }

        //return immediately when permitAll is set
        if (methodMetaData.isPermitAll()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Authorization granted for " + methodName + " on " + applicationName + " because permitAll is set.");
            }
            ejbAuditHashMap.put(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_EJB_PERMITALL);
            AuditAuthenticationResult auditAuthResult = new AuditAuthenticationResult(AuditAuthResult.SUCCESS, authzUserName, AuditEvent.CRED_TYPE_BASIC, null, AuditEvent.OUTCOME_SUCCESS);
            Audit.audit(Audit.EventID.SECURITY_AUTHZ_04, auditAuthResult, ejbAuditHashMap, req, webRequest, realm, subject, roles, Integer.valueOf("200"));

            return;
        }

        if (roles == null || roles.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Authorization granted for " + methodName + " on " + applicationName + " because no roles are required.");
            }
            ejbAuditHashMap.put(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_EJB_NO_ROLES);
            AuditAuthenticationResult auditAuthResult = new AuditAuthenticationResult(AuditAuthResult.SUCCESS, authzUserName, AuditEvent.CRED_TYPE_BASIC, null, AuditEvent.OUTCOME_SUCCESS);
            Audit.audit(Audit.EventID.SECURITY_AUTHZ_04, auditAuthResult, ejbAuditHashMap, req, webRequest, realm, subject, null, Integer.valueOf("200"));

            return;
        }
        waitForSecurity();
        SecurityService securityService = securityServiceRef.getService();
        AuthorizationService authzService = securityService.getAuthorizationService();

        if (authzService == null) {
            // If we can not get the authorization service, fail securely
            ejbAuditHashMap.put(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_EJB_NO_AUTHZ_SERVICE);
            AuditAuthenticationResult auditAuthResult = new AuditAuthenticationResult(AuditAuthResult.FAILURE, authzUserName, AuditEvent.CRED_TYPE_BASIC, null, AuditEvent.OUTCOME_FAILURE);
            Audit.audit(Audit.EventID.SECURITY_AUTHZ_04, auditAuthResult, ejbAuditHashMap, req, webRequest, realm, subject, roles, Integer.valueOf("403"));

            Tr.error(tc, "EJB_AUTHZ_SERVICE_NOTFOUND", authzUserName, methodName, applicationName);
            throw new EJBAccessDeniedException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                            TraceConstants.MESSAGE_BUNDLE,
                                                                            "EJB_AUTHZ_SERVICE_NOTFOUND",
                                                                            new Object[] { authzUserName, methodName, applicationName },
                                                                            "CWWKS9403E: The authorization service could not be found. As a result, the user is not authorized."));
        } else {
            if (!authzService.isAuthorized(applicationName, roles, subject)) {
                ejbAuditHashMap.put(AuditEvent.REASON_TYPE, AuditEvent.EJB);
                AuditAuthenticationResult auditAuthResult = new AuditAuthenticationResult(AuditAuthResult.FAILURE, authzUserName, AuditEvent.CRED_TYPE_BASIC, null, AuditEvent.OUTCOME_FAILURE);
                Audit.audit(Audit.EventID.SECURITY_AUTHZ_04, auditAuthResult, ejbAuditHashMap, req, webRequest, realm, subject, roles, Integer.valueOf("403"));

                Tr.audit(tc, "EJB_AUTHZ_FAILED", authzUserName, methodName, applicationName, roles);
                throw new EJBAccessDeniedException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                                TraceConstants.MESSAGE_BUNDLE,
                                                                                "EJB_AUTHZ_FAILED",
                                                                                new Object[] { authzUserName, methodName, applicationName,
                                                                                               roles },
                                                                                "CWWKS9400A: Authorization failed. The user is not granted access to any of the required roles."));
            } else {
                ejbAuditHashMap.put(AuditEvent.REASON_TYPE, AuditEvent.EJB);
                AuditAuthenticationResult auditAuthResult = new AuditAuthenticationResult(AuditAuthResult.SUCCESS, authzUserName, AuditEvent.CRED_TYPE_BASIC, null, AuditEvent.OUTCOME_SUCCESS);
                Audit.audit(Audit.EventID.SECURITY_AUTHZ_04, auditAuthResult, ejbAuditHashMap, req, webRequest, realm, subject, roles, Integer.valueOf("200"));
            }
        }
    }

    /**
     * @param methodMetaData
     * @return
     */
    protected Collection<String> getRequiredRoles(EJBMethodMetaData methodMetaData) {
        return methodMetaData.getRolesAllowed();
    }

    /**
     * Get the name of the application currently executing
     *
     * @return the application name
     */
    protected String getApplicationName(EJBMethodMetaData methodMetaData) {
        return methodMetaData.getEJBComponentMetaData().getJ2EEName().getApplication();
    }

    /**
     * Get the name of the module currently executing
     *
     * @return the module name
     */
    protected String getModuleName(EJBMethodMetaData methodMetaData) {
        return methodMetaData.getEJBComponentMetaData().getJ2EEName().getModule();
    }

    /**
     * Get the name of the component/bean currently executing
     *
     * @return the component/bean name
     */
    protected String getComponentName(EJBMethodMetaData methodMetaData) {
        return methodMetaData.getEJBComponentMetaData().getJ2EEName().getComponent();
    }

    private void performDelegationAudit(Subject initialSubject, String roleName, Subject delegationSubject, boolean success, AuthenticationService authService) {
        final Object httpRequest = auditManager == null ? null : auditManager.getHttpServletRequest();

        String outcome = success ? AuditConstants.SUCCESS : AuditConstants.FAILURE;

        // if HttpRequest is null, the audit does nothing, so don't call it if null
        if (httpRequest == null || !Audit.isAuditRequired(Audit.EventID.SECURITY_AUTHN_DELEGATION_01, outcome)) {
            return;
        }

        HashMap<String, Object> extraAuditData = new HashMap<String, Object>();
        extraAuditData.put("HTTP_SERVLET_REQUEST", httpRequest);
        extraAuditData.put("REASON_TYPE", "EJB");

        ArrayList<String> delUsers = new ArrayList<String>();
        Set<WSCredential> publicCredentials = (initialSubject == null ? null : initialSubject.getPublicCredentials(WSCredential.class));
        Iterator<WSCredential> it = null;
        if (publicCredentials != null && (it = publicCredentials.iterator()) != null && it.hasNext()) {
            WSCredential credential = it.next();
            try {
                extraAuditData.put("REALM", credential.getRealmName());
            } catch (CredentialExpiredException e) {
            } catch (CredentialDestroyedException e) {
            }
            try {
                delUsers.add("user:" + credential.getRealmSecurityName());
            } catch (CredentialExpiredException e1) {
                // TODO Auto-generated catch block
            } catch (CredentialDestroyedException e1) {
                // TODO Auto-generated catch block
            }
        }

        if (roleName == null) {
            delUsers.add("EJB_RUNAS_SYSTEM");
        } else {
            extraAuditData.put("RUN_AS_ROLE", roleName);
            if (delegationSubject != null) {
                String buff = delegationSubject.toString();
                if (buff != null) {
                    int a = buff.indexOf("accessId");
                    if (a != -1) {
                        buff = buff.substring(a + 9);
                        a = buff.indexOf(",");
                        if (a != -1) {
                            buff = buff.substring(0, a);
                            delUsers.add(buff);
                        }
                    }
                }
            } else {
                String invalidUser = authService.getInvalidDelegationUser();
                delUsers.add(invalidUser);
            }
        }

        extraAuditData.put("DELEGATION_USERS_LIST", delUsers);

        Audit.audit(Audit.EventID.SECURITY_AUTHN_DELEGATION_01, extraAuditData, outcome, success ? Integer.valueOf(200) : Integer.valueOf(401));
    }

    /**
     * Gets the run-as subject for the given EJB method, and sets it as the invocation subject.
     * If the run-as subject is null or the deployment descriptor specifies to run as the caller,
     * then the passed-in subject is set as the invocation subject instead.
     *
     * @param methodMetaData    the EJB method info
     * @param delegationSubject subject to set as the invocation when running as caller
     */
    private void performDelegation(EJBMethodMetaData methodMetaData, Subject delegationSubject) {

        final Subject initialSubject = delegationSubject;
        String applicationName = getApplicationName(methodMetaData);
        String methodName = methodMetaData.getMethodName();//TODO: which API to call? methodInfo.getMethodSignature()+":"+methodInfo.getInterfaceType().getValue();

        if (methodMetaData.isUseSystemPrincipal()) {
            // fail request because run-as-mode SYSTEM_IDENTITY is not supported on Liberty
            Tr.error(tc, "EJB_RUNAS_SYSTEM_NOT_SUPPORTED", methodName, applicationName);
            performDelegationAudit(initialSubject, null, null, false, null);
            throw new EJBAccessDeniedException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                            TraceConstants.MESSAGE_BUNDLE,
                                                                            "EJB_RUNAS_SYSTEM_NOT_SUPPORTED",
                                                                            new Object[] { methodName, applicationName },
                                                                            "CWWKS9405E: Authorization failed for EJB method"
                                                                                                                          + methodName
                                                                                                                          + " in the application "
                                                                                                                          + applicationName
                                                                                                                          + ". The run-as-mode of SYSTEM_IDENTITY specified in the ibm-ejb-jar-ext.xml is not supported and must be removed or replaced."));

        } else if (methodMetaData.isUseCallerPrincipal()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning without delegating because run-as-mode in ibm-ejb-jar-ext for " + methodName + " in " + applicationName + " is set to CALLER_IDENTITY.");
            }
            return;
        } else {
            String roleName = getRunAsRole(methodMetaData);
            if (roleName != null) {
                waitForSecurity();
                SecurityService securityService = securityServiceRef.getService();
                AuthenticationService authService = securityService.getAuthenticationService();
                boolean success;
                try {
                    delegationSubject = authService.delegate(roleName, getApplicationName(methodMetaData));
                    success = (delegationSubject != null);
                } catch (IllegalArgumentException e) {
                    success = false;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Exception performing delegation.", e);
                    }
                }

                performDelegationAudit(initialSubject, roleName, delegationSubject, success, authService);
            }
        }

        if (delegationSubject != null) {
            subjectManager.setInvocationSubject(delegationSubject);
        }
    }

    /**
     * Gets the run-as role specified in the deployment descriptor or
     * via annotations for the given EJB method info.
     *
     * @param methodMetaData the EJB method info
     * @return the runAs role
     */
    protected String getRunAsRole(EJBMethodMetaData methodMetaData) {
        return methodMetaData.getRunAs();
    }

    /**
     * If invoked and received cred are null, then set the unauthenticated subject.
     *
     * @param invokedSubject
     * @param receivedSubject
     * @return {@code true} if the unauthenticated subject was set, {@code false} otherwise.
     */
    private boolean setUnauthenticatedSubjectIfNeeded(Subject invokedSubject, Subject receivedSubject) {
        if ((invokedSubject == null) && (receivedSubject == null)) {
            // create the unauthenticated subject and set as the invocation subject
            subjectManager.setInvocationSubject(unauthenticatedSubjectServiceRef.getService().getUnauthenticatedSubject());
            return true;
        }
        return false;
    }

    @Override
    public boolean areRequestMethodArgumentsRequired() {
        EJBJaccService js = ejbJaccService.getService();
        boolean result = false;
        if (js != null) {
            result = js.areRequestMethodArgumentsRequired();
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.metadata.ComponentMetaDataListener#componentMetaDataCreated(com.ibm.ws.container.service.metadata.MetaDataEvent)
     */
    @Override
    public void componentMetaDataCreated(MetaDataEvent<ComponentMetaData> event) {
        EJBJaccService js = ejbJaccService.getService();
        if (js != null) {
            MetaData metaData = event.getMetaData();
            if (metaData instanceof BeanMetaData) {
                BeanMetaData bmd = (BeanMetaData) metaData;
                js.propagateEJBRoles(bmd);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.metadata.ComponentMetaDataListener#componentMetaDataDestroyed(com.ibm.ws.container.service.metadata.MetaDataEvent)
     */
    @Override
    public void componentMetaDataDestroyed(MetaDataEvent<ComponentMetaData> event) {

    }

    @FFDCIgnore(InterruptedException.class)
    private void waitForSecurity() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (waitedForSecurity || securityReadyService.isSecurityReady()) {
            waitedForSecurity = true;
            return;
        }

        try {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "Waiting " + securityWaitTime + " seconds for Security Service to be ready");
            if (securityReadyService.awaitSecurityReady(securityWaitTime, TimeUnit.SECONDS) == false) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Security Service did not come up within " + securityWaitTime + " seconds");
            }
        } catch (InterruptedException e) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "Waiting for Security Service failed: " + e);
        }

        waitedForSecurity = true;
    }
}
