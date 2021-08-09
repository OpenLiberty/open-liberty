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

import java.lang.reflect.Constructor;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.JsApiMessage;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * <p>This class will create and return an instance of a SystemContext.</p>
 *
 * <p>SIB build component: sib.exitpoint.systemcontext</p>
 *
 * @author nottinga
 * @version 1.8
 * @since 1.0
 */
public final class SystemContextFactory
{
  /** The trace component for this class */
  private static final TraceComponent _tc = SibTr.register(SystemContextFactory.class, TraceGroups.TRGRP_EXITPOINT, null);
  
  /** The constructor for the SystemContextImpl */
  private static Constructor _constructor;
  
  

  /* ------------------------------------------------------------------------ */
  /* createSystemContext method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This class creates and returns an instance of SystemContext.
   * 
   * @param msg          The message.
   * @return             A systemcontext.
   * @throws NoSuchMethodError if the constructor cannot be located.
   * @throws SIErrorException  if the SystemContext cannot be created.
   */
  
  //lohith liberty change
  /*public static final SystemContext createSystemContext(JsApiMessage msg)
  {
    Object[] arguments = new Object[] {msg};
    if (_tc.isEntryEnabled()) SibTr.entry(_tc, "createSystemContext", arguments);
    
    SystemContext result; 
    
    try
    {
      result = (SystemContext) getConstructor().newInstance(arguments);
    }
    catch (Exception e)
    {
      com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.exitpoint.systemcontext.SystemContextFactory.createSystemContext", "62");
      
      SIErrorException newE = new SIErrorException(e);
      
      if (_tc.isEntryEnabled()) SibTr.exit(_tc, "createSystemContext", newE);
      throw newE;
    }
    
    if (_tc.isEntryEnabled()) SibTr.exit(_tc, "createSystemContext", result);
    return result;
  }*/
  
  /* ------------------------------------------------------------------------ */
  /* getConstructor method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method creates and returns the constructor of the SystemContextImpl.
   * 
   * @return The SystemContextImpl constructor.
   */
  private static Constructor getConstructor()
  {
    if (_tc.isEntryEnabled()) SibTr.entry(_tc, "getConstructor");
    
    if (_constructor == null)
    {
      try
      {
        Class clazz = Class.forName("com.ibm.ws.sib.exitpoint.systemcontext.SystemContextImpl");
        _constructor = clazz.getConstructor(new Class[] {JsApiMessage.class});
      }
      catch (Exception e)
      {
        com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.exitpoint.systemcontext.SystemContextFactory.getConstructor", "66");
        
        NoSuchMethodError newE = new NoSuchMethodError();
        newE.initCause(e);
        
        if (_tc.isEntryEnabled()) SibTr.exit(_tc, "getConstructor", newE);
        throw newE;
      }
    }
    
    if (_tc.isEntryEnabled()) SibTr.exit(_tc, "getConstructor", _constructor);
    return _constructor;
  }
}
