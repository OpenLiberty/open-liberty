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
package com.ibm.ws.sip.container.asynch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import javax.servlet.sip.SipServletResponse;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.websphere.sip.AsynchronousWorkListener;
import com.ibm.ws.jain.protocol.ip.sip.message.SipResponseCodes;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;

/**
 * This listener is used by the sip container to receive asynch task completion events and to 
 * create a sip response to a container which initiated this task.
 *  
 * @author Galina
 *
 */
public class AsynchronousWorkTaskListener implements AsynchronousWorkListener {
	
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(AsynchronousWorkTaskListener.class);
    
	// Keep the sip request to return a result when the work is completed
    private SipServletRequestImpl _sipRequest = null;

    /**
	 * Content type used to send the asynch work as a body
	 */
	public final static String CONTENT_TYPE = "asynchwork/type";
	
	/**
	 * Constructor
	 * 
	 * @param request
	 */
	public AsynchronousWorkTaskListener(SipServletRequestImpl request) {
		_sipRequest = request;
	}

	/**
	 * This method is invoked when the task is completed and need to send a response to the task creator
	 */
	public void onCompleted(Serializable result) {
		
		// Need to send SIP response if there is a SIP request
		if (_sipRequest != null) {
			try {
				SipServletResponse response = _sipRequest.createResponse(SipResponseCodes.OK);
				if (result != null) {
					response.setContent(dataToByteArray(result), CONTENT_TYPE);
				}
				response.send();
			} catch (UnsupportedEncodingException e) {
				if (c_logger.isErrorEnabled()){
					c_logger.error("error.exception.UnsupportedEncodingException",Situation.SITUATION_REQUEST_COMPLETED,null,e);        	
				}
			} catch (IOException e) {
				if (c_logger.isErrorEnabled()){
					c_logger.error("error.exception.io",Situation.SITUATION_REQUEST_COMPLETED,null,e);        	
				}
			}
		}
	}

	/**
	 * 
	 * @param data
	 * @return
	 * @throws IOException
	 */
	private byte[] dataToByteArray(Serializable obj) throws IOException{
		if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this,"dataToByteArray",new Object[]{obj});
    	}
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bout);
		out.writeObject(obj);
		byte[] bits = bout.toByteArray();
		if (c_logger.isTraceEntryExitEnabled()){
    		c_logger.traceExit(this,"dataToByteArray",bits);
    	}
		return bits;
	}	
	
	/**
	 * This method is invoked when the task is failed and need to send a response to the task creator
	 */
	public void onFailed(int reasonCode, String reason) {
		// Need to send SIP response if there is a SIP request
		if (_sipRequest != null) {
			try {
				SipServletResponse response = _sipRequest.createResponse(reasonCode, reason);
				response.send();
			} catch (UnsupportedEncodingException e) {
				if (c_logger.isErrorEnabled()){
					c_logger.error("error.exception.UnsupportedEncodingException",Situation.SITUATION_REQUEST_COMPLETED,null,e);        	
				}
			} catch (IOException e) {
				if (c_logger.isErrorEnabled()){
					c_logger.error("error.exception.io",Situation.SITUATION_REQUEST_COMPLETED,null,e);        	
				}
			}
		}
	}

}
