/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.utils;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.ws.sib.admin.JsAdminUtils;
import com.ibm.ws.sib.mfp.JsApiMessage;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SITransaction;

/**
 * @author gatfora
 *
 * Class that is used to register with the SIBMessageTrace trace group.
 */
public class UserTrace
{
  //trace for messages
  public static final TraceComponent tc_mt =
    SibTr.register(
      UserTrace.class,
      TraceGroups.TRGRP_MESSAGETRACE,
      SIMPConstants.TRACE_MESSAGE_RESOURCE_BUNDLE);

  // NLS for component
  public static final TraceNLS nls_mt =
    TraceNLS.getTraceNLS(SIMPConstants.TRACE_MESSAGE_RESOURCE_BUNDLE);

  public static final void trace_Receive(
    SITransaction siTran,
    JsMessage message,
    SIDestinationAddress destAddr,
    long id)
  {
    if (message != null)
    {
      if (message.isApiMessage())
      {
        String apiMsgId = null;
        String correlationId = null;
  
        if (message instanceof JsApiMessage)
        {      
          apiMsgId = ((JsApiMessage) message).getApiMessageId();
          correlationId = ((JsApiMessage)message).getCorrelationId();        
        }
        else
        {
          if (message.getApiMessageIdAsBytes() != null)
            apiMsgId = new String(message.getApiMessageIdAsBytes());
          
          if (message.getCorrelationIdAsBytes() != null)
            correlationId = new String(message.getCorrelationIdAsBytes());
        }
  
        if (siTran != null)
        {      
          if (tc_mt.isDebugEnabled())
          {        
            SibTr.debug(
              tc_mt,
              nls_mt.getFormattedMessage(
                "CONSUMER_RECEIVE_CWSJU0040",
                new Object[] {
                  apiMsgId,
                  message.getSystemMessageId(),
                  correlationId,
                  new Long(id),
                  destAddr.getDestinationName(),
                  ((TransactionCommon) siTran).getPersistentTranId()},
                null));              
          }
        }
        else if (tc_mt.isDebugEnabled())
        {           
          SibTr.debug(
            tc_mt,
            nls_mt.getFormattedMessage(
              "CONSUMER_RECEIVE_NO_TRAN_CWSJU0041",
              new Object[] {
                apiMsgId,
                message.getSystemMessageId(),
                correlationId,
                new Long(id),
                destAddr.getDestinationName()},
              null));
        }
      }
    }
    else
    {
      if (siTran != null)
      {
        if (tc_mt.isDebugEnabled())
          SibTr.debug(
            tc_mt,
            nls_mt.getFormattedMessage(
              "CONSUMER_RECEIVE_NO_MESSAGE_CWSJU0042",
              new Object[] {
                new Long(id),
                destAddr.getDestinationName(),
                ((TransactionCommon) siTran).getPersistentTranId()},
              null));
      }
      else if (tc_mt.isDebugEnabled())
      {      
        SibTr.debug(
          tc_mt,
          nls_mt.getFormattedMessage(
            "CONSUMER_RECEIVE_NO_MESSAGE_NO_TRAN_CWSJU0043",
            new Object[] { new Long(id), destAddr.getDestinationName()},
            null));
      }

    }
  }
  
  /**
   * Traces a send message from this ME to the remote ME
   * 
   * @param jsMsg
   * @param targetMEUuid
   * @param destName
   */
  public static final void traceOutboundSend(JsMessage jsMsg,
                                             SIBUuid8 targetMEUuid, 
                                             String destName,
                                             boolean foreignBus,
                                             boolean mqLink,
                                             boolean temporary)
  {
    if (jsMsg.isApiMessage())
    {
      String apiMsgId = null;
      String correlationId = null;
  
      String msg = "OUTBOUND_MESSAGE_SENT_CWSJU0021";
      
      if (foreignBus)
        msg = "OUTBOUND_MESSAGE_SENT_CWSJU0023";
      else if (mqLink)
        msg = "OUTBOUND_MESSAGE_SENT_CWSJU0024";
      else if (temporary)
        msg = "OUTBOUND_MESSAGE_SENT_TEMP_CWSJU0121";
      
      if (jsMsg instanceof JsApiMessage)
      {
        apiMsgId = ((JsApiMessage) jsMsg).getApiMessageId();
        correlationId = ((JsApiMessage) jsMsg).getCorrelationId();
      }
      else
      {
        if (jsMsg.getApiMessageIdAsBytes() != null) 
          apiMsgId = new String(jsMsg.getApiMessageIdAsBytes());
  
        if (jsMsg.getCorrelationIdAsBytes() != null) 
          correlationId = new String(jsMsg.getCorrelationIdAsBytes());
      }
      
      String meName = null;
      // If we are not sending to a foreign bus then get hold of the ME Name,
      //  if it is a foreign bus then the likelyhood is that we can't obtain the
      //  name anyway so just use the uuid. Calling getMENameByUuidx has significant
      //  delays if the ME Name can't be found (1 minute plus)
      if (!foreignBus)
      {
        meName = JsAdminUtils.getMENameByUuidForMessage(targetMEUuid.toString());
      }
      
      if (meName == null)
        meName = targetMEUuid.toString();
      
      if (tc_mt.isDebugEnabled())
        SibTr.debug(UserTrace.tc_mt, nls_mt.getFormattedMessage(
            msg, new Object[]
            { apiMsgId, jsMsg.getSystemMessageId(), correlationId, meName,
                destName }, null));
    }
  }

  /**
   * Method used when forwarding a message to another ME/Bus
   * @param jsMsg
   * @param targetMEUuid
   * @param destName
   */
  public static final void forwardJSMessage(JsMessage jsMsg,
                                            SIBUuid8 targetMEUuid, 
                                            String destName,
                                            boolean temporary)
  {
    if (jsMsg.isApiMessage())
    {
      String apiMsgId = null;
      String correlationId = null;
      
      String msg = "MESSAGE_FORWARDED_CWSJU0022";
      if (temporary)
        msg = "MESSAGE_FORWARDED_TEMP_CWSJU0122";
      
      if (jsMsg instanceof JsApiMessage)
      {
        apiMsgId = ((JsApiMessage) jsMsg).getApiMessageId();
        correlationId = ((JsApiMessage) jsMsg).getCorrelationId();
      }
      else
      {
        if (jsMsg.getApiMessageIdAsBytes() != null) 
          apiMsgId = new String(jsMsg.getApiMessageIdAsBytes());
        
        if (jsMsg.getCorrelationIdAsBytes() != null) 
          correlationId = new String(jsMsg.getCorrelationIdAsBytes());
      }
      
      //  As we are forwarding to a foreign bus then the likelyhood is that we can't obtain the
      //  name anyway so just use the uuid. Calling getMENameByUuidx has significant
      //  delays if the ME Name can't be found (1 minute plus)
      String meName = targetMEUuid.toString();
      
      if (tc_mt.isDebugEnabled())
        SibTr.debug(UserTrace.tc_mt, nls_mt.getFormattedMessage(
            msg, new Object[]
             { apiMsgId, jsMsg.getSystemMessageId(), correlationId, meName,
                destName }, null));
      }
  }

}
