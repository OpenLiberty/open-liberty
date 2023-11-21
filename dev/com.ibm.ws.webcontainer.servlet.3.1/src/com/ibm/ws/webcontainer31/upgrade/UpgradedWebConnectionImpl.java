/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.ws.webcontainer31.upgrade;

import java.io.IOException;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.WebConnection;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.transport.access.TransportConstants;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.srt.SRTUpgradeInputStream31;
import com.ibm.ws.webcontainer31.srt.SRTUpgradeOutputStream31;
import com.ibm.ws.webcontainer31.util.UpgradeInputByteBufferUtil;
import com.ibm.ws.webcontainer31.util.UpgradeOutputByteBufferUtil;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

/**
 * @author Administrator
 *         Note: Added for "Upgrade Processing" of Java Servlet Specification  31.
 * 
 *         The WebConnection created in com.ibm.ws.webcontainer31.osgi.srt.SRTConnectionContext31
 *         This connection will be passed to application or HttpUpgradeHandler's init method.
 *         The i/o allowed is in byte streams.
 * 
 */
public class UpgradedWebConnectionImpl implements WebConnection {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(UpgradedWebConnectionImpl.class, 
                                                         WebContainerConstants.TR_GROUP, 
                                                         WebContainerConstants.NLS_PROPS );

    // TCP connection variables
    private TCPConnectionContext tcpConn; 
    private VirtualConnection    virtualConn;
    private ConnectionLink deviceConnLink,connLink,dispatcherLink ;

    //Input
    protected SRTUpgradeInputStream31 _in;
    protected UpgradeInputByteBufferUtil _inbb;

    //Output
    protected SRTUpgradeOutputStream31 _out;
    protected UpgradeOutputByteBufferUtil _outbb;


    HttpUpgradeHandlerWrapper _upgradeHandler;
    protected boolean _outputStreamObtained = false;

    protected IExtendedRequest _req;

    private boolean webConnection_closeComplete = false;
    private boolean upgradeHandler_DestroyComplete = false;
    private boolean upgradeHandler_DestroyStarted = false;
    private boolean outputStream_CloseStartedFromWC = false;
    private boolean webConnection_closeOngoing = false;
    private boolean inputCallback = false;
    
    private Exception closeUpgradeException = null; 

    /**
     * @param upgradeHandler
     */
    public UpgradedWebConnectionImpl(IExtendedRequest req, HttpUpgradeHandlerWrapper upgradeHandler) {
        super();
        this._req = req;
        this._upgradeHandler = upgradeHandler;

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception {         

        if(webConnection_closeComplete){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "WebConnection close previously completed ...return ");
            }
            return;
        }
        else{
            // this is for the case to avoid infinite loop if handler destroy end up calling webconnection close.  
            if(upgradeHandler_DestroyStarted){
                return;
            }
            synchronized(this) {
                //check it again 
                if(webConnection_closeComplete || webConnection_closeOngoing){
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "WebConnection close previously completed .....return ");
                    }
                    return;
                }
                webConnection_closeOngoing = true;


                //      Call UpgradeHandler destroy() API.      
                //upgradeHandler_DestroyComplete  is needed if close is called again from complete , we do not call destroy again
                if (!upgradeHandler_DestroyComplete && _upgradeHandler != null) { 
                    /// call application handler class destroy

                    try{ 
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "call Handler destroy "+ this._upgradeHandler);                      
                        } 
                        upgradeHandler_DestroyStarted= true;
                        _upgradeHandler.destroy();
                    }
                    finally{
                        upgradeHandler_DestroyComplete = true;
                        upgradeHandler_DestroyStarted= false;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Handler destroy successful"+ this._upgradeHandler);                      
                        }
                    }
                }          

                if(_in != null && (_in.getInputBufferHelper() != null) && (_in.getInputBufferHelper().get_tcpChannelCallback() != null)){
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, " input callback");
                    }
                    inputCallback = true;
                }

                //close the streams , make sure even if one fails we close rest. 
                try{
                    if(_in!= null)
                        _in.close();
                }catch(IOException ioe){
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, " closing of inputStream failed ..");
                    }
                    closeUpgradeException = ioe;
                }

                // now close output
                // close connection
                closeOutputandConnection();
            }
        }
    }


    /**
     * 
     */
    protected void closeOutputandConnection(){

        boolean hasOutputCallback_CloseLinkHere = false;
        if((_out != null) && (_out.callback != null)){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, " output callback");
            }
            hasOutputCallback_CloseLinkHere = true;
        }

        try{
            if(_out!= null){
                this.setOutputStream_CloseStartedFromWC(true);
                _out.close();
            }

        }catch(IOException ioe){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, " closing of outputStream failed ..");
            }
            closeUpgradeException = ioe;
        }

        if(_outbb != null && _outbb.isOutputStream_closed()) {
            try {
                // now call close on dispatcherlink
                if(hasOutputCallback_CloseLinkHere || inputCallback){                        
                    virtualConn.getStateMap().put(TransportConstants.CLOSE_UPGRADED_WEBCONNECTION, "true");
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, " call dispatcherLink close--> " +  this.dispatcherLink);
                    }                     
                    this.dispatcherLink.close(virtualConn, closeUpgradeException);                       
                }
                else{
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, " no callbacks, don't close here... ");
                    } 
                }
            }
            finally{
                webConnection_closeComplete = true;
                webConnection_closeOngoing = false;
                _in = null;
                _out = null;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, " webConnection_closeComplete--> " +  webConnection_closeComplete);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.WebConnection#getInputStream()
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {    

        if(_in == null){

            //create the inputstream 
            _in = new SRTUpgradeInputStream31();       
            _inbb = new UpgradeInputByteBufferUtil(this, this.getTCPConnectionContext());
            _in.init(_inbb);
        }
        return _in;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.WebConnection#getOutputStream()
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {

        if(_out == null ){
            //create the outputstream 
            _out = new SRTUpgradeOutputStream31();       
            _outbb = new UpgradeOutputByteBufferUtil(this, this.getTCPConnectionContext());
            _out.init(_outbb,_req);
        }
        return _out;
    }

    /**
     * @return
     */
    public TCPConnectionContext getTCPConnectionContext() {

        return tcpConn;
    }

    /**
     * @param input
     */
    public void setTCPConnectionContext(TCPConnectionContext input) {
        tcpConn = input;
    }

    /**
     * @return
     */
    public ConnectionLink getDeviceConnLink() {

        return deviceConnLink;
    }

    /**
     * @param input
     */
    public void setDeviceConnLink(ConnectionLink input) {
        deviceConnLink = input;
    }

    /**
     * @return
     */
    public VirtualConnection getVirtualConnection() {

        return virtualConn;
    }
    /**
     * @param input
     */
    public void setVirtualConnection(VirtualConnection input) {
        virtualConn = input;
    }

    /**
     * @param connLink the connLink to set
     */
    public void setConnLink(ConnectionLink connLink) {
        this.connLink = connLink;
    }

    /**
     * @param connLink the connLink to set
     */
    public void setHttpDisapctherConnLink(ConnectionLink dispatcherLink) {
        this.dispatcherLink = dispatcherLink;
    }

    public boolean isOutputStream_CloseStartedFromWC() {
        return outputStream_CloseStartedFromWC;
    }

    public void setOutputStream_CloseStartedFromWC(boolean outputStream_CloseStartedFromWC) {
        this.outputStream_CloseStartedFromWC = outputStream_CloseStartedFromWC;
    }
}
