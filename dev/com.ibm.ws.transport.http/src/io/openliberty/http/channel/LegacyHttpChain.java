package io.openliberty.http.channel;

import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.http.internal.HttpEndpointImpl;
import com.ibm.wsspi.channelfw.ChainEventListener;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;

/**
 * A legacy chain implementation that uses the older ChannelFramework APIs.
 * It implements {@link Chain} so it can be substituted for newer chain 
 * implementations without changing {@link HttpEndpointImpl}.
 */
public class LegacyHttpChain implements Chain, ChainEventListener{

    /**
     * Enumerates chain states used internally by the legacy system.
     * For external usage, we return int values from {@link #getChainState()}.
     */
    public static final int UNINITIALIZED = 0;
    public static final int DESTROYED = 1;
    public static final int INITIALIZED = 2;
    public static final int STOPPED = 3;
    public static final int QUIESCED = 4;
    public static final int STARTED = 5;
    public static final int STARTING = 6;
    public static final int STOPPING = 7;

    private final HttpEndpointImpl owner;
    private final boolean https;
    private final AtomicInteger chainState = new AtomicInteger(UNINITIALIZED);
    private volatile boolean enabled = false;

    /** The name of the chain in ChannelFramework. */
    private String chainName;

    /** Channel framework references needed by the legacy approach. */
    private ChannelFramework cfw;
    private EndPointMgr endpointMgr;

    /** The last known configuration for this chain. */
    private volatile ChainConfiguration currentConfig;

    /**
     * Constructs the legacy chain tied to a specific endpoint.
     * 
     * @param owner The endpoint that owns this chain.
     * @param isHttps Whether this chain is for HTTPS rather than HTTP.
     */
    public LegacyHttpChain(HttpEndpointImpl owner, boolean isHttps) {
        this.owner = owner;
        this.https = isHttps;
    }

    /**
     * Initializes references to ChannelFramework. Typically called
     * by {@link HttpEndpointImpl} once.
     * 
     * @param endpointId The ID used for naming the chain.
     * @param cfBundle The CHFWBundle that provides ChannelFramework references.
     */
    public void init(String endpointId, CHFWBundle cfBundle) {
        this.cfw = cfBundle.getFramework();
        this.endpointMgr = cfBundle.getEndpointManager();
        String suffix = (https ? "-ssl" : "");
        chainName = "CHAIN-" + endpointId + suffix;
        chainState.set(UNINITIALIZED);
    }

    @Override
    @FFDCIgnore({ ChannelException.class, ChainException.class })
    public synchronized void update(ChainConfiguration newConfig) {
        if (!enabled) {
            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Chain is disabled; skipping update.");
            }
            return;
        }
        if (newConfig == null || !newConfig.isComplete()) {
            // If unconfigurable or incomplete, we stop.
            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "newConfig is invalid; stopping chain.");
            }
            stop();
            currentConfig = newConfig;
            return;
        }
        if (currentConfig != null && !newConfig.requiresRestart(currentConfig)) {
            // If no meaningful changes that require rebind, skip.
            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "No changes requiring restart; ignoring update.");
            }
            // Possibly do partial updates if needed. But we won't rebind or re-init.
            currentConfig = newConfig;
            return;
        }
        // Something changed significantly -> we stop and restart.
        if (tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Configuration changed; stopping and restarting chain.");
        }
        stop();
        currentConfig = newConfig;

        chainState.set(STARTING);
        try {
            // Example init logic:
            if (cfw != null) {
                // Possibly remove a leftover chain from cfw, etc.
                // Rebuild channels using newConfig’s fields...
                // cfw.addChain(...), cfw.initChain(...), cfw.startChain(...)
                chainState.set(STARTED);
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "ChannelFramework is null, cannot start chain.");
                }
                chainState.set(STOPPED);
            }
        } catch (ChannelException | ChainException e) {
            // Handle startup error, possibly call onError() or log
            chainState.set(STOPPED);
        }
    }

}
