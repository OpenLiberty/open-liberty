/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.jaspi;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.RegistrationListener;
import javax.security.auth.message.config.ServerAuthConfig;
import javax.security.auth.message.config.ServerAuthContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.security.auth.WSLoginFailedException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.audit.Audit;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.webcontainer.internalRuntimeExport.srt.IPrivateRequestAttributes;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.JaspiService;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebAuthenticator;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorHelper;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.WebRequestImpl;
import com.ibm.ws.webcontainer.security.WebSecurityContext;
import com.ibm.ws.webcontainer.security.metadata.FormLoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.ws.webcontainer.security.util.WebConfigUtils;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.jaspi.ProviderService;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

@Component(service = { JaspiService.class, WebAuthenticator.class },
           name = "com.ibm.ws.security.jaspi",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM",
                        "com.ibm.ws.security.webAuthenticator.type=JASPI" })
public class JaspiServiceImpl implements JaspiService, WebAuthenticator {

    private static final TraceComponent tc = Tr.register(JaspiServiceImpl.class);
    private static final String AUTH_TYPE = "javax.servlet.http.authType";
    private static final String IS_MANDATORY_POLICY = "javax.security.auth.message.MessagePolicy.isMandatory";
    private static final String JACC_POLICY_CONTEXT = "javax.security.jacc.PolicyContext";
    public static final String UNAUTHENTICATED_ID = "UNAUTHENTICATED"; // TODO find a better home for this

    private static final String KEY_UNAUTHENTICATED_SUBJECT_SERVICE = "unauthenticatedSubjectService";
    private UnauthenticatedSubjectService unauthenticatedSubjectService;
    private boolean providerConfigModified = false;
    private WebProviderAuthenticatorHelper authHelper = null;
    private final SubjectHelper subjectHelper = new SubjectHelper();
    private SubjectManager subjectManager = null;
    public HashMap<String, Object> extraAuditData = new HashMap<String, Object>();

    public JaspiServiceImpl() {}

    @Reference(name = KEY_UNAUTHENTICATED_SUBJECT_SERVICE,
               service = UnauthenticatedSubjectService.class,
               cardinality = ReferenceCardinality.MANDATORY,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setUnauthenticatedSubjectService(UnauthenticatedSubjectService srv) {
        this.unauthenticatedSubjectService = srv;
    }

    protected void unsetUnauthenticatedSubjectService(UnauthenticatedSubjectService srv) {
        if (this.unauthenticatedSubjectService == srv) {
            this.unauthenticatedSubjectService = null;
        }
    }

    private static final String KEY_JASPI_PROVIDER = "jaspiProvider";
    protected final AtomicServiceReference<ProviderService> jaspiProviderServiceRef = new AtomicServiceReference<ProviderService>(KEY_JASPI_PROVIDER);
    private static final String KEY_JASPI_BRIDGE_BUILDER = "bridgeBuilder";
    protected final AtomicServiceReference<BridgeBuilderService> bridgeBuilderServiceRef = new AtomicServiceReference<BridgeBuilderService>(KEY_JASPI_BRIDGE_BUILDER);

    @Reference(name = KEY_JASPI_PROVIDER,
               service = ProviderService.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setJaspiProvider(ServiceReference<ProviderService> reference) {
        jaspiProviderServiceRef.setReference(reference);
        providerConfigModified = true;
        Tr.info(tc, "JASPI_PROVIDER_SERVICE_ACTIVATED", new Object[] { jaspiProviderServiceRef.getService().getClass() });
    }

    protected void unsetJaspiProvider(ServiceReference<ProviderService> reference) {
        Tr.info(tc, "JASPI_PROVIDER_SERVICE_DEACTIVATED", new Object[] { jaspiProviderServiceRef.getService() != null ? jaspiProviderServiceRef.getService().getClass() : null });
        jaspiProviderServiceRef.unsetReference(reference);
        providerConfigModified = true;
    }

    @Reference(name = KEY_JASPI_BRIDGE_BUILDER,
               service = BridgeBuilderService.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setBridgeBuilder(ServiceReference<BridgeBuilderService> reference) {
        bridgeBuilderServiceRef.setReference(reference);
    }

    protected void unsetBridgeBuilder(ServiceReference<BridgeBuilderService> reference) {
        bridgeBuilderServiceRef.unsetReference(reference);
    }

    public static final String KEY_SECURITY_SERVICE = "securityService";
    protected final AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);

    @Reference(name = KEY_SECURITY_SERVICE,
               service = SecurityService.class,
               cardinality = ReferenceCardinality.MANDATORY,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.setReference(reference);
    }

    protected void unsetSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.unsetReference(reference);
    }

    static final String KEY_LOCATION_SERVICE = "locationService";
    private final static AtomicServiceReference<WsLocationAdmin> locationService = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_SERVICE);

    @Reference(service = WsLocationAdmin.class, name = KEY_LOCATION_SERVICE,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setLocationService(ServiceReference<WsLocationAdmin> reference) {
        locationService.setReference(reference);
    }

    protected void unsetLocationService(ServiceReference<WsLocationAdmin> reference) {
        locationService.unsetReference(reference);
    }

    static final String SERVER_CONFIG_LOCATION = "${server.config.dir}";

    /**
     * Return the absolute path of the given resource path relative to the
     * server config dir. If the given resource path is already absolute
     * then just return it. If the location admin service is not available
     * then return null.
     *
     * @param resourcePath path to resource
     * @return absolute path or null
     */
    static String getServerResourceAbsolutePath(String resourcePath) {
        String path = null;
        File f = new File(resourcePath);
        if (f.isAbsolute()) {
            path = resourcePath;
        } else {
            WsLocationAdmin wla = locationService.getServiceWithException();
            if (wla != null)
                path = wla.resolveString(SERVER_CONFIG_LOCATION + "/" + resourcePath);
        }
        return path;
    }

    @Activate
    protected void activate(ComponentContext cc) {
        locationService.activate(cc);
        jaspiProviderServiceRef.activate(cc);
        bridgeBuilderServiceRef.activate(cc);
        securityServiceRef.activate(cc);
        AuthConfigFactoryWrapper.setFactoryImplementation();
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        locationService.deactivate(cc);
        jaspiProviderServiceRef.deactivate(cc);
        bridgeBuilderServiceRef.deactivate(cc);
        securityServiceRef.deactivate(cc);
    }

    public static class PostInvokeJaspiContext implements JaspiAuthContext {

        private final ServerAuthContext authContext;
        private final MessageInfo msgInfo;
        private boolean runSecureResponse;

        public PostInvokeJaspiContext(ServerAuthContext authContext, MessageInfo msgInfo) {
            this.authContext = authContext;
            this.msgInfo = msgInfo;
        }

        @Override
        public MessageInfo getMessageInfo() {
            return msgInfo;
        }

        @Override
        public ServerAuthContext getServerAuthContext() {
            return authContext;
        }

        @Override
        public boolean runSecureResponse() {
            return runSecureResponse;
        }

        @Override
        public void setRunSecureResponse(boolean isSet) {
            runSecureResponse = isSet;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer.security.WebAuthenticator#authenticate(com.ibm.ws.webcontainer.security.WebRequest)
     */
    @FFDCIgnore({ com.ibm.ws.security.authentication.AuthenticationException.class })
    @Override
    public AuthenticationResult authenticate(WebRequest webRequest) {
        Subject sessionSubject = null;
        JaspiRequest jaspiRequest = new JaspiRequest(webRequest, null);
        // can set authenticate method here if webrequest has no websecuritycontext
        AuthenticationResult result = null;
        AuthConfigProvider provider = getAuthConfigProvider(jaspiRequest.getAppContext());

        if (provider != null) {
            try {
                if (jaspiRequest.getWebSecurityContext() != null) {
                    JaspiAuthContext jaspiContext = getJaspiAuthContext(jaspiRequest, provider);
                    webRequest.getWebSecurityContext().setJaspiAuthContext(jaspiContext);
                }
                // if we have a jaspi session subject from a previous request
                // then set it as the current subject (both caller and invocation)
                sessionSubject = getSessionSubject(jaspiRequest);
                if (sessionSubject != null) {
                    SubjectManager sm = new SubjectManager();
                    sm.setCallerSubject(sessionSubject);
                    sm.setInvocationSubject(sessionSubject);
                }
                result = authenticate(new Subject(),
                                      getJaspiAuthType(jaspiRequest),
                                      jaspiRequest,
                                      provider);

            } catch (AuthenticationException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Internal error during JASPI authentication", e);
                if (result == null)
                    result = new AuthenticationResult(AuthResult.FAILURE, e.getMessage());
            }
        } else {
            result = new AuthenticationResult(AuthResult.CONTINUE, "No JASPIC provider found for request: " + webRequest.getHttpServletRequest().getRequestURI());
        }

        if (provider != null && provider.getClass() != null) {
            result.setAuditAuthConfigProviderName(provider.getClass().toString());
            result.setAuditAuthConfigProviderAuthType(getRequestAuthType(jaspiRequest.getHttpServletRequest(), "AUTH_TYPE"));
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer.security.WebAuthenticator#authenticate(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    @FFDCIgnore({ com.ibm.ws.security.authentication.AuthenticationException.class })
    public AuthenticationResult authenticate(HttpServletRequest request,
                                             HttpServletResponse response,
                                             HashMap<String, Object> props) throws Exception {
        WebRequest webRequest = new WebRequestImpl(request, response, ((WebAppConfig) props.get("webAppConfig")).getApplicationName(), null, (SecurityMetadata) props.get("securityMetadata"), null, (WebAppSecurityConfig) props.get("webAppSecurityConfig"));
        JaspiRequest jaspiRequest = new JaspiRequest(webRequest, (WebAppConfig) props.get("webAppConfig"));
        AuthenticationResult result = null;
        AuthConfigProvider provider = getAuthConfigProvider(jaspiRequest.getAppContext());
        if (provider != null) {
            try {
                if (jaspiRequest.getWebSecurityContext() != null) {
                    JaspiAuthContext jaspiContext = getJaspiAuthContext(jaspiRequest, provider);
                    webRequest.getWebSecurityContext().setJaspiAuthContext(jaspiContext);
                }
                result = authenticate(new Subject(),
                                      getJaspiAuthType(jaspiRequest),
                                      jaspiRequest,
                                      provider);

            } catch (AuthenticationException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Internal error during JASPI authentication", e);
                result = new AuthenticationResult(AuthResult.FAILURE, e.getMessage());

            }
        } else {
            result = new AuthenticationResult(AuthResult.CONTINUE, "No JASPI provider found for request: " + request.getRequestURI());

        }
        return result;
    }

    @FFDCIgnore({ javax.security.auth.message.AuthException.class,
                  com.ibm.websphere.security.auth.WSLoginFailedException.class })
    private AuthenticationResult authenticate(Subject clientSubject,
                                              String authType,
                                              JaspiRequest jaspiRequest,
                                              AuthConfigProvider provider) throws AuthenticationException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "authenticate", new Object[] { clientSubject, authType, jaspiRequest, provider });

        AuthenticationResult authResult = null;
        Subject serviceSubject = null;
        AuthStatus status = null;
        WebSecurityContext webSecurityContext = jaspiRequest.getWebSecurityContext();
        jaspiRequest.getHttpServletRequest().getServletContext().setAttribute("com.ibm.ws.security.jaspi.authenticated", Boolean.toString(Boolean.TRUE));

        try {
            ServerAuthContext authContext = getServerAuthContext(jaspiRequest, provider);
            MessageInfo msgInfo = jaspiRequest.getMessageInfo();
            setRequestAuthType(jaspiRequest.getHttpServletRequest(), authType);

            if (webSecurityContext != null) {
                setRunSecureResponse(true, (JaspiAuthContext) webSecurityContext.getJaspiAuthContext());
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "******* calling JASPI provider authContext.validateRequest", new Object[] { "authContext=" + authContext, clientSubject, msgInfo });
            }
            //-----------------------------------------------------------------------------
            // This is where we actually call the JASPI provider to do the authentication
            //-----------------------------------------------------------------------------
            status = authContext.validateRequest(msgInfo, clientSubject, serviceSubject);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "******* return from JASPI provider authContext.validateRequest, status: " + status);
            }
            authResult = createAuthenticationResult(clientSubject, jaspiRequest, status, msgInfo, isJsr375BridgeProvider(provider));
            if (provider != null && provider.getClass() != null) {
                authResult.setAuditAuthConfigProviderName(provider.getClass().toString());
                authResult.setAuditAuthConfigProviderAuthType(getRequestAuthType(jaspiRequest.getHttpServletRequest(), "AUTH_TYPE"));
            }
        } catch (AuthException e) {
            AuthenticationException ex = new AuthenticationException("JASPIC Authenticated with status: SEND_FAILURE, exception: " + e);
            ex.initCause(e);
            if (webSecurityContext != null) {
                setRunSecureResponse(false, (JaspiAuthContext) webSecurityContext.getJaspiAuthContext());
            }
            throw ex;
        } catch (WSLoginFailedException e) {
            AuthenticationException ex = new AuthenticationException("Custom login failure after JASPIC authentication completed successfully, exception: " + e);
            ex.initCause(e);
            throw ex;
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "authenticate", status);
        return authResult;
    }

    private AuthenticationResult createAuthenticationResult(@Sensitive Subject clientSubject, JaspiRequest jaspiRequest, AuthStatus status,
                                                            MessageInfo msgInfo, boolean isJSR375) throws WSLoginFailedException {
        if (System.getSecurityManager() == null) {
            return processAuthStatus(clientSubject, jaspiRequest, status, msgInfo, isJSR375);
        } else {
            return processAuthStatusWithJava2Security(clientSubject, jaspiRequest, status, msgInfo, isJSR375);
        }
    }

    @SuppressWarnings("rawtypes")
    private AuthenticationResult processAuthStatus(Subject clientSubject, JaspiRequest jaspiRequest, AuthStatus status,
                                                   MessageInfo msgInfo, boolean isJSR375) throws WSLoginFailedException {
        AuthenticationResult authResult;
        if (AuthStatus.SUCCESS == status || AuthStatus.SEND_SUCCESS == status) {
            // if the provider asked that the subject be used on subsequent
            // invocations then indicate that in the request object. later we will
            // create an ltpa token cookie for the jaspi session
            Map msgInfoMap = msgInfo.getMap();
            if (msgInfoMap != null) {
                String session = (String) msgInfoMap.get("javax.servlet.http.registerSession");
                boolean isJaspiSession = Boolean.valueOf(session).booleanValue();
                if (isJaspiSession) {
                    Map<String, Object> props = new HashMap<String, Object>();
                    props.put("javax.servlet.http.registerSession", session);
                    jaspiRequest.getWebRequest().setProperties(props);
                    setUsageAttribute(getCustomCredentials(clientSubject), AuthenticationConstants.INTERNAL_AUTH_PROVIDER_JASPIC);
                } else if ("FORM".equals(getRequestAuthType(jaspiRequest.getHttpServletRequest(), "AUTH_TYPE"))) {
                    if (isJSR375) {
                        // this is for jsr375 form/custom form without setting registerSession. the next call will reach to the provider,
                        // then the ltpatoken2 cookie will be deleted after successful return.
                        setUsageAttribute(getCustomCredentials(clientSubject), AuthenticationConstants.INTERNAL_AUTH_PROVIDER_JSR375_FORM);
                    } else {
                        // this is for original japsic provider which support the form login. this attribute indicates that LtpaToken2
                        // cookie can be used for the authentication just one time in the WebProviderAuthenticatorProxy then the cookie will be deleted.
                        setUsageAttribute(getCustomCredentials(clientSubject), AuthenticationConstants.INTERNAL_AUTH_PROVIDER_JASPIC_FORM);
                    }
                }
            }
            // if the provider gave us an HttServletRequestWrapper (or response) then save it
            // in the servlet context. Our Filter will set it as the request/response object
            // that is passed to the servlet when it is invoked.
            Object request = msgInfo.getRequestMessage();
            if (request != null && request != jaspiRequest.getHttpServletRequest()) {
                jaspiRequest.getHttpServletRequest().setAttribute("com.ibm.ws.security.jaspi.servlet.request.wrapper", request);
            }
            Object response = msgInfo.getResponseMessage();
            if (response != null && response != jaspiRequest.getHttpServletResponse()) {
                jaspiRequest.getHttpServletRequest().setAttribute("com.ibm.ws.security.jaspi.servlet.response.wrapper", response);
            }
            Subject callerSubject = doHashTableLogin(clientSubject, jaspiRequest);
            if (callerSubject != null && !callerSubject.getPrincipals().isEmpty()) {
                extraAuditData.put("jaspiSubject", callerSubject.getPrincipals().iterator().next().getName());
            }
            authResult = mapToAuthenticationResult(status, jaspiRequest, callerSubject);
            setRequestAuthType(msgInfo, jaspiRequest);
        } else {
            authResult = mapToAuthenticationResult(status, jaspiRequest, null);
        }
        return authResult;
    }

    @FFDCIgnore(java.security.PrivilegedActionException.class)
    private AuthenticationResult processAuthStatusWithJava2Security(@Sensitive Subject clientSubject, JaspiRequest jaspiRequest, AuthStatus status,
                                                                    MessageInfo msgInfo, boolean isJSR375) throws WSLoginFailedException {
        try {
            return AccessController.doPrivileged(new ProcessAuthStatusPrivilegedExceptionAction(clientSubject, jaspiRequest, status, msgInfo, isJSR375));
        } catch (PrivilegedActionException privException) {
            throw (WSLoginFailedException) privException.getException();
        }
    }

    /*
     * Class to avoid tracing Subject that otherwise is traced when using
     * an anonymous PrivilegedAction resulting in an ACE when application code
     * is in the call stack.
     */
    private class ProcessAuthStatusPrivilegedExceptionAction implements PrivilegedExceptionAction<AuthenticationResult> {

        private final Subject clientSubject;
        private final JaspiRequest jaspiRequest;
        private final AuthStatus status;
        private final MessageInfo msgInfo;
        private final boolean isJSR375;

        @Trivial
        public ProcessAuthStatusPrivilegedExceptionAction(Subject clientSubject, JaspiRequest jaspiRequest, AuthStatus status, MessageInfo msgInfo, boolean isJSR375) {
            this.clientSubject = clientSubject;
            this.jaspiRequest = jaspiRequest;
            this.status = status;
            this.msgInfo = msgInfo;
            this.isJSR375 = isJSR375;
        }

        @Override
        public AuthenticationResult run() throws Exception {
            return processAuthStatus(clientSubject, jaspiRequest, status, msgInfo, isJSR375);
        }

    }

    /**
     * Some comment why we're doing this
     */
    private AuthConfigProvider getAuthConfigProvider(String appContext) {
        AuthConfigProvider provider = null;
        AuthConfigFactory providerFactory = getAuthConfigFactory();
        if (providerFactory != null) {
            if (providerConfigModified &&
                providerFactory instanceof ProviderRegistry) {
                ((ProviderRegistry) providerFactory).setProvider(jaspiProviderServiceRef.getService());
                providerConfigModified = false;
            }
            provider = providerFactory.getConfigProvider("HttpServlet", appContext, (RegistrationListener) null);
        }
        return provider;
    }

    public JaspiAuthContext getJaspiAuthContext(JaspiRequest jaspiRequest, AuthConfigProvider provider) throws AuthenticationException {
        JaspiAuthContext jaspiContext = null;
        try {
            ServerAuthContext authContext = getServerAuthContext(jaspiRequest, provider);
            if (authContext != null) {
                jaspiContext = new JaspiServiceImpl.PostInvokeJaspiContext(authContext, jaspiRequest.getMessageInfo());
            }
        } catch (Exception e) {
            AuthenticationException ex = new AuthenticationException("Unable to get JASPI ServerAuthContext.");
            ex.initCause(e);
            throw ex;
        }
        return jaspiContext;
    }

    @SuppressWarnings("unchecked")
    protected MessageInfo newMessageInfo(JaspiRequest jaspiRequest) {
        HttpServletRequest req = jaspiRequest.getHttpServletRequest();
        MessageInfo msgInfo = new JaspiMessageInfo(req, jaspiRequest.getHttpServletResponse());
        msgInfo.getMap().put(IS_MANDATORY_POLICY, Boolean.toString(jaspiRequest.isMandatory()));
        return msgInfo;
    }

    protected ServerAuthContext getServerAuthContext(JaspiRequest jaspiRequest, AuthConfigProvider provider) throws AuthenticationException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getServerAuthContext", new Object[] { jaspiRequest.getWebSecurityContext(), provider });
        ServerAuthContext authContext = null;
        WebSecurityContext webSecurityContext = jaspiRequest.getWebSecurityContext();
        JaspiAuthContext jaspiContext = null;
        if (webSecurityContext != null && (jaspiContext = (JaspiAuthContext) webSecurityContext.getJaspiAuthContext()) != null) {
            authContext = (ServerAuthContext) jaspiContext.getServerAuthContext();
        } else {
            try {
                authContext = getAuthContextFromProvider(jaspiRequest, provider);
            } catch (Exception e) {
                AuthenticationException ex = new AuthenticationException("Unable to get JASPI ServerAuthContext.");
                ex.initCause(e);
                throw ex;
            }
        }
        return authContext;
    }

    protected ServerAuthContext getAuthContextFromProvider(JaspiRequest jaspiRequest, AuthConfigProvider provider) throws AuthException, SecurityException {
        ServerAuthContext authContext = null;
        String appContext = jaspiRequest.getAppContext();
        if (provider != null) {
            CallbackHandler jaspiCallbackHandler = getCallbackHandler(provider);
            ServerAuthConfig authConfig = provider.getServerAuthConfig("HttpServlet", appContext, jaspiCallbackHandler);
            MessageInfo msgInfo = newMessageInfo(jaspiRequest);
            jaspiRequest.setMessageInfo(msgInfo);
            String authContextID = authConfig.getAuthContextID(msgInfo);
            Map<String, String> props = getAuthContextProps(jaspiRequest);
            Subject serviceSubject = null;
            authContext = authConfig.getAuthContext(authContextID, serviceSubject, props);
        }
        return authContext;
    }

    private CallbackHandler getCallbackHandler(final AuthConfigProvider provider) {
        CallbackHandler callbackHandler;

        if (isJsr375BridgeProvider(provider)) {
            callbackHandler = new NonMappingCallbackHandler(this);
        } else {
            callbackHandler = new JaspiCallbackHandler(this);
        }

        return callbackHandler;
    }

    private boolean isJsr375BridgeProvider(AuthConfigProvider provider) {
        return BridgeBuilderService.PROVIDER_DESCRIPTION == getProviderDescription(provider);
    }

    private String getProviderDescription(final AuthConfigProvider provider) {
        String description;
        if (System.getSecurityManager() == null) {
            description = getDescription(provider);
        } else {
            description = AccessController.doPrivileged(new PrivilegedAction<String>() {

                @Override
                public String run() {
                    return getDescription(provider);
                }
            });
        }
        return description;
    }

    private String getDescription(AuthConfigProvider provider) {
        AuthConfigFactory factory = AuthConfigFactory.getFactory();
        String id = factory.getRegistrationIDs(provider)[0];
        return factory.getRegistrationContext(id).getDescription();
    }

    /*
     * The runtime must also ensure that the value returned by calling
     * getAuthType on the HttpServletRequest is consistent in terms of being null
     * or non-null with the value returned by getUserPrincipal. When getAuthType
     * is to return a non-null value, the runtime must consult the Map of
     * the MessageInfo object used in the call to validateRequest to determine
     * if it contains an entry for the key "javax.servlet.http.authType".
     * If the Map contains an entry for the key, the runtime must obtain (from
     * the Map) the value corresponding to the key and establish it as the
     * getAuthType return value. If the Map does not contain an entry for the key,
     * and an auth-method is defined in the login-config element of the
     * deployment descriptor for the web application, the runtime must establish
     * the value from the auth-method as the value returned by getAuthType.
     * If the Map does not contain an entry for the key, and the deployment
     * descriptor does not define an auth-method, the runtime must establish a
     * non-null value of its choice as the value returned by getAuthType.
     */
    protected void setRequestAuthType(MessageInfo msgInfo, JaspiRequest jaspiRequest) {
        String authType = null;
        if (msgInfo.getMap().containsKey(AUTH_TYPE)) {
            authType = (String) msgInfo.getMap().get(AUTH_TYPE);
        } else {
            authType = getDDAuthMethod(jaspiRequest);
            if (authType == null) {
                authType = "JASPI";
            }
        }
        HttpServletRequest req = (HttpServletRequest) msgInfo.getRequestMessage();
        setRequestAuthType(req, authType);
    }

    protected String getDDAuthMethod(JaspiRequest jaspiRequest) {
        String authType = null;
        if (jaspiRequest != null) {
            LoginConfiguration loginCfg = jaspiRequest.getLoginConfig();
            if (loginCfg != null) {
                authType = loginCfg.getAuthenticationMethod();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "login configuration", new Object[] { loginCfg, authType });
            }
        }
        return authType;
    }

    protected String getJaspiAuthType(JaspiRequest jaspiRequest) {
        String authType = getDDAuthMethod(jaspiRequest);
        if (authType != null &&
            !authType.equals(LoginConfiguration.BASIC) &&
            !authType.equals(LoginConfiguration.CLIENT_CERT)) {
            authType = LoginConfiguration.FORM;
        } else {
            authType = LoginConfiguration.BASIC;
        }
        return authType;
    }

    protected void setRequestAuthType(HttpServletRequest req, String authType) {
        if (authType != null) {

            setPrivateAttributes(req, "AUTH_TYPE", authType);
        }
    }

    protected String getRequestAuthType(HttpServletRequest req, String key) {
        return (String) getPrivateAttributes(req, key);
    }

    protected int getResponseStatus(HttpServletResponse rsp) {
        if (rsp instanceof IExtendedResponse) {
            return ((IExtendedResponse) rsp).getStatusCode();
        }
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    protected AuthenticationResult mapToAuthenticationResult(AuthStatus status, JaspiRequest jaspiRequest, Subject clientSubject) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "mapToAuthenticationResult", "AuthStatus=" + status);
        AuthenticationResult authResult = null;
        String pretty = "FAILURE";
        if (AuthStatus.SUCCESS == status || AuthStatus.SEND_SUCCESS == status) {

            authResult = new AuthenticationResult(AuthResult.SUCCESS, clientSubject);
            pretty = "SUCCESS";

        } else if (AuthStatus.SEND_CONTINUE == status) {

            int responseStatus = getResponseStatus(jaspiRequest.getHttpServletResponse());
            HttpServletRequest req = jaspiRequest.getHttpServletRequest();
            switch (responseStatus) {

                case HttpServletResponse.SC_UNAUTHORIZED:
                    String realm = (String) jaspiRequest.getMessageInfo().getMap().get(AttributeNameConstants.WSCREDENTIAL_REALM);
                    authResult = new AuthenticationResult(AuthResult.SEND_401, realm != null ? realm : (String) null);
                    pretty = "SEND_401";
                    break;

                case HttpServletResponse.SC_MOVED_TEMPORARILY:
                case HttpServletResponse.SC_SEE_OTHER:
                case HttpServletResponse.SC_TEMPORARY_REDIRECT:
                    String loginURL = getLoginURL(jaspiRequest, req);
                    String query = req.getQueryString();
                    String originalURL = req.getRequestURL().append(query != null ? "?" + query : "").toString();
                    authResult = new AuthenticationResult(AuthResult.REDIRECT, loginURL);
                    pretty = "REDIRECT";
                    ReferrerURLCookieHandler referrerURLHandler = WebConfigUtils.getWebAppSecurityConfig().createReferrerURLCookieHandler();
                    referrerURLHandler.setReferrerURLCookie(req, authResult, originalURL);
                    break;

                default:
                    authResult = new AuthenticationResult(AuthResult.RETURN, "Returning response from JASPIC Authenticated with status: " + responseStatus);
                    break;
            }

        } else if (AuthStatus.SEND_FAILURE == status) {
            pretty = "SEND_FAILURE";
            String detail = "Returning response from JASPIC Authenticated with status: " + pretty + ", map to AuthResult.RETURN";
            authResult = new AuthenticationResult(AuthResult.RETURN, detail);
            if (tc.isDebugEnabled())
                Tr.debug(tc, detail);
        } else {
            authResult = new AuthenticationResult(AuthResult.RETURN, "Returning response from JASPIC Authentication failed, unexpected JASPIC AuthStatus: " + status
                                                                     + ", map to AuthResult.RETURN");
            pretty = status.toString();
        }

        if (authResult.getStatus().equals(AuthResult.RETURN)) {
            Tr.info(tc, "JASPI_PROVIDER_FAILED_AUTHENTICATE", new Object[] { status, jaspiRequest.getHttpServletRequest().getRequestURI(),
                                                                             jaspiProviderServiceRef.getService() != null ? jaspiProviderServiceRef.getService().getClass() : null });
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "mapToAuthenticationResult", "Jaspi AuthenticationResult=" + pretty);
        return authResult;
    }

    /**
     * Create a WAS Subject using the HashTable obtained from the JASPI provider
     *
     * @param clientSubject Subject containing the JASPI HashTable
     * @param jaspiRequest
     * @return the WAS Subject
     * @throws WSLoginFailedException
     */
    protected Subject doHashTableLogin(Subject clientSubject, JaspiRequest jaspiRequest) throws WSLoginFailedException {
        Subject authenticatedSubject = null;
        final Hashtable<String, Object> hashTable = getCustomCredentials(clientSubject);
        String unauthenticatedSubjectString = UNAUTHENTICATED_ID;
        String user = null;
        if (hashTable == null) {
            // The JASPI provider did not add creds to the clientSubject. If we
            // have a session subject then just return success and we'll use the
            // session subject which has already been set on the thread
            Subject sessionSubject = getSessionSubject(jaspiRequest);
            if (sessionSubject != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "No HashTable returned by the JASPI provider. Using JASPI session subject.");
                return sessionSubject;
            }
            MessageInfo msgInfo = jaspiRequest.getMessageInfo();
            boolean isProtected = Boolean.parseBoolean((String) msgInfo.getMap().get(IS_MANDATORY_POLICY));
            if (isProtected) {
                String msg = "JASPI HashTable login cannot be performed, JASPI provider did not return a HashTable.";
                throw new WSLoginFailedException(msg);
            } else {
                user = unauthenticatedSubjectString;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Web resource is unprotected and Subject does not have a HashTable.");
            }
        } else {
            user = (String) hashTable.get(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME);
        }
        boolean isUnauthenticated = unauthenticatedSubjectString.equals(user);
        if (isUnauthenticated) {
            authenticatedSubject = getUnauthenticatedSubject();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "JASPI Subject is unauthenticated, HashTable login is not necessary.");
        } else {
            if (user == null)
                user = "";
            Subject loginSubject = clientSubject;
            // If we have a session subject then do the hashtable login with the session subject
            // so we pick up all the creds from the original session login. Add the custom
            // credential hashtable (that the JASPI provider returned) to the session subject.
            // Note we must "clone" the session subject as it is read only
            final Subject sessionSubject = getSessionSubject(jaspiRequest);
            if (sessionSubject != null) {
                final Subject clone = new Subject();
                clone.getPrivateCredentials().addAll(sessionSubject.getPrivateCredentials());
                clone.getPublicCredentials().addAll(sessionSubject.getPublicCredentials());
                clone.getPrincipals().addAll(sessionSubject.getPrincipals());
                // add the hashtable from the JASPI provider
                clone.getPrivateCredentials().add(hashTable);
                loginSubject = clone;
            }
            final HttpServletRequest req = jaspiRequest.getHttpServletRequest();
            final HttpServletResponse res = (HttpServletResponse) jaspiRequest.getMessageInfo().getResponseMessage();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "JASPI login with HashTable: " + hashTable);
            }
            AuthenticationResult result = getWebProviderAuthenticatorHelper().loginWithHashtable(req, res, loginSubject);

            authenticatedSubject = result.getSubject();
            // Remove the custom credential hashtable from the session subject
            if (sessionSubject != null) {
                removeCustomCredentials(sessionSubject, hashTable);
            }
            if (authenticatedSubject == null) {
                throw new WSLoginFailedException("JASPI HashTable login failed, user: " + user);
            }
        }
        return authenticatedSubject;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Hashtable<String, Object> getCustomCredentials(@Sensitive final Subject clientSubject) {
        return (Hashtable<String, Object>) subjectHelper.getSensitiveHashtableFromSubject(clientSubject);
    }

    private Subject getSessionSubject(JaspiRequest jaspiRequest) {
        Subject sessionSubject = null;
        Map<String, Object> requestProps = jaspiRequest.getWebRequest().getProperties();
        if (requestProps != null) {
            sessionSubject = (Subject) requestProps.get("javax.servlet.http.registerSession.subject");
        }
        return sessionSubject;
    }

    protected synchronized WebProviderAuthenticatorHelper getWebProviderAuthenticatorHelper() {
        if (authHelper == null) {
            authHelper = new WebProviderAuthenticatorHelper(securityServiceRef);
        }
        return authHelper;
    }

    @SuppressWarnings("rawtypes")
    private void removeCustomCredentials(Subject subject, Hashtable creds) {
        if (subject != null) {
            Set<Hashtable> s = subject.getPrivateCredentials(Hashtable.class);
            if (s == null || s.isEmpty()) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "No custom credentials to remove from Subject.");
            } else {
                for (Hashtable t : s) {
                    if (t == creds) {
                        s.remove(t);
                        break;
                    }
                }
            }
        }
    }

    void setRunSecureResponse(boolean isSet, JaspiAuthContext jaspiContext) {
        if (jaspiContext != null) {
            jaspiContext.setRunSecureResponse(isSet);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "setRunSecureResponse: " + isSet);
        }
    }

    private Map<String, String> getAuthContextProps(JaspiRequest jaspiRequest) {
        Map<String, String> props = new HashMap<String, String>();
        String jaccContext = null;

        // TODO fix when jacc is available
//        if (webRequest.isJaccEnabled())
//            jaccContext = WSAccessManager.getContextID(req.getAppName()) + "/" + webRequest.getModuleName();
//        else
//            jaccContext = "href:" + cellName + "/" + req.getAppName() + "/" + webRequest.getModuleName();
        jaccContext = "href:" + jaspiRequest.getApplicationName() + "/" + jaspiRequest.getModuleName();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "JACC Policy Context: " + jaccContext);
        props.put(JACC_POLICY_CONTEXT, jaccContext);
        return props;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer.security.JaspiService#getUnauthenticatedSubject()
     */
    @Override
    public Subject getUnauthenticatedSubject() {
        return unauthenticatedSubjectService.getUnauthenticatedSubject();
    }

    // TODO find a good home somewhere
    private static void setPrivateAttributes(HttpServletRequest req, String key, Object object) {
        HttpServletRequest sr = req;
        if (sr instanceof HttpServletRequestWrapper) {
            HttpServletRequestWrapper w = (HttpServletRequestWrapper) sr;
            // make sure we drill all the way down to an SRTServletRequest...there
            // may be multiple proxied objects
            sr = (HttpServletRequest) w.getRequest();
            while (sr != null && sr instanceof HttpServletRequestWrapper)
                sr = (HttpServletRequest) ((HttpServletRequestWrapper) sr).getRequest();
        }
        if (sr != null && sr instanceof IPrivateRequestAttributes) {
            ((IPrivateRequestAttributes) sr).setPrivateAttribute(key, object);
        }
    }

    // TODO find a good home somewhere
    private static Object getPrivateAttributes(HttpServletRequest req, String key) {
        HttpServletRequest sr = req;
        if (sr instanceof HttpServletRequestWrapper) {
            HttpServletRequestWrapper w = (HttpServletRequestWrapper) sr;
            sr = (HttpServletRequest) w.getRequest();
            while (sr != null && sr instanceof HttpServletRequestWrapper)
                sr = (HttpServletRequest) ((HttpServletRequestWrapper) sr).getRequest();
        }
        if (sr != null && sr instanceof IPrivateRequestAttributes) {
            return ((IPrivateRequestAttributes) sr).getPrivateAttribute(key);
        }
        return null;
    }

    // TODO find a good home somewhere
    private String getLoginURL(JaspiRequest jaspiRequest, HttpServletRequest req) {
        String loginURL = null;
        LoginConfiguration loginConfig = jaspiRequest.getLoginConfig();
        if (loginConfig != null) {
            FormLoginConfiguration formLoginConfig = loginConfig.getFormLoginConfiguration();
            if (formLoginConfig != null) {
                String formLoginPageURL = formLoginConfig.getLoginPage();
                StringBuilder builder = new StringBuilder(req.getRequestURL());
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "getFormURL", new Object[] { "formLoginPageURL=" + formLoginPageURL, " requestURL=" + builder });
                int hostIndex = builder.indexOf("//");
                int contextIndex = builder.indexOf("/", hostIndex + 2);
                builder.replace(contextIndex, builder.length(), normalizeURL(formLoginPageURL, req.getContextPath()));
                loginURL = builder.toString();
            }
        }
        return loginURL;
    }

    // TODO find a good home somewhere
    private String normalizeURL(String url, String contextPath) {
        if (contextPath.equals("/"))
            contextPath = "";
        if (!url.startsWith("/"))
            url = "/" + url;
        return contextPath + url;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer.security.JaspiService#postInvoke(com.ibm.ws.webcontainer.security.WebSecurityContext)
     */
    @Override
    public void postInvoke(WebSecurityContext webSecurityContext) throws AuthenticationException {
        AuthStatus status = null;
        if (webSecurityContext != null) {
            JaspiAuthContext jaspiContext = (JaspiAuthContext) webSecurityContext.getJaspiAuthContext();
            if (!jaspiContext.runSecureResponse()) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "postInvoke", "skip secureResponse.");
                return;
            }
            MessageInfo msgInfo = (MessageInfo) jaspiContext.getMessageInfo();
            ServerAuthContext authContext = (ServerAuthContext) jaspiContext.getServerAuthContext();
            Subject serviceSubject = webSecurityContext.getReceivedSubject();
            try {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "secureResponse with Jaspi",
                             new Object[] { "authContext=" + authContext, "serviceSubject=" + serviceSubject, msgInfo });

                status = authContext.secureResponse(msgInfo, serviceSubject);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "secureResponse status: " + status);

                // TODO Which reply or exception?
                if (AuthStatus.SEND_SUCCESS != status &&

                    AuthStatus.SEND_FAILURE != status &&

                    AuthStatus.SEND_CONTINUE != status) {

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "secureResponse  AuthStatus=" + status);
                    String msg = "Unexpected AuthStatus received during secureResponse() status=" + status
                                 + ", MessageInfo=" + msgInfo + ", ServerAuthContext=" + authContext;
                    throw new AuthenticationException(msg);
                }
            } catch (AuthException e) {
                /*
                 * The runtime must perform whatever processing it requires to complete the processing of a request that failed after
                 * (or during) service invocation, and prior to communicating the invocation result to the client runtime. The runtime
                 * may send (without calling secureResponse) an appropriate response message of its choice. If a failure message is
                 * returned, it should indicate that the failure in request processing occurred after the service invocation.
                 */
                throw new AuthenticationException("JASPI authentication failed after invoking the requested target service.", e);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer.security.JaspiService#logout(javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse, com.ibm.ws.webcontainer.security.WebAppSecurityConfig)
     */
    @Override
    public void logout(HttpServletRequest req,
                       HttpServletResponse res,
                       WebAppSecurityConfig webAppSecConfig) throws AuthenticationException {
        Subject subject = null;
        WebRequest webRequest = new WebRequestImpl(req, res, null, null, null, null, webAppSecConfig);
        JaspiRequest jaspiRequest = new JaspiRequest(webRequest, null);
        AuthConfigProvider provider = getAuthConfigProvider(jaspiRequest.getAppContext());
        if (provider != null) {
            try {
                ServerAuthContext authContext = getServerAuthContext(jaspiRequest, provider);
                MessageInfo msgInfo = newMessageInfo(jaspiRequest);
                subject = getSubjectManager().getInvocationSubject();
                if (subject == null) {
                    subject = new Subject();
                }
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "cleanSubject with Jaspi", new Object[] { "authContext=" + authContext, msgInfo });
                authContext.cleanSubject(msgInfo, subject);
            } catch (AuthException e) {
                AuthenticationException ex = new AuthenticationException("JASPI cleanSubject failure: " + e);
                ex.initCause(e);
                throw ex;
            }
        }

        AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, subject);

        authResult.setAuditCredType(AuditEvent.CRED_TYPE_JASPIC);
        if (provider != null)
            authResult.setAuditAuthConfigProviderName(provider.getClass().toString());
        authResult.setAuditAuthConfigProviderAuthType(getRequestAuthType(jaspiRequest.getHttpServletRequest(), "AUTH_TYPE"));
        authResult.setAuditOutcome(AuditEvent.OUTCOME_SUCCESS);
        Audit.audit(Audit.EventID.SECURITY_API_AUTHN_TERMINATE_01, req, authResult, Integer.valueOf(res.getStatus()));

    }

    private synchronized SubjectManager getSubjectManager() {
        if (subjectManager == null) {
            subjectManager = new SubjectManager();
        }
        return subjectManager;
    }

    private void setAuditCredValue(AuthenticationResult result) {
        if (extraAuditData.get("jaspiSubject") != null) {
            result.setAuditCredValue((String) extraAuditData.get("jaspiSubject"));
        }

    }

    /*
     * This is for performance - so we will not call jaspi processing for a request
     * if there are no providers registered.
     *
     * @see com.ibm.ws.webcontainer.security.JaspiService#isAnyProviderRegistered()
     */
    @Override
    public boolean isAnyProviderRegistered(WebRequest webRequest) {
        // default to true for case where a custom factory is used (i.e. not our ProviderRegistry)
        // we will assume that some provider is registered so we will call jaspi to
        // process the request.
        boolean result = true;
        AuthConfigFactory providerFactory = getAuthConfigFactory();
        BridgeBuilderService bridgeBuilderService = bridgeBuilderServiceRef.getService();
        if (bridgeBuilderService != null) {
            JaspiRequest jaspiRequest = new JaspiRequest(webRequest, null); //TODO: Some paths have a WebAppConfig that should be taken into accounnt when getting the appContext
            String appContext = jaspiRequest.getAppContext();
            bridgeBuilderService.buildBridgeIfNeeded(appContext, providerFactory);
        }

        if (providerFactory != null && providerFactory instanceof ProviderRegistry) {
            // if the user defined feature provider came or went, process that 1st
            if (providerConfigModified) {
                ((ProviderRegistry) providerFactory).setProvider(jaspiProviderServiceRef.getService());
            }
            providerConfigModified = false;
            result = ((ProviderRegistry) providerFactory).isAnyProviderRegistered();
        }
        return result;
    }

    private AuthConfigFactory getAuthConfigFactory() {
        return AccessController.doPrivileged(new PrivilegedAction<AuthConfigFactory>() {

            @Override
            public AuthConfigFactory run() {
                return AuthConfigFactoryWrapper.getFactory();
            }
        });
    }

    @Override
    public boolean isProcessingNewAuthentication(HttpServletRequest req) {
        BridgeBuilderService bridgeBuilderService = bridgeBuilderServiceRef.getService();
        if (bridgeBuilderService != null) {
            return bridgeBuilderService.isProcessingNewAuthentication(req);
        }
        return false;
    }

    @Override
    public boolean isCredentialPresent(HttpServletRequest req) {
        BridgeBuilderService bridgeBuilderService = bridgeBuilderServiceRef.getService();
        if (bridgeBuilderService != null) {
            return bridgeBuilderService.isCredentialPresent(req);
        }
        return false;
    }

    private void setUsageAttribute(Hashtable<String, Object> hashTable, String value) {
        if (hashTable != null) {
            hashTable.put(AuthenticationConstants.INTERNAL_AUTH_PROVIDER, value);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Token Usage is set as " + value);
            }
        }
    }
}
