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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.control.ControlCreateDurable;
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.schema.ControlAccess;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *  ControlCreateDurableImpl extends ControlMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the ControlCreateDurable interface.
 */
public class ControlCreateDurableImpl extends ControlMessageImpl implements ControlCreateDurable {

  private final static long serialVersionUID = 1L;

  private final static TraceComponent tc = SibTr.register(ControlCreateDurableImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /**
   *  Constructor for a new Control Create Durable Message.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  public ControlCreateDurableImpl() {
  }

  /**
   *  Constructor for a new Control Create Durable Message.
   *  To be called only by the ControlMessageFactory.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  public ControlCreateDurableImpl(int flag) throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");
    setControlMessageType(ControlMessageType.CREATEDURABLE);
    setDurableSelectorNamespaceMap(null);  // Must default this field as it is not always used
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound message
   */
  public ControlCreateDurableImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo ");
  }

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateDurable#getRequestID()
   */
  public final long getRequestID() {
    return jmo.getLongField(ControlAccess.BODY_CREATEDURABLE_REQUESTID);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateDurable#getDurableSubName()
   */
  public final String getDurableSubName() {
    return (String) jmo.getField(ControlAccess.BODY_CREATEDURABLE_SUBNAME);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateDurable#getDurableDiscriminator()
   */
  public final String getDurableDiscriminator() {
    return (String) jmo.getField(ControlAccess.BODY_CREATEDURABLE_DISCRIMINATOR);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateDurable#getDurableSelector()
   */
  public final String getDurableSelector() {
    return (String) jmo.getField(ControlAccess.BODY_CREATEDURABLE_SELECTOR);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateDurable#getDurableSelectorDomain()
   */
  public final int getDurableSelectorDomain() {
    return jmo.getIntField(ControlAccess.BODY_CREATEDURABLE_SELECTORDOMAIN);
  }

  /*
   *  Get the contents of the SecurityUserid field for the subscription.
   *
   *  Javadoc description supplied by ControlCreateDurable interface.
   */
  public final String getSecurityUserid() {
    return (String)jmo.getField(ControlAccess.BODY_CREATEDURABLE_SECURITYUSERID);
  }

  /*
   *  Indicate whether the message with a SecurityUserid field was sent by
   *  a system user.
   *
   *  Javadoc description supplied by ControlCreateDurable interface.
   */
  public final boolean isSecurityUseridSentBySystem() {
    return jmo.getBooleanField(ControlAccess.BODY_CREATEDURABLE_SECURITYSENTBYSYSTEM);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateDurable#isNoLocal()
   */
  public final boolean isNoLocal() {
    boolean result = false;
    // if the value is unset then it has come from an environment with back level schema
    if (jmo.getChoiceField(ControlAccess.BODY_CREATEDURABLE_NOLOCAL) != ControlAccess.IS_BODY_CREATEDURABLE_NOLOCAL_UNSET) {
      result = jmo.getBooleanField(ControlAccess.BODY_CREATEDURABLE_NOLOCAL_VALUE);
    }
    return result;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateDurable#isCloned()
   */
  public final boolean isCloned() {
    boolean result = false;
    // if the value is unset then it has come from an environment with back level schema
    if (jmo.getChoiceField(ControlAccess.BODY_CREATEDURABLE_CLONED) != ControlAccess.IS_BODY_CREATEDURABLE_CLONED_UNSET) {
      result = jmo.getBooleanField(ControlAccess.BODY_CREATEDURABLE_CLONED_VALUE);
    }
    return result;
  }

  /**
   * Get the map of prefixes to namespace URLs that are associated with the selector.
   *
   * @return the map of namespace prefixes
   */
  public Map<String,String> getDurableSelectorNamespaceMap() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getDurableSelectorNamespaceMap");
    Map<String,String> map = null;
    if (jmo.getChoiceField(ControlAccess.BODY_CREATEDURABLE_NAMESPACEMAP) == ControlAccess.IS_BODY_CREATEDURABLE_NAMESPACEMAP_MAP) {
      List<String> names = (List<String>)jmo.getField(ControlAccess.BODY_CREATEDURABLE_NAMESPACEMAP_MAP_NAME);
      List<Object> values = (List<Object>)jmo.getField(ControlAccess.BODY_CREATEDURABLE_NAMESPACEMAP_MAP_VALUE);
      map = (Map<String,String>)(Map<String,?>)new JsMsgMap(names, values);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getDurableSelectorNamespaceMap", map);
    return map;
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
    
    buff.append(",noLocal=");
    buff.append(isNoLocal());
    
    buff.append(",cloned=");
    buff.append(isCloned());
    
    buff.append(",durableSelectorNamespaceMap=");
    buff.append(getDurableSelectorNamespaceMap());
    
  }
  
  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#setRequestID(long)
   */
  public final void setRequestID(long value) {
    jmo.setLongField(ControlAccess.BODY_CREATEDURABLE_REQUESTID, value);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#setDurableSubName(String)
   */
  public final void setDurableSubName(String name) {
    jmo.setField(ControlAccess.BODY_CREATEDURABLE_SUBNAME, name);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#setDurableDiscriminator(String)
   */
  public final void setDurableDiscriminator(String name) {
    jmo.setField(ControlAccess.BODY_CREATEDURABLE_DISCRIMINATOR, name);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#setDurableSelector(String)
   */
  public final void setDurableSelector(String name) {
    jmo.setField(ControlAccess.BODY_CREATEDURABLE_SELECTOR, name);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateDurable#setDurableSelectorDomain(int)
   */
  public final void setDurableSelectorDomain(int domain) {
    jmo.setIntField(ControlAccess.BODY_CREATEDURABLE_SELECTORDOMAIN, domain);
  }

  /*
   *  Set whether the message with a SecurityUserid field was sent by
   *  a system user.
   *
   *  Javadoc description supplied by ControlCreateDurable interface.
   */
  public final void setSecurityUseridSentBySystem(boolean value) {
    jmo.setBooleanField(ControlAccess.BODY_CREATEDURABLE_SECURITYSENTBYSYSTEM, value);
  }

  /*
   *  Set the contents of the SecurityUserid field for the subscription.
   *
   *  Javadoc description supplied by ControlCreateDurable interface.
   */
  public final void setSecurityUserid(String value) {
    jmo.setField(ControlAccess.BODY_CREATEDURABLE_SECURITYUSERID, value);
  }


  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateDurable#setNoLocal(boolean)
   */
  public final void setNoLocal(boolean value) {
    jmo.setBooleanField(ControlAccess.BODY_CREATEDURABLE_NOLOCAL_VALUE, value);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateDurable#setCloned(boolean)
   */
  public final void setCloned(boolean value) {
    jmo.setBooleanField(ControlAccess.BODY_CREATEDURABLE_CLONED_VALUE, value);
  }

  /**
   * Sets a map of prefixes to namespace URLs that are associated with the selector.
   *
   * @param namespaceMap
   */
  public void setDurableSelectorNamespaceMap(Map<String,String> namespaceMap) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setDurableSelectorNamespaceMap", namespaceMap);
    if (namespaceMap == null) {
      jmo.setChoiceField(ControlAccess.BODY_CREATEDURABLE_NAMESPACEMAP, ControlAccess.IS_BODY_CREATEDURABLE_NAMESPACEMAP_UNSET);
    }
    else {
      List<String> names = new ArrayList<String>();
      List<String> values = new ArrayList<String>();
      Set<Map.Entry<String,String>> es = namespaceMap.entrySet();
      for (Map.Entry<String,String> entry : es) {
        names.add(entry.getKey());
        values.add(entry.getValue());
      }
      jmo.setField(ControlAccess.BODY_CREATEDURABLE_NAMESPACEMAP_MAP_NAME, names);
      jmo.setField(ControlAccess.BODY_CREATEDURABLE_NAMESPACEMAP_MAP_VALUE, values);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setDurableSelectorNamespaceMap");
  }
}
