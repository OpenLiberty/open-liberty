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
package com.ibm.ws.security.authorization.jacc.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.security.auth.Subject;
import javax.security.jacc.EJBMethodPermission;
import javax.security.jacc.EJBRoleRefPermission;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.WebRoleRefPermission;
import javax.security.jacc.WebUserDataPermission;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditAuthenticationResult;
import com.ibm.ws.security.authorization.jacc.JaccService;
import com.ibm.ws.security.authorization.jacc.MethodInfo;
import com.ibm.ws.security.authorization.jacc.RoleInfo;
import com.ibm.ws.security.authorization.jacc.common.PolicyConfigurationManager;
import com.ibm.ws.security.authorization.jacc.common.PolicyProxy;
import com.ibm.ws.security.authorization.jacc.common.ProviderServiceProxy;
import com.ibm.ws.security.authorization.jacc.ejb.EJBSecurityPropagator;
import com.ibm.ws.security.authorization.jacc.ejb.EJBSecurityValidator;
import com.ibm.ws.security.authorization.jacc.ejb.EJBService;
import com.ibm.ws.security.authorization.jacc.web.ServletService;
import com.ibm.ws.security.authorization.jacc.web.WebSecurityPropagator;
import com.ibm.ws.security.authorization.jacc.web.WebSecurityValidator;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = JaccService.class, name = "com.ibm.ws.security.authorization.jacc", configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM")
public class JaccServiceImpl implements JaccService {
    private static final TraceComponent tc = Tr.register(JaccServiceImpl.class);

    private static final String JACC_EJB_METHOD_ARGUMENT = "RequestMethodArgumentsRequired";
    static final String KEY_JACC_PROVIDER_SERVICE_PROXY = "jaccProviderServiceProxy";
    private final String KEY_LOCATION_ADMIN = "locationAdmin";
    private final AtomicServiceReference<ProviderServiceProxy> jaccProviderServiceProxy = new AtomicServiceReference<ProviderServiceProxy>(KEY_JACC_PROVIDER_SERVICE_PROXY);
    private final AtomicServiceReference<WsLocationAdmin> locationAdminRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_ADMIN);

    private static final String KEY_SERVLET_SERVICE = "servletService";
    private final AtomicServiceReference<ServletService> servletServiceRef = new AtomicServiceReference<ServletService>(KEY_SERVLET_SERVICE);
    private static final String KEY_EJB_SERVICE = "ejbService";
    private final AtomicServiceReference<EJBService> ejbServiceRef = new AtomicServiceReference<EJBService>(KEY_EJB_SERVICE);

    private String policyName = null;
    private String factoryName = null;

    private PolicyProxy policyProxy = null;
    private PolicyConfigurationFactory pcf = null;

    @Reference(service = ProviderServiceProxy.class, policy = ReferencePolicy.DYNAMIC, name = KEY_JACC_PROVIDER_SERVICE_PROXY)
    protected void setJaccProviderServiceProxy(ServiceReference<ProviderServiceProxy> reference) {
        jaccProviderServiceProxy.setReference(reference);
    }

    protected void unsetJaccProviderServiceProxy(ServiceReference<ProviderServiceProxy> reference) {
        jaccProviderServiceProxy.unsetReference(reference);
    }

    @Reference(service = WsLocationAdmin.class, name = KEY_LOCATION_ADMIN)
    protected void setLocationAdmin(ServiceReference<WsLocationAdmin> reference) {
        locationAdminRef.setReference(reference);
    }

    protected void unsetLocationAdmin(ServiceReference<WsLocationAdmin> reference) {
        locationAdminRef.unsetReference(reference);
    }

    @Reference(service = ServletService.class, name = KEY_SERVLET_SERVICE, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setServletService(ServiceReference<ServletService> reference) {
        servletServiceRef.setReference(reference);
    }

    protected void unsetServletService(ServiceReference<ServletService> reference) {
        servletServiceRef.unsetReference(reference);
    }

    @Reference(service = EJBService.class, name = KEY_EJB_SERVICE, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setEJBService(ServiceReference<EJBService> reference) {
        ejbServiceRef.setReference(reference);
    }

    protected void unsetEJBService(ServiceReference<EJBService> reference) {
        ejbServiceRef.unsetReference(reference);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        jaccProviderServiceProxy.activate(cc);
        ProviderServiceProxy serviceProxy = jaccProviderServiceProxy.getService();
        if (serviceProxy != null) {
            policyName = serviceProxy.getPolicyName();
            factoryName = serviceProxy.getFactoryName();
        }
        Tr.info(tc, "JACC_SERVICE_STARTING", new Object[] { policyName, factoryName });
        locationAdminRef.activate(cc);
        servletServiceRef.activate(cc);
        ejbServiceRef.activate(cc);
        if (loadClasses()) {
            Tr.info(tc, "JACC_SERVICE_STARTED", new Object[] { policyName, factoryName });
        } else {
            Tr.info(tc, "JACC_SERVICE_START_FAILURE", new Object[] { policyName, factoryName });
        }
    }

    @Modified
    protected void modify(Map<String, Object> props) {
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        locationAdminRef.deactivate(cc);
        jaccProviderServiceProxy.deactivate(cc);
        servletServiceRef.deactivate(cc);
        ejbServiceRef.deactivate(cc);
        Tr.info(tc, "JACC_SERVICE_STOPPED", new Object[] { policyName });
    }

    /**
     * Loads the JACC Policy and Factory classes.
     *
     * @return true if the initialization was successful
     */
    public boolean loadClasses() {
        Boolean result = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {

                policyProxy = jaccProviderServiceProxy.getService().getPolicyProxy();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "policy object" + policyProxy);
                // in order to support the CTS provider, Policy object should be set prior to
                // instanciate PolicyConfigFactory class.
                if (policyProxy == null) {
                    Exception e = new Exception("Policy object is null.");
                    Tr.error(tc, "JACC_POLICY_INSTANTIATION_FAILURE", new Object[] { policyName, e });
                    return Boolean.FALSE;
                }
                try {
                    policyProxy.setPolicy();
                    policyProxy.refresh();
                } catch (ClassCastException cce) {
                    Tr.error(tc, "JACC_POLICY_INSTANTIATION_FAILURE", new Object[] { policyName, cce });
                    return Boolean.FALSE;
                }

                pcf = jaccProviderServiceProxy.getService().getPolicyConfigFactory();
                if (pcf != null) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "factory object : " + pcf);
                    PolicyConfigurationManager.initialize(policyProxy, pcf);
                } else {
                    Tr.error(tc, "JACC_FACTORY_INSTANTIATION_FAILURE", new Object[] { factoryName });
                    return Boolean.FALSE;
                }
                return Boolean.TRUE;
            }
        });
        return result.booleanValue();
    }

    @Override
    public void propagateWebConstraints(String applicationName, String moduleName, Object webAppConfig) {
        WebSecurityPropagator wsp = getWsp(servletServiceRef.getService());
        if (wsp != null) {
            propagateWebConstraints(wsp, applicationName, moduleName, webAppConfig);
        } else {
            Tr.error(tc, "JACC_NO_WEB_PLUGIN");
        }
        return;
    }

    protected void propagateWebConstraints(WebSecurityPropagator wsp, String applicationName, String moduleName, Object webAppConfig) {
        wsp.propagateWebConstraints(pcf, getContextId(applicationName, moduleName), webAppConfig);
        return;
    }

    @Override
    public boolean isSSLRequired(String applicationName, String moduleName, String uriName, String methodName, Object req) {
        return !checkDataConstraints(applicationName, moduleName, uriName, methodName, req, null);
    }

    @Override
    public boolean isAccessExcluded(String applicationName, String moduleName, String uriName, String methodName, Object req) {
        return !checkDataConstraints(applicationName, moduleName, uriName, methodName, req, "CONFIDENTIAL");
    }

    /*
     * check DataConstraints
     * true if permission is is implied.
     * false otherwise.
     */
    protected boolean checkDataConstraints(String applicationName, String moduleName, String uriName, String methodName, Object req, String transportType) {
        boolean result = false;
        WebSecurityValidator wsv = getWsv(servletServiceRef.getService());
        if (wsv != null) {
            result = checkDataConstraints(wsv, applicationName, moduleName, uriName, methodName, req, transportType);
        } else {
            Tr.error(tc, "JACC_NO_WEB_PLUGIN");
        }
        return result;
    }

    protected boolean checkDataConstraints(WebSecurityValidator wsv, String applicationName, String moduleName, String uriName, String methodName, Object req,
                                           String transportType) {
        boolean result = false;
        String[] methodNameArray = new String[] { methodName };
        /**
         ** if uriName ends with "*", Web*Permission.implies won't work property since * is treated as a wildcard.
         ** In order to avoid this issue, substitute * as | which cannot be used as a part of real URL, but URLPatternSpec object doesn't care.
         */
        uriName = substituteAsterisk(uriName);
        WebUserDataPermission webUDPerm = new WebUserDataPermission(uriName, methodNameArray, transportType);
        result = wsv.checkDataConstraints(getContextId(applicationName, moduleName), req, webUDPerm, policyProxy);
        return result;
    }

    @Override
    public boolean isAuthorized(String applicationName, String moduleName, String uriName, String methodName, Object req, Subject subject) {
        boolean result = false;
        WebSecurityValidator wsv = getWsv(servletServiceRef.getService());
        if (wsv != null) {
            result = isAuthorized(wsv, applicationName, moduleName, uriName, methodName, req, subject);
        } else {
            Tr.error(tc, "JACC_NO_WEB_PLUGIN");
        }
        return result;
    }

    protected boolean isAuthorized(WebSecurityValidator wsv, String applicationName, String moduleName, String uriName, String methodName, Object req, Subject subject) {
        AuditAuthenticationResult authResult = null;
        String subjectName = null;
        String[] methodNameArray = new String[] { methodName };
        uriName = substituteAsterisk(uriName);
        WebResourcePermission webPerm = new WebResourcePermission(uriName, methodNameArray);
        boolean isAuthorized = wsv.checkResourceConstraints(getContextId(applicationName, moduleName), req, webPerm, subject, policyProxy);
        return isAuthorized;
    }

    @Override
    public boolean isSubjectInRole(String applicationName, String moduleName, String servletName, String role, Object req, Subject subject) {
        boolean result = false;
        WebSecurityValidator wsv = getWsv(servletServiceRef.getService());
        if (wsv != null) {
            result = isSubjectInRole(wsv, applicationName, moduleName, servletName, role, req, subject);
        } else {
            Tr.error(tc, "JACC_NO_WEB_PLUGIN");
        }
        return result;
    }

    protected boolean isSubjectInRole(WebSecurityValidator wsv, String applicationName, String moduleName, String servletName, String role, Object req, Subject subject) {
        WebRoleRefPermission webRolePerm = new WebRoleRefPermission(servletName, role);
        return wsv.checkResourceConstraints(getContextId(applicationName, moduleName), req, webRolePerm, subject, policyProxy);
    }

    @Override
    public void propagateEJBRoles(String applicationName,
                                  String moduleName,
                                  String beanName,
                                  Map<String, String> roleLinkMap,
                                  Map<RoleInfo, List<MethodInfo>> methodMap) {
        EJBSecurityPropagator esp = getEsp(ejbServiceRef.getService());
        if (esp != null) {
            propagateEJBRoles(esp, applicationName, moduleName, beanName, roleLinkMap, methodMap);
        } else {
            Tr.error(tc, "JACC_NO_EJB_PLUGIN");
        }
        return;
    }

    protected void propagateEJBRoles(EJBSecurityPropagator esp,
                                     String applicationName,
                                     String moduleName,
                                     String beanName,
                                     Map<String, String> roleLinkMap,
                                     Map<RoleInfo, List<MethodInfo>> methodMap) {
        PolicyConfigurationManager.setEJBSecurityPropagator(esp);
        esp.propagateEJBRoles(getContextId(applicationName, moduleName), applicationName, beanName, roleLinkMap, methodMap);
        return;
    }

    @Override
    public boolean isAuthorized(String applicationName, String moduleName, String beanName, String methodName, String methodInterface,
                                String methodSignature, List<Object> methodParameters, Object bean, Subject subject) {
        EJBSecurityValidator esv = getEsv(ejbServiceRef.getService());
        if (esv != null) {
            return isAuthorized(esv, applicationName, moduleName, beanName, methodName, methodInterface, methodSignature, methodParameters, bean, subject);
        } else {
            Tr.error(tc, "JACC_NO_EJB_PLUGIN");
            return false;
        }
    }

    protected boolean isAuthorized(EJBSecurityValidator esv, String applicationName, String moduleName, String beanName, String methodName,
                                   String methodInterface, String methodSignature, List<Object> methodParameters, Object bean, Subject subject) {
        String[] methodSignatureArray = convertMethodSignature(methodSignature);
        final EJBMethodPermission ejbPerm = new EJBMethodPermission(beanName, methodName, methodInterface, methodSignatureArray);
        return esv.checkResourceConstraints(getContextId(applicationName, moduleName), methodParameters, bean, ejbPerm, subject, policyProxy);
    }

    @Override
    public boolean isSubjectInRole(String applicationName, String moduleName, String beanName, String methodName,
                                   List<Object> methodParameters, String role, Object bean, Subject subject) {
        EJBSecurityValidator esv = getEsv(ejbServiceRef.getService());
        if (esv != null) {
            return isSubjectInRole(esv, applicationName, moduleName, beanName, methodName, methodParameters, role, bean, subject);
        } else {
            Tr.error(tc, "JACC_NO_EJB_PLUGIN");
            return false;
        }
    }

    protected boolean isSubjectInRole(EJBSecurityValidator esv, String applicationName, String moduleName, String beanName, String methodName,
                                      List<Object> methodParameters, String role, Object bean, Subject subject) {
        final EJBRoleRefPermission ejbPerm = new EJBRoleRefPermission(beanName, role);
        return esv.checkResourceConstraints(getContextId(applicationName, moduleName), methodParameters, bean, ejbPerm, subject, policyProxy);
    }

    @Override
    public boolean areRequestMethodArgumentsRequired() {
        boolean result = false;
        ProviderServiceProxy reference = jaccProviderServiceProxy.getService();
        if (reference != null) {
            Object obj = reference.getProperty(JACC_EJB_METHOD_ARGUMENT);
            if (obj instanceof String) {
                String value = (String) obj;
                if ("true".equalsIgnoreCase(value)) {
                    result = true;
                }
            }
        }
        return result;
    }

    private String getContextId(String applicationName, String moduleName) {
        StringBuffer output = new StringBuffer();
        WsLocationAdmin locationAdmin = locationAdminRef.getService();
        output.append(getHostName()).append("#").append(locationAdmin.resolveString("${wlp.user.dir}").replace('\\',
                                                                                                               '/')).append("#").append(locationAdmin.getServerName()).append("#");
        output.append(applicationName).append("#").append(moduleName);
        return output.toString();
    }

    private String[] convertMethodSignature(String methodSignature) {
        ArrayList<String> methodSignatureList = new ArrayList<String>();
        if (methodSignature != null && methodSignature.length() > 0) {
            int index = methodSignature.indexOf(":");
            if (index != -1) {
                String s = methodSignature.substring(index + 1);
                if (s != null && s.length() > 0) {
                    StringTokenizer st = new StringTokenizer(s, ",");
                    while (st.hasMoreTokens()) {
                        methodSignatureList.add(st.nextToken());
                    }
                }
            }
        }
        return methodSignatureList.toArray(new String[methodSignatureList.size()]);
    }

    private String substituteAsterisk(String uriName) {
        if (uriName != null && uriName.endsWith("/*")) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "The URI ends with \"/*\" which is substituted by \"/|\"");
            uriName = uriName.substring(0, uriName.lastIndexOf("*")) + "|";
        }
        return uriName;
    }

    /**
     * Get the host name.
     *
     * @return String value of the host name or "localhost" if not able to resolve
     */
    private String getHostName() {
        String hostName = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                try {
                    return java.net.InetAddress.getLocalHost().getCanonicalHostName().toLowerCase();
                } catch (java.net.UnknownHostException e) {
                    return "localhost";
                }
            }
        });
        return hostName;
    }

    protected EJBSecurityPropagator getEsp(EJBService es) {
        if (es != null) {
            return es.getPropagator();
        } else {
            return null;
        }
    }

    protected EJBSecurityValidator getEsv(EJBService es) {
        if (es != null) {
            return es.getValidator();
        } else {
            return null;
        }
    }

    protected WebSecurityPropagator getWsp(ServletService ws) {
        if (ws != null) {
            return ws.getPropagator();
        } else {
            return null;
        }
    }

    protected WebSecurityValidator getWsv(ServletService ws) {
        if (ws != null) {
            return ws.getValidator();
        } else {
            return null;
        }
    }

    @Override
    public void resetPolicyContextHandlerInfo() {
        try {
            AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() {
                    // resetting the handler info as per spec..
                    PolicyContext.setHandlerData(null);
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception when resetting setHandlerData. Ignoring.. " + e.getException());
        }
    }
}
