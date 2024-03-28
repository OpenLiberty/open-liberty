/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Jan 1, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.ibm.ws.webcontainer31.osgi.srt;

import java.io.IOException;

import javax.servlet.http.HttpUpgradeHandler;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink;
import com.ibm.ws.transport.access.TransportConnectionAccess;
import com.ibm.ws.transport.access.TransportConnectionUpgrade;
import com.ibm.ws.transport.access.TransportConstants;
import com.ibm.ws.webcontainer31.async.ThreadContextManager;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.osgi.response.IResponse31Impl;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppDispatcherContext;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer.srt.SRTServletResponse;
import com.ibm.ws.webcontainer31.srt.SRTServletRequest31;
import com.ibm.ws.webcontainer31.srt.SRTServletResponse31;
import com.ibm.ws.webcontainer31.upgrade.HttpUpgradeHandlerWrapper;
import com.ibm.ws.webcontainer31.upgrade.NettyUpgradedWebConnectionImpl;
import com.ibm.ws.webcontainer31.upgrade.UpgradedWebConnectionImpl;
import com.ibm.ws.webcontainer31.upgrade.WebTransportConnection;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;


public class SRTConnectionContext31 extends com.ibm.ws.webcontainer.osgi.srt.SRTConnectionContext
{

    private final static TraceComponent tc = Tr.register(SRTConnectionContext31.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    /**
     * Used for pooling the SRTConnectionContext31 objects.
     */
    public SRTConnectionContext31 nextContext;

    protected SRTServletRequest newSRTServletRequest() {
        return new SRTServletRequest31(this);
    }

    protected SRTServletResponse newSRTServletResponse() {
        return new SRTServletResponse31(this);
    }


    @Override
    public void finishConnection()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "finishConnection");
        }
        try
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "finishConnection  IExtendedRequest->"+_request+", IExtendedResponse"+_response);
            }  
            
            //findbugs says that _request is always an instanceof SRTServletRequest31, so just cast for now
            //if (_request instanceof SRTServletRequest31 && ((SRTServletRequest31)_request).isUpgradeInProgress()) {
            IResponse31Impl irImpl = (IResponse31Impl)_response.getIResponse();
            if (((SRTServletRequest31)_request).isUpgradeInProgress()) {
                boolean doInit = false;
                boolean doUpgradeInit = false;
                HttpUpgradeHandler handler = null;
                WebTransportConnection connection = null;
                UpgradedWebConnectionImpl upgradedCon = null;
                ConnectionLink cldevice,clLink,dispatcherLink = null;
                VirtualConnection vc = null;
                try {                   
                    TCPConnectionContext tcc = null;                   

                    tcc = irImpl.getTCPConnectionContext();
                    cldevice = irImpl.getDeviceConnectionLink();                     
                    vc = irImpl.getVC();
                    
                    handler = ((SRTServletRequest31)_request).getHttpUpgradeHandler();

                    if (vc.getStateMap().containsKey("com.ibm.ws.transport.http.http2InitError")) {
                        // the underlying http/2 connection initialization failed; we should not proceed here
                        return;
                    }
                    
                    if( handler instanceof TransportConnectionUpgrade) { // WebSocket

                        connection = new WebTransportConnection(new HttpUpgradeHandlerWrapper(_dispatchContext.getWebApp(), handler));
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "finishConnection  connection from upgradeHandler "+ connection.toString());
                        }
                        connection.setTCPConnectionContext(tcc);  
                        connection.setDeviceConnLink(cldevice);  
                        vc.getStateMap().put(TransportConstants.UPGRADED_CONNECTION, "true");
                        vc.getStateMap().put(TransportConstants.UPGRADED_WEB_CONNECTION_OBJECT, connection);
                        connection.setVirtualConnection(vc);

                        doInit = true;                            


                    }
                    else{
                     // create the WebConnection and pass the handler generated
                        HttpDispatcherLink link = (HttpDispatcherLink) irImpl.getHttpDispatcherLink();
                        if(link.isUsingNetty()) {
                            link.prepareForUpgrade();
                            upgradedCon = new NettyUpgradedWebConnectionImpl(_request, _response, new HttpUpgradeHandlerWrapper(_dispatchContext.getWebApp(), handler), link.getUpgradedChannel());
                            dispatcherLink = irImpl.getHttpDispatcherLink();
                            clLink = irImpl.getConnLink();
                            vc.getStateMap().put(TransportConstants.CLOSE_NON_UPGRADED_STREAMS, "true");
                            // remove the TransportConstants which if added for Upgrade previously
                            vc.getStateMap().put(TransportConstants.CLOSE_UPGRADED_WEBCONNECTION, null);
                            vc.getStateMap().put(TransportConstants.UPGRADED_LISTENER, null);
                            upgradedCon.setDeviceConnLink(cldevice);   
                            upgradedCon.setConnLink(clLink);
                            upgradedCon.setHttpDisapctherConnLink(dispatcherLink);   

                            upgradedCon.setVirtualConnection(vc);
                            doUpgradeInit = true;
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "finishConnection  webconnection from upgradeHandler "+ upgradedCon.toString());
                                Tr.debug(tc, "nettyChannel -->"+ tcc +" ,cldevice -->" + cldevice);
                            }
                            
                        }else {
                            upgradedCon = new UpgradedWebConnectionImpl(_request, new HttpUpgradeHandlerWrapper(_dispatchContext.getWebApp(), handler));
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "finishConnection  webconnection from upgradeHandler "+ upgradedCon.toString());
                                Tr.debug(tc, "tcc -->"+ tcc +" ,cldevice -->" + cldevice);
                            }
                            if(tcc != null) {
                                
                                dispatcherLink = irImpl.getHttpDispatcherLink();
                                clLink = irImpl.getConnLink();
                                
                                upgradedCon.setTCPConnectionContext(tcc);                           
                                upgradedCon.setDeviceConnLink(cldevice);   
                                upgradedCon.setConnLink(clLink);
                                upgradedCon.setHttpDisapctherConnLink(dispatcherLink);   
                                
                                vc.getStateMap().put(TransportConstants.CLOSE_NON_UPGRADED_STREAMS, "true");
                                // remove the TransportConstants which if added for Upgrade previously
                                vc.getStateMap().put(TransportConstants.CLOSE_UPGRADED_WEBCONNECTION, null);
                                vc.getStateMap().put(TransportConstants.UPGRADED_LISTENER, null);
                                
                                upgradedCon.setVirtualConnection(vc);

                                doUpgradeInit = true;
                            }
                        }
                    }                    
                }
                catch (Throwable t) {
                    // TODO reasonable to catch throwable here?
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(t, "com.ibm.ws.webcontainer.srt31.SRTConnectionContext.finishConnection", "122", this);
                }

                try
                {
                    ((SRTServletResponse31)_response).finishKeepConnection();
                    // close the current httpinput and httpoutput streams as the response is written out. 
                    if(dispatcherLink != null)
                        dispatcherLink.close(vc, null);
                    
                    if (doInit) {
                        ((TransportConnectionUpgrade) handler).init((TransportConnectionAccess) connection);
                    }
                    else if(doUpgradeInit){
                        
                        //Grab the saved off ThreadContextManager, which has the
                        // context data and then push it onto the thread
                        //This data will come from the service method in ServletWrapper
                        //What this does is bring the servlet context from the service method to thing such as
                        // a JNDI lookup can be done on the Upgrade path
                        WebContainerRequestState threadContextRequestState = WebContainerRequestState.getInstance(true);
                        ThreadContextManager tcm = (ThreadContextManager)threadContextRequestState.getAttribute("ApplicationsOriginalTCM");
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "finishConnection, retrieved the saved ThreadContextManager, pushing the context data onto the thread");
                            Tr.debug(tc, "tcm -->" + tcm);
                        }
                        tcm.pushContextData();

                        try {
                            //call application handler init 
                            handler.init(upgradedCon);
                        } finally {
                            tcm.popContextData();
                        }
                    }
                }
                catch (Throwable th)
                {
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, "com.ibm.ws.webcontainer.srt31.SRTConnectionContext.finishConnection", "87", this);
                }               

                try
                {
                    ((SRTServletRequest31)_request).finishKeepConnection();
                }
                catch (Throwable th)
                {
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, "com.ibm.ws.webcontainer.srt31.SRTConnectionContext.finishConnection", "96", this);
                    WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext)_request.getWebAppDispatcherContext();
                    dispatchContext.getWebApp().logError("Error while finishing the connection", th);
                }
                finally{
                    WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
//                    if(reqState!= null)
//                            reqState.removeAttribute("com.ibm.ws.webcontainer.upgrade.ThisThreadSetWL");                                    
                }

            } else {
                try
                {
                    _response.finish();
                }
                catch (Throwable th)
                {
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, "com.ibm.ws.webcontainer.srt31.SRTConnectionContext.finishConnection", "64", this);
                }		

                try
                {
                    _request.finish();
                }
                catch (Throwable th)
                {
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, "com.ibm.ws.webcontainer.srt31.SRTConnectionContext.finishConnection", "74", this);
                    WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext)_request.getWebAppDispatcherContext();
                    dispatchContext.getWebApp().logError("Error while finishing the connection", th);
                }
            }



            dispatchContextFinish();
        }
        finally
        {
            _request.initForNextRequest(null);
            _response.initForNextResponse(null);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "finishConnection");
        }
    }
}
