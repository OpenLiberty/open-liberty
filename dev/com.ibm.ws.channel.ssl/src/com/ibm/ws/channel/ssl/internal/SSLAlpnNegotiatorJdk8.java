/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channel.ssl.internal;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLEngine;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class enables the use of the ALPN SSL protocol extension, on JDK8.  Since there is no proper ALPN support
 * in JDK8 - that will come in JDK9 - users must override JSSE classes in order to use ALPN and by extension
 * secure HTTP/2 (h2).
 *
 * This class interfaces with the ALPN JSSE override provided by the Grizzly project.  That override requires users
 * to place the jar grizzly-npn-bootstrap-1.*.jar on the server's bootclasspath.  Additionally, the server must be
 * running on an Oracle JDK 8 (currently tested on JDK 1.8.0_(121,131,141,144)).
 *
 * On creation, the class tries to find the Grizzly classes.  If the classes are found, this object can be used to
 * interface with the Grizzly ALPN override; reflection is used to eliminate compile and run-time dependency on the
 * that override.
 * @author wtlucy
 */
public class SSLAlpnNegotiatorJdk8 implements java.lang.reflect.InvocationHandler {

    /** supported protocols */
    private final String h1 = "http/1.1";
    private final String h2 = "h2";
    private final String unknown = "";

    /** Mapping of SSLConnectionLink to SSLConnectionLink used to handle overridden SSLEngine call */
    private final ConcurrentHashMap<SSLEngine, SSLConnectionLink> linkMap = new ConcurrentHashMap<SSLEngine, SSLConnectionLink>();
    private static boolean grizzlyAlpnPresent = false;
    private static final TraceComponent tc = Tr.register(SSLAlpnNegotiatorJdk8.class,
                                                         SSLChannelConstants.SSL_TRACE_NAME,
                                                         SSLChannelConstants.SSL_BUNDLE);

    /** Classes to find via reflection */
    private static Class<?> negotiationSupport;
    private static Class<?> alpnClientNegotiator;
    private static Class<?> alpnServerNegotiator;
    private static Object negotiationSupportObject;

    static {
        try {
            negotiationSupport = ClassLoader.getSystemClassLoader().loadClass("org.glassfish.grizzly.npn.NegotiationSupport");
            alpnServerNegotiator = ClassLoader.getSystemClassLoader().loadClass("org.glassfish.grizzly.npn.AlpnServerNegotiator");
            alpnClientNegotiator = ClassLoader.getSystemClassLoader().loadClass("org.glassfish.grizzly.npn.AlpnClientNegotiator");
            negotiationSupportObject = negotiationSupport.newInstance();
            grizzlyAlpnPresent = true;
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Grizzly JDK8 ALPN module was not found on the classpath. ALPN will not be used.\n" + t);
            }
        }
    }

    /**
     * Intended to be invoked with the method selectProtocol(SSLEngine, (String ? String[])).
     * If either signature is used, handle ALPN protocol selection as required.
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "invoke entry" + method);
        }
        String methodName = method.getName();

        if (methodName.equals("selectProtocol")) {
            if (args != null && args.length == 2 && args[0] instanceof SSLEngine && args[1] instanceof String[]) {
                return selectProtocol((SSLEngine)args[0], (String[])args[1]);
            } else if (args != null && args.length == 2 && args[0] instanceof SSLEngine && args[1] instanceof String) {
                selectProtocol((SSLEngine)args[0], (String)args[1]);
            }
        }
        return null;
    }

    /**
     * Select a protocol when when engine.getUseClientMode=false.  Expect either "h2" or "http/1.1"
     * @param engine
     * @param protocolList
     * @return the protocol we want to use in order of preference: h2, http/1.1
     */
    protected String selectProtocol(SSLEngine engine, String[] protocolList) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "selectProtocol entry engine: " + engine + " protocols: " + protocolList);
        }
        if (engine != null && protocolList != null && protocolList.length > 0) {
            SSLConnectionLink link = removeSSLLink(engine);
            if (java.util.Arrays.asList(protocolList).contains(h2)) {
                if (link != null) {
                    link.setAlpnProtocol(h2);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "selectProtocol protocol h2 selected" + engine);
                }
                return h2;
            } else if (java.util.Arrays.asList(protocolList).contains(h1)) {
                if (link != null) {
                    link.setAlpnProtocol(h1);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "selectProtocol protocol http/1.1 selected" + engine);
                    }
                }
                return h1;
            }
        }
        return unknown;
    }

    /**
     * Select a protocol when engine.getUseClientMode=true.  Expect either "h2" or "http/1.1"
     * @param engine
     * @param protocol
     */
    protected void selectProtocol(SSLEngine engine, String protocol) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "selectProtocol entry engine: " + engine + " protocol: " + protocol);
        }
        if (engine != null && protocol != null) {
            SSLConnectionLink link = removeSSLLink(engine);
            if (protocol.equals(h2)) {
                if (link != null) {
                    link.setAlpnProtocol(h2);
                }
            } else if (protocol.equals(h1)) {
                if (link != null) {
                    link.setAlpnProtocol(h1);
                }
            }
        }
    }

    /**
     * If the Grizzly ALPN library is present, set the passed SSLEngine to use ALPN
     * @param SSLEngine
     */
    protected void initializeAlpn(SSLEngine engine) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "initializeAlpn entry " + engine);
        }

        if (negotiationSupport != null && alpnClientNegotiator != null && alpnServerNegotiator != null && negotiationSupportObject != null){
            try {
                if (!engine.getUseClientMode()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "initializeAlpn invoke AlpnServerNegotiator " + engine);
                    }
                    // client mode is disabled; call NegotiationSuppoer.addNegotiator(SSLEngine, (AlpnServerNegotiator) this)
                    Method m = negotiationSupport.getMethod("addNegotiator", SSLEngine.class, alpnServerNegotiator);
                    m.invoke(negotiationSupportObject, new Object[] {engine, java.lang.reflect.Proxy.newProxyInstance(
                                                                              alpnServerNegotiator.getClassLoader(),
                                                                              new java.lang.Class[] { alpnServerNegotiator },
                                                                              this)});
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "initializeAlpn invoke AlpnClientNegotiator " + engine);
                    }
                    // client mode is enabled; call NegotiationSuppoer.addNegotiator(SSLEngine, (AlpnClientNegotiator) this)
                    Method m = negotiationSupport.getMethod("addNegotiator", SSLEngine.class, alpnClientNegotiator);
                    m.invoke(negotiationSupportObject, new Object[] {engine, java.lang.reflect.Proxy.newProxyInstance(
                                                                              alpnClientNegotiator.getClassLoader(),
                                                                              new java.lang.Class[] { alpnClientNegotiator },
                                                                              this)});

                }
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "JDK8 oracle ALPN module hit an exception: " + e);
                }
            }
        }
    }

    /**
     * If the Grizzly jars are on the bootclasspath, try to initialize ALPN for the passed SSLEngine
     * @param SSLEngine
     */
    protected void tryToUseGrizzlyJdk8Alpn(SSLEngine engine) {
        if (grizzlyAlpnPresent) {
            initializeAlpn(engine);
        }
    }

    protected boolean isGrizzlyAlpnActive() {
        return grizzlyAlpnPresent;
    }

    /**
     * Maps a SSLConnectionLink to a SSLSession.  When selectProtocol() is later called, the link will be retrieved
     * from the map and the selected ALPN protocol will be set via its setAlpnProtocol()
     * @param SSLEngine
     * @param SSLConnectionLink
     */
    protected void setSSLLink(SSLEngine engine, SSLConnectionLink link) {
        if (link.getAlpnProtocol() == null) {
            linkMap.put(engine, link);
        }
    }

    protected SSLConnectionLink removeSSLLink(SSLEngine engine) {
        if (isGrizzlyAlpnActive()) {
            return linkMap.remove(engine);
        }
        return null;
    }
}
