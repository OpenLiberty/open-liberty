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

import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.msgstore.MessageStore;

/**
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 * 
 * @author matrober
 */
public class SIMPJsStandaloneFactoryImpl extends SIMPJsStandaloneFactory
{
  
  private SIMPJsStandaloneEngine _me ;
  
  public SIMPJsStandaloneEngine createNewMessagingEngine(
          String busName,
          String engineName,
          boolean clean,
          boolean initTrm) throws Exception
  {
    
    // Instantiate a standalone version of an ME
    if (initTrm)
    {
      _me = new SIMPJsStandaloneEngineImpl(busName, engineName);
    }
     
    
    // Initialize and start the ME
    _me.initialize(_me, clean, initTrm);
    _me.start(JsConstants.ME_START_DEFAULT);
    
    return _me;
    
  }
  
  public MessageStore createMessageStoreOnly(
      String busName,
      String engineName,
      boolean clean,
      boolean initTrm) throws Exception
  {
//  Instantiate a standalone version of an ME
    if (initTrm)
    {
      _me = new SIMPJsStandaloneEngineImpl(busName, engineName);
    }
    
    return _me.createMessageStoreOnly(clean);
  }
  
  /**
   * Returns the _me.
   * @return SIMPJsStandaloneEngineImpl
   */
  public SIMPJsStandaloneEngine get_me()
  {
    return _me;
  }

}
