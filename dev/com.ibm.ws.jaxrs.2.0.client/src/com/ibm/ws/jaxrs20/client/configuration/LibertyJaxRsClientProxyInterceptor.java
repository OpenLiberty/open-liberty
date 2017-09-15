/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.configuration;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxrs20.client.JAXRSClientConstants;

/**
 *
 */
public class LibertyJaxRsClientProxyInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final TraceComponent tc = Tr.register(LibertyJaxRsClientProxyInterceptor.class);

    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            private final static String DISABLED_SCHEMES_PROP = "jdk.http.auth.tunneling.disabledSchemes";

            @Override
            public Void run() {
                // This property is required for later versions of Java 8 due to Basic Auth
                // tunneling being disabled in the JVM by default. For more info, see section
                // labeled "Disable Basic authentication for HTTPS tunneling" here:
                // http://www.oracle.com/technetwork/java/javase/8u111-relnotes-3124969.html

                // Only set the property if it is not already set:
                String propVal = System.getProperty(DISABLED_SCHEMES_PROP);
                if (propVal == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "<clinit> setting property " + DISABLED_SCHEMES_PROP + "=''");
                    }
                    System.setProperty(DISABLED_SCHEMES_PROP, "");
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "<clinit> property " + DISABLED_SCHEMES_PROP + " already set to " + propVal);
                    }
                }

                return null;
            }
        });
    }

    @Trivial
    private static String toString(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof List<?>) {
            Object o2 = ((List<?>) o).get(0);
            o = o2;
        }
        if (o instanceof String) {
            return (String) o;
        }
        return o.toString();

    }

    /**
     * @param phase
     */
    public LibertyJaxRsClientProxyInterceptor(String phase) {
        super(phase);
    }

    @Override
    public void handleMessage(Message message) throws Fault {

        String host = toString(message.get(JAXRSClientConstants.PROXY_HOST));
        String port = toString(message.get(JAXRSClientConstants.PROXY_PORT));
        String type = toString(message.get(JAXRSClientConstants.PROXY_TYPE));
        String proxyAuthType = toString(message.get(JAXRSClientConstants.PROXY_AUTH_TYPE));
        String proxyAuthUser = toString(message.get(JAXRSClientConstants.PROXY_USERNAME));
        ProtectedString proxyAuthPW = (ProtectedString) message.get(JAXRSClientConstants.PROXY_PASSWORD);

        Conduit conduit = message.getExchange().getConduit(message);

        if (host != null) {
            String sHost = host.toString();
            if (!sHost.isEmpty() && conduit instanceof HTTPConduit) {
                configClientProxy((HTTPConduit) conduit, sHost, port, type, proxyAuthType,
                                  proxyAuthUser, proxyAuthPW);
            }

        }

    }

    @FFDCIgnore({ NoSuchMethodError.class, Exception.class, Exception.class })
    private void configClientProxy(HTTPConduit httpConduit, String host, String port, String type, String proxyAuthType,
                                   String proxyAuthUser, ProtectedString proxyAuthPW) {

        int iPort = JAXRSClientConstants.PROXY_PORT_DEFAULT;
        if (port != null) {
            try {
                iPort = Integer.parseInt(port);
            } catch (NumberFormatException e) {
                //The proxy server port value {0} that you specified in the property {1} on the JAX-RS Client side is invalid. The value is set to default.
                Tr.error(tc, "error.jaxrs.client.configuration.proxy.portinvalid", port, JAXRSClientConstants.PROXY_PORT, JAXRSClientConstants.PROXY_PORT_DEFAULT,
                         e.getMessage());

            }
        }

        ProxyServerType proxyServerType = ProxyServerType.HTTP;
        if (type != null) {
            try {
                proxyServerType = ProxyServerType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                //The proxy server type value {0} that you specified in the property {1} on the JAX-RS Client side is invalid. The value is set to default.
                Tr.error(tc, "error.jaxrs.client.configuration.proxy.typeinvalid", type, JAXRSClientConstants.PROXY_TYPE, ProxyServerType.HTTP, e.getMessage());

            }
        }

        HTTPClientPolicy clientPolicy = new HTTPClientPolicy();
        clientPolicy.setProxyServer(host);
        try {
            clientPolicy.setProxyServerPort(iPort);
        } catch (NoSuchMethodError e) {
            // This is a weird error seen in jaxrs-2.1 where autoboxing doesn't work -
            // even changing iPort from int to Integer doesn't work - both result in a
            // NoSuchMethodError.  Using reflection does seem to work.  My guess is that
            // there is some JDK bug related to the calling source being compiled for
            // Java 7 compliance but the called source being compiled for Java 8.
            // In any case, until we can work out the issue with the JDK or refactor
            // this code to exist separately in 2.0 and 2.1, we'll leave this little
            // hack in place to ensure that we can run in both environments.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "configClientProxy - NSME setProxyServerPort - retrying...", e);
            }
            Method m;
            try {
                m = HTTPClientPolicy.class.getMethod("setProxyServerPort", int.class);
                m.invoke(clientPolicy, iPort);
            } catch (Exception ex) {
                try {
                    m = HTTPClientPolicy.class.getMethod("setProxyServerPort", Integer.class);
                    m.invoke(clientPolicy, new Integer(iPort));
                } catch (Exception ex2) {
                    Method[] methods = HTTPClientPolicy.class.getMethods();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "configClientProxy - NSME setProxyServerPort(int OR Integer) - retrying...", new Object[] { methods, ex2 });
                    }
                }
            }
        }
        clientPolicy.setProxyServerType(proxyServerType);
        httpConduit.setClient(clientPolicy);

        ProxyAuthorizationPolicy authPolicy = null;

        if (proxyAuthUser != null || proxyAuthPW != null) { // authType / authUser / authPW client props
            authPolicy = new ProxyAuthorizationPolicy();
            // for now, always use Basic auth type
            if (proxyAuthType != null && !JAXRSClientConstants.PROXY_AUTH_TYPE_DEFAULT.equalsIgnoreCase(proxyAuthType)) {
                //TODO make warning
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unrecognized proxy authorization type, \"" + proxyAuthType + "\".  Only \"Basic\" is recognized.");
                }
            }
            proxyAuthType = JAXRSClientConstants.PROXY_AUTH_TYPE_DEFAULT;

            authPolicy.setAuthorizationType(proxyAuthType);

            if (proxyAuthUser != null) {
                authPolicy.setUserName(proxyAuthUser);
            } else {
                //TODO: make warning
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No proxy authorization username specified.  No proxy authorization data will be generated.");
                }
                authPolicy = null;
            }

            if (authPolicy != null && proxyAuthPW != null) {
                authPolicy.setPassword(new String(proxyAuthPW.getChars()));
            } else if (proxyAuthPW == null) {
                //TODO: make warning
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No proxy authorization password specified.  No proxy authorization data will be generated.");
                }
                authPolicy = null;
            }
        }

        if (authPolicy != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "configuring proxy auth policy " + authPolicy + " + to httpConduit " + httpConduit);
            }
            httpConduit.setProxyAuthorization(authPolicy);
        }

    }
}