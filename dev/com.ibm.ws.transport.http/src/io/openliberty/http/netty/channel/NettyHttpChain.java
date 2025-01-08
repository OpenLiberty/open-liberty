package io.openliberty.http.netty.channel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.internal.HttpChain.ActiveConfiguration;
import com.ibm.ws.http.internal.HttpEndpointImpl;
import com.ibm.ws.http.internal.HttpServiceConstants;
import com.ibm.ws.http.internal.VirtualHostMap;
import com.ibm.ws.http.netty.NettyChain;
import com.ibm.ws.http.netty.pipeline.HttpPipelineInitializer;
import com.ibm.ws.http.netty.pipeline.HttpPipelineInitializer.ConfigElement;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.openliberty.http.channel.AbstractHttpChain;
import io.openliberty.http.channel.Chain;
import io.openliberty.http.channel.ChainConfiguration;
import io.openliberty.http.channel.ChainState;
import io.openliberty.http.netty.quiesce.QuiesceStrategy;
import io.openliberty.netty.internal.ConfigConstants;
import io.openliberty.netty.internal.NettyFramework;
import io.openliberty.netty.internal.ServerBootstrapExtended;
import io.openliberty.netty.internal.exception.NettyException;


public class NettyHttpChain  extends AbstractHttpChain {

    private static final TraceComponent tc = Tr.register(NettyChain.class);

    private final NettyFramework framework;
    private ServerBootstrapExtended bootstrap;
    private volatile Channel channel;

    public NettyHttpChain(HttpEndpointImpl endpoint, boolean isHttps){
        super(endpoint, isHttps);
    }

    public void init(String endpointId, NettyFramework framework) {
        Objects.requireNonNull(framework, "NettyFramework cannot be null");
        this.framework = framework;

        final String root = endpointId + (isHttps() ? "-ssl" : "");

        endpointName = root;
        tcpName = "TCP-" + root;
        sslName = isHttps ? "SSL-" + root : null;
        httpName = "HTTP-" + root;
        dispatcherName = "HTTPD-" + root;
        chainName = "CHAIN-" + root;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Netty Chain initialized: Endpoint ID = " + endpointId + ", Endpoint Name = " + root);
        }
    }

    @Override
    public void stop() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(this, tc, "Stopping Netty Chain: " + endpointName + ", Current state: " + state().get());
        }

        if (state().get() == ChainState.STARTED || state().get() == ChainState.STARTING) {
            framework.getEndpointManager().endpointMgr.removeEndPoint(endpointName);
            state().set(ChainState.STOPPING);

            try {
                if (Objects.nonNull(channel) && channel.isOpen()) {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Server Channel is open, attempting to close");
                    }

                    framework.stop(channel, -1);
                    channel.closeFuture().syncUninterruptibly();
                    channel = null;
                }

            } finally {

                VirtualHostMap.notifyStopped(endpoint(), this.config().getHost(), config().port(), isHttps());
                //config().clearActivePort();
                String topic = endpoint().getEventTopic() + HttpServiceConstants.ENDPOINT_STOPPED;
                postEvent(topic, config(), null);

                state().set(ChainState.STOPPED);
                notifyAll();
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Netty Chain is not in a stoppable state. Current state: " + state().get());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(this, tc, "stop chain " + this);
        }
    }

    @Override
    public void update(ChainConfiguration config) {
       if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(this, tc, "Updating Netty Chain  " + endpointName + " Current state: " + state().get());
        }

        if (!enabled() || FrameworkState.isStopping()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.exit(this, tc, "Chain is disabled or framework is stopping, skipping update ");
            }
            return;
        }

        ChainConfiguration newConfig = config;

        if (config().requiresRestart(newConfig)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Stopping chain due to configuration " + newConfig);
            }
            // save the new/changed configuration before we start setting up the new chain
            this.config = newConfig;
            stopAndWait();
            state().set(ChainState.UNINITIALIZED);
        }

        else {

            if (config().requiresRestart(config)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "This configuration differs and should cause an update ");
                }
                config = newConfig;
                if (state().get() != ChainState.UNINITIALIZED) {
                    stopAndWait();
                }
            }
            start();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Channel restarted with new configuration");
        }

    }

    public synchronized void start(){
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(this, tc, "Starting Netty Channel: " + endpointName + ", Current state: " + state().get() + ", Enabled: " + enabled());
        }
        if (state().compareAndSet(ChainState.STOPPED, ChainState.STARTING) || state().compareAndSet(ChainState.UNINITIALIZED, ChainState.STARTING)) {
            try {
                Map<String, Object> httpOptions = new HashMap<>(endpoint().getHttpOptions());
                httpOptions.put(HttpConfigConstants.PROPNAME_ACCESSLOG_ID, endpoint().getName());
                // Put the protocol version, which allows the http channel to dynamically
                // know what http version it will use.
                if (endpoint().getProtocolVersion() != null) {
                    httpOptions.put(HttpConfigConstants.PROPNAME_PROTOCOL_VERSION, endpoint().getProtocolVersion());
                }

                EndPointInfo info = endpointMgr.defineEndPoint(this.endpointName, config().getHost(), config().port());

                Map<String, Object> tcpOptions = new HashMap<>(endpoint().getTcpOptions());
                tcpOptions.put(ConfigConstants.EXTERNAL_NAME, endpointName);

                bootstrap = framework.createTCPBootstrap(tcpOptions);
                HttpPipelineInitializer.HttpPipelineBuilder pipelineBuilder = new HttpPipelineInitializer.HttpPipelineBuilder(this)
                                                                                    .with(ConfigElement.COMPRESSION, endpoint().getCompressionConfig())
                                                                                    .with(ConfigElement.HTTP_OPTIONS, httpOptions)
                                                                                    .with(ConfigElement.HEADERS, endpoint().getHeadersConfig())
                                                                                    .with(ConfigElement.REMOTE_IP, endpoint().getRemoteIpConfig())
                                                                                    .with(ConfigElement.SAMESITE, endpoint().getSamesiteConfig());

                // Add SSL options only if the chain is SSL-enabled
                if (this.isHttps()) {
                    Map<String, Object> sslOptions = new HashMap<>(endpoint().getSslOptions());
                    pipelineBuilder.with(ConfigElement.SSL_OPTIONS, sslOptions);
                }

                HttpPipelineInitializer httpPipeline = pipelineBuilder.build();

                bootstrap.childOption(ChannelOption.ALLOW_HALF_CLOSURE, true);
                bootstrap.childHandler(httpPipeline);

                channel = framework.start(bootstrap, info.getHost(), info.getPort(), this::channelFutureHandler);

                VirtualHostMap.notifyStarted(endpoint(), () -> config().getHost(), config().port(), isHttps());
                String topic = endpoint().getEventTopic() + HttpServiceConstants.ENDPOINT_STARTED;
                postEvent(topic, config(), null);

            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.exit(this, tc, "Failed to start Netty Channel: " + e.getMessage());
                }
                state().set(ChainState.STOPPED);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(this, tc, "Finished starting Netty Channel: " + endpointName + ", Final state: " + state.get());
        }
    }

    private void channelFutureHandler(ChannelFuture future) {
        if (state().get() == ChainState.STOPPING) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Chain: " + endpointName + ", Current state: " + state().get() + ", is stopping so will not notify any virtual hosts and will just return");
            }
            return;
        }
        synchronized (this) {
            if (future.isSuccess()) {
                state().set(ChainState.STARTED);
                EndPointInfo info = framework().getEndpointManager().getEndPoint(this.endpointName);
                info = framework().getEndpointManager().defineEndPoint(this.endpointName, config().getHost(), config().port());
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Channel is now active and listening on port " + activePort());
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Channel failed to bind to port:  " + future.cause());
                }
                handleStartupError(new NettyException(future.cause()), config());

                if (config() != null) {
                    VirtualHostMap.notifyStopped(endpoint(), config().getHost(), config().port(), isHttps());
                    //currentConfig.clearActivePort();
                }
                state().set(ChainState.STOPPED);
            }
            //Register chain for quiesce, NO_OP is passed as the task as there is no special 
            //quiesce action required at this time
            framework.registerEndpointQuiesce(future.channel(), QuiesceStrategy.NO_OP.getTask());
            notifyAll();
        }
    }

    
}