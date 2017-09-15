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

package com.ibm.ws.sib.processor.test;

import com.ibm.ws.sib.msgstore.MessageStore;

/**
 * This class extends the abstract com.ibm.ws.sib.admin.JsStandaloneFactory
 * class and provides the concrete implementations of the methods for
 * creating instances of JsMessagingEngine.
 * <p>
 * The class must be public so that the abstract class static
 * initialization can create an instance of it at runtime.
/*
 * Message Processor: 
 * 
 * This class was copied from JsStandaloneFactoryImpl.
 * 
 * We are using this class because admin are extremely unwilling to update
 * JsStandaloneEngineImpl to cater for requirements (in this case the 
 * ability to control message store persistence recovery for warm restart tests.
 * 
 * This is a temporary measure and it should be easy to revert to admin's 
 * standalone in the future.
 */ 
 
public abstract class SIMPJsStandaloneFactory
{
  
  private static SIMPJsStandaloneFactory instance = null;  

  static
  {
    try
    {
      Class cls = Class.forName(
        "com.ibm.ws.sib.processor.test.SIMPJsStandaloneFactoryImpl");
      instance = (SIMPJsStandaloneFactory) cls.newInstance();
      
    } catch (Exception e)
    {
      // No FFDC code needed
      // ...code does not run in WAS environment
      e.printStackTrace();
      
    }//try
    
  }//static

  /**
   *  Get the singleton JsStandaloneFactory instance
   *
   *  @return JsStandaloneFactory
   */
  public static SIMPJsStandaloneFactory getInstance()
  {
    return instance;
  }
  
  /**
   *  @param busName
   *  @param engineName
   *  @param clean should be false if existing database should be
   *          used, true if you want to start completely clean.
   *  @return JsMessagingEngine 
   */
  public abstract SIMPJsStandaloneEngine createNewMessagingEngine(
    String busName, String engineName, boolean clean, boolean initTrm) throws Exception;
  
  /**
   * Returns the _me.
   * @return SIMPJsStandaloneEngineImpl
   */
  public abstract SIMPJsStandaloneEngine get_me();

  public abstract MessageStore createMessageStoreOnly(
      String busName,
      String engineName,
      boolean clean,
      boolean initTrm) throws Exception;
}
