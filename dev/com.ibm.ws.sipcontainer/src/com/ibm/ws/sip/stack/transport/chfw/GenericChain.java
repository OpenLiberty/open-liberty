/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.chfw;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.EndPointMgr;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.channelfw.ChainEventListener;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.RetryableChannelException;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * Encapsulation of steps for starting/stopping an SIP chain in a controlled/predictable
 * manner with a minimum of synchronization.
 */
abstract public class GenericChain implements ChainEventListener {
    
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(GenericChain.class);

	/** "localhost" string  */
	protected static final String LOCALHOST = "localhost";
	
	/** "id" string  */
    protected static final String ID = "id";

    /**   SipChannel prefix name */
    private static String SIP_Channel = "SIPChannel_";
    
    /**   Chain prefix name */
    private static String CHAIN = "Chain";
    
	/** this member is increased for each new chain when it is created. */
    protected static int s_chains = 0;

	/** ENUM which holds the current state for ths particular chain */
    enum ChainState {
        UNINITIALIZED(0, "UNINITIALIZED"),
        DESTROYED(1, "DESTROYED"),
        INITIALIZED(2, "INITIALIZED"),
        STOPPED(3, "STOPPED"),
        QUIESCED(4, "QUIESCED"),
        STARTED(5, "STARTED");

        final int val;
        final String name;

        @Trivial
        /**
         * Ctor
         * @param val
         * @param name
         */
        ChainState(int val, String name) {
            this.val = val;
            this.name = "name";
        }

        @Trivial
        /**
         * Prints the state
         * @param state
         * @return
         */
        public static final String printState(int state) {
            switch (state) {
                case 0:
                    return "UNINITIALIZED";
                case 1:
                    return "DESTROYED";
                case 2:
                    return "INITIALIZED";
                case 3:
                    return "STOPPED";
                case 4:
                    return "QUIESCED";
                case 5:
                    return "STARTED";
            }
            return "UNKNOWN";
        }
    }

    /** Referece to the class which is delayed the stop action  */
    private final StopWait stopWait = new StopWait();
    
    /** Owner of this Chain - Endpoint Impl	 */
    protected final GenericEndpointImpl owner;

    /** Endpoint name	 */
	private String endpointName;
	
	/** SipChannel name	 */
    protected String sipChannelName;
    
    /** Chain name	 */
    private String chainName;
    
    /** Reference to Channel Framework*/
    private ChannelFramework cfw;
    
    /** Endpoint manager */
    protected EndPointMgr endpointMgr;


	/**
     * The state of the chain according to values from {@link ChainState}.
     * Aside from the initial value assignment, new values are only assigned from
     * within {@link ChainEventListener} methods.
     */
    private final AtomicInteger chainState = new AtomicInteger(ChainState.UNINITIALIZED.val);

    /**
     * Toggled by enable/disable methods. This serves only to block activity
     * of some operations (start/update on disabled chain should no-op).
     */
    private volatile boolean enabled = false;

    /**
     * A snapshot of the configuration (collection of properties objects) last used
     * for a start/update operation.
     */
    private volatile ActiveConfiguration currentConfig = null;

    /**
     * Create the new chain with it's parent endpoint
     * 
     * @param sipEndpointImpl the owning endpoint: used for notifications
     * @param isTls true if this is to be an TLS chain.
     */
    public GenericChain(GenericEndpointImpl owner) {
        this.owner = owner;
    }
    
    
    /**
     * Return current configuration
     * @return
     */
	protected ActiveConfiguration getCurrentConfig() {
		return currentConfig;
	}


	/**
	 * Sets the current configuration
	 * @param currentConfig
	 */
	protected void setCurrentConfig(ActiveConfiguration currentConfig) {
		this.currentConfig = currentConfig;
	}


	/**
	 * Returns the Endpoint Name 
	 * @return
	 */
	protected String getEndpointName() {
		return endpointName;
	}


	/**
     * Initialize this chain manager: Channel and chain names shouldn't fluctuate as config changes,
     * so come up with names associated with this set of channels/chains that will be reused regardless
     * of start/stop/enable/disable/modify
     * 
     * @param endpointId The id of the sipEndpoint
     * @param componentId The DS component id
     * @param cfw Channel framework
     */
   public void init(String endpointId, Object componentId, CHFWBundle cfBundle, String name) {

	   String chainNumber = String.valueOf(s_chains++);
	   
        cfw = cfBundle.getFramework();
        endpointMgr = cfBundle.getEndpointManager();

        endpointName = endpointId;
        
        sipChannelName = SIP_Channel + getName() + "_" + endpointId + "_" + chainNumber;
        
        chainName = CHAIN + endpointId + "_" + chainNumber;
    }
   
   /**
    * returns ChainName
    * @return
    */
   public String getChainName() {
		return chainName;
	}


   
/**
    * Returns ChannelFramework
    * @return
    */
   public ChannelFramework getCfw() {
		return cfw;
	}

    /**
     * Enable this chain: this happens automatically for the sip chain,
     * but is delayed on the ssl chain until ssl support becomes available.
     * This does not change the chain's state. The caller should
     * make subsequent calls to perform actions on the chain.
     */
    public void enable() {
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug("enable chain " + this);
        }
        enabled = true;
    }

    /**
     * Disable this chain. This does not change the chain's state. The caller should
     * make subsequent calls to perform actions on the chain.
     */
    public void disable() {
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug("disable chain " + this);
        }
        enabled = false;
    }

    
    /**
     * retuns if this chain is enabled
     * @return
     */
    public boolean isEnabled() {
		return enabled;
	}


	/**
     * Returns the Endpoint owner of this Chain
     * @return
     */
    protected GenericEndpointImpl getOwner() {
		return owner;
	}
    
    /**
     * Stop this chain
     */
    public synchronized void stop() {
        if (c_logger.isEventEnabled()) {
            c_logger.event("stop chain " + this);
        }

        // We don't have to check enabled/disabled here: chains are always allowed to stop.
        if (currentConfig == null || chainState.get() <= ChainState.QUIESCED.val)
            return;

        // Quiesce and then stop the chain. The CFW internally uses a StopTimer for 
        // the quiesce/stop operation-- the listener method will be called when the chain
        // has stopped. So to see what happens next, visit chainStopped
        try {
            ChainData cd = cfw.getChain(chainName);
            if (cd != null) {
                cfw.stopChain(cd, cfw.getDefaultChainQuiesceTimeout());
            }
        } catch (ChannelException e) {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug("Error stopping chain " + chainName, this, e);
            }
        } catch (ChainException e) {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug("Error stopping chain " + chainName, this, e);
            }
        }
    }
    
    
    /**
     * Return channel by name from ChannelFramework
     * @param name
     * @return
     */
    protected ChannelData getChannel(String name){
    	return cfw.getChannel(name);
    }
    
    
    /**
     * Adding channel to the ChannelFramwork
     * @param name
     * @param factoryName
     * @param chanProps
     * @param newConfig
     * @return
     */
    protected ChannelData addChannel(String name, String factoryName, Map<Object, Object> chanProps, ActiveConfiguration newConfig){
    	try {
			return cfw.addChannel(name, cfw.lookupFactory(factoryName), chanProps);
		} catch (ChannelException e) {
			handleStartupError(e, newConfig); // FFDCIgnore: CFW will have logged and FFDCd already
		}
    	return null;
    }
    
    /**
     * Starts Chain
     * @param newConfig
     */
    protected synchronized void startChain(ActiveConfiguration newConfig) {
    	
    	
    	try {
			int retries = getOwner().retries;
			do {
				try {
					 cfw.startChain(chainName);
					// get here if chain started successfully
					 if (c_logger.isTraceDebugEnabled()) {
						 c_logger.traceDebug("startChannels", "chain started [" + chainName + ']');
					}
					retries = 0;
				}
				catch (RetryableChannelException e) {
					// failure, try again
					if (c_logger.isTraceDebugEnabled()) {
						 c_logger.traceDebug(
							"startChannels",
							"RetryableChannelException. Retries left [" + (retries-1) + ']',
							e);
					}
					if (--retries > 0) {
						try {
							Thread.sleep(getOwner().retry_delay);
						}
						catch (InterruptedException interruptedException) {
							if (c_logger.isTraceDebugEnabled()) {
								c_logger.traceDebug("startChannels", "", interruptedException);
							}
						}
					}
				}
			} while (retries > 0);
		}
    	 catch (ChannelException e) {
	        handleStartupError(e, newConfig); // FFDCIgnore: CFW will have logged and FFDCd already
	    } catch (ChainException e) {
	        handleStartupError(e, newConfig); // FFDCIgnore: CFW will have logged and FFDCd already
	    } catch (Exception e) {
	        // The exception stack for this is all internals and does not belong in messages.log.
	    	if (c_logger.isErrorEnabled())
	    		c_logger.error("start.sipChain.error", getName(), e.toString());
	        handleStartupError(e, newConfig);
	    }
    }
    
    /**
     * Stops the Chain
     * @param oldConfig
     */
    protected synchronized void stopChain(String oldConfig) {
    	
    	 // Stop the chain-- will have to be recreated when port is updated
        // notification/follow-on of stop operation is in the chainStopped listener method
        try {
            ChainData cd = cfw.getChain(chainName);
            if (cd != null) {
                cfw.stopChain(cd, cfw.getDefaultChainQuiesceTimeout());
                stopWait.waitForStop(cfw.getDefaultChainQuiesceTimeout()); // BLOCK
                cfw.destroyChain(cd);
                cfw.removeChain(cd);
            }
        } catch (ChannelException e) {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug("Error stopping chain " + chainName, oldConfig, e);
            }
        } catch (ChainException e) {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug("Error stopping chain " + chainName, oldConfig, e);
            }
        }
    }
    

    @FFDCIgnore({ ChannelException.class, ChainException.class })
    protected void removeChannel(String name) {
        // Neither of the thrown exceptions are permanent failures: 
        // they usually indicate that we're the victim of a race.
        // If the CFW is also tearing down the chain at the same time 
        // (for example, the SSL feature was removed), then this could
        // fail.
        try {
            cfw.removeChannel(name);
        } catch (ChannelException e) {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug("Error removing channel " + name, this, e);
            }
        } catch (ChainException e) {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug("Error removing channel " + name, this, e);
            }
        }
    }

    /**
     * 
     * @param e
     * @param cfg
     */
    private void handleStartupError(Exception e, ActiveConfiguration cfg) {
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug("Error starting chain " + chainName, this, e);
        }

        // Post an endpoint failed to start event to anyone listening
        String topic = owner.getEventTopic() + GenericServiceConstants.ENDPOINT_FAILED;
        postEvent(topic, cfg, e);

        // schedule a task to try again later.. 
    }

    /**
     * 
     * @return
     */
    public int getActivePort() {
        ActiveConfiguration cfg = currentConfig;
        if (cfg != null)
            return cfg.getActivePort();
        return -1;
    }

    /**
     * 
     * @return
     */
    public String getActiveHost() {
        ActiveConfiguration cfg = currentConfig;
        if (cfg != null){
            return cfg.activeHost;
        }
        return null;
    }

    /**
   	 * @return name of this chain
   	 */
   	abstract protected String getName();
   
   	/**
     * Setup event propertied - OSGI
     * @param eventProps
     */
    
   	abstract protected void setupEventProps(Map<String, Object> eventProps) ;
   	
   	/**
     * Create active configuration for this chain
     * @param cfg
     */
   	abstract protected ActiveConfiguration createActiveConfiguration();

   	/**
   	 * Create channels for this Chain
   	 * @param newConfig
   	 */
   	abstract protected void createChannels(ActiveConfiguration newConfig);
   	
   	/**
   	 * Rebuild all channels that related to this specific chain
   	 * @param oldConfig
   	 * @param newConfig
   	 */
   	abstract protected void rebuildTheChannel(ActiveConfiguration oldConfig, ActiveConfiguration newConfig) ;

    /**
     * ChainEventListener method.
     * This method can not be synchronized (deadlock with update/stop).
     * Rely on CFW synchronization of chain operations.
     */
    @Override
    public void chainInitialized(ChainData chainData) {
        chainState.set(ChainState.INITIALIZED.val);
    }

    /**
     * Update/start the chain configuration.
     */
    // TODO Liberty - if we are not going to support configuration runtime change - remove unnecessary code. 
    @FFDCIgnore({ ChannelException.class, ChainException.class })
    public synchronized void update() {
        if (c_logger.isEventEnabled()) {
            c_logger.event("update chain " + this);
        }

        // Don't update or start the chain if it is disabled.
        if (!isEnabled() || FrameworkState.isStopping())
            return;

        final ActiveConfiguration oldConfig = getCurrentConfig();

        // The old configuration was "valid" if it existed, and if it was correctly configured
        final boolean validOldConfig = oldConfig == null ? false : oldConfig.validConfiguration;
        final ActiveConfiguration newConfig = createActiveConfiguration();

        if (newConfig.configPort < 0 || !newConfig.isReady()) {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug("Stopping chain due to configuration " + newConfig);
            }

            // save the new/changed configuration before we start setting up the new chain
            setCurrentConfig(newConfig);

            stopChain(oldConfig.toString());
            return;
            
        } 
        
        if (validOldConfig && newConfig.unchanged(oldConfig)) {
            // If the old config was valid & the new one is the same.. 
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug("Configuration is unchanged " + newConfig);
            }
            // If configurations are identical, see if the listening port is also the same
            // which would indicate that the chain is running with the unchanged configuration
            int port = newConfig.getActivePort();
            if (port == oldConfig.getActivePort() && port != -1) {
                if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug("Chain is already started " + oldConfig);
                }
                return;
            }

            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug("Existing config must be started " + newConfig);
            }
        } else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("New/changed chain configuration "
						+ newConfig);
			}
		}

		if (validOldConfig) {
			rebuildTheChannel(oldConfig, newConfig);
		}

		createChannels(newConfig);
		// save the new/changed configuration before we start setting up the
		// new chain
		setCurrentConfig(newConfig);

		if (newConfig.validConfiguration) {
			startChain(newConfig);
		}
    }
    
    
	/**
     * ChainEventListener method.
     * This method can not be synchronized (deadlock with update/stop).
     * Rely on CFW synchronization of chain operations.
     */
    @Override
    public synchronized void chainStarted(ChainData chainData) {
    	
    	if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug("Chain " + toString() + " is started");
        }
    	
        chainState.set(ChainState.STARTED.val);

        final ActiveConfiguration cfg = currentConfig;
        final int port = cfg.getActivePort();

        if (port > 0) {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug("New configuration started " + cfg);
            }

            // Post an endpoint started event to anyone listening
            String topic = owner.getEventTopic() + GenericServiceConstants.ENDPOINT_STARTED;
            postEvent(topic, cfg, null);
        }
    }
    
    /**
     * ChainEventListener method.
     * This method can not be synchronized (deadlock with update/stop).
     * Rely on CFW synchronization of chain operations.
     */
    @Override
    public void chainStopped(ChainData chainData) {
        final ActiveConfiguration cfg = currentConfig;

        // Wake up anything waiting for the chain to stop
        // (see the update method for one example)
        stopWait.notifyStopped();

        // Post an endpoint stopped event to anyone listening
        String topic = owner.getEventTopic() + GenericServiceConstants.ENDPOINT_STOPPED;
        postEvent(topic, cfg, null);
    }

    /**
     * ChainEventListener method.
     * This method can not be synchronized (deadlock with update/stop).
     * Rely on CFW synchronization of chain operations.
     */
    @Override
    public void chainQuiesced(ChainData chainData) {
    	int oldState = chainState.getAndSet(ChainState.QUIESCED.val);

        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug("chainQuiesced, chainData = " + chainData + " oldState = " + oldState);
        }

    }

    /**
     * ChainEventListener method.
     * This method can not be synchronized (deadlock with update/stop).
     * Rely on CFW synchronization of chain operations.
     */
    @Override
    public void chainDestroyed(ChainData chainData) {
        chainState.set(ChainState.DESTROYED.val);
    }

    /**
     * ChainEventListener method.
     * This method can not be synchronized (deadlock with update/stop).
     * Rely on CFW synchronization of chain operations.
     */
    @Override
    public void chainUpdated(ChainData chainData) {
        // Not Applicable: this method is only called when the channels comprising the
        // chain change. We're using fixed chain configurations (in terms of channel
        // elements).
    }

    /**
     * Publish an event relating to a chain starting/stopping with the
     * given properties set about the chain.
     */
   protected void postEvent(String t, ActiveConfiguration c, Exception e) {
        Map<String, Object> eventProps = new HashMap<String, Object>(4);

        if (c.activeHost != null) {
            eventProps.put(GenericServiceConstants.ENDPOINT_ACTIVE_HOST, c.activeHost);
        }

        eventProps.put(GenericServiceConstants.ENDPOINT_ACTIVE_PORT, c.activePort);
        eventProps.put(GenericServiceConstants.ENDPOINT_CONFIG_HOST, c.configHost);
        eventProps.put(GenericServiceConstants.ENDPOINT_CONFIG_PORT, c.configPort);
        
        setupEventProps(eventProps);

        if (e != null) {
            eventProps.put(GenericServiceConstants.ENDPOINT_EXCEPTION, e.toString());
        }

        EventAdmin engine = GenericEndpointImpl.getEventAdmin();
        if (engine != null) {
            Event event = new Event(t, eventProps);
            engine.postEvent(event);
        }
    }


   /**
    * @see java.lang.Object#toString()
    */
   public String toString() {
        return this.getClass().getSimpleName()
               + "[@=" + System.identityHashCode(this)
               + ",enabled=" + enabled
               + ",state=" + ChainState.printState(chainState.get())
               + ",chainName=" + chainName
               + ",config=" + currentConfig + "]";
    }


    /**
     * Adds chain to the ChannelFramework
     * @param chanList
     * @param cd
     * @param newConfig 
     */
    protected void addChain(String[] chanList, ChainData cd, ActiveConfiguration newConfig) {
   	 try {
		cd = getCfw().addChain(getChainName(), FlowType.INBOUND, chanList);
		 cd.setEnabled(enabled);
	        getCfw().addChainEventListener(this, getChainName());

	        // initialize the chain: this will find/create the channels in the chain, 
	        // initialize each channel, and create the chain. If there are issues with any
	        // channel properties, they will surface here
	        // THIS INCLUDES ATTEMPTING TO BIND TO THE PORT
	        getCfw().initChain(chainName);
	        
	     // We configured the chain successfully
	        newConfig.validConfiguration = true;
	        
		} catch (ChannelException e) {
			handleStartupError(e, newConfig); // FFDCIgnore: CFW will have
												// logged and FFDCd already
		} catch (ChainException e) {
			handleStartupError(e, newConfig); // FFDCIgnore: CFW will have
												// logged and FFDCd already
		} catch (Exception e) {
			// The exception stack for this is all internals and does not belong
			// in messages.log.
			if(c_logger.isErrorEnabled())
				c_logger.error("config.sipChain.error", getName(), e.toString());
			handleStartupError(e, newConfig);
		}
	}


    /**
     * Class that is delaying the "stop" action for this chain
     */
    private class StopWait {
    	

    	/**
    	 * waits @param timeout before the actually stop the chain
    	 */
        synchronized void waitForStop(long timeout) {
            // wait for the configured timeout (the parameter) + a smidgen of time
            // to allow the cfw to stop the chain after that configured quiesce 
            // timeout expires
            long interval = timeout + 2345L;
            long waited = 0;

            // If, as far as we know, the chain hasn't been stopped yet, wait for 
            // the stop notification for at most the timeout amount of time.
            while (chainState.get() > ChainState.STOPPED.val && waited < interval) {
                long start = System.nanoTime();
                try {
                    wait(interval - waited);
                } catch (InterruptedException ie) {
                    // ignore
                }
                waited += System.nanoTime() - start;
            }
        }

        synchronized void notifyStopped() {
            notifyAll();
        }
    }
}
