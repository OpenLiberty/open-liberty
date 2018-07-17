/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channel.ssl.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class enables the use of the ALPN SSL protocol extension, and by extension secure HTTP/2.
 *
 * Users have four options to get ALPN support:
 * 1. JDK9 has native support for ALPN in javax.net.ssl
 * 2. IBM JDK8 sr5 fp15+ provides ALPN support via com.ibm.jsse2.ext.ALPNJSSEExt
 * 3. jetty-alpn can be put on the bootclasspath - alpn-boot-*.jar
 * 4. grizzly-npn can be put on the bootclasspath - grizzly-npn-bootstrap-1.*.jar
 *
 * Preference is given in the order of the list: native JDK9, IBM JDK8, grizzly-npn, then jetty-alpn.
 *
 * The Grizzly and Jetty extensions require users to configure a bootclasspath jar: either
 * grizzly-npn-bootstrap-1.*.jar or alpn-boot-*.jar. To use either of those jars, the server muse be running on
 * a JDK 8 provided by either Oracle or OpenJDK.
 *
 * On instantiation, the class tries to load the new JDK9 ALPN APIs. If those aren't found, it tries classes from the IBM JDK;
 * and if those are not found, it attempts to load jetty extension classes, and then grizzly-npn classes. If any set of classes
 * are found then this class is used to interface via reflection with the ALPN APIs provided by each project.
 *
 * @author wtlucy
 */
public class SSLAlpnNegotiator {

    private static final TraceComponent tc = Tr.register(SSLAlpnNegotiator.class,
                                                         SSLChannelConstants.SSL_TRACE_NAME,
                                                         SSLChannelConstants.SSL_BUNDLE);

    /** supported protocols */
    private final String h1 = "http/1.1";
    private final String h2 = "h2";
    private static boolean grizzlyAlpnPresent = false;
    private static boolean jettyAlpnPresent = false;
    private static boolean ibmAlpnPresent = false;
    private static boolean nativeAlpnPresent = false;

    /** Java 9+ ALPN API methods */
    private static Method nativeAlpnGet;
    private static Method nativeAlpnGetHandshake;
    private static Method nativeAlpnPut;

    static {
        try {
            nativeAlpnGet = SSLEngine.class.getMethod("getApplicationProtocol");
            nativeAlpnPut = SSLParameters.class.getMethod("setApplicationProtocols", String[].class);
            nativeAlpnGetHandshake = SSLEngine.class.getMethod("getHandshakeApplicationProtocol");

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "java 9+ methods found; ALPN is available");
            }
            nativeAlpnPresent = true;

        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "problem encountered initializing native java 9+ alpn provider: " + t);
            }
        }
    }

    /** IBM JSSE classes to find via reflection */
    private static Class<?> ibmAlpn;
    private static Method ibmAlpnGet;
    private static Method ibmAlpnPut;
    private static Method ibmAlpnDelete;

    static {
        if (!nativeAlpnPresent) {
            try {
                ibmAlpn = ClassLoader.getSystemClassLoader().loadClass("com.ibm.jsse2.ext.ALPNJSSEExt");
                ibmAlpnPresent = true;

                // invoke ALPNJSSEExt.init()
                ibmAlpn.getMethod("init").invoke(null);
                ibmAlpnGet = ibmAlpn.getMethod("get", SSLEngine.class);
                ibmAlpnPut = ibmAlpn.getMethod("put", SSLEngine.class, String[].class);
                ibmAlpnDelete = ibmAlpn.getMethod("delete", SSLEngine.class);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "com.ibm.jsse2.ext.ALPNJSSEExt was found on the classpath; ALPN is available");
                }
            } catch (Throwable t) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "problem encountered initializing IBM alpn provider: " + t);
                }
            }
        }
    }

    /** jetty-alpn classes to find via reflection */
    private static Class<?> jettyAlpn;
    private static Class<?> jettyServerProviderInterface;
    private static Class<?> jettyProviderInterface;

    static {
        if (!nativeAlpnPresent && !ibmAlpnPresent) {
            try {
                jettyAlpn = ClassLoader.getSystemClassLoader().loadClass("org.eclipse.jetty.alpn.ALPN");
                jettyServerProviderInterface = ClassLoader.getSystemClassLoader().loadClass("org.eclipse.jetty.alpn.ALPN$ServerProvider");
                jettyProviderInterface = ClassLoader.getSystemClassLoader().loadClass("org.eclipse.jetty.alpn.ALPN$Provider");
                jettyAlpnPresent = true;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "jetty-alpn module was found on the classpath; ALPN is available");
                }
            } catch (Throwable t) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "problem initializing jetty alpn provider: " + t);
                }
            }
        }
    }

    /** grizzly-npn classes to find via reflection */
    private static Class<?> grizzlyNegotiationSupport;
    private static Class<?> grizzlyAlpnClientNegotiator;
    private static Class<?> grizzlyAlpnServerNegotiator;
    private static Object grizzlyNegotiationSupportObject;

    static {
        if (!nativeAlpnPresent && !jettyAlpnPresent && !ibmAlpnPresent) {
            try {
                grizzlyNegotiationSupport = ClassLoader.getSystemClassLoader().loadClass("org.glassfish.grizzly.npn.NegotiationSupport");
                grizzlyAlpnServerNegotiator = ClassLoader.getSystemClassLoader().loadClass("org.glassfish.grizzly.npn.AlpnServerNegotiator");
                grizzlyAlpnClientNegotiator = ClassLoader.getSystemClassLoader().loadClass("org.glassfish.grizzly.npn.AlpnClientNegotiator");
                grizzlyNegotiationSupportObject = grizzlyNegotiationSupport.newInstance();
                grizzlyAlpnPresent = true;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "grizzly-npn module was found on the classpath; ALPN is available");
                }
            } catch (Throwable t) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "problem initializing grizzly alpn provider: " + t);
                }
            }
        }
    }

    public SSLAlpnNegotiator() {
        // no-op
    }

    /**
     * @return true if the Java 9 ALPN API is available
     */
    protected boolean isNativeAlpnActive() {
        return nativeAlpnPresent;
    }

    /**
     * @return true if com.ibm.jsse2.ext.ALPNJSSEExt is available
     */
    protected boolean isIbmAlpnActive() {
        return ibmAlpnPresent;
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
     * Check for the Java 9 ALPN API, IBM's ALPNJSSEExt, jetty-alpn, and grizzly-npn; if any are present, set up the connection for ALPN.
     * Order of preference is Java 9 ALPN API, IBM's ALPNJSSEExt, jetty-alpn, then grizzly-npn.
     *
     * @param SSLEngine
     * @param SSLConnectionLink
     * @param useAlpn true if alpn should be used
     * @return ThirdPartyAlpnNegotiator used for this connection,
     *         or null if ALPN is not available or the Java 9 / IBM provider was used
     */
    protected ThirdPartyAlpnNegotiator tryToRegisterAlpnNegotiator(SSLEngine engine, SSLConnectionLink link, boolean useAlpn) {
        if (isNativeAlpnActive()) {
            if (useAlpn) {
                registerNativeAlpn(engine);
            }
        } else if (isIbmAlpnActive()) {
            registerIbmAlpn(engine, useAlpn);
        } else if (this.isJettyAlpnActive() && useAlpn) {
            return registerJettyAlpn(engine, link);
        } else if (this.isGrizzlyAlpnActive() && useAlpn) {
            return registerGrizzlyAlpn(engine, link);
        }
        return null;
    }

    /**
     * If ALPN is active, try to remove the ThirdPartyAlpnNegotiator from the map of active negotiators
     *
     * @param ThirdPartyAlpnNegotiator
     * @param SSLEngine
     * @param SSLConnectionLink
     */
    protected void tryToRemoveAlpnNegotiator(ThirdPartyAlpnNegotiator negotiator, SSLEngine engine, SSLConnectionLink link) {
        // the Java 9 and IBM JSSE ALPN implementations don't use a negotiator object
        if (negotiator == null && isNativeAlpnActive()) {
            getNativeAlpnChoice(engine, link);
        } else if (negotiator == null && isIbmAlpnActive()) {
            getAndRemoveIbmAlpnChoice(engine, link);
        } else if (negotiator != null && isJettyAlpnActive() && negotiator instanceof JettyServerNegotiator) {
            ((JettyServerNegotiator) negotiator).removeEngine();
        } else if (negotiator != null && isGrizzlyAlpnActive() && negotiator instanceof GrizzlyAlpnNegotiator) {
            ((GrizzlyAlpnNegotiator) negotiator).removeServerNegotiatorEngine();
        }
    }

    /**
     * Register {h2, http/1.1} via the Java 9 ALPN API: SSLEngine.setApplicationProtocols(String[] protocols)
     *
     * @param SSLEngine
     */
    protected void registerNativeAlpn(SSLEngine engine) {
        if (isNativeAlpnActive()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "registerNativeAlpn entry " + engine);
            }
            try {
                String[] protocols = new String[] { h2, h1 };
                SSLParameters params = engine.getSSLParameters();
                nativeAlpnPut.invoke(params, (Object) protocols);
                engine.setSSLParameters(params);
                Method m = SSLParameters.class.getMethod("getApplicationProtocols");

            } catch (InvocationTargetException ie) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "registerNativeAlpn exception: " + ie.getTargetException());
                }
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "registerNativeAlpn exception: " + e);
                }
            }
        }
    }

    /**
     * Invoke SSLEngine.getApplicationProtocol() - Get the selected ALPN protocol, and set that protocol on the link.
     *
     * @param SSLEngine
     * @param SSLConnectionLink
     */
    protected void getNativeAlpnChoice(SSLEngine engine, SSLConnectionLink link) {
        if (isNativeAlpnActive()) {
            try {
                String alpnResult = (String) nativeAlpnGet.invoke(engine);
                if (alpnResult == null || alpnResult.isEmpty()) {
                    alpnResult = (String) nativeAlpnGetHandshake.invoke(engine);
                }

                if (alpnResult == null) {
                    Tr.debug(tc, "getNativeAlpnChoice: protocol not yet determined " + engine);
                } else if (alpnResult.isEmpty()) {
                    Tr.debug(tc, "getNativeAlpnChoice: ALPN will not be used " + engine);
                } else if (h2.equals(alpnResult)) {
                    // a protocol was selected
                    if (link.getAlpnProtocol() == null) {
                        Tr.debug(tc, "getNativeAlpnChoice: h2 protocol selected " + engine);
                        link.setAlpnProtocol(h2);
                    }
                }

            } catch (InvocationTargetException ie) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getNativeAlpnChoice exception: " + ie.getTargetException());
                }
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getNativeAlpnChoice exception: " + e);
                }
            }
        }
    }

    /**
     * invoke ALPNJSSEExt.put(SSLEngine, String[] protocols); if useAlpn is true, pass in "h2" and "http/1.1" (in order of preference),
     * and if useAlpn is false, don't pass any protocols to the alpn extension.
     *
     * @param SSLEngine
     * @param boolean useAlpn
     */
    protected void registerIbmAlpn(SSLEngine engine, boolean useAlpn) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "registerIbmAlpn entry " + engine);
        }
        try {
            // invoke ALPNJSSEExt.put(engine, String[] protocols)
            String[] protocols;
            if (useAlpn) {
                protocols = new String[] { h2, h1 };
            } else {
                // don't pass any protocols; alpn not used
                protocols = new String[] {};
            }
            ibmAlpnPut.invoke(null, engine, protocols);

        } catch (InvocationTargetException ie) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "registerIbmAlpn exception: " + ie.getTargetException());
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "registerIbmAlpn exception: " + e);
            }
        }
    }

    /**
     * Ask the JSSE ALPN provider for the protocol selected for the given SSLEngine, then delete the engine from
     * the ALPN provider's map. If the selected protocol was "h2", set that as the protocol to use on the given link.
     *
     * @param SSLEngine
     * @param SSLConnectionLink
     */
    protected void getAndRemoveIbmAlpnChoice(SSLEngine engine, SSLConnectionLink link) {
        if (this.isIbmAlpnActive()) {
            try {
                // invoke ALPNJSSEExt.get(engine)
                String[] alpnResult = (String[]) ibmAlpnGet.invoke(null, engine);

                // invoke ALPNJSSEExt.delete(engine)
                ibmAlpnDelete.invoke(null, engine);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("getAndRemoveIbmAlpnChoice");
                    if (alpnResult != null && alpnResult.length > 0) {
                        sb.append(" results:");
                        for (String s : alpnResult) {
                            sb.append(" " + s);
                        }
                        sb.append(" " + engine);
                    } else {
                        sb.append(": ALPN not used for " + engine);
                    }
                    Tr.debug(tc, sb.toString());
                }

                if (alpnResult != null && alpnResult.length == 1 && h2.equals(alpnResult[0])) {
                    if (link.getAlpnProtocol() == null) {
                        link.setAlpnProtocol(h2);
                    }
                }
            } catch (InvocationTargetException ie) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getAndRemoveIbmAlpnChoice exception: " + ie.getTargetException());
                }
            } catch (Exception e) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getAndRemoveIbmAlpnChoice exception: " + e);
                }
            }
        }
    }

    /**
     * Using grizzly-npn, set up a new GrizzlyAlpnNegotiator to handle ALPN for a given SSLEngine and link
     *
     * @param SSLEngine
     * @param SSLConnectionLink
     * @return GrizzlyAlpnNegotiator or null if ALPN was not set up
     */
    protected GrizzlyAlpnNegotiator registerGrizzlyAlpn(SSLEngine engine, SSLConnectionLink link) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "registerGrizzlyAlpn entry " + engine);
        }

        if (grizzlyNegotiationSupport != null && grizzlyAlpnClientNegotiator != null && grizzlyAlpnServerNegotiator != null && grizzlyNegotiationSupportObject != null) {
            try {
                GrizzlyAlpnNegotiator negotiator = new GrizzlyAlpnNegotiator(engine, link);

                if (!engine.getUseClientMode()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "initializeAlpn invoke AlpnServerNegotiator " + engine);
                    }
                    // client mode is disabled; call NegotiationSuppoer.addNegotiator(SSLEngine, (AlpnServerNegotiator) this)
                    Method m = grizzlyNegotiationSupport.getMethod("addNegotiator", SSLEngine.class, grizzlyAlpnServerNegotiator);
                    m.invoke(grizzlyNegotiationSupportObject, new Object[] { engine, java.lang.reflect.Proxy.newProxyInstance(
                                                                                                                              grizzlyAlpnServerNegotiator.getClassLoader(),
                                                                                                                              new java.lang.Class[] { grizzlyAlpnServerNegotiator },
                                                                                                                              negotiator) });
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "initializeAlpn invoke AlpnClientNegotiator " + engine);
                    }
                    // client mode is enabled; call NegotiationSuppoer.addNegotiator(SSLEngine, (AlpnClientNegotiator) this)
                    Method m = grizzlyNegotiationSupport.getMethod("addNegotiator", SSLEngine.class, grizzlyAlpnClientNegotiator);
                    m.invoke(grizzlyNegotiationSupportObject, new Object[] { engine, java.lang.reflect.Proxy.newProxyInstance(
                                                                                                                              grizzlyAlpnClientNegotiator.getClassLoader(),
                                                                                                                              new java.lang.Class[] { grizzlyAlpnClientNegotiator },
                                                                                                                              negotiator) });
                }
                return negotiator;
            } catch (InvocationTargetException ie) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "registerGrizzlyAlpn exception: " + ie.getTargetException());
                }
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "registerGrizzlyAlpn grizzly-npn exception: " + e);
                }
            }
        }
        return null;
    }

    /**
     * Using jetty-alpn, set up a new JettyServerNotiator to handle ALPN for a given SSLEngine and link
     *
     * @param SSLEngine
     * @param SSLConnectionLink
     * @return JettyServerNegotiator or null if ALPN was not set up
     */
    protected JettyServerNegotiator registerJettyAlpn(final SSLEngine engine, SSLConnectionLink link) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "registerJettyAlpn entry " + engine);
        }
        try {
            JettyServerNegotiator negotiator = new JettyServerNegotiator(engine, link);
            // invoke ALPN.put(engine, provider(this))
            Method m = jettyAlpn.getMethod("put", SSLEngine.class, jettyProviderInterface);
            m.invoke(null, new Object[] { engine, java.lang.reflect.Proxy.newProxyInstance(
                                                                                           jettyServerProviderInterface.getClassLoader(),
                                                                                           new java.lang.Class[] { jettyServerProviderInterface },
                                                                                           negotiator) });
            return negotiator;
        } catch (InvocationTargetException ie) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "registerJettyAlpn exception: " + ie.getTargetException());
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "registerJettyAlpn jetty-alpn exception: " + e);
            }
        }
        return null;
    }

    /**
     * Interface for the InvocationHandler ALPN implementations this class defines
     */
    public interface ThirdPartyAlpnNegotiator {}

    /**
     * Proxy class for jetty-alpn to implement org.eclipse.jetty.alpn.ALPN$ServerProvider
     */
    public class JettyServerNegotiator implements InvocationHandler, ThirdPartyAlpnNegotiator {

        private final SSLEngine engine;
        private final SSLConnectionLink link;

        public JettyServerNegotiator(SSLEngine e, SSLConnectionLink l) {
            this.engine = e;
            this.link = l;
        }

        /**
         * Intended to be invoked with either select(List<String>) or unsupported()
         */
        @SuppressWarnings("unchecked")
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
            } catch (InvocationTargetException ie) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "removeEngine exception: " + ie.getTargetException());
                }
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
    public class GrizzlyAlpnNegotiator implements InvocationHandler, ThirdPartyAlpnNegotiator {

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
            removeServerNegotiatorEngine();
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
            removeClientNegotiatorEngine();
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

        public void removeServerNegotiatorEngine() {
            try {
                Method m = grizzlyNegotiationSupport.getMethod("removeAlpnServerNegotiator", SSLEngine.class);
                m.invoke(grizzlyNegotiationSupportObject, this.engine);
            } catch (InvocationTargetException ie) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "removeServerNegotiatorEngine exception: " + ie.getTargetException());
                }
            } catch (Throwable t) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "removeServerNegotiatorEngine failed\n" + t);
                }
            }
        }

        public void removeClientNegotiatorEngine() {
            try {
                Method m = grizzlyNegotiationSupport.getMethod("removeAlpnClientNegotiator", SSLEngine.class);
                m.invoke(grizzlyNegotiationSupportObject, this.engine);
            } catch (InvocationTargetException ie) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "removeClientNegotiatorEngine exception: " + ie.getTargetException());
                }
            } catch (Throwable t) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "removeClientNegotiatorEngine failed\n" + t);
                }
            }
        }
    }
}
