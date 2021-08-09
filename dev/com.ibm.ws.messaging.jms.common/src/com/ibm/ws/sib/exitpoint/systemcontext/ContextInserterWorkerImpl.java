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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIBusMessage;

/**
 * <p>Implementation of the ContextInserterWorker.</p>
 *
 * <p>SIB build component: sib.exitpoint.systemcontext.impl</p>
 *
 * @author nottinga
 * @version 1.19
 * @since 1.0
 */
public class ContextInserterWorkerImpl implements ContextInserterWorker
{
  /** The TraceComponent for this class */
  private static final TraceComponent _tc =
    SibTr.register( ContextInserterWorkerImpl.class, 
                    TraceGroups.TRGRP_EXITPOINT, "");
  
  
  /** The Core SPI SystemContextInvoker. */
  //lohith liberty change
 // private static final SystemContextInvoker _invoker;
  
 /* static 
  {
    
    _invoker = SystemContextInvokerManager.getSystemContextInvoker(SystemContextConstants.CORE_SPI_TYPE);
  }*/


  /* ------------------------------------------------------------------------ */
  /* insertRequestContext method
  /* ------------------------------------------------------------------------ */
  /**
   * <p>If this method returns true then the context has been inserted into the
   *   supplied message, if it returns false then no context is in the message
   *   and it should not be sent. If the supplied SIBusMessage is not a
   *   supported implementation to have context inserted then an
   *   IllegalArgumentException will be thrown.
   * </p>
   *
   * <p>
   *
   * @param message the message context should be added to
   * @return true if the message has been populated, false otherwise
   * @throws IllegalArgumentException if the SIBusMessage is not a supported implementation
   * @throws UnsatisfiedLinkError if the underlying list of handlers cannot be located
   */
  public boolean insertRequestContext(SIBusMessage message)
  {
	  return false;
	//lohith liberty change  
	  
	  /*
    if (_tc.isEntryEnabled()) SibTr.entry(this, _tc, "insertRequestContext", new Object[]{message == null ? null : message.getSystemMessageId()});

    boolean result = true;
    SystemContext context;
    
    if (message instanceof JsApiMessage)
    {
      context = new SystemContextImpl((JsApiMessage)message);
      result = _invoker.insertRequestContext(context);
    }
    else
    {
      IllegalArgumentException e = new IllegalArgumentException();
      
      if (_tc.isEntryEnabled()) SibTr.exit(this, _tc, "insertRequestContext", e);
      
      throw e;
    }
    
    if (_tc.isEntryEnabled()) SibTr.exit(this, _tc, "insertRequestContext", result ? Boolean.TRUE : Boolean.FALSE);

    return result;
  */}

  /* ------------------------------------------------------------------------ */
  /* insertResponseContext method
  /* ------------------------------------------------------------------------ */
  /**
   * <p>If this method returns true then the context has been inserted into the
   *   supplied message, if it returns false then no context is in the message
   *   and it should not be sent. If the supplied SIBusMessage is not a
   *   supported implementation to have context inserted then an
   *   IllegalArgumentException will be thrown.
   * </p>
   *
   * <p>
   *
   * @param message the message context should be added to
   * @return true if the message has been populated, false otherwise
   * @throws IllegalArgumentException if the SIBusMessage is not a supported implementation
   * @throws UnsatisfiedLinkError if the underlying list of handlers cannot be located
   */
  public boolean insertResponseContext(SIBusMessage message)
  {
	  
	//lohith liberty change
	  return false;
	  /*
    if (_tc.isEntryEnabled()) SibTr.entry(this, _tc, "insertResponseContext", new Object[]{message == null ? null : message.getSystemMessageId()});

    boolean result = true;
    SystemContext context;
    
    if (message instanceof JsApiMessage)
    {
      context = new SystemContextImpl((JsApiMessage)message);
      result = _invoker.insertResponseContext(context);
    }
    else
    {
      IllegalArgumentException e = new IllegalArgumentException();
      
      if (_tc.isEntryEnabled()) SibTr.exit(this, _tc, "insertResponseContext", e);
      
      throw e;
    }
    
    if (_tc.isEntryEnabled()) SibTr.exit(this, _tc, "insertResponseContext", result ? Boolean.TRUE : Boolean.FALSE);

    return result;
  */}

  /* ------------------------------------------------------------------------ */
  /* requestFailed method
  /* ------------------------------------------------------------------------ */
  /**
   * <p>If, after calling insertContext, an error occurs the requestFailed 
   *   method should be called. This gives the system context providers an 
   *   opportunity to react to the failure.
   * </p>
   *
   * @throws UnsatisfiedLinkError if the underlying list of handlers cannot be located
   */
  public void requestFailed()
  {
    if (_tc.isEntryEnabled()) SibTr.entry(this, _tc, "requestFailed");

    //lohith liberty change
 //   _invoker.requestFailed();
    
    if (_tc.isEntryEnabled()) SibTr.exit(this, _tc, "requestFailed");
  }

  /* ------------------------------------------------------------------------ */
  /* requestSucceeded method
  /* ------------------------------------------------------------------------ */
  /**
   * <p>The requestSucceeded method should be called when the request has been
   *   correctly processed.
   * </p>
   *
   * @throws UnsatisfiedLinkError if the underlying list of handlers cannot be located
   */
  public void requestSucceeded()
  {
    if (_tc.isEntryEnabled()) SibTr.entry(this, _tc, "requestSucceeded");
    //lohith liberty change
  //  _invoker.requestSucceeded();
    
    if (_tc.isEntryEnabled()) SibTr.exit(this, _tc, "requestSucceeded");
  }
  
  /* ------------------------------------------------------------------------ */
  /* requestSucceeded method
  /* ------------------------------------------------------------------------ */
  /**
   * <p>The requestSucceeded method should be called when the request has been
   *   correctly processed.
   * </p>
   * @param message The response message.
   *
   * @throws UnsatisfiedLinkError if the underlying list of handlers cannot be located
   */
  public void requestSucceeded(SIBusMessage message)
  {
	  //lohith liberty change
	  
	  /*
    if (_tc.isEntryEnabled()) SibTr.entry(this, _tc, "requestSucceeded", new Object[] {message});

    SystemContext context;
    
    if (message instanceof JsApiMessage)
    {
      context = new SystemContextImpl((JsApiMessage)message);
      _invoker.requestSucceeded(context);
    }
    else
    {
      IllegalArgumentException e = new IllegalArgumentException();
      
      if (_tc.isEntryEnabled()) SibTr.exit(this, _tc, "requestSucceeded", e);
      
      throw e;
    }

    if (_tc.isEntryEnabled()) SibTr.exit(this, _tc, "requestSucceeded");
  */}
}
