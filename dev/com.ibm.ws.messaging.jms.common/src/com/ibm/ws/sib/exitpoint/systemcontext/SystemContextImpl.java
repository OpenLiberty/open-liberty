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
package com.ibm.ws.sib.exitpoint.systemcontext;

import java.io.IOException;
import java.io.Serializable;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.JsApiMessage;
import com.ibm.ws.sib.utils.PasswordUtils;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;

//lohith liberty change
//import commonj.sdo.DataGraph;

/**
 * <p>This class implements the SystemContext interface delegating most of the
 *   methods down onto the underlying Jetstream message.
 * </p>
 *
 * <p>SIB build component: sib.exitpoint.systemcontext.impl</p>
 *
 * @author nottinga
 * @version 1.11
 * @since 1.0
 */
public class SystemContextImpl      // implements SystemContext  //lohith liberty change
{
  /** The trace component for this class */
  private static final TraceComponent _tc = SibTr.register(SystemContextImpl.class, TraceGroups.TRGRP_EXITPOINT, null);
 
  /** The underlying message */
  private JsApiMessage _message;
  /** The underlying message as a JsSdoMessage */
  /*private JsSdoMessage _sdoMessage;
  *//** The MDB interface class *//*
  private DataGraph _currentView;*/
  /** The format of the current datagraph view */
  private String _currentViewFormat;

  /* ------------------------------------------------------------------------ */
  /* SystemContextImpl method
  /* ------------------------------------------------------------------------ */
  /**
   * This constructor creates the SystemContext class
   *
   * @param msg          The underlying message.
   */
  public SystemContextImpl(JsApiMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "SystemContextImpl", msg);

    _message = msg;

    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "SystemContextImpl", this);
  }

  /* ------------------------------------------------------------------------ */
  /* getSystemContextItem method
  /* ------------------------------------------------------------------------ */
  /**
   * @see com.ibm.wsspi.exitpoint.systemcontext.SystemContext#getSystemContextItem(java.lang.String)
   */
  public Serializable getSystemContextItem(String key)
      throws IllegalArgumentException, IOException, ClassNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "getSystemContextItem", key);

    Serializable result;
    try
    {
      result = _message.getSystemContextItem(key);
    }
    catch (IllegalArgumentException e)
    {
      com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.exitpoint.systemcontext.SystemContextImpl.getSystemContextItem", "109", this);

      if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "getSystemContextItem", e);
      throw e;
    }
    catch (IOException e)
    {
      com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.exitpoint.systemcontext.SystemContextImpl.getSystemContextItem", "113", this);

      if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "getSystemContextItem", e);
      throw e;
    }
    catch (ClassNotFoundException e)
    {
      com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.exitpoint.systemcontext.SystemContextImpl.getSystemContextItem", "117", this);

      if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "getSystemContextItem", e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "getSystemContextItem", result);
    return result;
  }
  /* ------------------------------------------------------------------------ */
  /* putSystemContextItem method
  /* ------------------------------------------------------------------------ */
  /**
   * @see com.ibm.wsspi.exitpoint.systemcontext.SystemContext#putSystemContextItem(java.lang.String, java.io.Serializable)
   */
  public void putSystemContextItem(String key, Serializable value)
      throws IllegalArgumentException, IOException
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "putSystemContextItem", new Object[]{key, value});

    try
    {
      _message.putSystemContextItem(key, value);
    }
    catch (IllegalArgumentException e)
    {
      com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.exitpoint.systemcontext.SystemContextImpl.putSystemContextItem", "153", this);

      if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "putSystemContextItem", e);
      throw e;
    }
    catch (IOException e)
    {
      com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.exitpoint.systemcontext.SystemContextImpl.putSystemContextItem", "160", this);

      if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "putSystemContextItem", e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "putSystemContextItem");
  }

  /* ------------------------------------------------------------------------ */
  /* userPropertyExists method
  /* ------------------------------------------------------------------------ */
  /**
   * @see SystemContext#propertyExists(String)
   */
  public boolean propertyExists(String name)
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "propertyExists", name);
    boolean result = _message.userPropertyExists(name);

    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "propertyExists", result);
    return result;
  }

  /* ------------------------------------------------------------------------ */
  /* getUserProperty method
  /* ------------------------------------------------------------------------ */
  /**
   * @see SystemContext#getProperty(String)
   */
  public Object getProperty(String name) throws IOException, ClassNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "getProperty", name);
    Object userProperty = _message.getUserProperty(name);

    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "getProperty", PasswordUtils.replaceValueIfKeyIsPassword(name,userProperty));
    return userProperty;
  }
  /* ------------------------------------------------------------------------ */
  /* getDataGraphView method
  /* ------------------------------------------------------------------------ */
  /**
   * @see SystemContext#getPayload(String)
   */
/*  public DataGraph getPayload(String format)
    throws NullPointerException ,SystemContextException
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "getPayload", format);

    if (_currentViewFormat == null || !_currentViewFormat.equals(format))
    {
      try
      {
        _currentView = getSdoMessage().getNewDataGraph(format);
        _currentViewFormat = format;
      }
      catch (SIException e)
      {
        com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.exitpoint.systemcontext.SystemContextImpl.getPayload", "234", this);

        //lohith liberty change
   //     SystemContextException newE = new SystemContextException(e);
        if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "getPayload", newE);
        throw newE;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "getPayload", _currentView);
    return _currentView;
  }*/

  /* ------------------------------------------------------------------------ */
  /* isReverseRoutingPathEmpty method
  /* ------------------------------------------------------------------------ */
  /**
   * @see SystemContext#isOneWay()
   */
  public boolean isOneWay()
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "isOneWay");
    boolean result = _message.isReverseRoutingPathEmpty();
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "isOneWay", result);
    return result;
  }

  /* ------------------------------------------------------------------------ */
  /* getSdoMessage method
  /* ------------------------------------------------------------------------ */
  /**
   * This method converts the underlying message to an JsSdoMessage. This method
   * will only do the convertion once and will then cache the result.
   *
   * @return The message as a JsSdoMessage.
   * @throws SIMessageException If the message cannot be converted
   */
 /* private JsSdoMessage getSdoMessage()
   throws SIMessageException
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "getSdoMessage");

    if (_sdoMessage == null)
    {
      try
      {
        _sdoMessage = (JsSdoMessage)JsSdoMessageFactory.getInstance().createSIBusSdoMessage(_message);
      }
      catch (SIMessageException e)
      {
        com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.exitpoint.systemcontext.SystemContextImpl.getSdoMessage", "256", this);

        if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "getSdoMessage", e);
        throw e;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "getSdoMessage", _sdoMessage);
    return _sdoMessage;
  }
*/
  /* ------------------------------------------------------------------------ */
  /* isFormatSupported method
  /* ------------------------------------------------------------------------ */
  /**
   * @see com.ibm.wsspi.exitpoint.systemcontext.SystemContext#isFormatSupported(java.lang.String)
   */
  public boolean isFormatSupported(String format)
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "isFormatSupported", format);
    boolean result = false;

    if (format != null && (format.startsWith("SOAP:") ||
        format.startsWith("JMS:") || format.startsWith("BEANS:")))
    {
      result = true;
    }

    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "isFormatSupported", result);
    return result;
  }
}
