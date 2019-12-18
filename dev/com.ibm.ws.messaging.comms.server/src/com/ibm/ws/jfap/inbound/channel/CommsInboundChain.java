/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jfap.inbound.channel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.EndPointMgr;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.event.Event;
import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.ChainEventListener;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * JFAP inbound chain class definition
 * As of now only two instances are possible: InboundBasic and InboundSecure
 */
public class CommsInboundChain implements ChainEventListener {
    private static final TraceComponent tc = Tr.register(CommsInboundChain.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

    private boolean _isSecureChain = false;
    private boolean _isEnabled = false;
    private String _chainName;

    private final CommsServerServiceFacade _commsServerFacade;

    private String _endpointName;
    private ChannelFramework _cfw;
    private EndPointMgr _endpointMgr;

    //channel names
    private String _tcpName;
    private String _sslName;
    private String _jfapName;

    private volatile boolean _isChainStarted = false;

    private ChainConfiguration _currentConfig;

    // will be used to wait for completion of actual stopChain
    private final StopWait stopWait = new StopWait();

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
        ChainState(int val, String name) {
            this.val = val;
            this.name = "name";
        }

        @Trivial
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

    /**
     * The state of the chain according to values from {@link ChainState}.
     * Aside from the initial value assignment, new values are only assigned from
     * within {@link ChainEventListener} methods.
     */
    private final AtomicInteger chainState = new AtomicInteger(ChainState.UNINITIALIZED.val);

    CommsInboundChain(CommsServerServiceFacade commsServer, boolean isSecureChain) {
        _commsServerFacade = commsServer;
        _isSecureChain = isSecureChain;

    }

    public void init(String endpointName, CHFWBundle cfBundle) {
        _cfw = cfBundle.getFramework();
        _endpointMgr = cfBundle.getEndpointManager();
        _endpointName = endpointName;

        if (_isSecureChain) {
            _chainName = "InboundSecureMessaging";
            _tcpName = _endpointName;
            _sslName = "SSL-" + _endpointName;
            _jfapName = "JFAP-" + _endpointName;
        } else {
            _chainName = "InboundBasicMessaging";
            _tcpName = _endpointName;
            _jfapName = "JFAP-" + _endpointName;
        }
    }

    public void enable(boolean enbaled) {
        _isEnabled = enbaled;
    }

    /**
     * stop will get called only from de-activate.
     */
    public void stop() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "stop");

        //stopchain() first quiesce's(invokes chainQuiesced) depending on the chainQuiesceTimeOut
        //Once the chain is quiesced StopChainTask is initiated.Hence we block until the actual stopChain is invoked
        try {
            ChainData cd = _cfw.getChain(_chainName);
            if (cd != null) {
                _cfw.stopChain(cd, _cfw.getDefaultChainQuiesceTimeout());
                stopWait.waitForStop(_cfw.getDefaultChainQuiesceTimeout()); //BLOCK till stopChain actually completes from StopChainTask
                _cfw.destroyChain(cd);
                _cfw.removeChain(cd);
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Failed in successfully cleaning(i.e stopping/destorying/removing) chain: ", e);
        } finally {
            _isChainStarted = false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "stop");
    }

    /**
     * 
     */
    public void update() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "update");

        if (!_isEnabled || FrameworkState.isStopping()) //dont do any thing.. just return
            return;

        final ChainConfiguration oldConfig = _currentConfig;

        // The old configuration was "valid" if it existed, and if it was correctly configured
        final boolean validOldConfig = oldConfig == null ? false : oldConfig.isValidConfig;

        Map<String, Object> tcpOptions = _commsServerFacade.getTcpOptions();
        Map<String, Object> sslOptions = (_isSecureChain) ? _commsServerFacade.getSslOptions() : null;

        final ChainConfiguration newConfig = new ChainConfiguration(
                        (_isSecureChain) ? _commsServerFacade.getConfigured_wasJmsSSLPort() : _commsServerFacade.getConfigured_wasJmsPort(),
                        _commsServerFacade.getConfigured_Host(),
                        tcpOptions,
                        sslOptions);

        if ((newConfig.configPort < 0) || !newConfig.complete()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(this, tc, "Stopping chain due to configuration " + newConfig);
            }

            // save the new/changed configuration before we start setting up the new chain
            _currentConfig = newConfig;

            //stop the chain
            stop();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "update");
            return;
        }

        //newConfig is valid one.. then compare to old one and take actions.
        if (validOldConfig && newConfig.unchanged(oldConfig)) {
            //new and old config are identical... then check whether listening ports also same .. 
            //in that case chain is already started with the same configuration.. so just exit
            if (newConfig.getActivePort() == oldConfig.getActivePort() && newConfig.getActivePort() != -1) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    SibTr.debug(this, tc, "Chain is already started " + oldConfig);
                }
                //exiting here as nothing to be done.. no need to save config as both old and new are identical
                return;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(this, tc, "Existing config must be started " + newConfig);
            }
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            SibTr.debug(this, tc, "New/changed chain configuration " + newConfig);
        }

        try {
            if (validOldConfig) {
                //we have good old config means.. chain was started successfully..
                //first stop the chain.. it should be harmless if chain is already stopped
                stop();
            }

            // Remove any channels that have to be rebuilt.. 
            if (newConfig.tcpChanged(oldConfig))
                removeChannel(_tcpName);

            if (newConfig.sslChanged(oldConfig))
                removeChannel(_sslName);

            //as of now, JFAP options are not exposed.. so no need to touch it.

            // save the new/changed configuration before we start setting up the new chain
            _currentConfig = newConfig;

            // define is a simple replace of the old value known to the endpointMgr
            //by defining with Endpoint Manager, Jfap endpoint can be queried from Mbeans
            _endpointMgr.defineEndPoint(_endpointName, newConfig.configHost, newConfig.configPort);

            Map<Object, Object> chanProps;

            // TCP Channel
            ChannelData tcpChannel = _cfw.getChannel(_tcpName);
            if (tcpChannel == null) {
                String typeName = (String) tcpOptions.get("type");
                chanProps = new HashMap<Object, Object>(tcpOptions);
                chanProps.put("endPointName", _endpointName);
                chanProps.put("hostname", newConfig.configHost);
                chanProps.put("port", String.valueOf(newConfig.configPort));

                tcpChannel = _cfw.addChannel(_tcpName, _cfw.lookupFactory(typeName), chanProps);
            }

            // SSL Channel
            if (_isSecureChain) {
                ChannelData sslChannel = _cfw.getChannel(_sslName);
                if (sslChannel == null) {
                    sslChannel = _cfw.addChannel(_sslName, _cfw.lookupFactory("SSLChannel"), new HashMap<Object, Object>(sslOptions));
                }
            }

            ChannelData jfapChannel = _cfw.getChannel(_jfapName);
            if (jfapChannel == null)
                jfapChannel = _cfw.addChannel(_jfapName, _cfw.lookupFactory("JFAPChannel"), null);

            final String[] chanList;
            if (_isSecureChain)
                chanList = new String[] { _tcpName, _sslName, _jfapName };
            else
                chanList = new String[] { _tcpName, _jfapName };

            ChainData cd = _cfw.addChain(_chainName, FlowType.INBOUND, chanList);
            cd.setEnabled(true);
            //add the chainevenlistener to the cfw for chain notification
            _cfw.addChainEventListener(this, _chainName);

            _cfw.startChain(cd);

            // get listening chains (non-null if chains started successfully)
            int jmsActivePort = -1;

            try {
                jmsActivePort = _cfw.getListeningPort(_chainName);
            } catch (ChainException e) {
                _isChainStarted = false;
                jmsActivePort = -1;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "JFAP chain InboundBasicMessaging failed in obtaining Listening port from ChannelFrameWork", e);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "update");
                return;
            }

            //Chain successfully started and bound to port. Channel Framework logs to System.out. So just add to trace
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "JFAP chain InboundBasicMessaging successfully started and bound to port: ", jmsActivePort);

            _isChainStarted = true;
            newConfig.isValidConfig = true;

        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(this, tc, "Problem in starting the chain  " + newConfig);
            }
        }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "update");
    }

    /**
     * Removes channel from cfw.
     */
    private void removeChannel(String name) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeChannel", name);
        // Neither of the thrown exceptions are permanent failures: 
        // they usually indicate that we're the victim of a race.
        // If the CFW is also tearing down the chain at the same time 
        // (for example, the SSL feature was removed), then this could
        // fail.
        try {
            _cfw.removeChannel(name);
        } catch (ChannelException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(this, tc, "Error removing channel " + name, e);
            }
        } catch (ChainException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(this, tc, "Error removing channel " + name, e);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removeChannel");
    }

    private final class ChainConfiguration {
        final int configPort;
        final String configHost;

        final Map<String, Object> tcpOptions;
        final Map<String, Object> sslOptions;

        volatile int activePort = -1;

        boolean isValidConfig = false;

        ChainConfiguration(int port, String host,
                           Map<String, Object> tcp,
                           Map<String, Object> ssl) {

            tcpOptions = tcp;
            sslOptions = ssl;
            configPort = port;
            configHost = host;
        }

        /**
         * @return returns listening port for the chain. if chain has some problem, returns -1
         */
        public int getActivePort() {
            if (configPort < 0)
                return -1;
            if (activePort == -1) {
                try {
                    activePort = _cfw.getListeningPort(_chainName);
                } catch (ChainException ce) {
                    activePort = -1;
                }
            }
            return activePort;
        }

        /**
         * @return true if the ActiveConfiguration contains the required
         *         configuration to start the jfap chain. Basic chain needs only tcpOptions,
         *         secure chain needs both tcpOptions and sslOptions
         */
        @Trivial
        public boolean complete() {
            if (tcpOptions == null)
                return false;

            if (_isSecureChain && sslOptions == null)
                return false;

            return true;
        }

        /**
         * @return true if config is unchanged (host,port,tcpOtions and sslOptions are unchanged) </br>
         *         otherwise returns false
         */
        protected boolean unchanged(ChainConfiguration other) {
            if (other == null)
                return false;

            // Only look at ssl options if this is an secure chain
            if (_isSecureChain) {
                return configHost.equals(other.configHost) &&
                       configPort == other.configPort &&
                       tcpOptions == other.tcpOptions &&
                       sslOptions == other.sslOptions;
            } else {
                return configHost.equals(other.configHost) &&
                       configPort == other.configPort &&
                       tcpOptions == other.tcpOptions;
            }
        }

        protected boolean tcpChanged(ChainConfiguration other) {
            if (other == null)
                return false;

            return !configHost.equals(other.configHost) ||
                   configPort != other.configPort ||
                   tcpOptions != other.tcpOptions;
        }

        protected boolean sslChanged(ChainConfiguration other) {
            if (other == null)
                return false;

            return sslOptions != other.sslOptions;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()
                   + "[host=" + configHost
                   + ",port=" + configPort
                   + ",listening=" + activePort
                   + ",complete=" + complete()
                   + "]";
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.channelfw.ChainEventListener#chainInitialized(com.ibm.websphere.channelfw.ChainData)
     */
    @Override
    public void chainInitialized(ChainData chainData) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "chainInitialized", chainData);

        chainState.set(ChainState.INITIALIZED.val);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "chainInitialized");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.channelfw.ChainEventListener#chainStarted(com.ibm.websphere.channelfw.ChainData)
     */
    @Override
    public void chainStarted(ChainData chainData) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "chainStarted", chainData);

        chainState.set(ChainState.STARTED.val);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "chainStarted");

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.channelfw.ChainEventListener#chainStopped(com.ibm.websphere.channelfw.ChainData)
     */
    @Override
    public void chainStopped(ChainData chainData) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "chainStopped", chainData);

        chainState.set(ChainState.STOPPED.val);

        // Wake up anything waiting for the chain to stop
        // (see the update method for one example)
        stopWait.notifyStopped();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "chainStopped");
    }

    /*
     * Before the chain is stopped/destroyed we send notification to clients so that clients
     * can close connections gracefully
     * 
     * This method can not be synchronized (deadlock with update/stop).
     * Rely on CFW synchronization of chain operations.
     * 
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.channelfw.ChainEventListener#chainQuiesced(com.ibm.websphere.channelfw.ChainData)
     */
    @Override
    public void chainQuiesced(ChainData chainData) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "chainQuiesced", chainData);

        chainState.set(ChainState.QUIESCED.val);

        //First stop any MP connections which are established through COMMS 
        //stopping connections is Non-blocking
        try {
            if (this._isSecureChain)
                _commsServerFacade.closeViaCommsMPConnections(JsConstants.ME_STOP_COMMS_SSL_CONNECTIONS);
            else
                _commsServerFacade.closeViaCommsMPConnections(JsConstants.ME_STOP_COMMS_CONNECTIONS);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Failed in stopping MP connections which are establised through COMMS: ", e);
        }

        // no current connections, notify the final stop can happen now
        signalNoConnections();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "chainQuiesced");
    }

    /**
     * Send an event to the channel framework that there are no more active
     * connections on this quiesced channel instance. This will allow an early
     * final chain stop instead of waiting the full quiesce timeout length.
     */
    private void signalNoConnections() {
        EventEngine events = _commsServerFacade.getEventEngine();
        if (null == events) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                SibTr.event(tc, "Unable to send event, missing service");
            }
            return;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            SibTr.event(tc, "No active connections, sending stop chain event");
        }
        Event event = events.createEvent(ChannelFramework.EVENT_STOPCHAIN);
        event.setProperty(ChannelFramework.EVENT_CHANNELNAME, _cfw.getChannel(_jfapName).getExternalName());
        events.postEvent(event);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.channelfw.ChainEventListener#chainDestroyed(com.ibm.websphere.channelfw.ChainData)
     */
    @Override
    public void chainDestroyed(ChainData chainData) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "chainDestroyed", chainData);

        chainState.getAndSet(ChainState.DESTROYED.val);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "chainDestroyed");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.channelfw.ChainEventListener#chainUpdated(com.ibm.websphere.channelfw.ChainData)
     */
    @Override
    public void chainUpdated(ChainData chainData) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "chainUpdated", chainData);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "chainUpdated");

    }

    private class StopWait {

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
