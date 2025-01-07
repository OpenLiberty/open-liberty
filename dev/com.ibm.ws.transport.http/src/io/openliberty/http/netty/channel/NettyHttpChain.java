package io.openliberty.http.netty.channel;

import java.util.concurrent.atomic.AtomicReference;

import com.ibm.ws.http.internal.HttpChain.ChainState;
import com.ibm.ws.http.internal.HttpEndpointImpl;
import com.ibm.ws.http.netty.NettyChain;

import io.netty.channel.Channel;
import io.openliberty.http.channel.Chain;
import io.openliberty.http.channel.ChainConfiguration;
import io.openliberty.netty.internal.NettyFramework;

/**
 * A chain that uses Netty rather than the older ChannelFramework.
 * Implements the {@link Chain} interface so it can be substituted
 * without changing {@link HttpEndpointImpl}.
 */
public class NettyHttpChain implements Chain {

    private static final TraceComponent tc = Tr.register(NettyChain.class);

    private final Object owner; // or HttpEndpointImpl if needed
    private final boolean https;
    private final AtomicReference<ChainState> state = new AtomicReference<>(ChainState.UNINITIALIZED);
    private volatile boolean enabled = false;

    private NettyFramework nettyFramework;
    private Channel serverChannel;

    /** The last known configuration for this chain. */
    private volatile ChainConfiguration currentConfig;


    @Override
    public boolean isHttps() {
        return https;
    }

    @Override
    public void enable() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Enabling Netty chain: " + this);
        }
        enabled = true;
    }

    @Override
    public void disable() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "disable chain " + this);
        }
        enabled = false;

    }

    @Override
    public void update(ChainConfiguration newConfig) {
        if (!enabled) {
            if (tc.isDebugEnabled()) {
                tc.debug("Chain is disabled; skipping update.");
            }
            return;
        }
        if (newConfig == null || !newConfig.isComplete()) {
            if (tc.isDebugEnabled()) {
                tc.debug("newConfig is invalid; stopping chain.");
            }
            stop();
            currentConfig = newConfig;
            return;
        }
        if (currentConfig != null && !newConfig.requiresRestart(currentConfig)) {
            if (tc.isDebugEnabled()) {
                tc.debug("No changes requiring a Netty rebind; skipping full restart.");
            }
            currentConfig = newConfig;
            return;
        }
        if (tc.isDebugEnabled()) {
            tc.debug("Netty config changed; stopping and restarting chain.");
        }
        stop();
        currentConfig = newConfig;
        state.set(ChainState.STARTING);

        try {
            if (nettyFramework != null) {
                // Example: serverChannel = nettyFramework.start(..., currentConfig.getHost(), currentConfig.getPort(), ...);
                state.set(ChainState.STARTED);
            } else {
                tc.debug("NettyFramework is null; cannot start channel.");
                state.set(ChainState.STOPPED);
            }
        } catch (Exception e) {
            state.set(ChainState.STOPPED);
        }
    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'stop'");
    }

    @Override
    public int getChainState() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getChainState'");
    }
    
}
