/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc.ejb.impl;

import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.ejb.EnterpriseBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.jacc.EJBMethodPermission;
import javax.security.jacc.EJBRoleRefPermission;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.security.jacc.EJBJaccService;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authorization.jacc.JaccService;
import com.ibm.ws.security.authorization.jacc.MethodInfo;
import com.ibm.ws.security.authorization.jacc.RoleInfo;
import com.ibm.ws.security.authorization.jacc.common.PolicyContextHandlerImpl;
import com.ibm.ws.security.authorization.jacc.common.PolicyProxy;
import com.ibm.ws.security.authorization.jacc.ejb.EJBSecurityPropagator;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = EJBJaccService.class, name = "com.ibm.ws.security.authorization.jacc.ejb.service", configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class EJBJaccServiceImpl implements EJBJaccService {

    private static final String JACC_EJB_METHOD_ARGUMENT = "RequestMethodArgumentsRequired";

    private static final TraceComponent tc = Tr.register(EJBJaccServiceImpl.class);
    private static PolicyContextHandlerImpl pch = PolicyContextHandlerImpl.getInstance();
    protected static final String KEY_JACC_SERVICE = "jaccService";

    enum HandlerProcessor {
        JACC15(true, false), AUTHORIZATION20_21(false, false), AUTHORIZATION30(false, true);

        final boolean principalMapperSupported;
        final boolean javaxSupported;

        HandlerProcessor(boolean javaxSupported, boolean principalMapperSupported) {
            this.principalMapperSupported = principalMapperSupported;
            this.javaxSupported = javaxSupported;
        }

        void setPolicyContextData(Subject subject, EnterpriseBean bean, Object[] methodParameters, Object messageContext, PolicyProxy policyProxy) throws PolicyContextException {
            final HashMap<String, Object> handlerObjects = new HashMap<String, Object>();

            PolicyContext.registerHandler("javax.security.auth.Subject.container", pch, true);
            handlerObjects.put("javax.security.auth.Subject.container", subject);

            if (principalMapperSupported) {
                PolicyContext.registerHandler("jakarta.security.jacc.PrincipalMapper", pch, true);
                handlerObjects.put("jakarta.security.jacc.PrincipalMapper", policyProxy.getPrincipalMapper());
            } else {
                PolicyContext.registerHandler("javax.ejb.arguments", pch, true);
                handlerObjects.put("javax.ejb.arguments", methodParameters);
            }

            if (javaxSupported) {
                PolicyContext.registerHandler("javax.ejb.EnterpriseBean", pch, true);
                PolicyContext.registerHandler("javax.xml.soap.SOAPMessage", pch, true);

                handlerObjects.put("javax.ejb.EnterpriseBean", bean);
                if (messageContext != null) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "javax.xml.soap.SOAPMessage is set: ", messageContext);
                    handlerObjects.put("javax.xml.soap.SOAPMessage", messageContext);
                }
            }

            PolicyContext.registerHandler("jakarta.ejb.EnterpriseBean", pch, true);
            PolicyContext.registerHandler("jakarta.ejb.arguments", pch, true);
            PolicyContext.registerHandler("jakarta.xml.soap.SOAPMessage", pch, true);

            handlerObjects.put("jakarta.ejb.EnterpriseBean", bean);
            handlerObjects.put("jakarta.ejb.arguments", methodParameters);

            PolicyContext.setHandlerData(handlerObjects);
        }
    }

    private static final HandlerProcessor handlerProcessor;

    static {
        if (PolicyContext.class.getName().startsWith("javax")) {
            handlerProcessor = HandlerProcessor.JACC15;
        } else {
            boolean principalMapperSupported = false;
            try {
                principalMapperSupported = pch.supports("jakarta.security.jacc.PrincipalMapper");
            } catch (PolicyContextException pce) {
                // our implementation doesn't throw an exception, but it is on the interface so need to catch it.
            }
            handlerProcessor = principalMapperSupported ? HandlerProcessor.AUTHORIZATION30 : HandlerProcessor.AUTHORIZATION20_21;
        }
    }

    /**
     * Are we running with <code>jakarta.ejb.*</code> packages? This will indicate we are running with (at least) Jakarta EE 9.
     *
     * This check may seem silly on the surface, but the packages are transformed at build time to swap the <code>javax.ejb.*</code> packages with
     * <code>jakarta.ejb.*</code>.
     */
    private static boolean isEENineOrHigher = SessionContext.class.getCanonicalName().startsWith("jakarta.ejb");

    private final EJBSecurityPropagator esp = new EJBSecurityPropagatorImpl();

    private final AtomicServiceReference<JaccService> jaccServiceRef = new AtomicServiceReference<JaccService>(KEY_JACC_SERVICE);

    @Reference(service = JaccService.class, policy = ReferencePolicy.DYNAMIC, name = KEY_JACC_SERVICE)
    protected void setJaccService(ServiceReference<JaccService> reference) {
        jaccServiceRef.setReference(reference);
    }

    protected void unsetJaccService(ServiceReference<JaccService> reference) {
        jaccServiceRef.unsetReference(reference);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        jaccServiceRef.activate(cc);
        JaccService jaccService = jaccServiceRef.getService();
        if (jaccService != null) {
            jaccService.getPolicyConfigurationManager().setEJBSecurityPropagator(esp);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        jaccServiceRef.deactivate(cc);
    }

    @Override
    public void propagateEJBRoles(BeanMetaData bmd) {
        propagateEJBRoles(esp, bmd.j2eeName.getApplication(), bmd.j2eeName.getModule(), bmd.enterpriseBeanName, bmd.ivRoleLinkMap,
                          JaccUtil.convertMethodInfoList(JaccUtil.mergeMethodInfos(bmd)));
    }

    protected void propagateEJBRoles(EJBSecurityPropagator esp,
                                     String applicationName,
                                     String moduleName,
                                     String beanName,
                                     Map<String, String> roleLinkMap,
                                     Map<RoleInfo, List<MethodInfo>> methodMap) {
        JaccService jaccService = jaccServiceRef.getService();
        if (jaccService != null) {
            esp.propagateEJBRoles(jaccService.getContextId(applicationName, moduleName), applicationName, beanName, roleLinkMap, methodMap,
                                  jaccService.getPolicyConfigurationManager());
        }
    }

    @Override
    public boolean isAuthorized(String applicationName, String moduleName, String beanName, String methodName, String methodInterface,
                                String methodSignature, List<Object> methodParameters, EnterpriseBean bean, Subject subject) {
        JaccService jaccService = jaccServiceRef.getService();
        if (jaccService != null) {
            String[] methodSignatureArray = convertMethodSignature(methodSignature);
            final EJBMethodPermission ejbPerm = new EJBMethodPermission(beanName, methodName, methodInterface, methodSignatureArray);
            return checkResourceConstraints(jaccService.getContextId(applicationName, moduleName), methodParameters, bean, ejbPerm, subject, jaccService.getPolicyProxy());
        }
        return false;
    }

    @Override
    public boolean isSubjectInRole(String applicationName, String moduleName, String beanName, String methodName,
                                   List<Object> methodParameters, String role, EnterpriseBean bean, Subject subject) {

        JaccService jaccService = jaccServiceRef.getService();
        if (jaccService != null) {
            final EJBRoleRefPermission ejbPerm = new EJBRoleRefPermission(beanName, role);
            return checkResourceConstraints(jaccService.getContextId(applicationName, moduleName), methodParameters, bean, ejbPerm, subject, jaccService.getPolicyProxy());
        }
        return false;
    }

    private boolean checkResourceConstraints(String contextId, List<Object> methodParameters, EnterpriseBean bean, Permission ejbPerm, Subject subject, PolicyProxy policyProxy) {
        boolean result = false;
        Object[] ma = null;

        /*
         * TODO Doesn't seem to handle EJB-3.0 annotated beans.
         */
        if (methodParameters != null && methodParameters.size() > 0) {
            ma = methodParameters.toArray(new Object[methodParameters.size()]);
        }
        try {
            result = checkMethodConstraints(contextId, ma, bean, ejbPerm, subject, policyProxy);
        } catch (PrivilegedActionException pae) {
            Tr.error(tc, "JACC_EJB_IMPLIES_FAILURE", new Object[] { contextId, pae.getException() });
        } // Moved resetHandlerInfo to postInvoke.
        return result;
    }

    private boolean checkMethodConstraints(final String contextId,
                                           final Object[] methodParameters,
                                           final EnterpriseBean bean,
                                           final Permission permission,
                                           final Subject subject,
                                           final PolicyProxy policyProxy) throws PrivilegedActionException {
        Boolean result = Boolean.FALSE;
        result = AccessController.doPrivileged(
                                               new PrivilegedExceptionAction<Boolean>() {
                                                   @Override
                                                   public Boolean run() throws javax.security.jacc.PolicyContextException {
                                                       PolicyContext.setContextID(contextId);

                                                       /*
                                                        * EE 8 and below support JAX-RPC MessageContext. EE 9 removed this support.
                                                        */
                                                       Object mc = null;
                                                       if (!isEENineOrHigher) {
                                                           try {
                                                               InitialContext ic = new InitialContext();
                                                               mc = getMessageContext(ic);
                                                           } catch (NamingException e) {
                                                               if (tc.isDebugEnabled())
                                                                   Tr.debug(tc, "NamingException is caught. Ignoring.", e);
                                                           }
                                                       }

                                                       if (tc.isDebugEnabled())
                                                           Tr.debug(tc, "Registering JACC context handlers and setting JACC handler data");
                                                       handlerProcessor.setPolicyContextData(subject, bean, methodParameters, mc, policyProxy);
                                                       if (tc.isDebugEnabled())
                                                           Tr.debug(tc, "Calling JACC implies. subject : " + subject);
                                                       return policyProxy.implies(contextId, subject, permission);
                                                   }
                                               });
        return result.booleanValue();
    }

    @FFDCIgnore({ NamingException.class, IllegalStateException.class })
    Object getMessageContext(Context c) {
        Object mc = null;
        try {
            SessionContext sc = (SessionContext) c.lookup("java:comp/EJBContext");
            if (sc != null) {
                mc = sc.getMessageContext();
            }
        } catch (NamingException ne) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "NamingException is caught. Safe to ignore.", ne);
        } catch (IllegalStateException ise) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "IllegalStateException is caught. Safe to ignore.", ise);
        }
        return mc;
    }

    @Override
    public boolean areRequestMethodArgumentsRequired() {
        JaccService jaccService = jaccServiceRef.getService();
        if (jaccService != null) {
            String value = jaccService.getProviderServiceProperty(JACC_EJB_METHOD_ARGUMENT);
            return "true".equalsIgnoreCase(value);
        }
        return false;
    }

    @Override
    public void resetPolicyContextHandlerInfo() {
        JaccService jaccService = jaccServiceRef.getService();
        if (jaccService != null) {
            jaccService.resetPolicyContextHandlerInfo();
        }
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
}
