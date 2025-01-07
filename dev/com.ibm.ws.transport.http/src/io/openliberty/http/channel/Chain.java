package io.openliberty.http.channel;

/**
 * Represents a common interface for managing HTTP or HTTPS chain lifecycles
 * in a predictable and controlled manner.
 */
public interface Chain {

    /**
     * Indicates whether this chain manages HTTPS.
     */
    boolean isHttps();

    /**
     * Moves the chain from a "disabled" state into an "enabled" state.
     */
    void enable();

    /**
     * Moves the chain from an "enabled" state into a "disabled" state.
     */
    void disable();

    /**
     * Applies new configuration to the chain. If the chain is enabled
     * and the new configuration differs in a way that requires a rebind
     * (port, host, etc.), it should stop and then attempt to start with
     * the new configuration.
     *
     * @param newConfig the new chain configuration
     */
    void update(ChainConfiguration newConfig);

    /**
     * Fully stops the chain, releasing resources. Does not necessarily
     * disable it; the chain can be updated later if still enabled.
     */
    void stop();

    /**
     * Retrieves the current state of the chain as an integer.
     *
     * @return integer representing the chain’s state
     */
    int getChainState();

    /**
     * Returns the active port the chain is bound to, or -1 if not bound.
     */
    default int getActivePort() {
        return -1;
    }

    /**
     * Returns the currently stored chain config if available, or null
     * if uninitialized.
     */
    default ChainConfiguration getCurrentConfig() {
        return null;
    }
}
