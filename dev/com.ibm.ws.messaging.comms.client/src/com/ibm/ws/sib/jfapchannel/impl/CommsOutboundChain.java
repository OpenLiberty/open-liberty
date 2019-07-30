/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.sib.comms.CommsClientServiceFacadeInterface;
import com.ibm.ws.sib.jfapchannel.ClientConnectionManager;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.impl.octracker.OutboundConnectionTracker;
import com.ibm.ws.sib.jfapchannel.richclient.impl.JFapChannelFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.ChannelConfiguration;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;

public class CommsOutboundChain {
    private static final TraceComponent tc = Tr.register(CommsOutboundChain.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);
    private static final TraceNLS nls = TraceNLS.getTraceNLS(JFapChannelConstants.MSG_BUNDLE);
    private final static String _OutboundChain_ConfigAlias = "wasJmsOutbound";

    private String _chainName = null;
    private String _tcpChannelName = null;
    private String _jfapChannelName = null;
    private String _sslChannelName = null;

    /** use _tcpOptions service direct instead of reference as _tcpOptions is a required service */
    private ChannelConfiguration _tcpOptions = null;

    /** Optional,dynamic reference to sslOptions */
    private final AtomicServiceReference<ChannelConfiguration> _sslOptions = new AtomicServiceReference<ChannelConfiguration>("sslOptions");

    /** Optional, dynamic reference to an SSL channel factory provider: could be used to start/stop SSL chains */
    private final AtomicServiceReference<ChannelFactoryProvider> _sslFactoryProvider = new AtomicServiceReference<ChannelFactoryProvider>("sslSupport");

    /** use _commsClientService service direct instead of reference as _commsClientService is a required service */
    private CommsClientServiceFacadeInterface _commsClientService = null;

    private volatile boolean _isChainStarted = false;
    private volatile boolean _isSSLEnabled = false;
    private volatile boolean _isSSLChain = false;
    //flag to check if activate() is invoked
    private volatile boolean _isActivated = false;

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param properties : Map containing service & config properties
     *            populated/provided by config admin
     */
    protected void activate(Map<String, Object> properties, ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "activate");

        _sslOptions.activate(context);
        _sslFactoryProvider.activate(context);

        _isSSLChain = MetatypeUtils.parseBoolean(_OutboundChain_ConfigAlias, "useSSL", properties.get("useSSL"), false);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Chain is configured for  " + (_isSSLChain ? "Secure " : "Non-Secure"));

        String Outboundname = (String) properties.get("id");

        _chainName = Outboundname;
        _tcpChannelName = Outboundname + "_JfapTcp";
        _sslChannelName = Outboundname + "_JfapSsl";
        _jfapChannelName = Outboundname + "_JfapJfap";

        //if chain is started first destroy then create it freshly
        if (_isChainStarted)
            performAction(destroyChainAction);
        performAction(createChainAction);

        _isActivated = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "activate");
    }

    @Trivial
    protected void setCommsClientService(CommsClientServiceFacadeInterface service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "setCommsClientService", service);
        }
        _commsClientService = service;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.exit(this, tc, "setCommsClientService");
        }
    }

    @Trivial
    protected void unsetCommsClientService(CommsClientServiceFacadeInterface service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "unsetCommsClientService", service);
        }
        _commsClientService = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.exit(this, tc, "unsetCommsClientService");
        }
    }

    @Trivial
    protected void setTcpOptions(ChannelConfiguration service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "setTcpOptions", service);
        }

        _tcpOptions = service;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setTcpOptions", _tcpOptions);
        }

    }

    @Trivial
    protected void unsetTcpOptions(ServiceReference<ChannelConfiguration> service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "unsetTcpOptions", service);
        }
        _tcpOptions = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "unsetTcpOptions");
        }
    }

    private Map<String, Object> getTcpOptions() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "getTcpOptions");
        }
        Map<String, Object> tcpOptions = null;
        if (_tcpOptions == null) {// which should never happen, because its a required service
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "getTcpOptions() returning NULL as __tcpOptions.getService() returned NUll _tcpOptions: ", _tcpOptions);

        } else
            tcpOptions = _tcpOptions.getConfiguration();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getTcpOptions", tcpOptions);
        }
        return tcpOptions;
    }

    /**
     * Set/store the bound ConfigurationAdmin service.
     * Also ensure that a default endpoint configuration exists.
     * 
     * SEE bnd file: "type=SSLChannel" means this will only
     * match SSL channel factory providers
     * 
     * @param ref
     */
    @Trivial
    protected void setSslSupport(ServiceReference<ChannelFactoryProvider> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "setSslSupport", new Object[] { ref.getProperty("type"), ref });
        }

        // If sslsupport is bound then set it to true 
        _isSSLEnabled = true;

        // bind can get called before unbind for new config/ modify.Hence the precaution
        if (_sslFactoryProvider.getReference() != null) { // existing chain is still there so destroy 
            if (_isChainStarted)
                performAction(destroyChainAction);
        }

        _sslFactoryProvider.setReference(ref);
        // we will only invoke createchain if 
        // 1) activate() is complete
        // this is because bind method get called first before activate and without activate we don't have any properties
        // 2) if useSSL is set in config 
        if (_isActivated && _isSSLChain) {
            //TODO: when executor service is used for performaction() make sure about sequence of destroy and create chain
            //Now currently its not threaded
            if (_isChainStarted)
                performAction(destroyChainAction);
            performAction(createChainAction);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setSslSupport", _isSSLEnabled);
        }
    }

    /**
     * This is a required reference-- set (with a new ChannelFactoryProvider) would be
     * called before the unset, unless the component has been deactivated.
     * 
     * @param ref ConfigurationAdmin instance to unset
     */
    @Trivial
    public void unsetSslSupport(ServiceReference<ChannelFactoryProvider> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "unsetSslSupport", new Object[] { ref.getProperty("type"), ref });
        }
        // see if its for the same service ref, if yes then destroy
        if (_sslFactoryProvider.getReference() == ref)
        {
            if (_isSSLChain) {
                if (_isChainStarted)
                    performAction(destroyChainAction);
            }

        }
        if (_sslFactoryProvider.unsetReference(ref)) {
            _isSSLEnabled = false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "unsetSslSupport", _isSSLEnabled);
        }

    }

    //SslOption related functions
    @Trivial
    protected void setSslOptions(ServiceReference<ChannelConfiguration> service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "setSslOptions", service);
        }

        // bind can get called before unbind for new config/ modify.Hence the precaution
        if (_sslOptions.getReference() != null) { // existing chain is still there so destroy 
            if (_isChainStarted)
                performAction(destroyChainAction);
        }

        _sslOptions.setReference(service);
        // we will only invoke createchain if 
        // 1) activate() is complete
        // this is because bind method get called first before activate and without activate we don't have any properties
        // 2) if useSSL is set in config 
        if (_isActivated && _isSSLChain) {
            //TODO: when executor service is used for performaction() make sure about sequence of destroy and create chain
            //Now currently its not threaded
            if (_isChainStarted)
                performAction(destroyChainAction);
            performAction(createChainAction);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setSslOptions");
        }

    }

    @Trivial
    protected void unsetSslOptions(ServiceReference<ChannelConfiguration> unbindServiceRef) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "unsetSslOptions", unbindServiceRef);
        }

        // see if its for the same service ref, if yes then destroy
        if (_sslOptions.getReference() == unbindServiceRef)
        {
            if (_isSSLChain) {
                if (_isChainStarted)
                    performAction(destroyChainAction);
            }

        }
        _sslOptions.unsetReference(unbindServiceRef);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "unsetSslOptions");
        }
    }

    private Map<String, Object> getSslOptions() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "getSslOptions");
        }

        ChannelConfiguration chanCnfgService = _sslOptions.getService();
        Map<String, Object> sslOptions = null;

        if (null != chanCnfgService) {
            sslOptions = chanCnfgService.getConfiguration();
        }
        else {// sslOptions is not bound yet
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "getSslOptions() returning NULL as _sslOptions.getService() returned NUll _sslOptions: ", _sslOptions);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.exit(tc, "getSslOptions", sslOptions);
        }

        return sslOptions;
    }

    private void createJFAPChain() throws ChannelException, ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createJFAPChain", _isChainStarted);

        if (!_isChainStarted) {

            try {

                ChannelFramework cfw = CommsClientServiceFacade.getChannelFramewrok();
                cfw.registerFactory("JFapChannelOutbound", JFapChannelFactory.class);

                Map<String, Object> tcpOptions = getTcpOptions();

                ChannelData tcpChannel = cfw.getChannel(_tcpChannelName);

                if (tcpChannel == null) {
                    String typeName = (String) tcpOptions.get("type");
                    tcpChannel = cfw.addChannel(_tcpChannelName, cfw.lookupFactory(typeName), new HashMap<Object, Object>(tcpOptions));
                }

                // SSL Channel
                if (_isSSLChain) {
                    Map<String, Object> sslOptions = getSslOptions();

                    // Now we are actually trying to create a ssl channel to the chain.Which requires sslOptions and _isSSLEnabled must be enabled
                    //without which there is no point in continuing
                    if (sslOptions == null) { // sslOptions service is not bound yet,so not point continuing
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "_sslOptions service is not bound which is required for secure chain,so no point continuing");
                        throw new ChainException(new Throwable(nls.getFormattedMessage("missingSslOptions.ChainNotStarted", new Object[] { _chainName }, null)));
                        //no problem if we exit from here.. even though TcpChannel is added to cfw.
                    }

                    if (!_isSSLEnabled) { //_sslFactoryProvider service is not bound yet,so not point continuing
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "_sslFactoryProvider service is not bound which is required for secure chain,so no point continuing");
                        throw new ChainException(new Throwable(nls.getFormattedMessage("missingSslOptions.ChainNotStarted", new Object[] { _chainName }, null)));
                        //no problem if we exit from here.. even though TcpChannel is added to cfw.
                    }

                    ChannelData sslChannel = cfw.getChannel(_sslChannelName);
                    if (sslChannel == null) {
                        sslChannel = cfw.addChannel(_sslChannelName, cfw.lookupFactory("SSLChannel"), new HashMap<Object, Object>(sslOptions));
                    }
                }

                ChannelData jfapChannel = cfw.getChannel(_jfapChannelName);
                if (jfapChannel == null)
                    jfapChannel = cfw.addChannel(_jfapChannelName, cfw.lookupFactory("JFapChannelOutbound"), null);

                final String[] chanList;
                if (_isSSLChain)
                    chanList = new String[] { _jfapChannelName, _sslChannelName, _tcpChannelName };
                else
                    chanList = new String[] { _jfapChannelName, _tcpChannelName };

                ChainData cd = cfw.addChain(_chainName, FlowType.OUTBOUND, chanList);
                cd.setEnabled(true);

                //if we are here then chain is started
                _isChainStarted = true;
            } catch (ChannelException e) {
                _isChainStarted = false;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "JFAP Outbound chain " + _chainName + " failed to get started");
                throw e;
            } catch (ChainException e) {
                _isChainStarted = false;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "JFAP Outbound chain " + _chainName + " failed to get started");
                throw e;
            } finally {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "createJFAPChain ", _isChainStarted);
            }

            if (_isSSLChain) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "JFAP Outbound secure chain" + _chainName + " successfully started ");
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "JFAP Outbound chain" + _chainName + " successfully started ");
            }
        }

    }

    /**
     * Removes channel from cfw.
     */
    private void removeChannel(String channelName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeChannel", channelName);
        // Neither of the thrown exceptions are permanent failures: 
        // they usually indicate that we're the victim of a race.
        // If the CFW is also tearing down the chain at the same time 
        // (for example, the SSL feature was removed), then this could
        // fail.
        ChannelFramework cfw = CommsClientServiceFacade.getChannelFramewrok();

        try {
            if (cfw.getChannel(channelName) != null)
                cfw.removeChannel(channelName);
        } catch (ChannelException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(this, tc, "Error removing channel " + channelName, e);
            }
        } catch (ChainException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(this, tc, "Error removing channel " + channelName, e);
            }
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "removeChannel");
        }
    }

    /**
     * _chainStartLock is to protect the 'starting' of chain my multiple connection factories </br>
     */
    private final Object _chainActionLock = new Object();

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param reason int representation of reason the component is stopping
     */
    protected void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "CommsClientServiceFacade deactivated, reason=");

        performAction(destroyChainAction);
        _sslOptions.deactivate(context);
        _sslFactoryProvider.deactivate(context);
        _isActivated = false;
    }

    private void destroyJFAPChain() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "destroyJFAPChain");

        synchronized (_chainActionLock) {
            try {
                terminateConnectionsAssociatedWithChain();
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Failure in terminating conservations and physical connections while destroying chain : " + _chainName, e);
            } finally {
                _isChainStarted = false;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "destroyJFAPChain", _isChainStarted);
    }

    private void performAction(Runnable action) {
        action.run();
    }

    private final Runnable destroyChainAction = new Runnable() {
        @Override
        @Trivial
        public void run() {
            synchronized (_chainActionLock) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "CommsOutboundChain: Destorying " + (_isSSLChain ? "Secure" : "Non-Secure") + " chain ", _chainName);

                //Catch any unchecked/uncaught exceptions so that it would not harm the code flow
                try {
                    destroyJFAPChain();
                } catch (Exception e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Exception in destorying chain", e);
                }
            }
        }
    };

    private final Runnable createChainAction = new Runnable() {
        @Override
        @Trivial
        public void run() {
            synchronized (_chainActionLock) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "CommsOutboundChain: Creating " + (_isSSLChain ? "Secure" : "Non-Secure") + " chain ", _chainName);

                //Catch any unchecked/uncaught exceptions so that it would not harm the code flow
                try {
                    createJFAPChain();
                } catch (Exception e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Exception in creating chain", e);
                }
            }
        }
    };

    /**
     * Terminate all the connections associated with the chain
     * 
     * @throws Exception
     */
    private void terminateConnectionsAssociatedWithChain() throws Exception {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "terminateConnectionsAssociatedWithChain");
        }

        ChannelFramework cfw = CommsClientServiceFacade.getChannelFramewrok();
        OutboundConnectionTracker oct = ClientConnectionManager.getRef().getOutboundConnectionTracker();
        if (oct != null) {
            oct.terminateConnectionsAssociatedWithChain(_chainName);
        } else {// if we dont have any oct that means there were no connections established

            if (cfw.getChain(_chainName) != null) {// see if chain exist only then destroy
                // we have to destroy using vcf because cfw does not allow to destroy outbound 
                // chains directly using cfw.destroyChain(chainname)
                // as it is valid only for inbound chains as of now
                cfw.getOutboundVCFactory(_chainName).destroy();
            }
        }
        ChainData cd = cfw.getChain(_chainName);
        if (cd != null) {
            cfw.removeChain(cd);
        }
        removeChannel(_tcpChannelName);
        if (_isSSLChain)
            removeChannel(_sslChannelName);
        removeChannel(_jfapChannelName);

    }
}
