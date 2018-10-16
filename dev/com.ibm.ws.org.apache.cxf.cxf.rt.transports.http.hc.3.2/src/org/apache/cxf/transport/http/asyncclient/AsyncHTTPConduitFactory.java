/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.transport.http.asyncclient;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.TimeUnit;


import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.ManagedNHttpClientConnectionFactory;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.ManagedNHttpClientConnection;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.protocol.HttpContext;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 *
 */
@NoJSR250Annotations
public class AsyncHTTPConduitFactory implements HTTPConduitFactory {

    //TCP related properties
    public static final String TCP_NODELAY = "org.apache.cxf.transport.http.async.TCP_NODELAY";
    public static final String SO_KEEPALIVE = "org.apache.cxf.transport.http.async.SO_KEEPALIVE";
    public static final String SO_LINGER = "org.apache.cxf.transport.http.async.SO_LINGER";
    public static final String SO_TIMEOUT = "org.apache.cxf.transport.http.async.SO_TIMEOUT";

    //ConnectionPool
    public static final String MAX_CONNECTIONS = "org.apache.cxf.transport.http.async.MAX_CONNECTIONS";
    public static final String MAX_PER_HOST_CONNECTIONS
        = "org.apache.cxf.transport.http.async.MAX_PER_HOST_CONNECTIONS";
    public static final String CONNECTION_TTL = "org.apache.cxf.transport.http.async.CONNECTION_TTL";
    public static final String CONNECTION_MAX_IDLE = "org.apache.cxf.transport.http.async.CONNECTION_MAX_IDLE";

    //AsycClient specific props
    public static final String THREAD_COUNT = "org.apache.cxf.transport.http.async.ioThreadCount";
    public static final String INTEREST_OP_QUEUED = "org.apache.cxf.transport.http.async.interestOpQueued";
    public static final String SELECT_INTERVAL = "org.apache.cxf.transport.http.async.selectInterval";

    //CXF specific
    public static final String USE_POLICY = "org.apache.cxf.transport.http.async.usePolicy";


    public enum UseAsyncPolicy {
        ALWAYS, ASYNC_ONLY, NEVER;

        public static UseAsyncPolicy getPolicy(Object st) {
            if (st instanceof UseAsyncPolicy) {
                return (UseAsyncPolicy)st;
            } else if (st instanceof String) {
                String s = ((String)st).toUpperCase();
                if ("ALWAYS".equals(s)) {
                    return ALWAYS;
                } else if ("NEVER".equals(s)) {
                    return NEVER;
                } else if ("ASYNC_ONLY".equals(s)) {
                    return ASYNC_ONLY;
                } else {
                    st = Boolean.parseBoolean(s);
                }
            }
            if (st instanceof Boolean) {
                return ((Boolean)st).booleanValue() ? ALWAYS : NEVER;
            }
            return ASYNC_ONLY;
        }
    };

    volatile PoolingNHttpClientConnectionManager connectionManager;
    volatile CloseableHttpAsyncClient client;

    boolean isShutdown;
    UseAsyncPolicy policy;
    int maxConnections = 5000;
    int maxPerRoute = 1000;
    int connectionTTL = 60000;
    int connectionMaxIdle = 60000;

    int ioThreadCount = IOReactorConfig.DEFAULT.getIoThreadCount();
    long selectInterval = IOReactorConfig.DEFAULT.getSelectInterval();
    boolean interestOpQueued = IOReactorConfig.DEFAULT.isInterestOpQueued();
    int soLinger = IOReactorConfig.DEFAULT.getSoLinger();
    int soTimeout = IOReactorConfig.DEFAULT.getSoTimeout();
    boolean soKeepalive = IOReactorConfig.DEFAULT.isSoKeepalive();
    boolean tcpNoDelay = true;


    AsyncHTTPConduitFactory() {
        super();
    }

    public AsyncHTTPConduitFactory(Map<String, Object> conf) {
        this();
        setProperties(conf);
    }

    public AsyncHTTPConduitFactory(Bus b) {
        this();
        addListener(b);
        setProperties(b.getProperties());
    }

    public UseAsyncPolicy getUseAsyncPolicy() {
        return policy;
    }

    public void update(Map<String, Object> props) {
        if (setProperties(props) && client != null) {
            restartReactor();
        }
    }

    private void restartReactor() {
        CloseableHttpAsyncClient client2 = client;
        resetVars();
        shutdown(client2);
    }
    private synchronized void resetVars() {
        client = null;
        connectionManager = null;
    }


    private boolean setProperties(Map<String, Object> s) {
        //properties that can be updated "live"
        if (s == null) {
            return false;
        }
        Object st = s.get(USE_POLICY);
        if (st == null) {
            st = SystemPropertyAction.getPropertyOrNull(USE_POLICY);
        }
        policy = UseAsyncPolicy.getPolicy(st);

        maxConnections = getInt(s.get(MAX_CONNECTIONS), maxConnections);
        connectionTTL = getInt(s.get(CONNECTION_TTL), connectionTTL);
        connectionMaxIdle = getInt(s.get(CONNECTION_MAX_IDLE), connectionMaxIdle);
        maxPerRoute = getInt(s.get(MAX_PER_HOST_CONNECTIONS), maxPerRoute);

        if (connectionManager != null) {
            connectionManager.setMaxTotal(maxConnections);
            connectionManager.setDefaultMaxPerRoute(maxPerRoute);
        }

        //properties that need a restart of the reactor
        boolean changed = false;

        int i = ioThreadCount;
        ioThreadCount = getInt(s.get(THREAD_COUNT), Runtime.getRuntime().availableProcessors());
        changed |= i != ioThreadCount;

        long l = selectInterval;
        selectInterval = getInt(s.get(SELECT_INTERVAL), 1000);
        changed |= l != selectInterval;

        i = soLinger;
        soLinger = getInt(s.get(SO_LINGER), -1);
        changed |= i != soLinger;

        i = soTimeout;
        soTimeout = getInt(s.get(SO_TIMEOUT), 0);
        changed |= i != soTimeout;

        boolean b = interestOpQueued;
        interestOpQueued = getBoolean(s.get(INTEREST_OP_QUEUED), false);
        changed |= b != interestOpQueued;

        b = tcpNoDelay;
        tcpNoDelay = getBoolean(s.get(TCP_NODELAY), true);
        changed |= b != tcpNoDelay;

        b = soKeepalive;
        soKeepalive = getBoolean(s.get(SO_KEEPALIVE), false);
        changed |= b != soKeepalive;

        return changed;
    }
    private int getInt(Object s, int defaultv) {
        int i = defaultv;
        if (s instanceof String) {
            i = Integer.parseInt((String)s);
        } else if (s instanceof Number) {
            i = ((Number)s).intValue();
        }
        if (i == -1) {
            i = defaultv;
        }
        return i;
    }

    private boolean getBoolean(Object s, boolean defaultv) {
        if (s instanceof String) {
            return Boolean.parseBoolean((String)s);
        } else if (s instanceof Boolean) {
            return ((Boolean)s).booleanValue();
        }
        return defaultv;
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    @Override
    public HTTPConduit createConduit(HTTPTransportFactory f,
                                     Bus bus,
                                     EndpointInfo localInfo,
                                     EndpointReferenceType target) throws IOException {

        return createConduit(bus, localInfo, target);
    }

    public HTTPConduit createConduit(Bus bus,
                                     EndpointInfo localInfo,
                                     EndpointReferenceType target) throws IOException {
        if (isShutdown) {
            return null;
        }
        return new AsyncHTTPConduit(bus, localInfo, target, this);
    }

    public void shutdown() {
        if (client != null) {
            shutdown(client);
            connectionManager = null;
            client = null;
        }
        isShutdown = true;
    }

    @FFDCIgnore(IOException.class)
    private static void shutdown(CloseableHttpAsyncClient client) {
        try {
            client.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }


    private void addListener(Bus b) {
        BusLifeCycleManager manager = b.getExtension(BusLifeCycleManager.class);
        if (manager != null) {

            manager.registerLifeCycleListener(new BusLifeCycleListener() {
                public void initComplete() {
                }
                public void preShutdown() {
                    shutdown();
                }
                public void postShutdown() {
                }
            });
        }
    }

    public synchronized void setupNIOClient(HTTPClientPolicy clientPolicy) throws IOReactorException {
        if (client != null) {
            return;
        }

        IOReactorConfig config = IOReactorConfig.custom()
                .setIoThreadCount(ioThreadCount)
                .setSelectInterval(selectInterval)
                .setInterestOpQueued(interestOpQueued)
                .setSoLinger(soLinger)
                .setSoTimeout(soTimeout)
                .setSoKeepAlive(soKeepalive)
                .setTcpNoDelay(tcpNoDelay)
                .build();

        Registry<SchemeIOSessionStrategy> ioSessionFactoryRegistry = RegistryBuilder.<SchemeIOSessionStrategy>create()
                    .register("http", NoopIOSessionStrategy.INSTANCE)
                    // Liberty change - doPriv
                    .register("https", AccessController.doPrivileged(new PrivilegedAction<SSLIOSessionStrategy>(){

                        @Override
                        public SSLIOSessionStrategy run() {
                            return SSLIOSessionStrategy.getSystemDefaultStrategy();
                        }}))
                    .build();


        ManagedNHttpClientConnectionFactory connectionFactory = new ManagedNHttpClientConnectionFactory() {

            @Override
            public ManagedNHttpClientConnection create(final IOSession iosession, final ConnectionConfig config) {
                ManagedNHttpClientConnection conn = super.create(iosession, config);
                return conn;
            }
        };

        DefaultConnectingIOReactor ioreactor = new DefaultConnectingIOReactor(config);
        connectionManager = new PoolingNHttpClientConnectionManager(
                ioreactor,
                connectionFactory,
                ioSessionFactoryRegistry,
                DefaultSchemePortResolver.INSTANCE,
                SystemDefaultDnsResolver.INSTANCE,
                connectionTTL, TimeUnit.MILLISECONDS);

        connectionManager.setDefaultMaxPerRoute(maxPerRoute);
        connectionManager.setMaxTotal(maxConnections);

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setBufferSize(clientPolicy.getChunkLength() > 0 ? clientPolicy.getChunkLength() : 16332)
                .build();

        connectionManager.setDefaultConnectionConfig(connectionConfig);

        RedirectStrategy redirectStrategy = new RedirectStrategy() {

            public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context)
                throws ProtocolException {
                return false;
            }
            public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context)
                throws ProtocolException {
                return null;
            }
        };

        HttpAsyncClientBuilder httpAsyncClientBuilder = HttpAsyncClients.custom()
            .setConnectionManager(connectionManager)
            .setRedirectStrategy(redirectStrategy)
            .setDefaultCookieStore(new BasicCookieStore() {
                private static final long serialVersionUID = 1L;
                public void addCookie(Cookie cookie) {
                }
            });

        adaptClientBuilder(httpAsyncClientBuilder);

        // Liberty change - doPriv
        client = AccessController.doPrivileged(new PrivilegedAction<CloseableHttpAsyncClient>(){

            @Override
            public CloseableHttpAsyncClient run() {
                return httpAsyncClientBuilder.build();
            }});
        // Start the client thread
        client.start();
        if (this.connectionTTL == 0) {
            //if the connection does not have an expiry deadline
            //use the ConnectionMaxIdle to close the idle connection
            new CloseIdleConnectionThread(connectionManager, client).start();
        }
    }

    //provide a hook to customize the builder
    protected void adaptClientBuilder(HttpAsyncClientBuilder httpAsyncClientBuilder) {
    }

    public CloseableHttpAsyncClient createClient(final AsyncHTTPConduit c) throws IOException {
        if (client == null) {
            setupNIOClient(c.getClient());
        }
        return client;
    }

    public class CloseIdleConnectionThread extends Thread {

        private final PoolingNHttpClientConnectionManager connMgr;

        private final CloseableHttpAsyncClient client;

        public CloseIdleConnectionThread(PoolingNHttpClientConnectionManager connMgr,
                                     CloseableHttpAsyncClient client) {
            super();
            this.connMgr = connMgr;
            this.client = client;
        }

        @FFDCIgnore(InterruptedException.class)
        @Override
        public void run() {
            try {
                while (client.isRunning()) {
                    synchronized (this) {
                        sleep(connectionMaxIdle);
                        // close connections
                        // that have been idle longer than specified connectionMaxIdle
                        connMgr.closeIdleConnections(connectionMaxIdle, TimeUnit.MILLISECONDS);
                    }
                }
            } catch (InterruptedException ex) {
                // terminate
            }
        }

    }
}