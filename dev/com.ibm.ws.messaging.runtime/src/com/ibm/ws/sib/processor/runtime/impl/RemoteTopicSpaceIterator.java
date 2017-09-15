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
package com.ibm.ws.sib.processor.runtime.impl;

import java.util.Iterator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.AnycastInputHandler;
import com.ibm.ws.sib.processor.impl.PubSubOutputHandler;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Iterates through the remote topic spaces for a TopicSpace
 */
public class RemoteTopicSpaceIterator extends BasicSIMPIterator
{
  private static final TraceComponent tc =
    SibTr.register(
      RemoteTopicSpaceIterator.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
 
  /**
   * A remote topic space can be one from which we are
   * consuming, or one to whch we are publishing,
   * or both.
   * This class pairs the two views into one object.
   */
  public static class PublishConsumePairing
  {
    private static final TraceComponent innerTc =
      SibTr.register(
        RemoteTopicSpaceIterator.PublishConsumePairing.class,
        SIMPConstants.MP_TRACE_GROUP,
        SIMPConstants.RESOURCE_BUNDLE);
        
    PubSubOutputHandler _psoh;
    AnycastInputHandler _aih;
    
    PublishConsumePairing(PubSubOutputHandler psoh, AnycastInputHandler aih)
    {
      if (innerTc.isEntryEnabled()) 
        SibTr.entry(innerTc, "PublishConsumePairing", new Object[]{psoh, aih});
      
      _psoh = psoh;
      _aih = aih;
      
      if (innerTc.isEntryEnabled()) 
        SibTr.exit(innerTc, "PublishConsumePairing", this);
    }
    
    public void setAnycastInputHandler(AnycastInputHandler aih)
    {
      if (innerTc.isEntryEnabled()) 
      {
        SibTr.entry(innerTc, "setAnycastInputHandler", aih);
        SibTr.exit(innerTc, "setAnycastInputHandler");
      }
      _aih = aih;
    }
    
    Object getControlAdapter()
    {
      if (innerTc.isEntryEnabled()) 
        SibTr.entry(innerTc, "getControlAdapter");
        
      Object returnControl = null;
      //we are guaranteed that both psoh and aih have the same
      //control adapter
      if(_psoh!=null)
      {
        returnControl = _psoh.getControlAdapter();
      }
      else
      {
        returnControl = _aih.getControlAdapter();
      }
      if (innerTc.isEntryEnabled()) 
        SibTr.exit(innerTc, "getControlAdapter", returnControl);
      return returnControl;
    }
  }
      
  /**
   * @param allOutputHandlers all of the pubsub outputhandlers
   * on the destination
   */
  public RemoteTopicSpaceIterator(Iterator allPairings)
  {
    super(allPairings);   
  }
  
  public Object next()
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "next", this);
         
    PublishConsumePairing pairing = 
      (PublishConsumePairing)super.next(); 
    Object next =  pairing.getControlAdapter();

    if (tc.isEntryEnabled()) 
      SibTr.exit(tc, "next", next);         
    return next;
    
  }

}
