/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc.ejb.impl;

import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.Policy;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.List;

import javax.ejb.EnterpriseBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import javax.xml.rpc.handler.MessageContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authorization.jacc.common.PolicyContextHandlerImpl;
import com.ibm.ws.security.authorization.jacc.ejb.EJBSecurityValidator;

public class EJBSecurityValidatorImpl implements EJBSecurityValidator {
    private static final TraceComponent tc = Tr.register(EJBSecurityValidatorImpl.class);
    private static String[] jaccHandlerKeyArray = new String[] { "javax.security.auth.Subject.container", "javax.ejb.EnterpriseBean", "javax.ejb.arguments",
                                                                "javax.xml.soap.SOAPMessage" };

    private static ProtectionDomain nullPd = new ProtectionDomain(new CodeSource(null, (java.security.cert.Certificate[]) null), null, null, null);
    private static CodeSource nullCs = new CodeSource(null, (java.security.cert.Certificate[]) null);
    private static PolicyContextHandlerImpl pch = PolicyContextHandlerImpl.getInstance();

    public EJBSecurityValidatorImpl() {}

    @Override
    public boolean checkResourceConstraints(String contextId, List<Object> methodParameters, Object bean, Permission ejbPerm, Subject subject) {
        boolean result = false;
        final String fci = contextId;
        final HashMap<String, Object> ho = new HashMap<String, Object>();
        final Subject s = subject;
        final Object[] ma = null;
        EnterpriseBean eb = null;
        if (bean != null) {
            try {
                eb = (EnterpriseBean) bean;
            } catch (ClassCastException cce) {
                Tr.error(tc, "JACC_EJB_SPI_PARAMETER_ERROR", new Object[] { bean.getClass().getName(), "checkResourceConstraints", "EnterpriseBean" });
                return false;
            }
        }
        final EnterpriseBean b = eb;
        if (methodParameters != null && methodParameters.size() > 0) {
            methodParameters.toArray(new Object[methodParameters.size()]);
        }
        final Permission p = ejbPerm;
        try {
            result = checkMethodConstraints(fci, ma, b, p, s, ho);
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
                                           final HashMap<String, Object> handlerObjects) throws PrivilegedActionException {
        Boolean result = Boolean.FALSE;
        result = AccessController.doPrivileged(
                        new PrivilegedExceptionAction<Boolean>() {
                            @Override
                            public Boolean run() throws javax.security.jacc.PolicyContextException {
                                PolicyContext.setContextID(contextId);

                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Registering JACC context handlers");
                                for (String key : jaccHandlerKeyArray) {
                                    PolicyContext.registerHandler(key, pch, true);
                                }
                                handlerObjects.put(jaccHandlerKeyArray[0], subject);
                                handlerObjects.put(jaccHandlerKeyArray[1], bean);
                                handlerObjects.put(jaccHandlerKeyArray[2], methodParameters);
                                MessageContext mc = null;
                                try {
                                    InitialContext ic = new InitialContext();
                                    mc = getMessageContext(ic);
                                } catch (NamingException e) {
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "NamingException is caught. Ignoring..", e);
                                }
                                if (mc != null) {
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "javax.xml.soap.SOAPMessage is set: ", mc);
                                    handlerObjects.put(jaccHandlerKeyArray[3], mc);
                                }
                                ProtectionDomain pd = null;

                                if (subject != null && subject.getPrincipals().size() > 0) {
                                    Principal[] principalArray = subject.getPrincipals().toArray(new Principal[subject.getPrincipals().size()]);
                                    pd = new ProtectionDomain(nullCs, null, null, principalArray);
                                } else {
                                    pd = nullPd;
                                }

                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Setting JACC handler data");
                                PolicyContext.setHandlerData(handlerObjects);
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Calling JACC implies. PD : " + pd);
                                return Policy.getPolicy().implies(pd, permission);
                            }
                        });
        return result.booleanValue();
    }

    @FFDCIgnore({ NamingException.class, IllegalStateException.class })
    public MessageContext getMessageContext(Context c) {
        MessageContext mc = null;
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
}
