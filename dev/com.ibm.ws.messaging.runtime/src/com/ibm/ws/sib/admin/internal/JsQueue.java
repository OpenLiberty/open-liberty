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

package com.ibm.ws.sib.admin.internal;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import com.ibm.websphere.messaging.mbean.QueueMBean;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIDataGraphSchemaNotFoundException;
import com.ibm.websphere.sib.exception.SIMessageException;
import com.ibm.ws.sib.admin.Controllable;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.mxbean.QueuedMessage;
import com.ibm.ws.sib.admin.mxbean.QueuedMessageDetail;
import com.ibm.ws.sib.mfp.IncorrectMessageTypeException;
import com.ibm.ws.sib.mfp.JsJmsBytesMessage;
import com.ibm.ws.sib.mfp.JsJmsMapMessage;
import com.ibm.ws.sib.mfp.JsJmsMessage;
import com.ibm.ws.sib.mfp.JsJmsObjectMessage;
import com.ibm.ws.sib.mfp.JsJmsStreamMessage;
import com.ibm.ws.sib.mfp.JsJmsTextMessage;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageType;
import com.ibm.ws.sib.mfp.ObjectFailedToSerializeException;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.processor.runtime.SIMPLocalQueuePointControllable;
import com.ibm.ws.sib.processor.runtime.SIMPQueuedMessageControllable;
import com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable;
import com.ibm.ws.sib.utils.ras.SibTr;


public class JsQueue implements QueueMBean, Controllable {

  
  

  private static final String CLASS_NAME = "com.ibm.ws.sib.admin.internal.JsQueue";
  private static final TraceComponent tc =
    SibTr.register(JsQueue.class, JsConstants.TRGRP_AS, JsConstants.MSG_BUNDLE);
  

  
  private static String MBEAN_TYPE=JsConstants.MBEAN_TYPE_QP;

  static {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "Source info: com/ibm/ws/sib/admin/internal/JsQueue.java");
  }

  // The name
  private String _name = null;

  // The instance of the actual controllable object
  private SIMPLocalQueuePointControllable _c = null;
  private JsMessagingEngineImpl _me = null;
//Properties of the MBean
 java.util.Properties props = new java.util.Properties();
  
  public JsQueue(JsMessagingEngineImpl me, Controllable c) {
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, CLASS_NAME + "().<init>");

    _c = (SIMPLocalQueuePointControllable) c;
    _name = c.getName();
    _me = me;
    props.setProperty(((JsBusImpl) me.getBus()).getMBeanType(), ((JsBusImpl) me.getBus()).getName());
    props.setProperty(me.getMBeanType(), me.getName());
    props.setProperty("Name", _c.getName());
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, CLASS_NAME + "().<init>");
  }

  // ATTRIBUTES

  public String getUuid() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getUuid");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getUuid", _c.getUuid());
    return _c.getUuid();
  }

  public String getName() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getName");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getName", _name);
    return _name;
  }

  public String getIdentifier() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getIdentifier");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getIdentifier", _name);
    return _name;
  }

  public String getId() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getId");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getId", _c.getId());
    return _c.getId();
  }

  public String getState() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getState");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getState", _c.getMessageHandler().getState());
    return _c.getMessageHandler().getState();
  }

  public long getDepth() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDepth");
    long depth = _c.getNumberOfQueuedMessages();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
      Long l = new Long(depth);
      SibTr.exit(tc, "getDepth", l);
    }
    return depth;
  }

  public long getMaxQueueDepth() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getHighMessageThreshold");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
      Long l = new Long(_c.getDestinationHighMsgs());
      SibTr.exit(tc, "getHighMessageThreshold", l);
    }
    return _c.getDestinationHighMsgs();
  }

  public boolean isSendAllowed() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isSendAllowed");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
      Boolean b = new Boolean(_c.isSendAllowed());
      SibTr.exit(tc, "isSendAllowed", b);
    }
    return _c.isSendAllowed();
  }

  // OPERATIONS

  public QueuedMessage[] listQueuedMessages() throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "listQueuedMessages");

    List list = new ArrayList();

    Iterator iter = _c.getQueuedMessageIterator();
    while (iter != null && iter.hasNext()) {
      SIMPQueuedMessageControllable o = (SIMPQueuedMessageControllable) iter.next();
      list.add(o);
    }

    List resultList = new ArrayList();
    iter = list.iterator();
    int i = 0;
    while (iter.hasNext()) {
      Object o = iter.next();
      QueuedMessage qm = SIBMBeanResultFactory.createSIBQueuedMessage((SIMPQueuedMessageControllable) o);
      resultList.add(qm);
    }

    QueuedMessage[] retValue = (QueuedMessage[])resultList.toArray(new QueuedMessage[0]);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "listQueuedMessages", retValue);
    return retValue;
  }

  public QueuedMessage getQueuedMessage(String messageId) throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getQueuedMessage", messageId);

    if (messageId == null) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getQueuedMessage", null);
      return null;
    }

    QueuedMessage retValue = null;

    try {
      SIMPQueuedMessageControllable qmc = _c.getQueuedMessageByID(messageId);
      QueuedMessage qm = SIBMBeanResultFactory.createSIBQueuedMessage(qmc);
      retValue = qm;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getQueuedMessage", retValue);
      return retValue;
    }
    catch (SIMPControllableNotFoundException e) {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Message no longer on queue point", messageId);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getQueuedMessage", null);
      return null;
    }
    catch (SIMPException e) {
      // No FFDC code needed
      SibTr.exception(tc, e);
      throw new Exception(e.getMessage());
    }
  }

  public QueuedMessageDetail getQueuedMessageDetail(String messageId) throws Exception {
    return getQueuedMessageDetail(messageId, null);
  }

  public QueuedMessageDetail getQueuedMessageDetail(String messageId, Locale locale) throws Exception
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getQueuedMessageDetail", new Object[] { messageId,
          locale });

    if (messageId == null) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getQueuedMessageDetail", null);
      return null;
    }

    QueuedMessageDetail retValue = null;

    try {
      SIMPQueuedMessageControllable qmc = _c.getQueuedMessageByID(messageId);
      QueuedMessageDetail qmd = SIBMBeanResultFactory.createSIBQueuedMessageDetail(qmc, locale);
      retValue = qmd;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getQueuedMessageDetail", retValue);
      return retValue;
    }
    catch (SIMPControllableNotFoundException e) {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Message no longer on queue point", messageId);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getQueuedMessageDetail", null);
      return null;
    }
    catch (SIMPException e) {
      // No FFDC code needed
      SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getQueuedMessageDetail", e);
      throw new Exception(e.getMessage());
    }
  }

  public byte[] getMessageData(String messageId, java.lang.Integer size) throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getMessageData", new Object[] {messageId, size});

    if (messageId == null || size == null) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getMessageData", null);
      return null;
    }

    byte[] retValue = null;

    SIMPQueuedMessageControllable qmc = null;
    try {
      qmc = _c.getQueuedMessageByID(messageId);
    }
    catch (SIMPControllableNotFoundException e) {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Message no longer on queue point", messageId);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getMessageData", null);
      return null;
    }
    catch (SIMPException e) {
      // No FFDC code needed
      SibTr.exception(tc, e);
      throw new Exception(e.getMessage());
    }
    retValue = getMessageData(qmc, size);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMessageData", retValue);
    return retValue;
  }

  public void deleteQueuedMessage(String messageId, java.lang.Boolean moveMessage) throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deleteQueuedMessage", new Object[] {messageId, moveMessage});

    if (messageId == null) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "deleteQueuedMessage");
      return;
    }

    boolean discard = true;
    if (moveMessage == null || moveMessage.booleanValue())
      discard = false;
    try {
      SIMPQueuedMessageControllable qmc = _c.getQueuedMessageByID(messageId);
      if (qmc != null) {
        qmc.moveMessage(discard);
      }
    }
    catch (SIMPControllableNotFoundException e) {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Message no longer on queue point", messageId);
    }
    catch (SIMPRuntimeOperationFailedException e)
    {
      // No FFDC code needed
      SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(
          tc,
          "deleteQueuedMessage",
          "SIBRuntimeOperationFailedException");
      throw new Exception(e.getMessage());
    }
    catch (SIMPException e) {
      // No FFDC code needed
      SibTr.exception(tc, e);
      throw new Exception(e.getMessage());
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteQueuedMessage");
  }

  public void deleteAllQueuedMessages(java.lang.Boolean moveMessage) throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deleteAllQueuedMessages", new Object[] {moveMessage});

    boolean discard = true;

    if (moveMessage == null || moveMessage.booleanValue()) {
      discard = false;
    }

    try {
      _c.moveMessages(discard);
    }
    catch (SIMPException e) {
      // No FFDC code needed
      SibTr.exception(tc, e);
      throw new Exception(e.getMessage());
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteAllQueuedMessages");
  }

  // 673411 -start
  public QueuedMessage[] getQueuedMessages(java.lang.Integer fromIndexInteger,java.lang.Integer toIndexInteger,java.lang.Integer totalMessagesPerpageInteger) throws Exception {
  
  int fromIndex=fromIndexInteger.intValue();
  int toIndex=toIndexInteger.intValue();
  int totalMessagesPerpage=totalMessagesPerpageInteger.intValue();
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getQueuedMessages  fromIndex="+fromIndex+" toIndex= "+toIndex+" totalMsgs= "+totalMessagesPerpage);

    List list = new ArrayList();

    Iterator iter = _c.getQueuedMessageIterator(fromIndex,toIndex,totalMessagesPerpage);//673411
	
    while (iter != null && iter.hasNext()) {
      SIMPQueuedMessageControllable o = (SIMPQueuedMessageControllable) iter.next();
      list.add(o);
    }

    List resultList = new ArrayList();
    iter = list.iterator();
    int i = 0;
    while (iter.hasNext()) {
      Object o = iter.next();
      QueuedMessage qm = SIBMBeanResultFactory.createSIBQueuedMessage((SIMPQueuedMessageControllable) o);
      resultList.add(qm);
    }

    QueuedMessage[] retValue = (QueuedMessage[])resultList.toArray(new QueuedMessage[0]);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getQueuedMessagesfromIndex="+fromIndex+" toIndex= "+toIndex+" totalMsgs= "+totalMessagesPerpage, retValue);
    return retValue;
  }
  // 673411-end
  //---------------- Properties from JsmessagePoint
  public java.util.Properties getProperties() {
	    return (java.util.Properties) props.clone();
	  }

	  private byte[] getData(byte[] in, java.lang.Integer size) {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	      SibTr.entry(tc, "getData", new Object[] { in, size });
	    byte tmp[] = null;
	    if (in != null) {
	      int len = 1024;
	      if (size.intValue() > 0)
	        len = size.intValue();
	      if (len > in.length)
	        len = in.length;
	      tmp = new byte[len];
	      System.arraycopy(in, 0, tmp, 0, len);
	    }
	    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	      SibTr.exit(tc, "getData", tmp);
	    return tmp;
	  }

	  public byte[] getMessageData(SIMPQueuedMessageControllable qmc, java.lang.Integer size) throws Exception {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	      SibTr.entry(tc, "getMessageData", size.toString());

	    byte[] data = null;

	    if (qmc != null) {
	      try {
	        JsMessage jm = qmc.getJsMessage();
	        data = getMessageData(jm, size);
	      }
	      catch (SIMPException e) {
	        // No FFDC code needed
	        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	          SibTr.exit(tc, "getMessageData", e);
	        throw new Exception(e.getMessage());
	      }
	    }
	    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	      SibTr.exit(tc, "getMessageData");
	    return data;
	  }

	  public byte[] getMessageData(SIMPRemoteMessageControllable rmc, java.lang.Integer size) throws Exception {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	      SibTr.entry(tc, "getMessageData", size.toString());

	    byte[] data = null;

	    if (rmc != null) {
	      try {
	        JsMessage jm = rmc.getJsMessage();
	        data = getMessageData(jm, size);
	      }
	      catch (SIMPException e) {
	        // No FFDC code needed
	        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	          SibTr.exit(tc, "getMessageData", e);
	        throw new Exception(e.getMessage());
	      }
	    }
	    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	      SibTr.exit(tc, "getMessageData");
	    return data;
	  }

	  private byte[] getMessageData(JsMessage jm, java.lang.Integer size) throws IncorrectMessageTypeException, SIDataGraphSchemaNotFoundException, SIMessageException {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	      SibTr.entry(tc, "getMessageData", new Object[] { jm, size });

	    byte data[] = null;

	    MessageType mt = jm.getJsMessageType();

	    if (mt == MessageType.JMS) {

	      JsJmsMessage m = jm.makeInboundJmsMessage();

	        if (m instanceof JsJmsTextMessage) {
	          JsJmsTextMessage msg = (JsJmsTextMessage) m;
	          String text = null;
	          try {
	            text = msg.getText();
	          }
	          catch (UnsupportedEncodingException e) {
	            // No FFDC code needed
	          }
	          // Check that there is a message body to call getBytes on.
	          if (text != null) {
	            data = getData(text.getBytes(), size);
	          }
	        }
	        else if (m instanceof JsJmsBytesMessage) {
	          JsJmsBytesMessage msg = (JsJmsBytesMessage) m;
	          data = getData(msg.getBytes(), size);
	        }
	        else if (m instanceof JsJmsObjectMessage) {
	          JsJmsObjectMessage msg = (JsJmsObjectMessage) m;
	          try
	          {
	        	  data = getData(msg.getSerializedObject(), size);
	          }
	          catch(ObjectFailedToSerializeException ofse)
	          {
	              // No FFDC code needed
	        	  data = null;
	          }
	        }
	        else if (m instanceof JsJmsMapMessage) {
	          JsJmsMapMessage msg = (JsJmsMapMessage) m;
	          try {
	            data = getData(msg.getUserFriendlyBytes(), size);
	          }
	          catch (UnsupportedEncodingException e) {
	            // No FFDC code needed
	          }
	        }
	        else if (m instanceof JsJmsStreamMessage) {
	          JsJmsStreamMessage msg = (JsJmsStreamMessage) m;
	          try {
	            data = getData(msg.getUserFriendlyBytes(), size);
	          }
	          catch (UnsupportedEncodingException e) {
	            // No FFDC code needed
	          }
	        }
	        else {
	        }
	    }
	    

	    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	      SibTr.exit(tc, "getMessageData", data);
	    return data;
	  }

	@Override
	public String getConfigId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRemoteEngineUuid() {
		// TODO Auto-generated method stub
		return null;
	}
 
  
}

