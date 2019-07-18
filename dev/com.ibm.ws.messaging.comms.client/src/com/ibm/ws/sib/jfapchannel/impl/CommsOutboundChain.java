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
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

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

    // The state of the desired chain, which is the chain that OSGI is or has configured.
    
    private String _chainName = null;
    
    /** use _tcpOptions service direct instead of reference as _tcpOptions is a required service */
    private ChannelConfiguration _tcpOptions = null;

    /** Optional,dynamic reference to sslOptions */
    private final AtomicServiceReference<ChannelConfiguration> _sslOptions = new AtomicServiceReference<ChannelConfiguration>("sslOptions");

    /** Optional, dynamic reference to an SSL channel factory provider: could be used to start/stop SSL chains */
    private final AtomicServiceReference<ChannelFactoryProvider> _sslFactoryProvider = new AtomicServiceReference<ChannelFactoryProvider>("sslSupport");

    /** Required, dynamic reference to an executor service to schedule chain operations */
    private final AtomicServiceReference<ExecutorService> executorService = new AtomicServiceReference<ExecutorService>("executorService");
    
    /** use _commsClientService service direct instead of reference as _commsClientService is a required service */
    private CommsClientServiceFacadeInterface _commsClientService = null;

   
    private volatile boolean _isSSLChain = false;
    private volatile boolean _isActivated = false;

    // The single thread that attempts to create the chain as configured above.
    
    private boolean newRequest = false;
    private Future<?> actionFuture = null;
    
    // The state of the current chain (if any)
    private boolean isCurrentChainStarted = false;
    private boolean isCurrentChainSSLEnabled = false;
    private boolean isCurrentChainSSL = false;
    private boolean isCurrentChainActivated = false;
    private String currentChainName = null;
    private String tcpChannelName = null;
    private String jfapChannelName = null;
    private String sslChannelName = null;
        
    /**
     * Declarative Services (DS) method for setting the required dynamic executor service reference.
     *
     * @param bundle
     */
    protected void setExecutorService(ServiceReference<ExecutorService> executorService) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "setExecutorService", executorService);
        }
        
        this.executorService.setReference(executorService);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.exit(this, tc, "setExecutorService");
        }
    }

    /**
     * Declarative Services (DS) method for clearing the required dynamic executor service reference.
     * This is a required reference, but will be called if the dynamic reference is replaced
     */
    protected void unsetExecutorService(ServiceReference<ExecutorService> executorService) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "unsetExecutorService", executorService);
        }
        
        this.executorService.unsetReference(executorService);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.exit(this, tc, "unsetExecutorService");
        }
    }
    
    protected void setCommsClientService(CommsClientServiceFacadeInterface service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "setCommsClientService", service);
        }
    
        _commsClientService = service;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.exit(this, tc, "setCommsClientService");
        }
    }

    protected void unsetCommsClientService(CommsClientServiceFacadeInterface service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "unsetCommsClientService", service);
        }
        _commsClientService = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.exit(this, tc, "unsetCommsClientService");
        }
    }

    protected void setTcpOptions(ChannelConfiguration service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "setTcpOptions", service);
        }

        _tcpOptions = service;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setTcpOptions", _tcpOptions);
        }
    }

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
    protected void setSslSupport(ServiceReference<ChannelFactoryProvider> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "setSslSupport", new Object[] { ref.getProperty("type"), ref });
        }

        _sslFactoryProvider.setReference(ref);
        // we will only invoke createchain if 
        // 1) activate() is complete
        // this is because bind method get called first before activate and without activate we don't have any properties
        // 2) if useSSL is set in config 
        if (_isActivated && _isSSLChain) {
            performAction();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setSslSupport" );
        }
    }

    /**
     * This is a required reference-- set (with a new ChannelFactoryProvider) would be
     * called before the unset, unless the component has been deactivated.
     * 
     * @param ref ConfigurationAdmin instance to unset
     */
    public void unsetSslSupport(ServiceReference<ChannelFactoryProvider> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "unsetSslSupport", new Object[] { ref.getProperty("type"), ref });
        }
       
        _sslFactoryProvider.unsetReference(ref);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "unsetSslSupport");
        }

    }

    //SslOption related functions
    protected void setSslOptions(ServiceReference<ChannelConfiguration> service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "setSslOptions", service);
        }

        _sslOptions.setReference(service);
        // we will only invoke createchain if 
        // 1) activate() is complete
        // this is because bind method get called first before activate and without activate we don't have any properties
        // 2) if useSSL is set in config 
        if (_isActivated && _isSSLChain) {
            performAction();
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setSslOptions");
        }

    }

    protected void unsetSslOptions(ServiceReference<ChannelConfiguration> unbindServiceRef) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "unsetSslOptions", unbindServiceRef);
        }

        // see if its for the same service ref, if yes then destroy
        if (_sslOptions.getReference() == unbindServiceRef)
        {
            if (_isSSLChain) {
                performAction();
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

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param properties : Map containing service & config properties
     *            populated/provided by config admin
     */
    protected void activate(Map<String, Object> properties, ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "activate", new Object[] {properties, context});

        executorService.activate(context);
        _sslOptions.activate(context);
        _sslFactoryProvider.activate(context);

        _isSSLChain = MetatypeUtils.parseBoolean(_OutboundChain_ConfigAlias, "useSSL", properties.get("useSSL"), false);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Chain is configured for  " + (_isSSLChain ? "Secure " : "Non-Secure"));

        _chainName = (String) properties.get("id");
        _isActivated = true;

        performAction();
      
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "activate", _chainName);
    }
 
   /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param context
     */
    protected void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "deactive", context);

        _isActivated = false;
        performAction();
        executorService.deactivate(context);
        _sslOptions.deactivate(context);
        _sslFactoryProvider.deactivate(context);
              
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "deactive");
    }

   private void performAction() {
        ExecutorService exec = executorService.getService();
        synchronized (actionsRunner) {
            newRequest = true;
            if ((actionFuture == null)) {
                actionFuture = exec.submit(actionsRunner);
            }
        }
    }
    
    private final Runnable actionsRunner = new Runnable() {
        @Override
        public void run() {        
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.entry(this, tc, "actionsRunner.run",newRequest);
            
            for (;;) {
                synchronized (actionsRunner) {
                    newRequest = false;
                }
                
                doAction();
                
                synchronized (actionsRunner) {
                    if (newRequest == false) {
                      actionFuture = null; 
                      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                          SibTr.exit(this, tc, "actionsRunner.run","Channel state caught up.");
                      return;
                    }
                }              
            }          
       }
        
        private void doAction() {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "actionsRunner.doAction", new Object[] {isCurrentChainStarted, isCurrentChainSSLEnabled, isCurrentChainSSL, isCurrentChainActivated, currentChainName});
           
            if(isCurrentChainStarted) { 
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  Tr.debug(this, tc, "Destroy the existing chain.");
                
                try {
                    terminateConnectionsAssociatedWithChain();
                } catch (Exception e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Failure in terminating conservations and physical connections while destroying chain : " + currentChainName, e);
                } finally {
                    isCurrentChainStarted = false;
                }              
            }
            
            // Adopt any new state. If we do not observe part of the state here because it is currently being set then another
            // request to doAction will follow once the new state is set.
            isCurrentChainSSLEnabled = (_sslFactoryProvider.getReference() != null);
            isCurrentChainSSL = _isSSLChain;
            isCurrentChainActivated = _isActivated;
            currentChainName = _chainName;
            tcpChannelName = currentChainName + "_JfapTcp";
            sslChannelName = currentChainName + "_JfapSsl";
            jfapChannelName = currentChainName + "_JfapJfap";
            Map<String, Object> sslOptions = getSslOptions();
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "actionsRunner.doAction", new Object[] {isCurrentChainStarted, isCurrentChainSSLEnabled, isCurrentChainSSL, isCurrentChainActivated, currentChainName});
            
            try {
                if (isCurrentChainActivated && currentChainName != null) {
                    if (!isCurrentChainSSL && !isCurrentChainSSLEnabled && sslOptions == null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(this, tc, "Creating a new non SSL chain.");
                        createJFAPChain(sslOptions);

                    } else if (isCurrentChainSSL && isCurrentChainSSLEnabled && sslOptions != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(this, tc, "Creating a new SSL chain.");
                        createJFAPChain(sslOptions);

                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(this, tc, "State not consistent with either SSL or Non SSL chains.");
                    }
                }
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Exception in creating chain", e);
            }
            
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.exit(this, tc, "actionsRunner.doAction");
        }
        
        /**
         * Terminate all the connections associated with the chain
         * 
         * @throws Exception
         */
        protected void terminateConnectionsAssociatedWithChain() throws Exception {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "terminateConnectionsAssociatedWithChain");
            }

            ChannelFramework cfw = CommsClientServiceFacade.getChannelFramewrok();
            OutboundConnectionTracker oct = ClientConnectionManager.getRef().getOutboundConnectionTracker();
            if (oct != null) {
                oct.terminateConnectionsAssociatedWithChain(currentChainName);
            
            } else {
                // if we don't have any oct that means there were no connections established
                if (cfw.getChain(currentChainName) != null) {// see if chain exist only then destroy
                    // we have to destroy using vcf because cfw does not allow to destroy outbound 
                    // chains directly using cfw.destroyChain(chainname)
                    // as it is valid only for inbound chains as of now
                    cfw.getOutboundVCFactory(currentChainName).destroy();
                }
            }
            ChainData cd = cfw.getChain(currentChainName);
            if (cd != null) {
                cfw.removeChain(cd);
            }
            removeChannel(tcpChannelName);
            if (isCurrentChainSSL)
                removeChannel(sslChannelName);
            removeChannel(jfapChannelName);

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
    
        private void createJFAPChain(Map<String, Object> sslOptions) throws ChannelException, ChainException {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "createJFAPChain", isCurrentChainStarted);

            try {
                ChannelFramework cfw = CommsClientServiceFacade.getChannelFramewrok();
                cfw.registerFactory("JFapChannelOutbound", JFapChannelFactory.class);

                Map<String, Object> tcpOptions = getTcpOptions();

                ChannelData tcpChannel = cfw.getChannel(tcpChannelName);

                if (tcpChannel == null) {
                    String typeName = (String) tcpOptions.get("type");
                    tcpChannel = cfw.addChannel(tcpChannelName, cfw.lookupFactory(typeName), new HashMap<Object, Object>(tcpOptions));
                }

                // SSL Channel
                if (isCurrentChainSSL) {
                    ChannelData sslChannel = cfw.getChannel(sslChannelName);
                    if (sslChannel == null) {
                        sslChannel = cfw.addChannel(sslChannelName, cfw.lookupFactory("SSLChannel"), new HashMap<Object, Object>(sslOptions));
                    }
                }

                ChannelData jfapChannel = cfw.getChannel(jfapChannelName);
                if (jfapChannel == null)
                    jfapChannel = cfw.addChannel(jfapChannelName, cfw.lookupFactory("JFapChannelOutbound"), null);

                final String[] chanList;
                if (isCurrentChainSSL)
                    chanList = new String[] { jfapChannelName, sslChannelName, tcpChannelName };
                else
                    chanList = new String[] { jfapChannelName, tcpChannelName };

                ChainData cd = cfw.addChain(_chainName, FlowType.OUTBOUND, chanList);
                cd.setEnabled(true);

                //if we are here then chain is started
                isCurrentChainStarted = true;
                
                if (isCurrentChainSSL) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "JFAP Outbound secure chain" + _chainName + " successfully started ");
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                       SibTr.debug(tc, "JFAP Outbound chain" + _chainName + " successfully started ");
                }

            } catch (ChannelException | ChainException exception) {
                isCurrentChainStarted = false;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "JFAP Outbound chain:" + _chainName + " failed to get started exception:"+exception);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "createJFAPChain ", isCurrentChainStarted);
                throw exception;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createJFAPChain ", isCurrentChainStarted);
        }
    };
}