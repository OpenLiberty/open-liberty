/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.saf.internal;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialExpiredException;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.WebSphereRuntimePermission;
import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.audit.Audit;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.authorization.AccessDecisionService;
import com.ibm.ws.security.authorization.AuthorizationService;
import com.ibm.ws.security.authorization.AuthorizationTableService;
import com.ibm.ws.security.authorization.saf.SAFRoleMapper;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.credentials.saf.SAFCredentialsService;
import com.ibm.ws.security.delegation.DelegationProvider;
import com.ibm.ws.security.saf.SAFException;
import com.ibm.ws.security.saf.SAFServiceResult;
import com.ibm.ws.zos.core.NativeService;
import com.ibm.ws.zos.core.structures.NativeRcvt;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.ws.zos.jni.NativeMethodUtils;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.authorization.saf.AccessLevel;
import com.ibm.wsspi.security.authorization.saf.LogOption;
import com.ibm.wsspi.security.authorization.saf.SAFAuthorizationException;
import com.ibm.wsspi.security.authorization.saf.SAFAuthorizationService;
import com.ibm.wsspi.security.credentials.saf.SAFCredential;

/**
 * SAF-based authorization.
 *
 * The user is authorized by checking the user's access level (e.g. READ, NONE)
 * to various EJBROLE profiles. The EJBROLE profile names correspond to the
 * app/resource/role that the user is being authorized against.
 *
 * SAFRoleMapper controls how app/resource/role names are mapped to EJBROLE profile names.
 *
 */
@Component(name = "com.ibm.ws.security.authorization.saf",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = { "service.vendor=IBM",
                        "id=saf",
                        "com.ibm.ws.security.authorization.type=SAF" })
public class SAFAuthorizationServiceImpl implements AuthorizationService, SAFAuthorizationService {

    /**
     * TraceComponent for issuing messages.
     */
    private static final TraceComponent tc = Tr.register(SAFAuthorizationServiceImpl.class);

    /**
     * The NativeMethodManager service for loading native methods.
     */
    private NativeMethodManager nativeMethodManager = null;

    /**
     * Reference to SAFCredentialsService, for retrieving SAFCredentialTokens from the
     * SAFCredential.
     */
    private SAFCredentialsService safCredentialsService = null;

    /**
     * SAFRoleMappers.
     * At least 1 is always configured (our built-in version). The user may supply
     * additional impls. Only 1 is used, whichever one's component.name property
     * matches the config attribute <safAuthorization roleMapper="??" />.
     */
    private final Map<String, SAFRoleMapper> safRoleMappers = new ConcurrentHashMap<String, SAFRoleMapper>();

    /**
     * The attribute used to specify the role mapper in the <safAuthorization>
     * config element.
     */
    protected static final String ROLE_MAPPER_KEY = "roleMapper";

    /**
     * The value of the roleMapper config attribute.
     */
    private String roleMapperName = null;

    /**
     * A reference to the SAFRoleMapper currently in use. The component.name of the
     * SAFRoleMapper is specified by the config attribute <safAuthorization roleMapper="??" />
     */
    private SAFRoleMapper currentSafRoleMapper = null;

    /**
     * The SAF delegation provider is created if <safAuthorization enableDelegation="true" />.
     */
    private SAFDelegationProvider safDelegationProvider;

    /**
     * Keep track of the service registration so we can unregister at deactivation.
     */
    private ServiceRegistration<DelegationProvider> safDelegationProviderRegistration;

    /**
     * Needed by SAFDelegationProvider.
     */
    private SecurityService securityService;

    /**
     * The attribute used to specify how to log
     * ICH408I RACF messages for EJBROLE authz checks.
     * Attribute is in the <safAuthorization>
     * config element.
     */
    protected static final String EJBROLE_LOG_OPTION = "racRouteLog";

    /**
     * The EJB logging option.
     */
    private LogOption ejbLogOption = LogOption.NONE;

    /**
     * Number of times to retry the native operation when it fails
     * because the SAF cred token is being freed
     */
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * Flag indicates whether we've already issued the "couldn't create default credential" message.
     * We only want to issue that message once, not every time we try to create the default cred.
     * Reset the flag once we're able to successfully create the default cred.
     */
    private boolean defaultCredMsgAlreadyIssued = false;

    /**
     * Flag indicating if we've already issued the "SAF resource profile does not exist" error message.
     * The message would be sent many times per user action otherwise. Resets on a successful
     * SAF authorization.
     */
    private boolean missingProfileMsgAlreadyIssued = false;

    /**
     * Target name for the Permission object. To be used to guard access to the method that checks access
     * to the SAF resource.
     */
    private final static WebSphereRuntimePermission SAF_AUTHZ_PERM = new WebSphereRuntimePermission("safAuthorizationService");

    /**
     * '**' will represent the Servlet 3.1 defined all authenticated Security constraint
     */
    private static final String UNDEFINED_STARSTAR_ALL_AUTHENTICATED = "**";

    /**
     * 'starstar' will represent if ** was specified as a defined role
     */
    private static final String DEFINED_STARSTAR_ROLE = "_starstar_";

    private static final String ADMIN_RESOURCE_NAME = "com.ibm.ws.management.security.resource";

    protected static final String KEY_ACCESS_DECISION_SERVICE = "accessDecisionService";

    protected static final String KEY_AUTHORIZATION_TABLE_SERVICE = "authorizationTableService";

    private final AtomicServiceReference<AccessDecisionService> accessDecisionServiceRef = new AtomicServiceReference<AccessDecisionService>(KEY_ACCESS_DECISION_SERVICE);

    private final AtomicServiceReference<AuthorizationTableService> managementAuthzTable = new AtomicServiceReference<AuthorizationTableService>(KEY_AUTHORIZATION_TABLE_SERVICE);

    private Boolean reportAuthzCheckDetails;

    private NativeRcvt nativeRcvt = null;

    /**
     * Sets the NativeRcvt object reference.
     *
     * @param The NativeRcvt reference.
     */
    @Reference
    protected void setNativeRcvt(NativeRcvt nativeRcvt) {
        this.nativeRcvt = nativeRcvt;
    }

    /**
     * Unsets the NativeRcvt object reference.
     *
     * @param nativeRcvt The NativeRcvt reference.
     */
    protected void unsetNativeRcvt(NativeRcvt nativeRcvt) {
        if (this.nativeRcvt == nativeRcvt) {
            this.nativeRcvt = null;
        }
    }

    @Reference(name = KEY_ACCESS_DECISION_SERVICE,
               service = AccessDecisionService.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setAccessDecisionService(ServiceReference<AccessDecisionService> ref) {
        accessDecisionServiceRef.setReference(ref);
    }

    protected void unsetAccessDecisionService(ServiceReference<AccessDecisionService> ref) {
        accessDecisionServiceRef.unsetReference(ref);
    }

    @Reference(name = KEY_AUTHORIZATION_TABLE_SERVICE,
               service = AuthorizationTableService.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               target = "(com.ibm.ws.security.authorization.table.name=Management)")
    protected void setAuthorizationTableService(ServiceReference<AuthorizationTableService> ref) {
        managementAuthzTable.setReference(ref);
    }

    protected void unsetAuthorizationTableService(ServiceReference<AuthorizationTableService> ref) {
        managementAuthzTable.unsetReference(ref);
    }

    /**
     * DS Inject the NativeMethodManager service.
     */
    @Reference
    protected void setNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = nativeMethodManager;
        this.nativeMethodManager.registerNatives(SAFAuthorizationServiceImpl.class);
    }

    /**
     * DS inject
     */
    @Reference
    protected void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    /**
     * DS inject the SAFCredentialsService ref.
     */
    @Reference
    protected void setSafCredentialsService(SAFCredentialsService safCredentialsService) {
        this.safCredentialsService = safCredentialsService;
    }

    /**
     * Add the SAFRoleMapper ref. 1 or more SAFRoleMappers may be set.
     */
    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE,
               policy = ReferencePolicy.DYNAMIC)
    protected void setSafRoleMapper(SAFRoleMapper safRoleMapper, Map<String, Object> props) {
        safRoleMappers.put((String) props.get("component.name"), safRoleMapper);
    }

    /**
     * Remove the SAFRoleMapper ref.
     */
    protected void unsetSafRoleMapper(SAFRoleMapper safRoleMapper, Map<String, Object> props) {
        String componentName = (String) props.get("component.name");
        if (componentName.equals(roleMapperName)) {
            currentSafRoleMapper = null; // Unset cached ref to the SAFRoleMapper that has been deactivated.
        }
        safRoleMappers.remove(componentName);
    }

    /**
     * Retrieve the desired SAFRoleMapper from the map,
     * as indicated by the roleMapper config attribute.
     */
    protected SAFRoleMapper getRoleMapper() {
        if (currentSafRoleMapper == null) {
            currentSafRoleMapper = safRoleMappers.get(roleMapperName);
        }
        return currentSafRoleMapper;
    }

    /**
     * Required native authz service.
     */
    @Reference(target = "(&(native.service.name=CHKACCES)(is.authorized=true))")
    protected void setNativeService_CHKACCES(NativeService nativeService) {
    }

    /**
     * Required native authz service.
     */
    @Reference(target = "(&(native.service.name=CLSACTIV)(is.authorized=true))")
    protected void setNativeService_CLSACTIV(NativeService nativeService) {
    }

    /**
     * Required native authz service.
     */
    @Reference(target = "(&(native.service.name=RACREXTR)(is.authorized=true))")
    protected void setNativeService_RACREXTR(NativeService nativeService) {
    }

    /**
     * Invoked by OSGi when service is activated.
     */
    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        updateConfig(cc, props);
        accessDecisionServiceRef.activate(cc);
        managementAuthzTable.activate(cc);
    }

    /**
     * Invoked by OSGi when service is deactivated.
     */
    @Deactivate
    protected void deactivate(ComponentContext cc) {
        unregisterSafDelegationProvider();
        accessDecisionServiceRef.deactivate(cc);
        managementAuthzTable.deactivate(cc);
    }

    /**
     * Invoked by OSGi when the <safAuthorization> configuration has changed.
     */
    @Modified
    protected void modify(ComponentContext cc, Map<String, Object> props) {
        updateConfig(cc, props);
    }

    /**
     * This method is called whenever the SAF authorization config is updated.
     */
    protected void updateConfig(ComponentContext cc, Map<String, Object> props) {

        // Reset the safRoleMapper.  Gets refreshed under getRoleMapper()
        roleMapperName = (String) props.get(ROLE_MAPPER_KEY);
        currentSafRoleMapper = null;

        // Refresh ejbLogOption
        ejbLogOption = LogOption.NONE; // Default.
        Object ejbroleLogOption = props.get(EJBROLE_LOG_OPTION);
        if (ejbroleLogOption != null) {
            ejbLogOption = LogOption.valueOf((String) ejbroleLogOption);
        }

        // !! NOTE: this must be done AFTER roleMapperName has been refreshed.
        // Always unregister first, in case there's already a SAFDelegationProvider installed.
        unregisterSafDelegationProvider();
        if (props.get("enableDelegation") != null && (Boolean) props.get("enableDelegation")) {
            registerSafDelegationProvider(cc);
        }

        // Refresh reportAuthorizationCheckDetails
        reportAuthzCheckDetails = (Boolean) props.get("reportAuthorizationCheckDetails");

    }

    /**
     * Register the SAF delegation provider.
     */
    private void registerSafDelegationProvider(ComponentContext cc) {

        safDelegationProvider = new SAFDelegationProvider();
        safDelegationProvider.setSecurityService(securityService);
        safDelegationProvider.setSAFRoleMapper(getRoleMapper());
        safDelegationProvider.setNativeMethodManager(nativeMethodManager);

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("type", "safProvider");

        safDelegationProviderRegistration = cc.getBundleContext().registerService(DelegationProvider.class,
                                                                                  safDelegationProvider,
                                                                                  props);
    }

    /**
     * Un-register the SAF delegation provider.
     */
    private void unregisterSafDelegationProvider() {
        if (safDelegationProviderRegistration != null) {
            safDelegationProviderRegistration.unregister();
            safDelegationProviderRegistration = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @FFDCIgnore(SAFException.class)
    public boolean isEveryoneGranted(String resourceName, Collection<String> requiredRoles) {
        assertNotNull(resourceName, "resourceName is null");
        assertNotNull(requiredRoles, "requiredRoles is null");

        try {
            // Note that if the defaultCred is created successfully, then it will only be created
            // once and the SAFCredentialsService will cache it. If, however, the defaultCred CANNOT
            // be created, then this code will try to create it EVERY time. This allows the code to
            // respond dynamically (without a restart) if the user fixes up the defaultCredential
            // in the meantime.
            SAFCredential defaultCred = safCredentialsService.getDefaultCredential();

            // The defaultCred is created "lazily".  This call force its creation now.
            // If the creation fails, we catch the SAFException, log a msg, and ignore it.
            // If creation succeeds, we reset the defaultCredMsgAlreadyIssues flag.
            safCredentialsService.getSAFCredentialTokenBytes(defaultCred);

            // Successfully created cred.  Reset the flag (in case it gets broken again).
            defaultCredMsgAlreadyIssued = false;

            // Check whether the unauthenticated user has access.
            return checkRoles(defaultCred, resourceName, requiredRoles, LogOption.NONE);
        } catch (SAFException se) {
            // Failed to create the default credential. Issue a message (only once!) to alert the user.
            if (!defaultCredMsgAlreadyIssued) {
                Tr.warning(tc, "UNABLE_TO_CREATE_DEFAULT_CRED", se.getMessage());
                defaultCredMsgAlreadyIssued = true;
            }
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * Reference for results(Non official designations)
     * 'RCVT'= RACF
     * 'RTSS'= Top Secret
     * 'ACF2'= CA ACF2
     *
     */
    @Override
    public String getRCVTID() {

        return nativeRcvt.getRCVTID();
    }

    /**
     *
     * This isAuthorized is called by (web/ejb) container security when authorization is required.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isAuthorized(String resourceName,
                                Collection<String> requiredRoles,
                                Subject subject) {
        assertNotNull(resourceName, "resourceName is null");
        assertNotNull(requiredRoles, "requiredRoles is null");

        SAFCredential safCred = safCredentialsService.getSAFCredentialFromSubject(subject);

        // For collective certificate authenticate, we did not create the SAFCredential.
        // Use the management authorization table to retrieve the administrator role.
        if (isManagementAuthorizationNeeded(resourceName, safCred)) {
            return authorizeWithManagementAuthorizationTable(resourceName, requiredRoles, subject);
        }

        /*
         * Servlet 3.1 support a role called "**" that grants access to all authenticated users.
         */
        if (safCred != null && safCred.isAuthenticated() && requiredRoles.contains(UNDEFINED_STARSTAR_ALL_AUTHENTICATED)) {
            return true;
        }

        boolean authz = checkRoles(safCred, resourceName, requiredRoles, ejbLogOption);

        if (!authz) {
            // The given Subject is not granted. Check the unauthenticated user.
            // The unauthenticated user represents "everyone".
            authz = isEveryoneGranted(resourceName, requiredRoles);
        }

        return authz;
    }

    private boolean isManagementAuthorizationNeeded(String resourceName, SAFCredential safCred) {
        return safCred == null && resourceName.equalsIgnoreCase(ADMIN_RESOURCE_NAME);
    }

    private boolean authorizeWithManagementAuthorizationTable(String resourceName, Collection<String> requiredRoles, Subject subject) {
        boolean grant = false;
        SubjectHelper subjectHelper = new SubjectHelper();
        WSCredential wsCred = subjectHelper.getWSCredential(subject);
        if (wsCred != null) {
            String accessId = getAccessId(wsCred);
            String type = AccessIdUtil.getEntityType(accessId);
            String realm = AccessIdUtil.getRealm(accessId);
            if ("server".equalsIgnoreCase(type) && "collective".equalsIgnoreCase(realm) ||
                "user".equalsIgnoreCase(type) && "odr".equalsIgnoreCase(realm)) {
                AuthorizationTableService managementAuthzTableSvc = managementAuthzTable.getService();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "managementAuthzTableSvc: " + managementAuthzTableSvc);
                }
                if (managementAuthzTableSvc != null) {
                    AccessDecisionService accessDecisionService = accessDecisionServiceRef.getService();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "accessDecisionService: " + accessDecisionService);
                    }

                    if (accessDecisionService != null) {
                        if (isAllAuthenticatedGranted(accessDecisionService, managementAuthzTableSvc, resourceName, requiredRoles, subject)) {
                            grant = true;
                        } else {
                            Collection<String> assignedRoles = managementAuthzTableSvc.getRolesForAccessId(resourceName, accessId);
                            grant = accessDecisionService.isGranted(resourceName, requiredRoles, assignedRoles, subject);
                        }
                    }
                }
            }
        }
        return grant;
    }

    private boolean isAllAuthenticatedGranted(AccessDecisionService accessDecisionService, AuthorizationTableService managementAuthzTableSvc, String resourceName,
                                              Collection<String> requiredRoles, Subject subject) {
        Collection<String> roles = managementAuthzTableSvc.getRolesForSpecialSubject(resourceName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
        return accessDecisionService.isGranted(resourceName, requiredRoles, roles, subject);
    }

    /**
     *
     * This isAuthorized delegates to isAuthorized(String, String, AccessLevel, LogOption).
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isAuthorized(String className,
                                String resourceName,
                                AccessLevel accessLevel) {
        assertNotNull(className, "className is null");
        assertNotNull(resourceName, "resourceName is null");
        if (className.equalsIgnoreCase("DATASET")) {
            throw new IllegalArgumentException("Cannot check authorization for DATASET class. Use 'isAuthorizedToDataset' method.");
        }
        return isAuthorized(className, resourceName, accessLevel, null);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.ibm.wsspi.security.authorization.saf.SAFAuthorizationService#isAuthorized(java.lang.String, java.lang.String, com.ibm.wsspi.security.authorization.saf.AccessLevel,
     *      com.ibm.wsspi.security.authorization.saf.LogOption)
     */
    @Override
    public boolean isAuthorized(String className,
                                String resourceName,
                                AccessLevel accessLevel,
                                LogOption logOption) {
        assertNotNull(className, "className is null");
        assertNotNull(resourceName, "resourceName is null");
        if (className.equalsIgnoreCase("DATASET")) {
            throw new IllegalArgumentException("Cannot check authorization for DATASET class. Use 'isAuthorizedToDataset' method.");
        }
        try {
            return isAuthorized(className, resourceName, accessLevel, logOption, false);
        } catch (SAFAuthorizationException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAuthorized(String className,
                                String resourceName,
                                AccessLevel accessLevel,
                                LogOption logOption,
                                boolean throwExceptionOnFailure) throws SAFAuthorizationException {
        securityManagerCheck();
        assertNotNull(resourceName, "resourceName is null");
        assertNotNull(className, "className is null");

        if (className.equalsIgnoreCase("DATASET")) {
            throw new IllegalArgumentException("Cannot check authorization for DATASET class. Use 'isAuthorizedToDataset' method.");
        }

        boolean authResponse = false;
        final Subject subject = getEffectiveSubject();

        try {
            authResponse = isSubjectAuthorized(subject, className, resourceName, accessLevel, logOption, throwExceptionOnFailure, null, false);
            if (reportAuthzCheckDetails == true && throwExceptionOnFailure == true && authResponse == true) {
                Audit.audit(Audit.EventID.SECURITY_SAF_AUTHZ_DETAILS, 0, 0, 0, getSubjectUserId(subject), resourceName, className, authResponse, getPrincipalName(),
                            safCredentialsService.getProfilePrefix());
            }
        } catch (SAFException se) {
            handleThrowExceptionOnFailure(throwExceptionOnFailure, se, false);
        }
        return authResponse;
    }

    /**
     * This isAuthorized delegates to isAuthorized(Subject, String, String, AccessLevel, LogOption).
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isAuthorized(Subject subject,
                                String className,
                                String resourceName,
                                AccessLevel accessLevel) {
        assertNotNull(className, "className is null");
        assertNotNull(resourceName, "resourceName is null");
        if (className.equalsIgnoreCase("DATASET")) {
            throw new IllegalArgumentException("Cannot check authorization for DATASET class. Use 'isAuthorizedToDataset' method.");
        }
        return isAuthorized(subject, className, resourceName, accessLevel, null);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.ibm.wsspi.security.authorization.saf.SAFAuthorizationService#isAuthorized(javax.security.auth.Subject, java.lang.String, java.lang.String,
     *      com.ibm.wsspi.security.authorization.saf.AccessLevel, com.ibm.wsspi.security.authorization.saf.LogOption)
     */
    @Override
    public boolean isAuthorized(Subject subject,
                                String className,
                                String resourceName,
                                AccessLevel accessLevel,
                                LogOption logOption) {
        assertNotNull(className, "className is null");
        assertNotNull(resourceName, "resourceName is null");
        if (className.equalsIgnoreCase("DATASET")) {
            throw new IllegalArgumentException("Cannot check authorization for DATASET class. Use 'isAuthorizedToDataset' method.");
        }
        try {
            return isAuthorized(subject, className, resourceName, accessLevel, logOption, false);
            // Should not go to catch ever but in case, return false
        } catch (SAFAuthorizationException e) {
            return false;
        }
    }

    @Override
    public boolean isAuthorized(Subject subject,
                                String className,
                                String resourceName,
                                AccessLevel accessLevel,
                                LogOption logOption,
                                boolean throwExceptionOnFailure) throws SAFAuthorizationException {
        securityManagerCheck();
        assertNotNull(resourceName, "resourceName is null");
        assertNotNull(className, "className is null");
        if (className.equalsIgnoreCase("DATASET")) {
            throw new IllegalArgumentException("Cannot check authorization for DATASET class. Use 'isAuthorizedToDataset' method.");
        }
        boolean authResponse = false;
        try {
            authResponse = isSubjectAuthorized(subject, className, resourceName, accessLevel, logOption, throwExceptionOnFailure, null, false);
            if (reportAuthzCheckDetails == true && throwExceptionOnFailure == true && authResponse == true) {
                Audit.audit(Audit.EventID.SECURITY_SAF_AUTHZ_DETAILS, 0, 0, 0, getSubjectUserId(subject), resourceName, className, authResponse, getPrincipalName(),
                            safCredentialsService.getProfilePrefix());
            }
        } catch (SAFException se) {
            handleThrowExceptionOnFailure(throwExceptionOnFailure, se, false);
        }
        return authResponse;
    }

    /**
     * This isAuthorized delegates to isAuthorized(SAFCredential, String, String, AccessLevel, LogOption).
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isAuthorized(SAFCredential safCred,
                                String className,
                                String resourceName,
                                AccessLevel accessLevel) {
        assertNotNull(className, "className is null");
        assertNotNull(resourceName, "resourceName is null");
        if (className.equalsIgnoreCase("DATASET")) {
            throw new IllegalArgumentException("Cannot check authorization for DATASET class. Use 'isAuthorizedToDataset' method.");
        }
        return isAuthorized(safCred, className, resourceName, accessLevel, null);
    }

    /**
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.security.authorization.saf.SAFAuthorizationService#isAuthorized(com.ibm.wsspi.security.credentials.saf.SAFCredential, java.lang.String, java.lang.String,
     *      com.ibm.wsspi.security.authorization.saf.AccessLevel, com.ibm.wsspi.security.authorization.saf.LogOption)
     */
    @Override
    public boolean isAuthorized(SAFCredential safCred,
                                String className,
                                String resourceName,
                                AccessLevel accessLevel,
                                LogOption logOption) {
        securityManagerCheck();
        assertNotNull(resourceName, "resourceName is null");
        assertNotNull(className, "className is null");
        if (className.equalsIgnoreCase("DATASET")) {
            throw new IllegalArgumentException("Cannot check authorization for DATASET class. Use 'isAuthorizedToDataset' method.");
        }
        try {
            return isAuthorized(safCred, className, resourceName, accessLevel, logOption, false);
        } catch (SAFAuthorizationException e) {
            return false;
        }
    }

    @Override
    public boolean isAuthorized(SAFCredential safCredential,
                                String className,
                                String resourceName,
                                AccessLevel accessLevel,
                                LogOption logOption,
                                boolean throwExceptionOnFailure) throws SAFAuthorizationException {
        securityManagerCheck();
        assertNotNull(resourceName, "resourceName is null");
        assertNotNull(className, "className is null");
        if (className.equalsIgnoreCase("DATASET")) {
            throw new IllegalArgumentException("Cannot check authorization for DATASET class. Use 'isAuthorizedToDataset' method.");
        }
        boolean authResponse = false;
        try {
            authResponse = isSAFCredentialAuthorized(safCredential, className, resourceName, accessLevel, logOption, throwExceptionOnFailure, null, false);
            if (reportAuthzCheckDetails == true && throwExceptionOnFailure == true && authResponse == true) {
                Audit.audit(Audit.EventID.SECURITY_SAF_AUTHZ_DETAILS, 0, 0, 0, safCredential.getMvsUserId(), resourceName, className, authResponse, getPrincipalName(),
                            safCredentialsService.getProfilePrefix());
            }
        } catch (SAFException se) {
            handleThrowExceptionOnFailure(throwExceptionOnFailure, se, false);
        }
        return authResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGroupAuthorized(String groupName,
                                     String className,
                                     String resourceName,
                                     AccessLevel accessLevel,
                                     LogOption logOption) throws SAFAuthorizationException {
        return isGroupAuthorized(groupName, className, resourceName, accessLevel, logOption, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGroupAuthorized(String groupName,
                                     String className,
                                     String resourceName,
                                     AccessLevel accessLevel,
                                     LogOption logOption,
                                     boolean throwExceptionOnFailure) throws SAFAuthorizationException {
        securityManagerCheck();
        assertNotNull(groupName, "groupName is null");
        assertNotNull(resourceName, "resourceName is null");
        assertNotNull(className, "className is null");
        if (className.equalsIgnoreCase("DATASET")) {
            throw new IllegalArgumentException("Cannot check authorization for DATASET class. Use 'isGroupAuthorizedToDataset' method.");
        }
        groupName = groupName.toUpperCase();
        boolean authResponse = false;
        try {
            authResponse = checkGroupAccess(NativeMethodUtils.convertToEBCDIC(groupName),
                                            NativeMethodUtils.convertToEBCDIC(className),
                                            NativeMethodUtils.convertToEBCDIC(resourceName),
                                            null,
                                            false,
                                            accessLevel,
                                            logOption,
                                            throwExceptionOnFailure);
            if (reportAuthzCheckDetails == true && throwExceptionOnFailure == true && authResponse == true) {
                Audit.audit(Audit.EventID.SECURITY_SAF_AUTHZ_DETAILS, 0, 0, 0, groupName, resourceName, className, authResponse, getPrincipalName(),
                            safCredentialsService.getProfilePrefix());
            }
        } catch (SAFException se) {
            handleThrowExceptionOnFailure(throwExceptionOnFailure, se, true);
        }
        return authResponse;
    }

    /**
     * {@inheritDoc}
     *
     * @throws SAFAuthorizationException
     */
    @Override
    public boolean isGroupAuthorizedToDataset(String groupName,
                                              String resourceName,
                                              String volser,
                                              boolean vsam,
                                              AccessLevel accessLevel,
                                              LogOption logOption,
                                              boolean throwExceptionOnFailure) throws SAFAuthorizationException {
        securityManagerCheck();
        assertNotNull(groupName, "groupName is null");
        validateDatasetArguments(resourceName, volser);

        groupName = groupName.toUpperCase();
        boolean authResponse = false;
        try {
            authResponse = checkGroupAccess(NativeMethodUtils.convertToEBCDIC(groupName),
                                            NativeMethodUtils.convertToEBCDIC("DATASET"),
                                            NativeMethodUtils.convertToEBCDIC(resourceName),
                                            NativeMethodUtils.convertToEBCDIC(volser),
                                            vsam,
                                            accessLevel,
                                            logOption,
                                            throwExceptionOnFailure);

            if (reportAuthzCheckDetails == true && throwExceptionOnFailure == true && authResponse == true) {
                Audit.audit(Audit.EventID.SECURITY_SAF_AUTHZ_DETAILS, 0, 0, 0, groupName, resourceName, "DATASET", authResponse, getPrincipalName(),
                            safCredentialsService.getProfilePrefix());
            }
        } catch (SAFException se) {
            handleThrowExceptionOnFailure(throwExceptionOnFailure, se, true);
        }
        return authResponse;
    }

    /**
     * Retrieves the SAFCredential from the Subject and calls isSAFCredentialAuthorized.
     *
     * @throws SAFException
     */
    private boolean isSubjectAuthorized(Subject subject,
                                        String className,
                                        String resourceName,
                                        AccessLevel accessLevel,
                                        LogOption logOption,
                                        boolean throwExceptionOnFailure,
                                        String volser,
                                        boolean vsam) throws SAFException {
        SAFCredential safCred = safCredentialsService.getSAFCredentialFromSubject(subject);

        return isSAFCredentialAuthorized(safCred, className, resourceName, accessLevel, logOption, throwExceptionOnFailure, volser, vsam);
    }

    /**
     * Retrieves the SAFCredentialToken from the SAFCredential, converts Strings to EBCDIC byte[],
     * and calls checkAccess.
     *
     * @throws SAFException
     */
    private boolean isSAFCredentialAuthorized(SAFCredential safCred,
                                              String className,
                                              String resourceName,
                                              AccessLevel accessLevel,
                                              LogOption logOption,
                                              boolean reportFailureDetails,
                                              String volser,
                                              boolean vsam) throws SAFException {
        try {
            return checkAccess(safCred,
                               NativeMethodUtils.convertToEBCDIC(className),
                               NativeMethodUtils.convertToEBCDIC(resourceName),
                               accessLevel,
                               logOption,
                               reportFailureDetails,
                               true,
                               NativeMethodUtils.convertToEBCDIC(volser),
                               vsam);

        } catch (SAFException se) {
            // Cut audit here due to SAFException thrown in checkAccess
            SAFServiceResult sr = se.getSAFServiceResult();
            int wasRC = sr.getWasReturnCode();
            boolean isDatasetCheck = className.equalsIgnoreCase("DATASET");
            Audit.audit(Audit.EventID.SECURITY_SAF_AUTHZ,
                        (wasRC == 0) ? sr.getSAFReturnCode() : -1,
                        (wasRC == 0) ? sr.getRacfReturnCode() : -1,
                        (wasRC == 0) ? sr.getRacfReasonCode() : -1,
                        safCred.getUserId(),
                        resourceName,
                        className,
                        false,
                        getPrincipalName(),
                        safCredentialsService.getProfilePrefix(),
                        accessLevel.name,
                        (wasRC != 0) ? sr.getMessage() : null,
                        (isDatasetCheck) ? "isAuthorizedToDataset" : "isAuthorized",
                        (isDatasetCheck) ? volser : null,
                        (isDatasetCheck) ? String.valueOf(vsam) : null);
            // Throw SAFException so authorization failure details can be reported
            if (reportFailureDetails) {
                throw se;
            }

            // FFDC.  SAFExceptions are not percolated.  Just return false.
            return false;
        }
    }

    /**
     * Wraps a SAFCredential around the given mvsUserId, then calls isAuthorized(SAFCredential, ...)
     *
     * @return true if the given mvsUserId has the given accessLevel to the given className and resourceName.
     * @throws SAFAuthorizationException
     */
    @Override
    public boolean isAuthorized(String mvsUserId,
                                String className,
                                String resourceName,
                                AccessLevel accessLevel,
                                LogOption logOption) throws SAFAuthorizationException {
        assertNotNull(mvsUserId, "mvsUserId is null");
        assertNotNull(className, "className is null");
        assertNotNull(resourceName, "resourceName is null");
        if (className.equalsIgnoreCase("DATASET")) {
            throw new IllegalArgumentException("Cannot check authorization for DATASET class. Use 'isAuthorizedToDataset' method.");
        }

        try {
            return isAuthorized(mvsUserId,
                                className,
                                resourceName,
                                accessLevel,
                                logOption,
                                false);
        } catch (SAFAuthorizationException se) {
            throw se;
        }
    }

    /**
     * Wraps a SAFCredential around the given mvsUserId, then calls isAuthorized(SAFCredential, ...)
     *
     * @return true if the given mvsUserId has the given accessLevel to the given className and resourceName.
     * @throws SAFAuthorizationException
     */
    @Override
    public boolean isAuthorized(String mvsUserId,
                                String className,
                                String resourceName,
                                AccessLevel accessLevel,
                                LogOption logOption,
                                boolean throwExceptionOnFailure) throws SAFAuthorizationException {
        securityManagerCheck();
        assertNotNull(mvsUserId, "mvsUserId is null");
        assertNotNull(resourceName, "resourceName is null");
        assertNotNull(className, "className is null");
        if (className.equalsIgnoreCase("DATASET")) {
            throw new IllegalArgumentException("Cannot check authorization for DATASET class. Use 'isAuthorizedToDataset' method.");
        }

        boolean authResponse = false;
        boolean createdCredential = false;
        SAFCredential safCred = null;

        try {
            safCred = safCredentialsService.createAssertedCredential(mvsUserId, null, 0);
            createdCredential = true;
            authResponse = isSAFCredentialAuthorized(safCred, className, resourceName, accessLevel, logOption, throwExceptionOnFailure, null, false);
            // Cut audit report when user authentication is successful
            if (reportAuthzCheckDetails == true && throwExceptionOnFailure == true && authResponse == true) {
                Audit.audit(Audit.EventID.SECURITY_SAF_AUTHZ_DETAILS, 0, 0, 0, mvsUserId, resourceName, className, authResponse, getPrincipalName(),
                            safCredentialsService.getProfilePrefix());
            }
        } catch (SAFException se) {
            // If createdCredential false, saf credential services failed in this isAuth so we cut the audit here.
            // If createdCredential true, saf credential services fails in isSAFCredentialAuthorized --> cut audit there
            if (!createdCredential) {
                SAFServiceResult sr = se.getSAFServiceResult();
                int wasRC = sr.getWasReturnCode();
                Audit.audit(Audit.EventID.SECURITY_SAF_AUTHZ,
                            (wasRC == 0) ? sr.getSAFReturnCode() : -1,
                            (wasRC == 0) ? sr.getRacfReturnCode() : -1,
                            (wasRC == 0) ? sr.getRacfReasonCode() : -1,
                            mvsUserId,
                            resourceName,
                            className,
                            false,
                            getPrincipalName(),
                            safCredentialsService.getProfilePrefix(),
                            accessLevel.name,
                            (wasRC != 0) ? sr.getMessage() : null,
                            "isAuthorized",
                            null,
                            null);
                // SAFException came from createAssertedCredential which does not set these by default.
                // Setting them here to get picked up by handleThrowExceptionOnFailure
                se.getSAFServiceResult().setSafClass(className);
                se.getSAFServiceResult().setSafProfile(resourceName);
            }
            handleThrowExceptionOnFailure(throwExceptionOnFailure, se, false);
        }
        return authResponse;
    }

    @Override
    public boolean isAuthorizedToDataset(String mvsUserId,
                                         String resourceName,
                                         String volser,
                                         boolean vsam,
                                         AccessLevel accessLevel,
                                         LogOption logOption,
                                         boolean throwExceptionOnFailure) throws SAFAuthorizationException {
        securityManagerCheck();
        assertNotNull(mvsUserId, "mvsUserId is null");
        validateDatasetArguments(resourceName, volser);

        boolean authResponse = false;
        boolean createdCredential = false;
        SAFCredential safCred = null;

        try {
            safCred = safCredentialsService.createAssertedCredential(mvsUserId, null, 0);
            createdCredential = true;
            authResponse = isSAFCredentialAuthorized(safCred, "DATASET", resourceName, accessLevel, logOption, throwExceptionOnFailure, volser, vsam);
            // Cut audit report when user authentication is successful
            if (reportAuthzCheckDetails == true && throwExceptionOnFailure == true && authResponse == true) {
                Audit.audit(Audit.EventID.SECURITY_SAF_AUTHZ_DETAILS, 0, 0, 0, mvsUserId, resourceName, "DATASET", authResponse, getPrincipalName(),
                            safCredentialsService.getProfilePrefix());
            }
        } catch (SAFException se) {
            // If createdCredential false, saf credential services failed in this isAuth so we cut the audit here.
            // If createdCredential true, saf credential services fails in isSAFCredentialAuthorized --> cut audit there
            if (!createdCredential) {
                SAFServiceResult sr = se.getSAFServiceResult();
                int wasRC = sr.getWasReturnCode();
                Audit.audit(Audit.EventID.SECURITY_SAF_AUTHZ,
                            (wasRC == 0) ? sr.getSAFReturnCode() : -1,
                            (wasRC == 0) ? sr.getRacfReturnCode() : -1,
                            (wasRC == 0) ? sr.getRacfReasonCode() : -1,
                            mvsUserId,
                            resourceName,
                            "DATASET",
                            false,
                            getPrincipalName(),
                            safCredentialsService.getProfilePrefix(),
                            accessLevel.name,
                            (wasRC != 0) ? sr.getMessage() : null,
                            "isAuthorizedToDataset",
                            volser,
                            String.valueOf(vsam));
                // SAFException came from createAssertedCredential which does not set these by default.
                // Setting them here to get picked up by handleThrowExceptionOnFailure
                se.getSAFServiceResult().setSafClass("DATASET");
                se.getSAFServiceResult().setSafProfile(resourceName);
            }
            handleThrowExceptionOnFailure(throwExceptionOnFailure, se, false);
        }
        return authResponse;
    }

    @Override
    public boolean isAuthorizedToDataset(String resourceName,
                                         String volser,
                                         boolean vsam,
                                         AccessLevel accessLevel,
                                         LogOption logOption,
                                         boolean throwExceptionOnFailure) throws SAFAuthorizationException {
        securityManagerCheck();
        validateDatasetArguments(resourceName, volser);

        boolean authResponse = false;
        final Subject subject = getEffectiveSubject();

        try {
            authResponse = isSubjectAuthorized(subject, "DATASET", resourceName, accessLevel, logOption, throwExceptionOnFailure, volser, vsam);
            if (reportAuthzCheckDetails == true && throwExceptionOnFailure == true && authResponse == true) {
                Audit.audit(Audit.EventID.SECURITY_SAF_AUTHZ_DETAILS, 0, 0, 0, getSubjectUserId(subject), resourceName, "DATASET", authResponse, getPrincipalName(),
                            safCredentialsService.getProfilePrefix());
            }
        } catch (SAFException se) {
            handleThrowExceptionOnFailure(throwExceptionOnFailure, se, false);
        }
        return authResponse;
    }

    @Override
    public boolean isAuthorizedToDataset(Subject subject,
                                         String resourceName,
                                         String volser,
                                         boolean vsam,
                                         AccessLevel accessLevel,
                                         LogOption logOption,
                                         boolean throwExceptionOnFailure) throws SAFAuthorizationException {
        securityManagerCheck();
        validateDatasetArguments(resourceName, volser);

        boolean authResponse = false;
        try {

            authResponse = isSubjectAuthorized(subject, "DATASET", resourceName, accessLevel, logOption, throwExceptionOnFailure, volser, vsam);
            if (reportAuthzCheckDetails == true && throwExceptionOnFailure == true && authResponse == true) {
                Audit.audit(Audit.EventID.SECURITY_SAF_AUTHZ_DETAILS, 0, 0, 0, getSubjectUserId(subject), resourceName, "DATASET", authResponse, getPrincipalName(),
                            safCredentialsService.getProfilePrefix());
            }
        } catch (SAFException se) {
            handleThrowExceptionOnFailure(throwExceptionOnFailure, se, false);
        }
        return authResponse;
    }

    @Override
    public boolean isAuthorizedToDataset(SAFCredential safCredential,
                                         String resourceName,
                                         String volser,
                                         boolean vsam,
                                         AccessLevel accessLevel,
                                         LogOption logOption,
                                         boolean throwExceptionOnFailure) throws SAFAuthorizationException {
        securityManagerCheck();
        validateDatasetArguments(resourceName, volser);

        boolean authResponse = false;
        try {
            authResponse = isSAFCredentialAuthorized(safCredential, "DATASET", resourceName, accessLevel, logOption, throwExceptionOnFailure, volser, vsam);
            if (reportAuthzCheckDetails == true && throwExceptionOnFailure == true && authResponse == true) {
                Audit.audit(Audit.EventID.SECURITY_SAF_AUTHZ_DETAILS, 0, 0, 0, safCredential.getUserId(), resourceName, "DATASET", authResponse, getPrincipalName(),
                            safCredentialsService.getProfilePrefix());
            }
        } catch (SAFException se) {
            handleThrowExceptionOnFailure(throwExceptionOnFailure, se, false);
        }
        return authResponse;
    }

    /**
     * Check whether the given SAFCredentialToken has READ access to AT LEAST ONE of
     * the given requiredRoles.
     *
     * @param safCred       The SAFCredential for the user whom is being authorized.
     * @param resourceName  The protected resource.
     * @param requiredRoles A list of roles that may access the resource.
     * @param logOption     The logging/auditing option to pass to the SAF service.
     *
     * @return true if the user represented by the safCred is authorized to any of the requiredRoles or
     *         the requiredRoles list is empty.
     *         false if the user represented by the safCred is not authorized to any of the requiredRoles.
     *
     */
    protected boolean checkRoles(SAFCredential safCred,
                                 String resourceName,
                                 Collection<String> requiredRoles,
                                 LogOption logOption) {
        if (requiredRoles.isEmpty()) {
            return true;
        }

        for (Iterator<String> iter = requiredRoles.iterator(); iter.hasNext();) {
            String reqRole = iter.next();
            if (reqRole.equals(DEFINED_STARSTAR_ROLE)) {
                reqRole = "**";
            }
            String profile = getRoleMapper().getProfileFromRole(resourceName, reqRole);

            try {
                boolean result = checkAccess(safCred,
                                             NativeMethodUtils.convertToEBCDIC("EJBROLE"),
                                             NativeMethodUtils.convertToEBCDIC(profile),
                                             AccessLevel.READ,
                                             logOption,
                                             false,
                                             false,
                                             null,
                                             false);
                if (result) {
                    return true;
                }
            } catch (SAFException se) {
                // FFDC only. Don't percolate. Just return false.
                return false;
            }
        }

        // If we got here, then the safCredToken is not authorized to any of the required roles.
        return false;
    }

    /**
     * Check whether the given SAFCredentialToken has access to the resource.
     *
     * @param className            The SAF class (in null-terminated EBCDIC bytes).
     * @param resourceName         The protected resource (in null-terminated EBCDIC bytes).
     * @param accessLevel          The required access level. If null, then default is READ.
     * @param logOption            The SAF logging/auditing option. if null, then default is ASIS.
     * @param reportFailureDetails If true, detailed authorization failure information will be reported
     *                                 within a SAFException
     * @param isExternalCall       If true, audit report for SECURITY_SAF_AUTHZ will be cut. Otherwise it
     *                                 will not be.
     * @param volser               The volume serial number for DATASET authorization checks
     * @param vsam                 Flag to indicate whether dataset is vsam or non-vsam. Only set for DATASET
     *                                 class checks.
     *
     * @return true if the SAFCredentialToken is authorized to access the resource; false otherwise.
     *
     * @throws SAFException if the safcredtoken could not be obtained.
     */
    protected boolean checkAccess(SAFCredential safCred,
                                  byte[] className,
                                  byte[] resourceName,
                                  AccessLevel accessLevel,
                                  LogOption logOption,
                                  boolean reportFailureDetails,
                                  boolean isExternalCall,
                                  byte[] volser,
                                  boolean vsam) throws SAFException {
        accessLevel = (accessLevel != null) ? accessLevel : AccessLevel.READ;
        logOption = (logOption != null) ? logOption : LogOption.ASIS;
        int retryCount = 0;
        int rc = 0;
        SAFServiceResult safServiceResult = null;
        int wasRC = 0;

        do {
            safServiceResult = new SAFServiceResult();
            byte[] safCredToken = safCredentialsService.getSAFCredentialTokenBytes(safCred);
            rc = ntv_checkAccess(safCredToken,
                                 resourceName,
                                 className,
                                 NativeMethodUtils.convertToEBCDIC(safCredentialsService.getProfilePrefix()),
                                 volser,
                                 accessLevel.value,
                                 logOption.value,
                                 false,
                                 true,
                                 vsam,
                                 safServiceResult.getBytes());

        } while (rc != 0 && safServiceResult.isRetryable() && retryCount++ < MAX_RETRY_COUNT && SAFServiceResult.yield());

        // On WAS_INTERNAL error, omit saf/racf codes and put internal error message from safServiceResults in audit record
        if (isExternalCall && safServiceResult != null) {
            // Audit cut in isSAFAuthorizedCredentials when reportFailureDetails is true, otherwise cut audit here
            if (!(reportFailureDetails && rc != 0)) {
                boolean isDatasetCheck = "DATASET".equalsIgnoreCase(NativeMethodUtils.convertToASCII(className));
                wasRC = safServiceResult.getWasReturnCode();
                Audit.audit(Audit.EventID.SECURITY_SAF_AUTHZ,
                            (wasRC == 0) ? safServiceResult.getSAFReturnCode() : -1,
                            (wasRC == 0) ? safServiceResult.getRacfReturnCode() : -1,
                            (wasRC == 0) ? safServiceResult.getRacfReasonCode() : -1,
                            safCred.getUserId(),
                            NativeMethodUtils.convertToASCII(resourceName),
                            NativeMethodUtils.convertToASCII(className),
                            (rc == 0) ? true : false,
                            getPrincipalName(),
                            safCredentialsService.getProfilePrefix(),
                            accessLevel.name,
                            (wasRC != 0) ? safServiceResult.getMessage() : null,
                            (isDatasetCheck) ? "isAuthorizedToDataset" : "isAuthorized",
                            (isDatasetCheck) ? NativeMethodUtils.convertToASCII(volser) : null,
                            (isDatasetCheck) ? String.valueOf(vsam) : null);
            }
        }

        if (rc == 0) {
            // Authorization success.
            missingProfileMsgAlreadyIssued = false;
            return true;
        } else {
            // If it's an unexpected error, log it.
            if (safServiceResult.isUnexpected()) {
                safServiceResult.setAuthorizationFields(safCred.getUserId(),
                                                        NativeMethodUtils.convertToASCII(resourceName),
                                                        NativeMethodUtils.convertToASCII(className),
                                                        safCredentialsService.getProfilePrefix(),
                                                        NativeMethodUtils.convertToASCII(volser),
                                                        vsam);

                // Don't re-issue a missing profile error.
                if (!(safServiceResult.getMessage().contains("CWWKS2911E") && missingProfileMsgAlreadyIssued)) {
                    safServiceResult.logIfUnexpected();
                }

                // If a missing profile error, don't log again in the future.
                if (safServiceResult.getMessage().contains("CWWKS2911E")) {
                    missingProfileMsgAlreadyIssued = true;
                }
            }
        }

        if (reportFailureDetails) {
            // Set fields in safServiceResult so isAuthorized can get fields to fill exception
            safServiceResult.setAuthorizationFields(safCred.getUserId(),
                                                    NativeMethodUtils.convertToASCII(resourceName),
                                                    NativeMethodUtils.convertToASCII(className),
                                                    safCredentialsService.getProfilePrefix(),
                                                    NativeMethodUtils.convertToASCII(volser),
                                                    vsam);
            throw new SAFException(safServiceResult);
        }

        return false;
    }

    /**
     * Check whether the given GroupID has access to the resource.
     *
     * @param groupName            Name of group (in null-terminated EBCDIC bytes).
     * @param className            The SAF class (in null-terminated EBCDIC bytes).
     * @param resourceName         The protected resource (in null-terminated EBCDIC bytes).
     * @param volser               The volume serial number of a dataset resource
     * @param vsam                 Flag that indicates whether dataset is vsam or non-vsam
     * @param accessLevel          The required access level. If null, then default is READ.
     * @param logOption            The SAF logging/auditing option. if null, then default is ASIS.
     * @param reportFailureDetails If true, detailed authorization failure information will be reported
     *                                 within a SAFException
     *
     * @return true if the GroupID is authorized to access the resource; false otherwise.
     */
    protected boolean checkGroupAccess(byte[] groupName,
                                       byte[] className,
                                       byte[] resourceName,
                                       byte[] volser,
                                       boolean vsam,
                                       AccessLevel accessLevel,
                                       LogOption logOption,
                                       boolean reportFailureDetails) throws SAFException {

        accessLevel = (accessLevel != null) ? accessLevel : AccessLevel.READ;
        logOption = (logOption != null) ? logOption : LogOption.ASIS;

        int retryCount = 0;
        int rc = 0;
        SAFServiceResult safServiceResult;
        int wasRC = 0;

        do {
            safServiceResult = new SAFServiceResult();

            rc = ntv_checkGroupAccess(groupName,
                                      resourceName,
                                      className,
                                      NativeMethodUtils.convertToEBCDIC(safCredentialsService.getProfilePrefix()),
                                      accessLevel.value,
                                      logOption.value,
                                      false,
                                      volser,
                                      vsam,
                                      safServiceResult.getBytes());

        } while (rc != 0 && safServiceResult.isRetryable() && retryCount++ < MAX_RETRY_COUNT && SAFServiceResult.yield());

        // On WAS_INTERNAL error, omit saf/racf codes and get internal error message from safServiceResults
        wasRC = safServiceResult.getWasReturnCode();
        boolean isDatasetCheck = "DATASET".equalsIgnoreCase(NativeMethodUtils.convertToASCII(className));
        Audit.audit(Audit.EventID.SECURITY_SAF_AUTHZ,
                    (wasRC == 0) ? safServiceResult.getSAFReturnCode() : -1,
                    (wasRC == 0) ? safServiceResult.getRacfReturnCode() : -1,
                    (wasRC == 0) ? safServiceResult.getRacfReasonCode() : -1,
                    NativeMethodUtils.convertToASCII(groupName),
                    NativeMethodUtils.convertToASCII(resourceName),
                    NativeMethodUtils.convertToASCII(className),
                    (rc == 0) ? true : false,
                    getPrincipalName(),
                    safCredentialsService.getProfilePrefix(),
                    accessLevel.name,
                    (wasRC != 0) ? safServiceResult.getMessage() : null,
                    (isDatasetCheck) ? "isGroupAuthorizedToDataset" : "isGroupAuthorized",
                    (isDatasetCheck) ? NativeMethodUtils.convertToASCII(volser) : null,
                    (isDatasetCheck) ? String.valueOf(vsam) : null);

        if (rc == 0) {
            // Authorization success.
            missingProfileMsgAlreadyIssued = false;
            return true;
        } else {
            //             If it's an unexpected error, log it.
            if (safServiceResult.isUnexpected()) {
                safServiceResult.setGroupAuthorizationFields(NativeMethodUtils.convertToASCII(groupName),
                                                             NativeMethodUtils.convertToASCII(resourceName),
                                                             NativeMethodUtils.convertToASCII(className),
                                                             safCredentialsService.getProfilePrefix(),
                                                             NativeMethodUtils.convertToASCII(volser),
                                                             vsam);

                // Don't re-issue a missing profile error.
                if (!(safServiceResult.getMessage().contains("CWWKS2911E") && missingProfileMsgAlreadyIssued)) {
                    safServiceResult.logIfUnexpected();
                }

                // If a missing profile error, don't log again in the future.
                if (safServiceResult.getMessage().contains("CWWKS2911E")) {
                    missingProfileMsgAlreadyIssued = true;
                }
            }
        }
        if (reportFailureDetails) {
            // Set fields in safServiceResult so isAuthorized can get fields to fill exception
            safServiceResult.setGroupAuthorizationFields(NativeMethodUtils.convertToASCII(groupName),
                                                         NativeMethodUtils.convertToASCII(resourceName),
                                                         NativeMethodUtils.convertToASCII(className),
                                                         safCredentialsService.getProfilePrefix(),
                                                         NativeMethodUtils.convertToASCII(volser),
                                                         vsam);
            throw new SAFException(safServiceResult);
        }
        return false;
    }

    /**
     * Check whether or not the given SAF CLASS is active (via RACROUTE REQUEST=STAT).
     *
     * @param className The SAF CLASS to check.
     *
     * @return true if the SAF CLASS is active; false if it is inactive.
     */
    protected boolean isSAFClassActive(String className) {
        int rc = ntv_isSAFClassActive(NativeMethodUtils.convertToEBCDIC(className));
        if (rc == 1) {
            return true;
        } else if (rc == 0) {
            return false;
        } else {
            throw new RuntimeException("unexpected SAF failure", new SAFException("ntv_isSAFClassActive returned an unexpected rc: " + rc));
        }
    }

    /**
     * Simple utility method that compares the first parm to null, and if it is,
     * throws a NullPointerException using the second parm as the message.
     */
    @Trivial
    protected void assertNotNull(Object o, String msg) {
        if (o == null) {
            throw new NullPointerException(msg);
        }
    }

    /**
     * Check that the caller has permission to access SAF_AUTHZ_PERM.
     */
    private void securityManagerCheck() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(SAF_AUTHZ_PERM);
        }
    }

    /**
     * Helper method that validates that resourceName and volser are not null and that
     * they are within the correct length range.
     *
     * @param resourceName
     * @param volser
     */
    @Trivial
    private void validateDatasetArguments(String resourceName, String volser) {
        assertNotNull(resourceName, "resourceName is null.");
        assertNotNull(volser, "volser is null.");
        if (volser.length() > 6)
            throw new IllegalArgumentException("Volser too long. Must be maximum of 6 characters.");
        if (resourceName.length() > 44)
            throw new IllegalArgumentException("Resource name too long. Must be maximum of 44-bytes.");
    }

    /**
     * Look for the Subject associated with this thread to authorize.
     */
    private Subject getEffectiveSubject() {
        Subject subject = null;

        SubjectManager sm = new SubjectManager();
        subject = sm.getInvocationSubject();

        if (subject == null) {
            subject = sm.getCallerSubject();
        }

        return subject;
    }

    /**
     * Check whether or not the given SAF CLASS is active (via RACROUTE REQUEST=STAT).
     *
     * @param className        The SAF CLASS to check.
     * @param safServiceResult Output parm, contains the SAF service rc/rsn codes.
     *
     * @return 1 if the SAF CLASS is active;
     *         0 if inactive;
     *         an error code if there was an unexpected failure.
     */
    protected native int ntv_isSAFClassActive(byte[] className);

    /**
     * Fairly generic native utility to check whether or not the given SAF credential has
     * authority to access the given resource (SAF profile) under the given class for the
     * given applid. The required authority is indicated by accessLevel.
     *
     * The authorization check is made against the underlying SAF product, using native SAF
     * authorized services (RACROUTE).
     *
     * Defined in security_saf_authz.c.
     *
     * @param safCredentialToken Token returned by a previous call to SAFCredentialsService.create*Credential.
     * @param resource           The SAF resource profile to be authorized against.
     * @param className          The CLASS of the given resource profile.
     * @param applid             The APPLNAME.
     * @param volser             The Volume Serial Number (Used for Dataset authorization checks only).
     * @param accessLevel        A saf_access_level indicating the required authority (e.g. READ, UPDATE, etc).
     * @param logOption          A saf_log_option that tells SAF how to log the authz request.
     * @param msgSuppress        Boolean that tells SAF whether or not to suppress SAF messages.
     * @param fastAuth           Use RACROUTE REQUEST=FASTAUTH instead of REQUEST=AUTH.
     * @param vsam               Flag that inidicates whether dataset is vsam or non-vsam.
     * @param safServiceResult   Output parm where SAF return/reason codes are copied back to Java.
     *
     * @return 0 if credential is authorized; otherwise a non-zero error code. See jsafServiceResult
     *         for SAF failure codes.
     */
    protected native int ntv_checkAccess(byte[] safCredentialToken,
                                         byte[] resource,
                                         byte[] className,
                                         byte[] applid,
                                         byte[] volser,
                                         int accessLevel,
                                         int logOption,
                                         boolean suppressMessage,
                                         boolean fastAuth,
                                         boolean vsam,
                                         byte[] safServiceResult);

    /**
     * Fairly generic native utility to check whether or not the given SAF credential has
     * authority to access the given resource (SAF profile) under the given class for the
     * given applid. The required authority is indicated by accessLevel.
     *
     * The authorization check is made against the underlying SAF product, using native SAF
     * authorized services (RACROUTE).
     *
     * Defined in security_saf_authz.c.
     *
     * @param groupName        The name of the group who's access is being checked for the given resource.
     * @param resource         The SAF resource profile to be authorized against.
     * @param className        The CLASS of the given resource profile.
     * @param applid           The APPLNAME.
     * @param accessLevel      A saf_access_level indicating the required authority (e.g. READ, UPDATE, etc).
     * @param logOption        A saf_log_option that tells SAF how to log the authz request.
     * @param msgSuppress      Boolean that tells SAF whether or not to suppress SAF messages.
     * @param volser           The Volume Serial Number (Used for Dataset authorization checks only).
     * @param vsam             Flag that inidicates whether dataset is vsam or non-vsam.
     * @param safServiceResult Output parm where SAF return/reason codes are copied back to Java.
     *
     * @return 0 if credential is authorized; otherwise a non-zero error code. See jsafServiceResult
     *         for SAF failure codes.
     */
    protected native int ntv_checkGroupAccess(byte[] groupName,
                                              byte[] resource,
                                              byte[] className,
                                              byte[] applid,
                                              int accessLevel,
                                              int logOption,
                                              boolean suppressMessage,
                                              byte[] volser,
                                              boolean vsam,
                                              byte[] safServiceResult);

    /**
     * Get the access ID from the specified credential.
     *
     * @parm cred the WSCredential to search
     * @return the user access id of the credential, or null when the
     *         cred is expired or destroyed
     */
    private String getAccessId(WSCredential cred) {
        String accessId = null;
        try {
            accessId = cred.getAccessId();
        } catch (CredentialExpiredException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting the access id: " + e);
            }
        } catch (CredentialDestroyedException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting the access id: " + e);
            }
        }
        return accessId;
    }

    private String getPrincipalName() {
        Subject subject = getEffectiveSubject();
        String pName = null;
        Set<WSPrincipal> principals;
        if (subject == null)
            return null;
        try {
            principals = subject.getPrincipals(WSPrincipal.class);
        } catch (NullPointerException e) {
            return null;
        }

        if (principals == null)
            return null;

        if (!principals.isEmpty() && principals.size() == 1) {
            pName = principals.iterator().next().getName();
        }
        return pName;
    }

    private String getSubjectUserId(Subject subject) {
        SAFCredential s = safCredentialsService.getSAFCredentialFromSubject(subject);
        return s.getUserId();
    }

    /**
     * Helper method that handles throwing an exception on saf authorization failure. This creates
     * and throws a SAFAuthorizationException which is filled in based on how the throwExceptionOnFailure flag
     * is set and creates a SECURITY_SAF_AUTHZ_DETAILS audit record.
     *
     * @param throwExceptionOnFailure - if true, fill in details of SAFAuthorizationException and write audit record
     * @param se                      - SAFException which holds authorization check results
     * @param isGroupAuth             - if true, use groupSecurityName, otherwise use userSecurityName
     * @throws SAFAuthorizationException
     */
    private void handleThrowExceptionOnFailure(boolean throwExceptionOnFailure, SAFException se, boolean isGroupAuth) throws SAFAuthorizationException {
        // Check if we want to throw an exception
        if (throwExceptionOnFailure) {
            SAFAuthorizationException safAuthException;
            // The reportAuthCheckDetails flag defaults to false and is set in config by
            // <safAuthorization reportAuthorizationCheckDetails="true" />.
            if (reportAuthzCheckDetails == true) {
                // Create exception with details
                SAFServiceResult safServiceRes = se.getSAFServiceResult();
                int safReturnCode = safServiceRes.getSAFReturnCode();
                int racfReturnCode = safServiceRes.getRacfReturnCode();
                int racfReasonCode = safServiceRes.getRacfReasonCode();
                // The group or user security name depending on if this is called by isGroupValid or isUserValid
                String securityName = (isGroupAuth) ? safServiceRes.getGroupSecurityName() : safServiceRes.getUserSecurityName();
                String applid = safServiceRes.getApplID();
                String resourceName = safServiceRes.getSafProfile();
                String className = safServiceRes.getSafClass();
                safAuthException = new SAFAuthorizationException(safReturnCode, racfReturnCode, racfReasonCode, securityName, applid, resourceName, className);

                // Cuts audit record when user authentication is failure
                Audit.audit(Audit.EventID.SECURITY_SAF_AUTHZ_DETAILS, safReturnCode, racfReturnCode, racfReasonCode, securityName, resourceName, className, false,
                            getPrincipalName(), applid);
            } else {
                // Create blank exception, values initialize to default
                safAuthException = new SAFAuthorizationException();
            }
            throw safAuthException;
        }
    }
}
