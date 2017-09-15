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

import com.ibm.websphere.messaging.mbean.TopicMBean;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.admin.Controllable;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.mxbean.MessagingSubscription;
import com.ibm.ws.sib.admin.mxbean.QueuedMessage;
import com.ibm.ws.sib.admin.mxbean.QueuedMessageDetail;
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
import com.ibm.ws.sib.processor.exceptions.SIMPInvalidRuntimeIDException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.processor.runtime.SIMPLocalSubscriptionControllable;
import com.ibm.ws.sib.processor.runtime.SIMPLocalTopicSpaceControllable;
import com.ibm.ws.sib.processor.runtime.SIMPQueuedMessageControllable;
import com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;


public class TopicImpl implements TopicMBean, Controllable {

  private static final TraceComponent tc =
    SibTr.register(TopicImpl.class, JsConstants.TRGRP_AS, JsConstants.MSG_BUNDLE);
  
  
  private static final String CLASS_NAME = "com.ibm.ws.sib.admin.internal.TopicImpl";
  // Debugging aid
  static {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "com/ibm/ws/sib/admin/internal/TopicImpl.java");
  }

  // The name
  private String _name = null;

  // The instance of the actual controllable object
  private SIMPLocalTopicSpaceControllable _c = null;
  private JsMessagingEngineImpl _me = null;
//Properties of the MBean
 java.util.Properties props = new java.util.Properties();

  public java.util.Properties getProperties() {
    return (java.util.Properties) props.clone();
  }
  
  public TopicImpl(JsMessagingEngineImpl me, Controllable c) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, CLASS_NAME, new Object[] { me, c });

    _c = (SIMPLocalTopicSpaceControllable) c;
    _name = c.getName();
    _me = me;

    props.setProperty(((JsBusImpl) me.getBus()).getMBeanType(), ((JsBusImpl) me.getBus()).getName());
    props.setProperty(me.getMBeanType(), me.getName());
    props.setProperty("ID", _c.getId());

    if (_c.getConfigId() == null)
//      activateMBean(JsConstants.MBEAN_TYPE_PP, c.getName(), props);
//    else
//      activateMBean(JsConstants.MBEAN_TYPE_PP, c.getName(), props, _c.getConfigId());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, CLASS_NAME, this);
  }

  public TopicImpl(JsMessagingEngineImpl me, Controllable c, String name) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, CLASS_NAME, new Object[] { me, c, name });

    if (c == null)
      _c = (SIMPLocalTopicSpaceControllable) this;
    else
      _c = (SIMPLocalTopicSpaceControllable) c;

    _name = name;
    _me = me;

    java.util.Properties props = new java.util.Properties();
    props.setProperty(((JsBusImpl) me.getBus()).getMBeanType(), ((JsBusImpl) me.getBus()).getName());
    props.setProperty(me.getMBeanType(), me.getName());
    props.setProperty("UUID", _c.getId());

    if (_c.getConfigId() == null)
//      activateMBean(JsConstants.MBEAN_TYPE_PP, _name, props);
//    else
//      activateMBean(JsConstants.MBEAN_TYPE_PP, _name, props, _c.getConfigId());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, CLASS_NAME, this);
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

  public long getMaxQueueSize() {
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
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isSendAllowed");
    return _c.isSendAllowed();
  }

  public long getDepth() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDepth");
    long depth = _c.getNumberOfQueuedMessages();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getDepth", Long.valueOf(depth));
    return depth;
  }

  // OPERATIONS

  public MessagingSubscription[] listSubscriptions() throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getSubscriptions");

    List list = new ArrayList();
    Iterator iter = _c.getTopicSpace().getLocalSubscriptionIterator();
    while (iter != null && iter.hasNext()) {
      SIMPLocalSubscriptionControllable o = (SIMPLocalSubscriptionControllable) iter.next();
      list.add(o);
    }

    MessagingSubscription[] retValue = new MessagingSubscription[list.size()];

    iter = list.iterator();
    int i = 0;
    while (iter.hasNext()) {
      Object o = iter.next();
      retValue[i++] = SIBMBeanResultFactory.createSIBSubscription((SIMPLocalSubscriptionControllable) o);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getSubscriptions");
    return retValue;
  }

  public MessagingSubscription getSubscription(String subId) throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getSubscription", subId);
    try {
      SIMPLocalSubscriptionControllable qpc = _c.getTopicSpace().getLocalSubscriptionControlByID(subId);
      if (qpc != null)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "getSubscription", qpc);
        return SIBMBeanResultFactory.createSIBSubscription(qpc);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getSubscription", null);
      return null;
    }
    catch (SIMPException e) {
      // No FFDC code needed
      SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getSubscription", e);
      throw new Exception(e.getMessage());
    }
  }

  public MessagingSubscription getSubscriptionByName(String subName) throws Exception {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	      SibTr.entry(tc, "getSubscriptionByName", subName);

	    try {
	    SIMPLocalSubscriptionControllable qpc = _c.getTopicSpace().getLocalSubscriptionControlByName(subName);
	      if (qpc != null)
	      {
	        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	          SibTr.exit(tc, "getSubscriptionByName", qpc);
          return SIBMBeanResultFactory.createSIBSubscription(qpc);
	      }
	
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	        SibTr.exit(tc, "getSubscriptionByName", null);
	      return null;
	    }
	    catch (SIMPException e) {
	      // No FFDC code needed
	      SibTr.exception(tc, e);
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	        SibTr.exit(tc, "getSubscriptionByName", e);
	      throw new Exception(e.getMessage());
	    }	
	  }

  public void deleteSubscription(String subId)
    throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deleteSubscription", subId);
    try {
      _c.getTopicSpace().deleteLocalSubscriptionControlByID(subId);
    }
    catch (SIDurableSubscriptionNotFoundException e) {
      // No FFDC code needed
      SibTr.exception(tc, e);
      throw new Exception(e.getMessage());
    }
    catch (SIMPInvalidRuntimeIDException e) {
      // No FFDC code needed
      SibTr.exception(tc, e);
      throw new Exception(e.getMessage());
    }
    catch (SIIncorrectCallException e) {
      // No FFDC code needed
      SibTr.exception(tc, e);
      throw new Exception(e.getMessage());
    }
    catch (SIResourceException e) {
      // No FFDC code needed
      SibTr.exception(tc, e);
      throw new Exception(e.getMessage());
    }
    catch (SIMPControllableNotFoundException e) {
      // No FFDC code needed
      SibTr.exception(tc, e);
      throw new Exception(e.getMessage());
    }
    catch (SIDestinationLockedException e) {
      // No FFDC code needed
      SibTr.exception(tc, e);
      throw new Exception(e.getMessage());
    }
    catch (SIMPException e) {
      // No FFDC code needed
      SibTr.exception(tc, e);
      throw new Exception(e.getMessage());
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteSubscription");
  }

  public QueuedMessage[] getSubscriptionMessages(String subId) throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getSubscriptionMessages", subId);
    QueuedMessage[] retValue = null;;
    List list = new ArrayList();
    SIMPLocalSubscriptionControllable lsc = null;
    try {
      lsc = _c.getTopicSpace().getLocalSubscriptionControlByID(subId);
    }
    catch (SIMPException e) {
      // No FFDC code needed
      SibTr.exception(tc, e);
      throw new Exception(e.getMessage());
    }

    if (lsc != null) {
      Iterator iter = lsc.getQueuedMessageIterator();
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

      retValue = (QueuedMessage[])resultList.toArray(new QueuedMessage[0]);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getSubscriptionMessages", retValue);
    return retValue;
  }

  public QueuedMessage getSubscriptionMessage(String subId, String messageId) throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getSubscriptionMessage", subId + " " + messageId);
    SIMPLocalSubscriptionControllable lsc = null;
    SIMPQueuedMessageControllable qmc = null;
    QueuedMessage qm = null;
    try {
      lsc = _c.getTopicSpace().getLocalSubscriptionControlByID(subId);
      if (lsc != null) qmc = lsc.getQueuedMessageByID(messageId);
      if (qmc != null) {
        QueuedMessage qmsg = SIBMBeanResultFactory.createSIBQueuedMessage(qmc);
        qm = qmsg;
      }
    }
    catch (SIMPControllableNotFoundException e) {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Message no longer on subscription point", messageId);
    }
    catch (SIMPException e) {
      // No FFDC code needed
      SibTr.exception(tc, e);
      throw new Exception(e.getMessage());
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getSubscriptionMessage", qm);
    return qm;
  }

  public QueuedMessageDetail getSubscriptionMessageDetail(String subId, String messageId) throws Exception {
    return getSubscriptionMessageDetail(subId, messageId, null);
  }


  public QueuedMessageDetail getSubscriptionMessageDetail(String subId, String messageId, Locale locale) throws Exception
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getSubscriptionMessageDetail", new Object[] { subId,
          messageId, locale });

    SIMPLocalSubscriptionControllable lsc = null;
    SIMPQueuedMessageControllable qmc = null;
    QueuedMessageDetail qmd = null;
    try {
      lsc = _c.getTopicSpace().getLocalSubscriptionControlByID(subId);
      if (lsc != null) qmc = lsc.getQueuedMessageByID(messageId);
      if (qmc != null)
      {
        QueuedMessageDetail qmsgd = SIBMBeanResultFactory.createSIBQueuedMessageDetail(qmc, locale);
        qmd = qmsgd;
      }
    }
    catch (SIMPControllableNotFoundException e) {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Message no longer on subscription point", messageId);
    }
    catch (SIMPException e) {
      // No FFDC code needed
      SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getSubscriptionMessageDetail", e);
      throw new Exception(e.getMessage());
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getSubscriptionMessageDetail", qmd);
    return qmd;
  }

  public byte[] getSubscriptionMessageData(String subId, String messageId, java.lang.Integer size) throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getSubscriptionMessageData", subId + " " + messageId + " " + size.toString());
    SIMPLocalSubscriptionControllable lsc = null;
    SIMPQueuedMessageControllable qmc = null;
    try {
      lsc = _c.getTopicSpace().getLocalSubscriptionControlByID(subId);
      if (lsc != null) qmc = lsc.getQueuedMessageByID(messageId);
    }
    catch (SIMPControllableNotFoundException e) {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Message no longer on subscription point", messageId);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getSubscriptionMessageData");
      return null;
    }
    catch (SIMPException e) {
      // No FFDC code needed
      SibTr.exception(tc, e);
      throw new Exception(e.getMessage());
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getSubscriptionMessageData");
    return getMessageData(qmc, size);
  }

  public void deleteSubscriptionMessage(String subId, String messageId, java.lang.Boolean moveMessage)
    throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deleteSubscriptionMessage", messageId);
    boolean discard = true;
    SIMPLocalSubscriptionControllable lsc = null;
    SIMPQueuedMessageControllable qmc = null;
    if (moveMessage.booleanValue() == true)
      discard = false;
    try {
      lsc = _c.getTopicSpace().getLocalSubscriptionControlByID(subId);
      if (lsc != null) qmc = lsc.getQueuedMessageByID(messageId);
      if (qmc != null) qmc.moveMessage(discard);
    }
    catch (SIMPControllableNotFoundException e) {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Message no longer on subscription point", messageId);
    }
    catch (SIMPRuntimeOperationFailedException e)
    {
      // No FFDC code needed
      SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(
          tc,
          "deleteSubscriptionMessage",
          "SIBRuntimeOperationFailedException");
      throw new Exception(e.getMessage());
    }
    catch (SIMPException e) {
      // No FFDC code needed
      SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "deleteSubscriptionMessage", e);
      throw new Exception(e.getMessage());
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteSubscriptionMessage");
  }  
  
  /////////  Methods from JsMessagePoint
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
	        throw e;
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
	        throw e;
	      }
	    }
	    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	      SibTr.exit(tc, "getMessageData");
	    return data;
	  }

	  private byte[] getMessageData(JsMessage jm, java.lang.Integer size) throws Exception {
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
	            throw new Exception(e.getMessage());
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
	            throw new Exception(ofse.getMessage());
	          }
	        }
	        else if (m instanceof JsJmsMapMessage) {
	          JsJmsMapMessage msg = (JsJmsMapMessage) m;
	          try {
	            data = getData(msg.getUserFriendlyBytes(), size);
	          }
	          catch (UnsupportedEncodingException e) {
	            // No FFDC code needed
	            throw new Exception(e.getMessage());
	          }
	        }
	        else if (m instanceof JsJmsStreamMessage) {
	          JsJmsStreamMessage msg = (JsJmsStreamMessage) m;
	          try {
	            data = getData(msg.getUserFriendlyBytes(), size);
	          }
	          catch (UnsupportedEncodingException e) {
	            // No FFDC code needed
	            throw new Exception(e.getMessage());
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

