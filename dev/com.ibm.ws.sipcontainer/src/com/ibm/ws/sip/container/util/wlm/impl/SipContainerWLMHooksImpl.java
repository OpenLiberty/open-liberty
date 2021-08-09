/*******************************************************************************
 * Copyright (c) 2003,2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.util.wlm.impl;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.util.wlm.QueueableTransformer;
import com.ibm.ws.sip.container.util.wlm.SipContainerWLMHooks;
import com.ibm.ws.sip.container.util.wlm.WLMDialogFailoverListner;

/**
 * Implementation Class for WLM hooks
 * @author dror
 *
 */
public class SipContainerWLMHooksImpl implements SipContainerWLMHooks {
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(SipContainerWLMHooksImpl.class);
	
	/**
	 * Singleton instance
	 */
	private static SipContainerWLMHooks s_instance = null;
	
	/**
	 * QueueableTransofmer to be used.
	 */
	private QueueableTransformer m_transformer = null;
	/**
	 * WLMDialogFailoverListner to be used
	 */
	private WLMDialogFailoverListner m_WLMDailogFailoverListner = null;
	
	static{
		s_instance = new SipContainerWLMHooksImpl();
	}
	
	private SipContainerWLMHooksImpl(){
		
	}
	
	/**
	 * @return Singleton instance
	 */
	public static SipContainerWLMHooks getSipContainerWLMHooksInstance(){
		return s_instance;
	}
	
	/**
	 * @see com.ibm.ws.sip.container.util.wlm.SipContainerWLMHooks#setQueueableTransformer(com.ibm.ws.sip.container.util.wlm.QueueableTransformer)
	 */
	public void setQueueableTransformer(QueueableTransformer qt)
			throws Exception {
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this,"setQueueableTransformer",new Object[]{qt});
		}
		if (m_transformer == null){
			m_transformer = qt;
		}else{
			if(c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug(this,"setQueueableTransformer","QueueableTransformer set");
			}
			throw new Exception("QueueableTransformer already set"); 
			
		}
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(this,"setQueueableTransformer");
		}
	}
	
	/**
	 * @see com.ibm.ws.sip.container.util.wlm.SipContainerWLMHooks#getQueueableTransformer()
	 */
	public QueueableTransformer getQueueableTransformer() {
		return m_transformer;
	}
	/**
	 * @see com.ibm.ws.sip.container.util.wlm.SipContainerWLMHooks#setWLMDailogFailoverListner(com.ibm.ws.sip.container.util.wlm.WLMDialogFailoverListner)
	 */
	public void setWLMDailogFailoverListner(WLMDialogFailoverListner listener) throws Exception {
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this,"setWLMDailogFailoverListner",new Object[]{listener});
		}
		if (m_WLMDailogFailoverListner == null){
			m_WLMDailogFailoverListner = listener;
		}else{
			if(c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug(this,"setWLMDailogFailoverListner","WLMDialogFailoverListner set");
			}
			throw new Exception("WLMDialogFailoverListner already set"); 
			
		}
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(this,"setWLMDailogFailoverListner");
		}
	}

	/**
	 * @see com.ibm.ws.sip.container.util.wlm.SipContainerWLMHooks#getWLMDailogFailoverListner()
	 */
	public WLMDialogFailoverListner getWLMDailogFailoverListner() {
		return m_WLMDailogFailoverListner;
	}

}
