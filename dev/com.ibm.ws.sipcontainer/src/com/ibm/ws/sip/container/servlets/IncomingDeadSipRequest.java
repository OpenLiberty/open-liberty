/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.servlets;

import javax.servlet.sip.SipServletResponse;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;


/**
 * @author Anat Fradin, July 27, 2008
 * Dead Implementation for the Sip Servlet Request API.
 * This class is used instead of cloning IncomingSipServletRequest in
 * case when we need a partial copy of IncomingSipServletRequest.
 * This request doesn't contain ServerTransaction for example and
 * response created on this response sent on different way than each
 * usual response.
 * 
 * usage:
 * Derived Session in B2B mode for pending messages.
 *  
 * @see javax.servlet.sip.SipServletRequest
 * 
 * This request
 */
public class IncomingDeadSipRequest extends IncomingSipServletRequest {

	/**
   * Class Logger. 
   */
   private static final LogMgr c_logger =
       Log.get(IncomingDeadSipRequest.class);

 
   public IncomingDeadSipRequest(IncomingSipServletRequest request,
  		 														TransactionUserWrapper tu){
  	 super(request);
  	 setTransactionUser(tu);
  	 _isInternallyGenerated = true;
   }
   
   
   /**
 	 * @see javax.servlet.sip.SipServletRequest#createResponse(int)
 	 */
     public SipServletResponse createResponse(int statusCode)
     {
         return createResponseForCommitedRequest(statusCode, null);
     }

     /**
      * @see javax.servlet.sip.SipServletRequest#createResponse(int, java.lang.String)
      */
     public SipServletResponse createResponse(
         int statusCode,
         String reasonPhrase)
     {
         return createResponseForCommitedRequest(statusCode, reasonPhrase);
     }
     
     
   /**
    * 
    * Create a response for committed request .
    * Used in case when additional response received
    * on UAC in B2B mode and response should
    * be sent on the UAS leg (after UAS leg
    * was already CONFIRMED)
    * 
    * @param statusCode the status of the response
    * @param reasonPhrase the response phrase
    * @return the new response
    */
   public OutgoingSipServletResponse createResponseForCommitedRequest(
										int statusCode, 
										String reasonPhrase) {
		if (c_logger.isTraceDebugEnabled()) {
			Object[] params = { statusCode, reasonPhrase };
			c_logger.traceEntry(this, "createResponseForCommitedRequest", params);
		}

		if (!isLiveMessage("createResponse"))
			return null;

	//	If it is a final response mark the request as committed. 
	    if (statusCode >= 200)
	    {
	        setIsCommited(true);
	    }
	    
	    String toTag = getRequest().getToHeader().getTag();
	    
		OutgoingSipServletResponse response = createOutgoingResponse(	statusCode,
																		reasonPhrase, 
																		toTag,
																		null);
		
		response.setShouldBeSentWithoutST(true);

		return response;
	}
}
