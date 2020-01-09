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
import com.ibm.ws.ffdc.FFDCFilter;
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

    /** use _commsClientService service direct instead of reference as _commsClientService is a required service */
    private CommsClientServiceFacadeInterface _commsClientService = null;

    /* The CommsOuboundChain should be activated once and then deactivated once. */
    private boolean _isChainActivated = false;
    private boolean _isChainDeactivated = false;
    /** If useSSL is set to true in the outbound connection configuration */
    private boolean _isSSLChain = false;
   
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
    protected void setTcpOptions(ChannelConfiguration service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "setTcpOptions", service);

        _tcpOptions = service;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "setTcpOptions");
    }

    private Map<String, Object> getTcpOptions() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "getTcpOptions");
        }
        
        Map<String, Object> tcpOptions = null;
        if (_tcpOptions == null) {
            // Should not occur.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "getTcpOptions() returning NULL as __tcpOptions.getService() returned NUll _tcpOptions: ", _tcpOptions);

        } else {
            tcpOptions = _tcpOptions.getConfiguration();
        }
            
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "getTcpOptions", tcpOptions);
        return tcpOptions;
    }

    @Trivial
    protected void setSslOptions(ServiceReference<ChannelConfiguration> service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "setSslOptions", service);

       _sslOptions.setReference(service);
       createJFAPChain();
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "setSslOptions");
    }

    private Map<String, Object> getSslOptions() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "getSslOptions");

        ChannelConfiguration chanCnfgService = _sslOptions.getService();
        Map<String, Object> sslOptions = null;

        if (chanCnfgService == null) {
                  // Should not occur.
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "getSslOptions() returning NULL as _sslOptions.getService() returned NUll _sslOptions: ", _sslOptions);
        } else {
            sslOptions = chanCnfgService.getConfiguration();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "getSslOptions", sslOptions);
        return sslOptions;
    }

    /**
     * Declarative Services method to activate this component.
     * Best practice: this should be a protected method, not public or private.
     * 
     * @param properties : Map containing service & config properties
     *            populated/provided by config admin
     * 
     * activate() is called exactly once because we are not dynamic.
     * @see com.ibm.ws.messaging.comms.client/bnd.bnd
     */
    protected void activate(Map<String, Object> properties, ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "activate", new Object[] {properties, context, _isChainActivated, _isChainDeactivated});

        _sslOptions.activate(context);

        _isSSLChain = MetatypeUtils.parseBoolean(_OutboundChain_ConfigAlias, "useSSL", properties.get("useSSL"), false);
        
        String Outboundname = (String) properties.get("id");
        _chainName = Outboundname;
        _tcpChannelName = Outboundname + "_JfapTcp";
        _sslChannelName = Outboundname + "_JfapSsl";
        _jfapChannelName = Outboundname + "_JfapJfap";

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "CommsOutboundChain: Creating " + (_isSSLChain ? "Secure" : "Non-Secure") + " chain ", _chainName);

        if ( _isChainActivated ) {    
            FFDCFilter.processException
            (new IllegalStateException("_isChainActivated="+_isChainActivated+" _isChainDeactivated="+_isChainDeactivated), CommsOutboundChain.class.getName(), "050819_210", new Object[] {_isChainActivated, _isChainDeactivated});

        } else {       
            _isChainActivated = true;
            createJFAPChain();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "activate");
    }
    
    private synchronized void createJFAPChain() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createJFAPChain", new Object[] {_chainName, _isSSLChain, _isChainActivated, _isChainDeactivated });

        if(!_isChainActivated || _isChainDeactivated) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createJFAPChain", "Chain is not activated, or is deactivated");
            return;
        }
        
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

                // Now we are actually trying to create an ssl channel in the chain. Which requires sslOptions and that _isSSLEnabled is true.
                if (sslOptions == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "_sslOptions not set, continue waiting");
                    throw new ChainException(new Throwable(nls.getFormattedMessage("missingSslOptions.ChainNotStarted", new Object[] { _chainName }, null)));
                    // TcpChannel is created but keep waiting for sslOptions.
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
                      
            // The Fat test:
            // /com.ibm.ws.messaging_fat/fat/src/com/ibm/ws/messaging/fat/CommsWithSSL/CommsWithSSLTest.java
            // Checks for the presence of this string in the trace log, in order to ascertain that the chain has been enabled.
            if (_isSSLChain) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "JFAP Outbound secure chain" + _chainName + " successfully started ");
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "JFAP Outbound chain" + _chainName + " successfully started ");
            }

        } catch (ChannelException | ChainException exception) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "JFAP Outbound chain " + _chainName + " failed to start, exception ="+exception);
        } 
               
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createJFAPChain");
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param reason int representation of reason the component is stopping
     */
    protected void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "deactivate", context, _isChainActivated,  _isChainDeactivated);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "CommsOutboundChain: Destroying " + (_isSSLChain ? "Secure" : "Non-Secure") + " chain ", _chainName);

        // The chain may have failed to activate if, for example, sslOptions and sslSupport were not defined.
        if ( _isChainDeactivated ) {
            FFDCFilter.processException(new IllegalStateException("_isChainActivated=" + _isChainActivated + " _isChainDeactivated=" + _isChainDeactivated),
                                        CommsOutboundChain.class.getName(), "050819_297", new Object[] { _isChainActivated, _isChainDeactivated });

        } else {
            _isChainDeactivated = true;
            terminateConnectionsAssociatedWithChain();
        }
        
        _sslOptions.deactivate(context);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this,tc, "deactivate");
    }

    /**
     * Terminate all of the connections associated with the chain, then remove the chain from the channel framework.
     * 
     * @throws Exception
     */
    private synchronized void terminateConnectionsAssociatedWithChain() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "terminateConnectionsAssociatedWithChain", _chainName);
        }

        try {
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
            
        } catch (Exception exception) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Failure in terminating conservations and physical connections while destroying chain : " + _chainName, exception);
        }
        
        removeChannel(_tcpChannelName);
        if (_isSSLChain)
            removeChannel(_sslChannelName);
        removeChannel(_jfapChannelName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "terminateConnectionsAssociatedWithChain");
        }
    }
    
    /**
     * Remove a channel from the channel framework.
     * @param channelName of the channel to be removed.
     */
    private void removeChannel(String channelName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeChannel", channelName);
        
        ChannelFramework cfw = CommsClientServiceFacade.getChannelFramewrok();

        try {
            if (cfw.getChannel(channelName) != null)
                cfw.removeChannel(channelName);
        } catch (ChannelException | ChainException exception) {
            // Neither of these exceptions are permanent failures. 
            // They usually indicate that we're the looser of a race with the channel framework to
            // tear down the chain. For example, the SSL feature was removed.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Error removing channel:" + channelName, exception);
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeChannel");
    }
}
