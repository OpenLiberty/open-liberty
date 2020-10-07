/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.ldap.context;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.ContextNotEmptyException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapName;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.wim.adapter.ldap.BEROutputStream;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.security.wim.exception.EntityAlreadyExistsException;
import com.ibm.wsspi.security.wim.exception.EntityHasDescendantsException;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.exception.InvalidInitPropertyException;
import com.ibm.wsspi.security.wim.exception.OperationNotSupportedException;
import com.ibm.wsspi.security.wim.exception.WIMApplicationException;
import com.ibm.wsspi.security.wim.exception.WIMSystemException;

/**
 * This class manages {@link TimedDirContext}s for LDAP connections. It has additional support for
 * <ul>
 * <li>fail-over servers</li>
 * <li>context pooling</li>
 * </ul>
 *
 * Before a {@link ContextManager} can be used it needs to be configured and initialized. For example:
 *
 * <pre>
 * <code>
 * ContextManager cm = new ContextManager();
 * cm.setPrimaryServer("localhost", 389); // !!! MINIMUM CONFIGURATION !!!
 * cm.initialize();
 *
 * TimedDirContext ctx = cm.getDirContext();
 * </code>
 * </pre>
 */
public class ContextManager {

    /** Default initial number of contexts to create at context pool creation. */
    private static final int DEFAULT_INIT_POOL_SIZE = 1;

    /** Default maximum number of contexts that the context pool can create. */
    private static final int DEFAULT_MAX_POOL_SIZE = 0;

    /** Default amount of time a context is valid for being stale. 0 means contexts in the pool never go stale. */
    private static final int DEFAULT_POOL_TIME_OUT = 0;

    /** Default amount of time (in milliseconds) to wait to re-query the pool for a context when the pool has no more contexts. */
    private static final int DEFAULT_POOL_WAIT_TIME = 3000;

    /** Default preferred number of contexts in the context pool. */
    private static final int DEFAULT_PREF_POOL_SIZE = 3;

    /** The default connect time limit - 1 minute. */
    private static final long DEFAULT_CONNECT_TIMEOUT = 60000L;

    /** The default read time limit - 1 minute. */
    private static final long DEFAULT_READ_TIMEOUT = 60000L;

    /** Key to use for accessing the active URL from the JNDI environment table. */
    private static final String ENVKEY_ACTIVE_URL = "_ACTIVE_URL_";

    /** Key to use for accessing the URL list from the JNDI environment table. */
    private static final String ENVKEY_URL_LIST = "_URL_LIST_";

    /** JNDI connection timeout. */
    private static final int LDAP_CONNECT_TIMEOUT_TRACE = 1000;

    /** JNDI property for binary attributes setting. */
    private static final String LDAP_ENV_PROP_ATTRIBUTES_BINARY = "java.naming.ldap.attributes.binary";

    /** JNDI property for connection timeout setting. */
    private static final String LDAP_ENV_PROP_CONNECT_TIMEOUT = "com.sun.jndi.ldap.connect.timeout";

    /** JNDI property for dereferencing aliases setting. */
    private static final String LDAP_ENV_PROP_DEREF_ALIASES = "java.naming.ldap.derefAliases";

    /** JNDI property for the socket factory settings. */
    private static final String LDAP_ENV_PROP_FACTORY_SOCKET = "java.naming.ldap.factory.socket";

    /** JNDI property for read timeout setting. */
    private static final String LDAP_ENV_PROP_READ_TIMEOUT = "com.sun.jndi.ldap.read.timeout";

    /** JNDI property for the packet dump setting. */
    private static final String LDAP_ENV_PROP_JNDI_BER_OUTPUT = "com.sun.jndi.ldap.trace.ber";

    /** Timestamp of quick bind statistics trace. */
    private static final AtomicLong LDAP_STATS_TIMER = new AtomicLong(0);

    /**
     * Initial context factory class provided by SUN JNDI LDAP Provider
     */
    private static final String LDAP_SUN_SPI = "com.sun.jndi.ldap.LdapCtxFactory";

    /** Prefix for LDAP URLs. */
    private static final String LDAP_URL_PREFIX = "ldap://";

    /** Prefix for LDAPS URLs. */
    private static final String LDAP_URL_SSL_PREFIX = "ldaps://";

    /** Number of quick binds. */
    private static final AtomicInteger QUICK_LDAP_BIND = new AtomicInteger(0);

    /** Register the class to trace service. */
    private static final TraceComponent tc = Tr.register(ContextManager.class);

    /** Constant for URL sequence */
    private static final int URLTYPE_SEQUENCE = 1;

    /** Constant for Single URL */
    private static final int URLTYPE_SINGLE = 0;

    /** The name of WAS SSL Socket factory class. */
    private static final String WAS_SSL_SOCKET_FACTORY = "com.ibm.ws.ssl.protocol.LibertySSLSocketFactory";

    /** The names of all binary attributes, each separated by a space. */
    private String iBinaryAttributeNames;

    /** The administrative bind DN. */
    private String iBindDN;

    /** The administrative bind password. */
    private SerializableProtectedString iBindPassword;

    /** JNDI connection controls. */
    private final Control[] iConnCtls = null;

    /** The LDAP connection timeout. */
    private Long iConnectTimeout;

    /** Is Context Pool enabled? */
    private boolean iContextPoolEnabled = true;

    /** List that acts as a storage for the Pool of Directory contexts. */
    private List<TimedDirContext> iContexts = null;

    private String iDerefAliases = null;

    /** The table that stores the LDAP environment. */
    private Hashtable<String, Object> iEnvironment = null;

    /** Fail-over LDAP servers. */
    private final List<HostPort> iFailoverServers = new ArrayList<HostPort>();

    /** The initial pool size for the DirContext pool. */
    private int iInitPoolSize = DEFAULT_INIT_POOL_SIZE;

    /** Whether to dump JNDI packets to system out */
    private Boolean iJndiOutputEnabled = false;

    /** The timestamp of the last query for the return to primary. */
    private long iLastQueryTime = System.currentTimeMillis() / 1000;

    /** The number of live context objects in the pool. */
    private int iLiveContexts = 0;

    /** Semaphore for locking. */
    private final Object iLock = new Object() {};

    /** The Maximum pool size for the DirContext pool. */
    private int iMaxPoolSize = DEFAULT_MAX_POOL_SIZE;

    /** Create pool time stamp milliseconds. */
    private long iPoolCreateTimestampMillisec = 0;

    /** Create pool time stamp. */
    private long iPoolCreateTimestampSeconds = 0;

    /** The pool timeout in seconds. Defaults to infinite (zero). */
    private long iPoolTimeOut = DEFAULT_POOL_TIME_OUT;

    /** The Pool wait time in milliseconds. */
    private long iPoolWaitTime = DEFAULT_POOL_WAIT_TIME;

    /** The Preferred pool size. */
    private int iPrefPoolSize = DEFAULT_PREF_POOL_SIZE;

    /** The primary LDAP server. */
    private HostPort iPrimaryServer;

    /** Interval for querying whether we can return to the primary LDAP server. */
    private long iQueryInterval = 900;

    /** LDAP read timeout. */
    private Long iReadTimeout;

    /** Referral behavior ("ignore" or "follow"). */
    private String iReferral = "ignore";

    /** Return to primary server configuration. */
    private boolean iReturnToPrimary = false;

    /** SSL alias to use for out-bound SSL connections. */
    private String iSSLAlias = null;

    /** Whether to enable SSL for out-bound connections. */
    private boolean iSSLEnabled;

    /** Write to secondary server configuration. */
    private boolean iWriteToSecondary = false;

    /**
     * Add a fail-over LDAP server hostname and port.
     *
     * @param hostname The hostname for the primary LDAP server.
     * @param port The port for the primary LDAP server.
     */
    public void addFailoverServer(String hostname, int port) {
        iFailoverServers.add(new HostPort(hostname, port));
    }

    /**
     * Check whether we can write on the LDAP server the context is currently connected to. It is not
     * permissible to write to a fail-over server if write to secondary is disabled.
     *
     * @param ctx The context with a connection to the current LDAP server.
     * @throws OperationNotSupportedException If write to secondary is disabled and the context
     *             is not connected to the primary LDAP server.
     */
    public void checkWritePermission(TimedDirContext ctx) throws OperationNotSupportedException {
        if (!iWriteToSecondary) {
            String providerURL = getProviderURL(ctx);
            if (!getPrimaryURL().equalsIgnoreCase(providerURL)) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.WRITE_TO_SECONDARY_SERVERS_NOT_ALLOWED, WIMMessageHelper.generateMsgParms(providerURL));
                throw new OperationNotSupportedException(WIMMessageKey.WRITE_TO_SECONDARY_SERVERS_NOT_ALLOWED, msg);
            }
        }
    }

    /**
     * Check whether we can write on the LDAP server the context is currently connected to. It is not
     * permissible to write to a fail-over server if write to secondary is disabled.
     *
     * @param ctx the current directory context, can be null
     * @param currentURL The URL of the current LDAP server.
     * @param contextPoolEnabled Boolean to see if a context pool is enabled
     * @param currentTimeSeconds the current time in seconds
     * @return updated context if necessary otherwise will return original context
     * @throws NamingException If the primary server context isn't able to close properly or we are unavailable to ping the primary server or if we are unable to create the
     *                             contextPool
     */
    @FFDCIgnore(NamingException.class)
    private TimedDirContext checkPrimaryServer(TimedDirContext ctx, String currentURL, long currentTimeSeconds) throws WIMSystemException {
        String METHODNAME = "checkPrimaryServer";
        if (iReturnToPrimary && (currentTimeSeconds - iLastQueryTime) > iQueryInterval) {

            try {
                String primaryURL = getPrimaryURL();
                if (!primaryURL.equalsIgnoreCase(currentURL)) {
                    Hashtable<String, Object> env = getEnvironment(URLTYPE_SINGLE, primaryURL);
                    boolean primaryOK = false;
                    try {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, METHODNAME + " Ping primary server '" + primaryURL + "'...");
                        TimedDirContext testCtx = createDirContext(env);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, METHODNAME + " Ping primary server '" + primaryURL + "': success");
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, WIMMessageKey.CURRENT_LDAP_SERVER, WIMMessageHelper.generateMsgParms(getActiveURL()));
                        primaryOK = true;
                        if (ctx != null) {
                            TimedDirContext tempCtx = ctx;
                            try {
                                tempCtx.close();
                            } catch (NamingException e) {
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, METHODNAME + " Can not close LDAP connection: " + e.toString(true));
                            }
                        }
                        ctx = testCtx;
                    } catch (NamingException e) {
                        if (tc.isInfoEnabled())
                            Tr.info(tc, WIMMessageKey.CANNOT_CONNECT_LDAP_SERVER, WIMMessageHelper.generateMsgParms(primaryURL));
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, METHODNAME + " Ping primary server '" + primaryURL + "': fail");
                    }

                    // Refresh context pool if another thread has not already done so
                    if (primaryOK && iContextPoolEnabled) {
                        synchronized (iLock) {
                            if (!getActiveURL().equalsIgnoreCase(primaryURL)) {
                                createContextPool(iLiveContexts - 1, primaryURL);
                                ctx.setCreateTimestamp(iPoolCreateTimestampSeconds);
                            }
                        }
                    }
                }
                iLastQueryTime = currentTimeSeconds;
            } catch (NamingException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
                throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
            }
        }
        return ctx;
    }

    /**
     * Close the context pool.
     *
     * @param contexts The contexts in the pool to close.
     */
    @FFDCIgnore(NamingException.class)
    private void closeContextPool(List<TimedDirContext> contexts) {
        final String METHODNAME = "closeContextPool";
        if (contexts != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " Context pool being closed by " + Thread.currentThread() + ", Context pool size=" + contexts.size());
            }
            for (int i = 0; i < contexts.size(); i++) {
                TimedDirContext context = contexts.get(i);
                try {
                    context.close();
                    iLiveContexts--;
                } catch (NamingException e) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, METHODNAME + " Can not close LDAP connection: " + e.toString(true));
                }
            }
        }
    }

    /**
     * Create a directory context pool of the specified size.
     *
     * @param poolSize The initial size of the pool.
     * @param providerURL The URL of the LDAP provider.
     * @throws NamingException If there was an error connecting while creating the context pool.
     */
    private void createContextPool(Integer poolSize, String providerURL) throws NamingException {
        final String METHODNAME = "createContextPool";

        /*
         * Validate provider URL
         */
        if (providerURL == null) {
            providerURL = getPrimaryURL();
        }

        /*
         * Default the pool size if one was not provided.
         */
        if (poolSize == null) {
            poolSize = DEFAULT_INIT_POOL_SIZE;
        }

        /*
         * Enable the context pool
         */
        if (iContextPoolEnabled) {
            long currentTimeMillisec = System.currentTimeMillis();
            long currentTimeSeconds = roundToSeconds(currentTimeMillisec);

            // Don't purge the pool more than once per second
            // This prevents multiple threads from purging the pool
            if (currentTimeMillisec - iPoolCreateTimestampMillisec > 1000) {
                List<TimedDirContext> contexts = new Vector<TimedDirContext>(poolSize);
                Hashtable<String, Object> env = getEnvironment(URLTYPE_SEQUENCE, providerURL);

                String currentURL = null;
                try {
                    for (int i = 0; i < poolSize; i++) {
                        TimedDirContext ctx = createDirContext(env, currentTimeSeconds);
                        currentURL = getProviderURL(ctx);
                        if (!providerURL.equalsIgnoreCase(currentURL)) {
                            env = getEnvironment(URLTYPE_SEQUENCE, currentURL);
                            providerURL = currentURL;
                        }
                        contexts.add(ctx);
                        //iLiveContexts++;
                    }
                } catch (NamingException e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " Context Pool creation FAILED for " + Thread.currentThread() + ", iLiveContext=" + iLiveContexts, e);
                    }
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " Cleanup contexts in temp pool: " + contexts.size());
                    }
                    for (int j = 0; j < contexts.size(); j++) {
                        try {
                            TimedDirContext ctx = contexts.get(j);
                            ctx.close();
                        } catch (Exception ee) {
                        }
                    }
                    throw e;
                }
                iLiveContexts += poolSize;

                // set active URL
                setActiveURL(providerURL);

                List<TimedDirContext> oldCtxs = iContexts;
                iContexts = contexts;
                iPoolCreateTimestampSeconds = currentTimeSeconds;
                iPoolCreateTimestampMillisec = currentTimeMillisec;
                closeContextPool(oldCtxs);

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, METHODNAME + " Active Provider URL: " + getActiveURL());
                    Tr.debug(tc, METHODNAME + " ContextPool: total=" + iLiveContexts + ", poolSize=" + iContexts.size(),
                             ", iPoolCreateTimestampSeconds=" + iPoolCreateTimestampSeconds);
                }
            } else if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " Pool has already been purged within past second... skipping purge");
            }
        } else {
            setActiveURL(providerURL);
        }
    }

    /**
     * Create a directory context.
     *
     * @param env The JNDI environment to create the context with.
     * @return The {@link TimedDirContext}.
     * @throws NamingException If there was an issue binding to the LDAP server.
     */
    private TimedDirContext createDirContext(Hashtable<String, Object> env) throws NamingException {
        return createDirContext(env, roundToSeconds(System.currentTimeMillis()));
    }

    /**
     * Create a directory context.
     *
     * @param env The JNDI environment to create the context with.
     * @param createTimestamp The timestamp to use as the creation timestamp for the {@link TimedDirContext}.
     * @return The {@link TimedDirContext}.
     * @throws NamingException If there was an issue binding to the LDAP server.
     */
    private TimedDirContext createDirContext(Hashtable<String, Object> env, long createTimestamp) throws NamingException {

        /*
         * Check if the credential is a protected string. It will be unprotected if this is an anonymous bind
         */
        Object o = env.get(Context.SECURITY_CREDENTIALS);
        if (o instanceof ProtectedString) {
            // Reset the bindPassword to simple string.
            ProtectedString sps = (ProtectedString) env.get(Context.SECURITY_CREDENTIALS);
            String password = sps == null ? "" : new String(sps.getChars());
            String decodedPassword = PasswordUtil.passwordDecode(password.trim());
            env.put(Context.SECURITY_CREDENTIALS, decodedPassword);
        }

        SSLUtilImpl sslUtil = new SSLUtilImpl();
        Properties currentSSLProps = sslUtil.getSSLPropertiesOnThread();
        try {
            if (iSSLAlias != null) {
                try {
                    sslUtil.setSSLAlias(iSSLAlias, env);
                } catch (Exception ssle) {
                    throw new NamingException(ssle.getMessage());
                }
            }

            /*
             * Set the classloader so that a class in the package can be loaded by JNDI
             */
            ClassLoader origCL = getContextClassLoader();
            setContextClassLoader(getClass());
            try {
                TimedDirContext ctx = new TimedDirContext(env, getConnectionRequestControls(), createTimestamp);
                String newURL = getProviderURL(ctx);
                // Set the active URL if context pool is disabled,
                // otherwise active URL will be set when pool is refreshed
                if (!iContextPoolEnabled)
                    if (!newURL.equalsIgnoreCase(getActiveURL()))
                        setActiveURL(newURL);

                return ctx;
            } finally {
                setContextClassLoader(origCL);
            }
        } finally {
            sslUtil.setSSLPropertiesOnThread(currentSSLProps);
        }
    }

    /**
     * Create a directory context.
     *
     * @param principal The principal name to bind with.
     * @param credential The password / credential.
     * @return The {@link TimedDirContext} of the new connection.
     * @throws NamingException If the bind failed.
     */
    @FFDCIgnore(NamingException.class)
    public TimedDirContext createDirContext(String principal, byte[] credential) throws NamingException {
        final String METHODNAME = "createDirContext(String, byte[])";

        String activeURL = getActiveURL();
        Hashtable<String, Object> environment = getEnvironment(URLTYPE_SINGLE, activeURL);
        environment.put(Context.SECURITY_PRINCIPAL, principal);
        environment.put(Context.SECURITY_CREDENTIALS, credential);

        SSLUtilImpl sslUtil = new SSLUtilImpl();
        Properties currentSSLProps = sslUtil.getSSLPropertiesOnThread();
        try {
            if (iSSLAlias != null) {
                try {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, METHODNAME + " Use WAS SSL Configuration.");
                    sslUtil.setSSLAlias(iSSLAlias, environment);
                } catch (Exception ssle) {
                    throw new NamingException(ssle.getMessage());
                }
            }

            // Set the classloader so that a class in the package can be loaded by JNDI
            ClassLoader origCL = getContextClassLoader();
            setContextClassLoader(getClass());
            try {
                TimedDirContext ctx = null;
                try {
                    ctx = new TimedDirContext(environment, getConnectionRequestControls(), roundToSeconds(System.currentTimeMillis()));
                } catch (NamingException e) {
                    if (!isConnectionException(e)) {
                        throw e;
                    } else {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Encountered an exception while creating a context: " + e.getMessage());
                    }

                    // Get the Next URL
                    environment = getEnvironment(URLTYPE_SEQUENCE, getNextURL(activeURL));
                    // Reset the security credentials on the environment.
                    environment.put(Context.SECURITY_PRINCIPAL, principal);
                    environment.put(Context.SECURITY_CREDENTIALS, credential);

                    ctx = new TimedDirContext(environment, getConnectionRequestControls(), roundToSeconds(System.currentTimeMillis()));
                    String newURL = getProviderURL(ctx);
                    long creationTimeMillisec = System.currentTimeMillis();

                    synchronized (iLock) {
                        // Refresh context pool if another thread hasn't already done so
                        if (creationTimeMillisec > iPoolCreateTimestampMillisec) {
                            createContextPool(iLiveContexts, newURL);
                            ctx.setCreateTimestamp(iPoolCreateTimestampSeconds);
                        }
                    }
                }
                return ctx;
            } finally {
                setContextClassLoader(origCL);
            }
        } finally {
            sslUtil.setSSLPropertiesOnThread(currentSSLProps);
        }
    }

    /**
     * Creates and binds a new context, along with associated attributes.
     *
     * @param name The name to bind the new context.
     * @param attrs The attributes to bind on the new context.
     * @return The new context.
     * @throws OperationNotSupportedException If connected to a fail-over server and write to secondary is disabled.
     * @throws EntityAlreadyExistsException If the entity already exists.
     * @throws EntityNotFoundException If part of the name cannot be found to create the entity.
     * @throws WIMSystemException If any other {@link NamingException} occurs or the context cannot be released.
     */
    public DirContext createSubcontext(String name,
                                       Attributes attrs) throws OperationNotSupportedException, WIMSystemException, EntityAlreadyExistsException, EntityNotFoundException {
        final String METHODNAME = "createSubcontext";
        DirContext dirContext = null;
        TimedDirContext ctx = getDirContext();
        checkWritePermission(ctx);
        try {
            try {
                long startTime = System.currentTimeMillis();
                dirContext = ctx.createSubcontext(new LdapName(name), attrs);
                long endTime = System.currentTimeMillis();
                if ((endTime - startTime) > LDAP_CONNECT_TIMEOUT_TRACE) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, METHODNAME + " **LDAPConnect time: " + (endTime - startTime) + " ms, lock held " + Thread.holdsLock(iLock)
                                     + ", principal=" + name);
                } else {
                    handleBindStat(endTime - startTime);
                }
            } catch (NamingException e) {
                if (!isConnectionException(e)) {
                    throw e;
                }
                ctx = reCreateDirContext(ctx, e.toString());
                long startTime = System.currentTimeMillis();
                dirContext = ctx.createSubcontext(new LdapName(name), attrs);
                long endTime = System.currentTimeMillis();
                if ((endTime - startTime) > LDAP_CONNECT_TIMEOUT_TRACE) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, METHODNAME + " **LDAPConnect time: " + (endTime - startTime) + " ms, lock held " + Thread.holdsLock(iLock)
                                     + ", principal=" + name);
                } else {
                    handleBindStat(endTime - startTime);
                }
            }
        } catch (NameAlreadyBoundException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.ENTITY_ALREADY_EXIST, WIMMessageHelper.generateMsgParms(name));
            throw new EntityAlreadyExistsException(WIMMessageKey.ENTITY_ALREADY_EXIST, msg, e);
        } catch (NameNotFoundException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.PARENT_NOT_FOUND, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new EntityNotFoundException(WIMMessageKey.PARENT_NOT_FOUND, msg, e);
        } catch (NamingException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
        } finally {
            releaseDirContext(ctx);
        }
        return dirContext;
    }

    /**
     * Delete the given name from the LDAP tree.
     *
     * @param name The distinguished name to delete.
     * @throws EntityHasDescendantsException The context being destroyed is not empty.
     * @throws EntityNotFoundException If part of the name cannot be found to destroy the entity.
     * @throws WIMSystemException If any other {@link NamingException} occurs or the context cannot be released.
     */
    public void destroySubcontext(String name) throws EntityHasDescendantsException, EntityNotFoundException, WIMSystemException {
        TimedDirContext ctx = getDirContext();
        // checkWritePermission(ctx); // TODO Why are we not checking permissions here?
        try {
            try {
                ctx.destroySubcontext(new LdapName(name));
            } catch (NamingException e) {
                if (!isConnectionException(e)) {
                    throw e;
                }
                ctx = reCreateDirContext(ctx, e.toString());
                ctx.destroySubcontext(new LdapName(name));
            }
        } catch (ContextNotEmptyException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.ENTITY_HAS_DESCENDENTS, WIMMessageHelper.generateMsgParms(name));
            throw new EntityHasDescendantsException(WIMMessageKey.ENTITY_HAS_DESCENDENTS, msg, e);
        } catch (NameNotFoundException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.LDAP_ENTRY_NOT_FOUND, WIMMessageHelper.generateMsgParms(name, e.toString(true)));
            throw new EntityNotFoundException(WIMMessageKey.LDAP_ENTRY_NOT_FOUND, msg, e);
        } catch (NamingException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
        } finally {
            releaseDirContext(ctx);
        }
    }

    /**
     * Format the given address as an IPv6 Address.
     *
     * @param host The address.
     * @return The IPv6 formatted address.
     */
    private static String formatIPv6Addr(String host) {
        if (host == null) {
            return null;
        } else {
            return (new StringBuilder()).append("[").append(host).append("]").toString();
        }
    }

    /**
     * Helper method to get the current active URL.
     *
     * @return The active URL.
     */
    @Trivial
    private String getActiveURL() {
        return (String) iEnvironment.get(ENVKEY_ACTIVE_URL);
    }

    /**
     * Get the connection request controls.
     *
     * @return The connection request controls.
     */
    private Control[] getConnectionRequestControls() {
        return iConnCtls;
    }

    /**
     * Convenience method to get the context ClassLoader for the current thread
     * using {@link AccessController#doPrivileged(PrivilegedAction)}.
     *
     * @return The {@link ClassLoader} returned from
     *         {@link Thread#currentThread()#getContextClassLoader()}.
     */
    private static ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    /**
     * Get a directory context.
     *
     * @return The context.
     * @throws WIMSystemException If any {@link NamingException}s occurred.
     */
    @FFDCIgnore({ InterruptedException.class, NamingException.class })
    public TimedDirContext getDirContext() throws WIMSystemException {
        final String METHODNAME = "getDirContext";
        TimedDirContext ctx = null;
        long currentTimeSeconds = roundToSeconds(System.currentTimeMillis());

        if (iContextPoolEnabled) {
            do {
                //Get the lock for the current domain
                synchronized (iLock) {
                    if (iContexts == null) {
                        try {
                            createContextPool(iInitPoolSize, null);
                        } catch (NamingException e) {
                            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
                            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
                        }
                    }

                    if (iContexts.size() > 0) {
                        ctx = iContexts.remove(iContexts.size() - 1);
                    } else if (iLiveContexts < iMaxPoolSize || iMaxPoolSize == 0) {
                        //Will create later outside of the synchronized code.
                        iLiveContexts++;
                    } else {
                        try {
                            iLock.wait(iPoolWaitTime);
                        } catch (InterruptedException e) {
                            // This is ok...if exception occurs, then continue...
                        }
                        continue;
                    }
                }

                TimedDirContext oldCtx = null;
                if (ctx != null) {
                    /*
                     * Has the context from the pool expired?
                     *
                     * If iPoolTimeOut > 0, check if the DirContex expires or not. If iPoolTimeOut = 0,
                     * the DirContext will be used forever until it is staled.
                     */
                    if (iPoolTimeOut > 0 && (currentTimeSeconds - ctx.getPoolTimestamp()) > iPoolTimeOut) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, METHODNAME + " ContextPool: context is time out. currentTime=" + currentTimeSeconds + ", createTime="
                                         + ctx.getPoolTimestamp() + ", iPoolTimeOut=" + iPoolTimeOut);
                        }
                        oldCtx = ctx;
                        ctx = null;
                    }
                } else {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " ContextPool: no free context, create a new one...");
                    }
                }
                // Test if primaryURL is available
                ctx = checkPrimaryServer(ctx, getActiveURL(), currentTimeSeconds);

                if (ctx == null) {
                    try {
                        ctx = createDirContext(getEnvironment(URLTYPE_SEQUENCE, getActiveURL()));
                    } catch (NamingException e) {
                        iLiveContexts--;
                        String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
                        throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
                    }
                }
                if (oldCtx != null) {
                    try {
                        oldCtx.close();
                    } catch (NamingException e) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, METHODNAME + " Can not close LDAP connection: " + e.toString(true));
                    }
                }
            } while (ctx == null);

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " ContextPool: total=" + iLiveContexts + ", poolSize=" + iContexts.size() + ", currentTime=" + currentTimeSeconds + ", createTime="
                             + ctx.getPoolTimestamp());
            }
        } else {
            try {
                // Test if primaryURL is available
                ctx = checkPrimaryServer(null, getActiveURL(), currentTimeSeconds);

                // create the connection
                if (ctx == null) {
                    ctx = createDirContext(getEnvironment(URLTYPE_SEQUENCE, getActiveURL()));

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, WIMMessageKey.CURRENT_LDAP_SERVER, WIMMessageHelper.generateMsgParms(getActiveURL()));
                }
            } catch (NamingException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
                throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
            }
        }
        return ctx;
    }

    /**
     * Returns LDAP environment containing specified URL sequence.
     *
     * @param type Single or sequence
     * @param startingURL Starting URL
     * @return Environment containing specified URL sequence
     */
    @SuppressWarnings("unchecked")
    private Hashtable<String, Object> getEnvironment(int type, String startingURL) {
        Hashtable<String, Object> env = new Hashtable<String, Object>(iEnvironment);
        List<String> urlList = (List<String>) env.remove(ENVKEY_URL_LIST);
        int numURLs = urlList.size();

        // get active URL index
        int startingURLIndex = getURLIndex(startingURL, urlList);

        // generate the sequence
        String ldapUrl = null;
        for (int i = startingURLIndex; i < startingURLIndex + numURLs; i++) {
            if (i > startingURLIndex)
                ldapUrl = ldapUrl + " " + urlList.get(i % numURLs);
            else
                ldapUrl = urlList.get(i % numURLs);

            if (type == URLTYPE_SINGLE)
                break;
        }

        env.put(Context.PROVIDER_URL, ldapUrl);
        env.remove(ENVKEY_ACTIVE_URL);

        return env;
    }

    /**
     * Helper method to get the configured list of URLs.
     *
     * @return The list of URLs.
     */
    @SuppressWarnings("unchecked")
    @Trivial
    private List<String> getEnvURLList() {
        return (List<String>) iEnvironment.get(ENVKEY_URL_LIST);
    }

    /**
     * Returns the next URL after the specified URL.
     *
     * @param currentURL Current URL
     * @return Next URL
     */
    @Trivial
    private String getNextURL(String currentURL) {
        List<String> urlList = getEnvURLList();
        int urlIndex = getURLIndex(currentURL, urlList);
        return urlList.get((urlIndex + 1) % urlList.size());
    }

    /**
     * Helper method to get the configured Primary URL
     *
     * @return The primary URL.
     */
    @Trivial
    private String getPrimaryURL() {
        return getEnvURLList().get(0);
    }

    /**
     * Get the provider URL from the given directory context.
     *
     * @param ctx
     * @return
     */
    @Trivial
    @FFDCIgnore(NamingException.class)
    private String getProviderURL(TimedDirContext ctx) {
        try {
            return (String) ctx.getEnvironment().get(Context.PROVIDER_URL);
        } catch (NamingException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getProviderURL", e.toString(true));
            }
            return "(null)";
        }
    }

    /**
     * Returns URL index in the URL list.
     *
     * @param url URL
     * @param urlList List of URLs
     * @return URL index
     */
    private int getURLIndex(String url, List<String> urlList) {
        int urlIndex = 0;
        int numURLs = urlList.size();

        // get URL index
        if (url != null)
            for (int i = 0; i < numURLs; i++)
                if ((urlList.get(i)).equalsIgnoreCase(url)) {
                    urlIndex = i;
                    break;
                }

        return urlIndex;
    }

    /**
     * Track the count of "quick" binds. Dump the updated statistic to the log at most once every 30 seconds.
     *
     * @param elapsedTime The time in milliseconds that the bind took
     */
    private void handleBindStat(long elapsedTime) {
        String METHODNAME = "handleBindStat(long)";
        if (elapsedTime < LDAP_CONNECT_TIMEOUT_TRACE) {
            QUICK_LDAP_BIND.getAndIncrement();
        }

        long now = System.currentTimeMillis();

        /*
         * Print out at most every 30 minutes the latest number of "quick" binds
         */
        if (now - LDAP_STATS_TIMER.get() > 1800000) {
            //Update the last update time, then make certain no one beat us to it
            long lastUpdated = LDAP_STATS_TIMER.getAndSet(now);
            if (now - lastUpdated > 1800000) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, METHODNAME + " **LDAPBindStat: " + QUICK_LDAP_BIND.get() + " binds took less then " + LDAP_CONNECT_TIMEOUT_TRACE + " ms");
            }
        }
    }

    /**
     * Initialize the {@link ContextManager}. This should be called before creating any contexts.
     *
     * @returns The result for this initialization call.
     * @throws WIMApplicationException
     */
    public InitializeResult initialize() throws WIMApplicationException {
        final String METHODNAME = "initialize";
        iEnvironment = new Hashtable<String, Object>();
        iEnvironment.put(Context.INITIAL_CONTEXT_FACTORY, LDAP_SUN_SPI);

        String urlPrefix = null;
        if (iSSLEnabled) {
            iEnvironment.put(LDAP_ENV_PROP_FACTORY_SOCKET, WAS_SSL_SOCKET_FACTORY);
            iEnvironment.put(Context.SECURITY_PROTOCOL, "ssl");
            urlPrefix = LDAP_URL_SSL_PREFIX;
        } else {
            urlPrefix = LDAP_URL_PREFIX;
        }

        List<String> urlList = new ArrayList<String>();

        /*
         * Add the primary server to the URL list.
         */
        if (iPrimaryServer == null || iPrimaryServer.hostname == null || iPrimaryServer.hostname.trim().isEmpty()) {
            return InitializeResult.MISSING_PRIMARY_SERVER;
        }
        String mainHost = iPrimaryServer.hostname;
        int mainPort = iPrimaryServer.port;
        urlList.add(urlPrefix + mainHost.trim() + ":" + mainPort);

        /*
         * Add the fail-over servers to the URL list.
         */
        for (HostPort failoverServer : iFailoverServers) {
            String ldapHost = failoverServer.hostname;
            if (ldapHost == null || ldapHost.trim().isEmpty()) {
                continue;
            }

            if (!(ldapHost.startsWith("[") && ldapHost.endsWith("]"))) {
                if (isIPv6Addr(ldapHost)) {
                    ldapHost = formatIPv6Addr(ldapHost);
                }
            }

            if (failoverServer.port != null) {
                urlList.add(urlPrefix + ldapHost.trim() + ":" + failoverServer.port);
            }
        }

        if (urlList != null && urlList.size() > 0) {
            String url = urlList.get(0);
            iEnvironment.put(ENVKEY_URL_LIST, urlList);
            iEnvironment.put(ENVKEY_ACTIVE_URL, url);
            iEnvironment.put(Context.PROVIDER_URL, url);
        }

        /*
         * If no administrative credentials, allow anonymous bind.
         */
        if (iBindDN != null && !iBindDN.isEmpty()) {
            iEnvironment.put(Context.SECURITY_PRINCIPAL, iBindDN);
            SerializableProtectedString sps = iBindPassword;
            String password = sps == null ? "" : new String(sps.getChars());
            String decodedPassword = PasswordUtil.passwordDecode(password.trim());

            /*
             * A password is required if we had a bind DN.
             */
            if (decodedPassword == null || decodedPassword.length() == 0) {
                return InitializeResult.MISSING_PASSWORD;
            }
            iEnvironment.put(Context.SECURITY_CREDENTIALS, new ProtectedString(decodedPassword.toCharArray()));
        }

        /*
         * Set the LDAP connection time out
         */
        if (iConnectTimeout != null) {
            iEnvironment.put(LDAP_ENV_PROP_CONNECT_TIMEOUT, iConnectTimeout.toString());
        } else {
            iEnvironment.put(LDAP_ENV_PROP_CONNECT_TIMEOUT, String.valueOf(DEFAULT_CONNECT_TIMEOUT));
        }

        /*
         * Set the LDAP read time out.
         */
        if (iReadTimeout != null) {
            iEnvironment.put(LDAP_ENV_PROP_READ_TIMEOUT, iReadTimeout.toString());
        } else {
            iEnvironment.put(LDAP_ENV_PROP_READ_TIMEOUT, String.valueOf(DEFAULT_READ_TIMEOUT));
        }

        /*
         * Enabled JNDI BER output if required.
         */
        if (iJndiOutputEnabled != null && iJndiOutputEnabled) {
            iEnvironment.put(LDAP_ENV_PROP_JNDI_BER_OUTPUT, new BEROutputStream());
        }

        /*
         * TODO Support different authentication mechanisms.
         *
         * String authen = (String) configProps.get(ConfigConstants.CONFIG_PROP_AUTHENTICATION);
         * iEnvironment.put(Context.SECURITY_AUTHENTICATION, authen);
         */

        /*
         * Determine referral handling behavior.
         */
        iEnvironment.put(Context.REFERRAL, iReferral);

        /*
         * Determine alias dereferencing behavior. JNDI defaults to "always",
         * so only set if not null and not "always".
         */
        if (iDerefAliases != null && !"always".equalsIgnoreCase(iDerefAliases)) {
            iEnvironment.put(LDAP_ENV_PROP_DEREF_ALIASES, iDerefAliases);
        }

        /*
         * Add binary attribute names
         */
        if (iBinaryAttributeNames != null && iBinaryAttributeNames.length() > 0) {
            iEnvironment.put(LDAP_ENV_PROP_ATTRIBUTES_BINARY, iBinaryAttributeNames);
        }

        /*
         * TODO Support other environment properties.
         *
         * // Initialize additional environment properties. These environ props will overwrite the above settings.
         * List envProps = server.getList(CONFIG_PROP_ENVIRONMENT_PROPERTIES);
         * for (int i = 0; i < envProps.size(); i++) {
         * DataObject envProp = (DataObject) envProps.get(i);
         * String name = envProp.getString(CONFIG_PROP_NAME);
         * String value = envProp.getString(CONFIG_PROP_VALUE);
         * iEnvironment.put(name, value);
         * }
         */

        /*
         * Create Context Pool
         */
        try {
            createContextPool(iInitPoolSize, null);
        } catch (NamingException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " Can not create context pool: " + e.toString(true));
            }
        }

        if (tc.isDebugEnabled()) {
            StringBuffer strBuf = new StringBuffer();
            strBuf.append("\nLDAP Server(s): ").append(urlList).append("\n");
            strBuf.append("\tBind DN: ").append(iBindDN).append("\n");
            // strBuf.append("\tAuthenticate: ").append(authen).append("\n");
            strBuf.append("\tReferral: ").append(iReferral).append("\n");
            strBuf.append("\tDeref Aliases: ").append(iDerefAliases).append("\n");
            strBuf.append("\tBinary Attributes: ").append(iBinaryAttributeNames).append("\n");
            // strBuf.append("\tAdditional Evn Props: ").append(envProps);

            if (iContextPoolEnabled) {
                strBuf.append("\nContext Pool is enabled: ").append("\n");
                strBuf.append("\tInitPoolSize: ").append(iInitPoolSize).append("\n");
                strBuf.append("\tMaxPoolSize: ").append(iMaxPoolSize).append("\n");
                strBuf.append("\tPrefPoolSize: ").append(iPrefPoolSize).append("\n");
                strBuf.append("\tPoolTimeOut: ").append(iPoolTimeOut).append("\n");
                strBuf.append("\tPoolWaitTime: ").append(iPoolWaitTime);
            } else {
                strBuf.append("\nContext Pool is disabled");
            }
            Tr.debug(tc, METHODNAME + strBuf.toString());
        }

        return InitializeResult.SUCCESS;
    }

    /**
     * Most LDAPs throw CommunicationException when LDAP server is down, but
     * z/OS sometime throws ServiceUnavailableException when ldap server is down.
     *
     * @param e The {@link NamingException} to check.
     */
    public static boolean isConnectionException(NamingException e) {
        return (e instanceof CommunicationException) || (e instanceof ServiceUnavailableException);
    }

    /**
     * Is the address an IPv6 Address.
     *
     * @param host The host string to check.
     * @return True if the string is in the format of an IPv6 address.
     */
    private static boolean isIPv6Addr(String host) {
        if (host != null) {
            if (host.contains("[") && host.contains("]"))
                host = host.substring(host.indexOf("[") + 1, host.indexOf("]"));
            host = host.toLowerCase();
            Pattern p1 = Pattern.compile("^(?:(?:(?:(?:[a-f0-9]{1,4}(?::[a-f0-9]{1,4}){7})|(?:(?!(?:.*[a-f0-9](?::|$)){7,})(?:[a-f0-9]{1,4}(?::[a-f0-9]{1,4}){0,5})?::(?:[a-f0-9]{1,4}(?::[a-f0-9]{1,4}){0,5})?)))|(?:(?:(?:[a-f0-9]{1,4}(?::[a-f0-9]{1,4}){5}:)|(?:(?!(?:.*[a-f0-9]:){5,})(?:[a-f0-9]{1,4}(?::[a-f0-9]{1,4}){0,3})?::(?:[a-f0-9]{1,4}(?::[a-f0-9]{1,4}){0,3}:)?))?(?:(?:25[0-5])|(?:2[0-4][0-9])|(?:1[0-9]{2})|(?:[1-9]?[0-9]))(?:\\.(?:(?:25[0-5])|(?:2[0-4][0-9])|(?:1[0-9]{2})|(?:[1-9]?[0-9]))){3}))$");
            Pattern p2 = Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}$");
            Matcher m1 = p1.matcher(host);
            boolean b1 = m1.matches();
            Matcher m2 = p2.matcher(host);
            boolean b2 = !m2.matches();
            return b1 && b2;
        } else {
            return false;
        }
    }

    /**
     * Recreate a Directory context, where the oldContext failed with the given error message.
     *
     * @param oldCtx The context that failed.
     * @param errorMessage The error message from the failure.
     * @return The new {@link TimedDirContext}. It is possible this context is connected to another LDAP server than the
     *         old context was.
     * @throws WIMSystemException If any {@link NamingException}s occurred.
     */
    public TimedDirContext reCreateDirContext(TimedDirContext oldCtx, String errorMessage) throws WIMSystemException {
        final String METHODNAME = "DirContext reCreateDirContext(String errorMessage)";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " Communication exception occurs: " + errorMessage + " Creating a new connection.");
        }

        try {
            Long oldCreateTimeStampSeconds = oldCtx.getCreateTimestamp();
            TimedDirContext ctx;

            /*
             * If the old context was created before the context pool was created, then
             * we can just request a context from the pool.
             *
             * If the pool is older than the context, then we should create a new context
             * and also create a new context pool if context pooling is enabled.
             */
            if (oldCreateTimeStampSeconds < iPoolCreateTimestampSeconds) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, METHODNAME + " Pool refreshed, skip to getDirContext. oldCreateTimeStamp: " + oldCreateTimeStampSeconds + " iPoolCreateTimestampSeconds:"
                                 + iPoolCreateTimestampSeconds);
                }
                ctx = getDirContext();
            } else {
                String oldURL = getProviderURL(oldCtx);
                ctx = createDirContext(getEnvironment(URLTYPE_SEQUENCE, getNextURL(oldURL)));
                String newURL = getProviderURL(ctx);

                synchronized (iLock) {
                    // Refresh context pool if another thread hasn't already done so
                    if (oldCtx.getCreateTimestamp() >= iPoolCreateTimestampSeconds) {
                        createContextPool(iLiveContexts - 1, newURL);
                        ctx.setCreateTimestamp(iPoolCreateTimestampSeconds);
                    }
                }
            }

            oldCtx.close();

            if (tc.isDebugEnabled())
                Tr.debug(tc, WIMMessageKey.CURRENT_LDAP_SERVER, WIMMessageHelper.generateMsgParms(getActiveURL()));
            return ctx;
        } catch (NamingException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
        }
    }

    /**
     * Release the given directory context.
     *
     * @param ctx The context to release.
     * @throws WIMSystemException If there were any {@link NamingException}s while releasing the context.
     */
    @FFDCIgnore(NamingException.class)
    public void releaseDirContext(TimedDirContext ctx) throws WIMSystemException {
        final String METHODNAME = "releaseDirContext";
        if (iContextPoolEnabled) {

            //Get the lock for the current domain
            synchronized (iLock) {
                // If the DirContextTTL is 0, no need to put it back to pool
                // If the size of the pool is larger than minimum size or total dirContexts larger than max size
                // If context URL no longer matches active URL, then discard
                if (iContexts.size() >= iPrefPoolSize || (iMaxPoolSize != 0 && iLiveContexts > iMaxPoolSize)
                    || ctx.getCreateTimestamp() < iPoolCreateTimestampSeconds
                    || !getProviderURL(ctx).equalsIgnoreCase(getActiveURL())) {
                    try {
                        iLiveContexts--; //PM95697
                        ctx.close();
                    } catch (NamingException e) {
                        String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
                        throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
                    }

                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " Context is discarded.");
                    }
                } else {
                    if (iContexts != null && iContexts.size() > 0 && iContexts.contains(ctx)) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, METHODNAME + " Context already present in Context pool. No need to add it again to context pool.  ContextPool: total=" + iLiveContexts
                                         + ", poolSize=" + iContexts.size());
                        }
                    } else {
                        if (iContexts != null)
                            iContexts.add(ctx);
                        if (iPoolTimeOut > 0) {
                            ctx.setPoolTimeStamp(roundToSeconds(System.currentTimeMillis()));
                        }

                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, METHODNAME + " Before Notifying the waiting threads and Context is back to pool.  ContextPool: total=" + iLiveContexts
                                         + ", poolSize=" + iContexts.size());
                        }
                    }
                    iLock.notifyAll();
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " Context is back to pool.");
                    }

                }
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " ContextPool: total=" + iLiveContexts + ", poolSize=" + iContexts.size());
            }
        } else {
            try {
                ctx.close();
            } catch (NamingException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
                throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
            }
        }
    }

    /**
     * Round a millisecond time stamp to seconds.
     *
     * @param timeInMilliseconds The millisecond time to round to seconds.
     * @return The time stamp rounded to seconds.
     */
    private static long roundToSeconds(long timeInMilliseconds) {
        long returnInSeconds = timeInMilliseconds / 1000;
        if (timeInMilliseconds % 1000 > 499) {
            returnInSeconds++;
        }
        return returnInSeconds;
    }

    /**
     * Helper function to set the given URL as the active URL
     *
     * @param activeURL
     */
    @Trivial
    private void setActiveURL(String activeURL) {
        synchronized (iLock) {
            iEnvironment.put(ENVKEY_ACTIVE_URL, activeURL);
        }
    }

    /**
     * Set the binary attribute names.
     *
     * @param binaryAttributeNames The names of all binary attributes, each separated by a space.
     */
    public void setBinaryAttributeNames(String binaryAttributeNames) {
        this.iBinaryAttributeNames = binaryAttributeNames;
    }

    /**
     * Set the LDAP connection timeout.
     *
     * @param connectTimeout The LDAP connection timeout in milliseconds.
     */
    public void setConnectTimeout(Long connectTimeout) {
        this.iConnectTimeout = connectTimeout;
    }

    /**
     * Convenience method to set the context ClassLoader for the current thread
     * using {@link AccessController#doPrivileged(PrivilegedAction)}.
     *
     * @param clazz The class to get the {@link ClassLoader} to set when calling.
     *            {@link Thread#currentThread()#setContextClassLoader(ClassLoader)}.
     */
    private static void setContextClassLoader(final Class<?> clazz) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
                return null;
            }
        });
    }

    /**
     * Convenience method to set the context ClassLoader for the current thread
     * using {@link AccessController#doPrivileged(PrivilegedAction)}.
     *
     * @param classLoader The {@link ClassLoader} to set when calling.
     *            {@link Thread#currentThread()#setContextClassLoader(ClassLoader)}.
     */
    private static void setContextClassLoader(final ClassLoader classLoader) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                Thread.currentThread().setContextClassLoader(classLoader);
                return null;
            }
        });
    }

    /**
     * Set the context pool configuration.<p/>
     *
     * The context pool parameters are not required when <code>enableContextPool == false</code>. If <code>enableContextPool == true</code> and any of the context pool parameters
     * are null, that parameter will be set to the default value.
     *
     * @param enableContextPool Whether the context pool is enabled.
     * @param initPoolSize The initial context pool size.
     * @param prefPoolSize The preferred context pool size. Not required when <code>enableContextPool == false</code>.
     * @param maxPoolSize The maximum context pool size. A size of '0' means the maximum size is unlimited. Not required when <code>enableContextPool == false</code>.
     * @param poolTimeOut The context pool timeout in milliseconds. This is the amount of time a context is valid for in
     *            the context pool is valid for until it is discarded. Not required when <code>enableContextPool == false</code>.
     * @param poolWaitTime The context pool wait time in milliseconds. This is the amount of time to wait when getDirContext() is called
     *            and no context is available from the pool before checking again. Not required when <code>enableContextPool == false</code>.
     * @throws InvalidInitPropertyException If <code>initPoolSize > maxPoolSize</code> or <code>prefPoolSize > maxPoolSize</code> when <code>maxPoolSize != 0</code>.
     */
    public void setContextPool(boolean enableContextPool, Integer initPoolSize, Integer prefPoolSize, Integer maxPoolSize, Long poolTimeOut,
                               Long poolWaitTime) throws InvalidInitPropertyException {

        final String METHODNAME = "setContextPool";

        this.iContextPoolEnabled = enableContextPool;

        if (iContextPoolEnabled) {
            this.iInitPoolSize = initPoolSize == null ? DEFAULT_INIT_POOL_SIZE : initPoolSize;
            this.iMaxPoolSize = maxPoolSize == null ? DEFAULT_MAX_POOL_SIZE : maxPoolSize;
            this.iPrefPoolSize = prefPoolSize == null ? DEFAULT_PREF_POOL_SIZE : prefPoolSize;
            this.iPoolTimeOut = poolTimeOut == null ? DEFAULT_POOL_TIME_OUT : poolTimeOut;
            this.iPoolWaitTime = poolWaitTime == null ? DEFAULT_POOL_WAIT_TIME : poolWaitTime;

            if (iMaxPoolSize != 0 && iMaxPoolSize < iInitPoolSize) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.INIT_POOL_SIZE_TOO_BIG,
                                              WIMMessageHelper.generateMsgParms(Integer.valueOf(iInitPoolSize), Integer.valueOf(iMaxPoolSize)));
                throw new InvalidInitPropertyException(WIMMessageKey.INIT_POOL_SIZE_TOO_BIG, msg);
            }
            if (iMaxPoolSize != 0 && iPrefPoolSize != 0 && iMaxPoolSize < iPrefPoolSize) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.PREF_POOL_SIZE_TOO_BIG,
                                              WIMMessageHelper.generateMsgParms(Integer.valueOf(iInitPoolSize), Integer.valueOf(iMaxPoolSize)));
                throw new InvalidInitPropertyException(WIMMessageKey.PREF_POOL_SIZE_TOO_BIG, msg);
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " Context Pool is disabled.");
            }
        }
    }

    /**
     * Configure handling for dereferencing aliases.
     *
     * @param derefAliases The setting for dereferencing aliases.
     */
    public void setDerefAliases(String derefAliases) {
        iDerefAliases = derefAliases;
    }

    /**
     * Set the primary LDAP server hostname and port.
     *
     * @param hostname The hostname for the primary LDAP server.
     * @param port The port for the primary LDAP server.
     */
    public void setPrimaryServer(String hostname, int port) {
        this.iPrimaryServer = new HostPort(hostname, port);
    }

    /**
     * Set the interval to query the primary LDAP server after failing over to a fail-over server.
     * The {@link #setReturnToPrimary(boolean)} method must be called with <code>true</code> for this to
     * take effect.
     *
     * @param queryInterval The interval to query for the return of the primary LDAP server.
     * @see #setReturnToPrimary(boolean)
     */
    public void setQueryInterval(long queryInterval) {
        this.iQueryInterval = queryInterval;
    }

    /**
     * Set the LDAP read timeout.
     *
     * @param readTimeout The LDAP read timeout in milliseconds.
     */
    public void setReadTimeout(Long readTimeout) {
        this.iReadTimeout = readTimeout;
    }

    /**
     * Set JndiOutput
     *
     * @param jndiOutputEnabled whether the output is enabled.
     */
    public void setJndiOutputEnabled(Boolean jndiOutputEnabled) {
        this.iJndiOutputEnabled = jndiOutputEnabled;

    }

    /**
     * Set JNDI referral behavior.
     *
     * @param referral Should be one of either "ignore" or "follow".
     */
    public void setReferral(String referral) {
        this.iReferral = referral;
    }

    /**
     * Set whether to return to the primary LDAP server when it becomes available.
     *
     * @param returnToPrimary True to return to the primary LDAP server; false to remain on the secondary.
     * @see #setQueryInterval(long)
     */
    public void setReturnToPrimary(boolean returnToPrimary) {
        this.iReturnToPrimary = returnToPrimary;
    }

    /**
     * Set the administrative credentials used for simple authentication.
     *
     * @param bindDn The administrative bind DN.
     * @param bindPassword The administrative bind password.
     */
    public void setSimpleCredentials(String bindDn, SerializableProtectedString bindPassword) {
        this.iBindDN = bindDn;
        this.iBindPassword = bindPassword;
    }

    /**
     * Set the SSL alias.
     *
     * @param sslAlias The SSL alias to use for outgoing SSL connections.
     */
    public void setSSLAlias(String sslAlias) {
        this.iSSLAlias = sslAlias;
    }

    /**
     * Set whether SSL is enabled.
     *
     * @param sslEnabled True to enable SSL; false to disable.
     */
    public void setSSLEnabled(boolean sslEnabled) {
        this.iSSLEnabled = sslEnabled;
    }

    /**
     * Set whether to restrict writes to primary or to allow writes to fail-over servers.
     *
     * @param writeToSecondary True to allow writes to fail-over servers; false to prohibit.
     */
    public void setWriteToSecondary(boolean writeToSecondary) {
        this.iWriteToSecondary = writeToSecondary;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("ContextManager {");
        sb.append("iBindDN=").append(iBindDN);
        sb.append(", iBindPassword=").append(iBindPassword);
        sb.append(", iSSLAlias=").append(iSSLAlias);
        sb.append(", iSSLEnabled=").append(iSSLEnabled);
        sb.append(", iConnectTimeout=").append(iConnectTimeout);
        sb.append(", iReadTimeout=").append(iReadTimeout);
        sb.append(", iJndiOutputEnabled=").append(iJndiOutputEnabled);
        sb.append(", iPrimaryServer=").append(iPrimaryServer);
        sb.append(", iFailoverServers=").append(iFailoverServers);
        sb.append(", iContextPoolEnabled=").append(iContextPoolEnabled);
        sb.append(", iInitPoolSize=").append(iInitPoolSize);
        sb.append(", iPrefPoolSize=").append(iPrefPoolSize);
        sb.append(", iMaxPoolSize=").append(iMaxPoolSize);
        sb.append(", iPoolTimeOut=").append(iPoolTimeOut);
        sb.append(", iPoolWaitTime=").append(iPoolWaitTime);
        sb.append(", iWriteToSecondary=").append(iWriteToSecondary);
        sb.append(", iQueryInterval=").append(iQueryInterval);
        sb.append(", iReturnToPrimary=").append(iReturnToPrimary);
        sb.append(", iReferral=").append(iReferral);
        sb.append(", iBinaryAttributeNames=").append(iBinaryAttributeNames);
        sb.append("}");

        return sb.toString();
    }

    /** Simple class for storing a host name and port. */
    @Trivial
    private class HostPort {
        final String hostname;
        final Integer port;

        HostPort(String hostname, Integer port) {
            this.hostname = hostname;
            this.port = port;
        }

        @Override
        public String toString() {
            return this.hostname + ":" + this.port;
        }
    }

    /**
     * Enumeration of possible results returned from the {@link ContextManager#initialize()} method.
     */
    @Trivial
    public enum InitializeResult {
        /**
         * Initialization failed because a bind DN was specified but either no password or an empty
         * password was supplied.
         */
        MISSING_PASSWORD,

        /**
         * Initialization failed because there was no primary server specified. Call the
         * {@link ContextManager#setPrimaryServer(String, int)} method to set a primary LDAP server.
         */
        MISSING_PRIMARY_SERVER,

        /** Initialization succeeded with no errors. */
        SUCCESS;
    }
}
