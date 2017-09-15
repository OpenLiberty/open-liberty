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
package com.ibm.ws.sib.mfp.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.control.ControlCreateStream;
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.schema.ControlAccess;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *  ControlCreateStreamImpl extends ControlMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the ControlCreateStream interface.
 */
public class ControlCreateStreamImpl extends ControlMessageImpl implements ControlCreateStream {

  private final static long serialVersionUID = 1L;

  private final static TraceComponent tc = SibTr.register(ControlCreateStreamImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /**
   *  Constructor for a new Control Create Stream Message.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  public ControlCreateStreamImpl() {
  }

  /**
   *  Constructor for a new Control Create Stream Message.
   *  To be called only by the ControlMessageFactory.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  public ControlCreateStreamImpl(int flag) throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");
    setControlMessageType(ControlMessageType.CREATESTREAM);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound message
   */
  public ControlCreateStreamImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo ");
  }

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#getRequestID()
   */
  public final long getRequestID() {
    return jmo.getLongField(ControlAccess.BODY_CREATESTREAM_REQUESTID);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#getDurableSubName()
   */
  public String getDurableSubName() {
    return (String) jmo.getField(ControlAccess.BODY_CREATESTREAM_SUBNAME);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#getDurableDiscriminator()
   */
  public final String getDurableDiscriminator() {
    return (String) jmo.getField(ControlAccess.BODY_CREATESTREAM_DISCRIMINATOR);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#getDurableSelector()
   */
  public final String getDurableSelector() {
    return (String) jmo.getField(ControlAccess.BODY_CREATESTREAM_SELECTOR);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#getDurableSelectorDomain()
   */
  public int getDurableSelectorDomain() {
    return jmo.getIntField(ControlAccess.BODY_CREATESTREAM_SELECTORDOMAIN);
  }

  /*
   *  Indicate whether the message with a SecurityUserid field was sent by
   *  a system user.
   *
   *  Javadoc description supplied by ControlCreateStream interface.
   */
  public final boolean isSecurityUseridSentBySystem() {
    return jmo.getBooleanField(ControlAccess.BODY_CREATESTREAM_SECURITYSENTBYSYSTEM);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#isNoLocal()
   */
  public final boolean isNoLocal() {
    boolean result = false;
    // if the value is unset then it has come from an environment with back level schema
    if (jmo.getChoiceField(ControlAccess.BODY_CREATESTREAM_NOLOCAL) != ControlAccess.IS_BODY_CREATESTREAM_NOLOCAL_UNSET) {
      result = jmo.getBooleanField(ControlAccess.BODY_CREATESTREAM_NOLOCAL_VALUE);
    }
    return result;
  }

  /* (non-Javadoc)
    * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#isCloned()
    */
   public final boolean isCloned() {
     boolean result = false;
     // if the value is unset then it has come from an environment with back level schema
     if (jmo.getChoiceField(ControlAccess.BODY_CREATESTREAM_CLONED) != ControlAccess.IS_BODY_CREATESTREAM_CLONED_UNSET) {
       result = jmo.getBooleanField(ControlAccess.BODY_CREATESTREAM_CLONED_VALUE);
     }
     return result;
   }

  /*
   *  Get the contents of the SecurityUserid field for the subscription.
   *
   *  Javadoc description supplied by ControlCreateStream interface.
   */
  public final String getSecurityUserid() {
    return (String)jmo.getField(ControlAccess.BODY_CREATESTREAM_SECURITYUSERID);
  }

  /*
   * Get summary trace line for this message 
   * 
   *  Javadoc description supplied by ControlMessage interface.
   */
  public void getTraceSummaryLine(StringBuilder buff) {
    
    // Get the common fields for control messages
    super.getTraceSummaryLine(buff);

    buff.append(",requestID=");
    buff.append(getRequestID());
    
    buff.append(",durableSubName=");
    buff.append(getDurableSubName());
    
    buff.append(",durableDiscriminator=");
    buff.append(getDurableDiscriminator());
    
    buff.append(",durableSelector=");
    buff.append(getDurableSelector());
    
    buff.append(",durableSelectorDomain=");
    buff.append(getDurableSelectorDomain());
    
    buff.append(",securityUserid=");
    buff.append(getSecurityUserid());
    
    buff.append(",securityUseridSentBySystem=");
    buff.append(isSecurityUseridSentBySystem());
    
    buff.append(",noLocal=");
    buff.append(isNoLocal());
    
    buff.append(",cloned=");
    buff.append(isCloned());
    
  }
    
  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#setRequestID(long)
   */
  public final void setRequestID(long value) {
    jmo.setLongField(ControlAccess.BODY_CREATESTREAM_REQUESTID, value);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#setDurableSubName(String)
   */
  public void setDurableSubName(String name) {
    jmo.setField(ControlAccess.BODY_CREATESTREAM_SUBNAME, name);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#setDurableDiscriminator(String)
   */
  public void setDurableDiscriminator(String name) {
    jmo.setField(ControlAccess.BODY_CREATESTREAM_DISCRIMINATOR, name);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#setDurableSelector(String)
   */
  public void setDurableSelector(String name) {
    jmo.setField(ControlAccess.BODY_CREATESTREAM_SELECTOR, name);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#setDurableSelectorDomain(int)
   */
  public void setDurableSelectorDomain(int domain) {
    jmo.setIntField(ControlAccess.BODY_CREATESTREAM_SELECTORDOMAIN, domain);
  }

  /*
   *  Set whether the message with a SecurityUserid field was sent by
   *  a system user.
   *
   *  Javadoc description supplied by ControlCreateStream interface.
   */
  public final void setSecurityUseridSentBySystem(boolean value) {
    jmo.setBooleanField(ControlAccess.BODY_CREATESTREAM_SECURITYSENTBYSYSTEM, value);
  }

  /*
   *  Set the contents of the SecurityUserid field for the subscription.
   *
   *  Javadoc description supplied by ControlCreateStream interface.
   */
  public final void setSecurityUserid(String value) {
    jmo.setField(ControlAccess.BODY_CREATESTREAM_SECURITYUSERID, value);
  }



  /* (non-Javadoc)
  * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#setNoLocal(boolean)
  */
 public final void setNoLocal(boolean value) {
   jmo.setBooleanField(ControlAccess.BODY_CREATESTREAM_NOLOCAL_VALUE, value);
 }

 /* (non-Javadoc)
  * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#setCloned(boolean)
  */
 public final void setCloned(boolean value) {
   jmo.setBooleanField(ControlAccess.BODY_CREATESTREAM_CLONED_VALUE, value);
 }


}
