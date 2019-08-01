/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
//package com.ibm.ws.sip.container.proxy;
//
//import java.io.IOException;
//
//import javax.servlet.sip.*;
//
//import com.ibm.ws.sip.container.servlets.IncomingSipServletRequest;
//import com.ibm.ws.sip.container.transaction.ClientTransactionListener;
//import com.ibm.sip.util.log.*;
//
///**
// * @author yaronr
// * 
// * Represents a stateful proxy with the parallel flag set to false
// */
//public class StatefulSequenceProxy extends StatefullProxy
//{
//    /**
//    * Class Logger. 
//    */
//    private static final LogMgr c_logger = Log.get(StatefulSequenceProxy.class);
//
//    /**
//     * Hold current request
//     */
//    private SipServletRequest m_currentRequest = null;
//
//    /**
//     * Are we waiting for any kind of response 
//     */
//    private boolean m_waitingForResponse = false;
//
//    /**
//     * Constuctor
//     * 
//     * @param director
//     * @param originalReq
//     */
//    StatefulSequenceProxy(ProxyDirector director)
//    {
//        super(director);
//        if (c_logger.isTraceDebugEnabled())
//        {
//            c_logger.traceDebug(this, "StatefulSequenceProxy", "");
//        }
//    }
//
//    /**
//     * @see com.ibm.ws.sip.container.proxy.ProxyImpl#cancel()
//     */
//    public void cancel()
//    {
//        if (c_logger.isTraceDebugEnabled())
//        {
//            c_logger.traceDebug(this, "cancel", "");
//        }
//        if (m_waitingForResponse)
//        {
//            cancelRequest(m_currentRequest, this);
//        }
//    }
//
//    /**
//     * @see com.ibm.ws.sip.container.proxy.ProxyImpl#startSending()
//     */
//    protected void startSending()
//    {
//        if (c_logger.isTraceDebugEnabled())
//        {
//            c_logger.traceDebug(this, "startSending", "");
//        }
//
//        //	Are there any URI waitng?
//        if (areAllBranchesCompleted())
//        {
//            if (c_logger.isTraceDebugEnabled())
//            {
//                c_logger.traceDebug(
//                    this,
//                    "startSending",
//                    "Waiting URIs list is empty");
//            }
//            return;
//        }
//
//        // Send to the first branch in the list
//        tryNextHop();
//
//    }
//
//    /**
//     * Try the next hop in the waiting URIs list
//     *
//     */
//    protected void tryNextHop()
//    {
//        if(m_waitingForResponse)
//        {
//            return;
//        }
//        
//        //	Are there any URI waitng?
//        if (areAllBranchesCompleted())
//        {
//            IncomingSipServletRequest original =
//                (IncomingSipServletRequest) m_director.getOriginalRequest();
//
//            forwardResponse(m_bestResponse.getBestResponse(), original, false);
//            return;
//        }
//
//        //	Remove the first address from the list
//        URI uri = (URI) m_waitingURIs.remove(0);
//
//        // Proxy to this URI, add ourselves as listeners for the transaction 	
//        try
//        {
//            m_waitingForResponse = true;
//            m_currentRequest = proxy(uri, this);
//        }
//        catch (IOException e)
//        {
//            if(c_logger.isErrorEnabled())
//            {
//	            Object[] args = { uri };
//	            c_logger.error(
//	                "error.proxying.to.branch",
//	                Situation.SITUATION_REQUEST,
//	                args,
//	                e);
//            }
//        }
//    }
//
//    /**
//     * @see com.ibm.ws.sip.container.proxy.StatefullProxy#branchCompleted(javax.servlet.sip.SipServletResponse)
//     */
//    protected void branchCompleted(ProxyBranch branch, SipServletResponse response)
//    {
//        // Update the best response
//        updateBestResponse(response);
//
//        // Should we forward the best response?
//        if (areAllBranchesCompleted())
//        {
//            IncomingSipServletRequest original =
//                (IncomingSipServletRequest) m_director.getOriginalRequest();
//
//            // only if all branches has completed
//            forwardResponse(m_bestResponse.getBestResponse(), original, true);
//        }
//        else
//        {
//            //	no request
//            m_currentRequest = null;
//            m_waitingForResponse = false;
//
//            // Go to next branch
//            tryNextHop();
//        }
//    }
//
//    /**
//     * @see com.ibm.ws.sip.container.proxy.StatefullProxy#areAllBranchesCompleted()
//     */
//    protected boolean areAllBranchesCompleted()
//    {
//        // Is there any branch waiting?
//        return (m_waitingURIs.isEmpty());
//    }
//
//    /* (non-Javadoc)
//     * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#processTimeout(javax.servlet.sip.SipServletRequest)
//     */
//    public void processTimeout(SipServletRequest request)
//    {
//        if (c_logger.isTraceDebugEnabled())
//        {
//            c_logger.traceDebug(this, "processTimeout", "" + request);
//        }
//
//        //	no request
//        m_currentRequest = null;
//        m_waitingForResponse = false;
//
//        //Let base do the real processing
//        tryNextHop();
//    }
//
//    public void processResponse(ProxyBranch branch, SipServletResponse response)
//    {
//        // Is it a final response?
//        int status = response.getStatus();
//        if (status >= 200)
//        {
//            // Yes it is
//            m_waitingForResponse = false;
//        }
//
//        // Let base do the real processing
//        super.processResponse(branch, response);
//    }
//
//    /* (non-Javadoc)
//     * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#onSendingRequest(javax.servlet.sip.SipServletRequest)
//     */
//    public void onSendingRequest(SipServletRequest request)
//    {
//        // TODO 
//        // do we care?
//        // I don't think so
//        // but how do we listen to the original transaction
//        // cause the application might send response
//        // and we need to know that (section 8.2.2 in the spec.)
//    }
//}
