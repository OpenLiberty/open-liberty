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
import java.util.List;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class enables the use of the ALPN SSL protocol extension, on JDK8. Since there is no proper ALPN support
 * in JDK8 - that will come in JDK9 - users must override JSSE classes in order to use ALPN and by extension
 * secure HTTP/2 (h2).
 *
 * This class interfaces with the ALPN JSSE override provided by the Grizzly project, as well as the one provided
 * by the Jetty project. Those overrides require users to configure a bootclasspath jar: either
 * grizzly-npn-bootstrap-1.*.jar or alpn-boot-*.jar. To use either of those jars, the server muse be running on
 * a JDK 8 provided by either Oracle or OpenJDK.
 *
 * On instantiation, the class tries to load classes from jetty-alpn; if those are not found, it attempts to load
 * grizzly-npn classes. If either set of classes are found then this class can be used to interface with the
 * ALPN APIs provided by each project.
 *
 * @author wtlucy
 */
public class SSLAlpnNegotiatorJdk8 {

    private static final TraceComponent tc = Tr.register(SSLAlpnNegotiatorJdk8.class,
                                                         SSLChannelConstants.SSL_TRACE_NAME,
                                                         SSLChannelConstants.SSL_BUNDLE);

    /** supported protocols */
    private final String h1 = "http/1.1";
    private final String h2 = "h2";
    private final String unknown = "";
    private static boolean grizzlyAlpnPresent = false;
    private static boolean jettyAlpnPresent = false;

    /** jetty-alpn classes to find via reflection */
    private static Class<?> jettyAlpn;
    private static Class<?> jettyServerProviderInterface;
    private static Class<?> jettyProviderInterface;

    static {
        try {
            jettyAlpn = ClassLoader.getSystemClassLoader().loadClass("org.eclipse.jetty.alpn.ALPN");
            jettyServerProviderInterface = ClassLoader.getSystemClassLoader().loadClass("org.eclipse.jetty.alpn.ALPN$ServerProvider");
            jettyProviderInterface = ClassLoader.getSystemClassLoader().loadClass("org.eclipse.jetty.alpn.ALPN$Provider");
            jettyAlpnPresent = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "jetty-alpn module was found on the classpath; ALPN is available");
            }
        } catch (Throwable t) {
            // no-op
        }
    }

    /** grizzly-npn classes to find via reflection */
    private static Class<?> grizzlyNegotiationSupport;
    private static Class<?> GrizzlyAlpnClientNegotiator;
    private static Class<?> GrizzlyAlpnServerNegotiator;
    private static Object grizzlyNegotiationSupportObject;

    static {
        try {
            if (!jettyAlpnPresent) {
                grizzlyNegotiationSupport = ClassLoader.getSystemClassLoader().loadClass("org.glassfish.grizzly.npn.NegotiationSupport");
                GrizzlyAlpnServerNegotiator = ClassLoader.getSystemClassLoader().loadClass("org.glassfish.grizzly.npn.AlpnServerNegotiator");
                GrizzlyAlpnClientNegotiator = ClassLoader.getSystemClassLoader().loadClass("org.glassfish.grizzly.npn.AlpnClientNegotiator");
                grizzlyNegotiationSupportObject = grizzlyNegotiationSupport.newInstance();
                grizzlyAlpnPresent = true;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "grizzly-npn module was found on the classpath; ALPN is available");
                }
            }
        } catch (Throwable t) {
            // no-op
        }
    }

    public SSLAlpnNegotiatorJdk8() {
        // no-op
    }

    /**
     * @return true if the grizzly-npn project is available
     */
    protected boolean isGrizzlyAlpnActive() {
        return grizzlyAlpnPresent;
    }

    /**
     * @return true if the jetty-alpn project is available
     */
    protected boolean isJettyAlpnActive() {
        return jettyAlpnPresent;
    }

    /**
     * If either jetty-alpn or grizzly-npn is present, set up a connection for ALPN
     *
     * @param SSLEngine
     * @param SSLConnectionLink
     */
    protected void tryToRegisterAlpnNegotiator(SSLEngine engine, SSLConnectionLink link) {
        if (this.isJettyAlpnActive()) {
            registerJettyAlpn(engine, link);
        } else if (this.isGrizzlyAlpnActive()) {
            registerGrizzlyAlpn(engine, link);
        }
    }

    /**
     * Using grizzly-npn, set up a new GrizzlyAlpnNegotiator to handle ALPN for a given SSLEngine and link
     *
     * @param SSLEngine
     * @param SSLConnectionLink
     */
    protected void registerGrizzlyAlpn(SSLEngine engine, SSLConnectionLink link) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "registerGrizzlyAlpn entry " + engine);
        }

        if (grizzlyNegotiationSupport != null && GrizzlyAlpnClientNegotiator != null && GrizzlyAlpnServerNegotiator != null && grizzlyNegotiationSupportObject != null) {
            try {
                GrizzlyAlpnNegotiator negotiator = new GrizzlyAlpnNegotiator(engine, link);

                if (!engine.getUseClientMode()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "initializeAlpn invoke AlpnServerNegotiator " + engine);
                    }
                    // client mode is disabled; call NegotiationSuppoer.addNegotiator(SSLEngine, (AlpnServerNegotiator) this)
                    Method m = grizzlyNegotiationSupport.getMethod("addNegotiator", SSLEngine.class, GrizzlyAlpnServerNegotiator);
                    m.invoke(grizzlyNegotiationSupportObject, new Object[] { engine, java.lang.reflect.Proxy.newProxyInstance(
                                                                                                                              GrizzlyAlpnServerNegotiator.getClassLoader(),
                                                                                                                              new java.lang.Class[] { GrizzlyAlpnServerNegotiator },
                                                                                                                              negotiator) });
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "initializeAlpn invoke AlpnClientNegotiator " + engine);
                    }
                    // client mode is enabled; call NegotiationSuppoer.addNegotiator(SSLEngine, (AlpnClientNegotiator) this)
                    Method m = grizzlyNegotiationSupport.getMethod("addNegotiator", SSLEngine.class, GrizzlyAlpnClientNegotiator);
                    m.invoke(grizzlyNegotiationSupportObject, new Object[] { engine, java.lang.reflect.Proxy.newProxyInstance(
                                                                                                                              GrizzlyAlpnClientNegotiator.getClassLoader(),
                                                                                                                              new java.lang.Class[] { GrizzlyAlpnClientNegotiator },
                                                                                                                              negotiator) });
                }
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "registerGrizzlyAlpn grizzly-npn exception: " + e);
                }
            }
        }
    }

    /**
     * Using jetty-alpn, set up a new JettyServerNotiator to handle ALPN for a given SSLEngine and link
     *
     * @param SSLEngine
     * @param SSLConnectionLink
     */
    protected void registerJettyAlpn(final SSLEngine engine, SSLConnectionLink link) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "registerJettyAlpn entry " + engine);
        }
        try {
            JettyServerNotiator negotiator = new JettyServerNotiator(engine, link);
            // invoke ALPN.put(engine, provider(this))
            Method m = jettyAlpn.getMethod("put", SSLEngine.class, jettyProviderInterface);
            m.invoke(null, new Object[] { engine, java.lang.reflect.Proxy.newProxyInstance(
                                                                                           jettyServerProviderInterface.getClassLoader(),
                                                                                           new java.lang.Class[] { jettyServerProviderInterface },
                                                                                           negotiator) });

        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "registerJettyAlpn jetty-alpn exception: " + e);
            }
        }
    }

    /**
     * Proxy class for jetty-alpn to implement org.eclipse.jetty.alpn.ALPN$ServerProvider
     */
    public class JettyServerNotiator implements java.lang.reflect.InvocationHandler {

        private final SSLEngine engine;
        private final SSLConnectionLink link;

        public JettyServerNotiator(SSLEngine e, SSLConnectionLink l) {
            this.engine = e;
            this.link = l;
        }

        /**
         * Intended to be invoked with either select(List<String>) or unsupported()
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "invoke entry" + method);
            }
            String methodName = method.getName();

            if (methodName.equals("select")) {
                if (args != null && args.length == 1 && args[0] instanceof List<?>) {
                    return select((List<String>) args[0]);
                }
            } else if (methodName.equals("unsupported")) {
                if (args == null) {
                    unsupported();
                }
            }
            return null;
        }

        /**
         * Choose a protocol. Expect either "h2" or "http/1.1"
         *
         * @param SSLEngine
         * @param List<String> protocolList
         * @return the protocol we want to use in order of preference: h2, http/1.1
         */
        public String select(List<String> protocols) throws SSLException {
            removeEngine(); // engine must be removed on every callback
            if (protocols.contains(h2)) {
                if (link != null) {
                    link.setAlpnProtocol(h2);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "select protocol h2 selected" + engine);
                }
                return h2;
            } else if (protocols.contains(h1)) {
                if (link != null) {
                    link.setAlpnProtocol(h1);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "select protocol http/1.1 selected" + engine);
                    }
                }
                return h1;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "select no protocol matched, returning http/1.1" + engine);
            }
            return h1;
        }

        public void unsupported() {
            removeEngine();
        }

        public void removeEngine() {
            try {
                // invoke ALPN.remove()
                Method m = jettyAlpn.getMethod("remove", SSLEngine.class);
                m.invoke(null, this.engine);
            } catch (Throwable t) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "removeEngine failed\n" + t);
                }
            }
        }
    }

    /**
     * Proxy class for grizzly-npn to implement org.glassfish.grizzly.npn.AlpnServerNegotiator and
     * org.glassfish.grizzly.npn.AlpnClientNegotiator
     */
    public class GrizzlyAlpnNegotiator implements java.lang.reflect.InvocationHandler {

        private final SSLEngine engine;
        private final SSLConnectionLink link;

        public GrizzlyAlpnNegotiator(SSLEngine e, SSLConnectionLink l) {
            this.engine = e;
            this.link = l;
        }

        /**
         * Intended to be invoked with either selectProtocol(SSLEngine, String) or selectProtocol(SSLEngine, String[]).
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "invoke entry" + method);
            }
            String methodName = method.getName();

            if (methodName.equals("selectProtocol")) {
                if (args != null && args.length == 2 && args[0] instanceof SSLEngine && args[1] instanceof String[]) {
                    return selectProtocol((SSLEngine) args[0], (String[]) args[1]);
                } else if (args != null && args.length == 2 && args[0] instanceof SSLEngine && args[1] instanceof String) {
                    selectProtocol((SSLEngine) args[0], (String) args[1]);
                }
            }
            return null;
        }

        /**
         * Select a protocol when when engine.getUseClientMode=false. Expect either "h2" or "http/1.1"
         *
         * @param SSLEngine
         * @param String[] protocolList
         * @return the protocol we want to use in order of preference: h2, http/1.1
         */
        protected String selectProtocol(SSLEngine engine, String[] protocolList) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "selectProtocol entry engine: " + engine + " protocols: " + protocolList);
            }
            if (engine != null && protocolList != null && protocolList.length > 0) {
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
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "selectProtocol no protocol matched, returning http/1.1" + engine);
            }
            return h1;
        }

        /**
         * Select a protocol when engine.getUseClientMode=true. Expect either "h2" or "http/1.1"
         *
         * @param engine
         * @param protocol
         */
        protected void selectProtocol(SSLEngine engine, String protocol) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "selectProtocol entry engine: " + engine + " protocol: " + protocol);
            }
            if (engine != null && protocol != null) {
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

    }
}
