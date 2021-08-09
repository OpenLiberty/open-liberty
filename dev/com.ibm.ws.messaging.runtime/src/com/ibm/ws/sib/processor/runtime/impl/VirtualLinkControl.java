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

import java.util.HashSet;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.sib.admin.ControllableType;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.impl.LinkHandler;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.PtoPInputHandler;
import com.ibm.ws.sib.processor.runtime.SIMPForeignBusControllable;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPLinkRemoteMessagePointControllable;
import com.ibm.ws.sib.processor.runtime.SIMPVirtualLinkControllable;
import com.ibm.ws.sib.utils.ras.SibTr;

public class VirtualLinkControl extends AbstractRegisteredControlAdapter implements SIMPVirtualLinkControllable
{
  private static TraceComponent tc =
    SibTr.register(
      VirtualLinkControl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

 
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);  
  
  private LinkHandler _link;
  private MessageProcessor _messageProcessor;

  private HashSet<SIMPLinkRemoteMessagePointControllable> _remotePoints;

  public VirtualLinkControl(MessageProcessor messageProcessor, LinkHandler link)
  {
    super(messageProcessor, ControllableType.SIB_LINK ); 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "VirtualLinkControl", new Object[] { messageProcessor, link});
            
    _link = link;
    _messageProcessor = messageProcessor;
 
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "VirtualLinkControl", this); 
  }
  
  public VirtualLinkControl(MessageProcessor messageProcessor)
  {
    super(messageProcessor, ControllableType.SIB_LINK );
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "VirtualLinkControl", messageProcessor);
    _messageProcessor = messageProcessor;    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "VirtualLinkControl", this);   
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId()
   */
  public String getId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getId");    
    String id = "" + _link.getUuid();    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getId", id);   
    return id;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getName()
   */
  public String getName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getName");    
    String name = _link.getName();    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getName", name);    
    return name;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#checkValidControllable()
   */
  public void assertValidControllable() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "assertValidControllable");

    if(_link == null || !_link.isInStore())
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"VirtualLinkControl.assertValidControllable",
                          "1:131:1.33",
                          _link},
            null));
            
      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exception(tc, finalE);
      throw finalE; 
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "assertValidControllable");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#dereferenceControllable()
   */
  public void dereferenceControllable()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "dereferenceControllable");

    _link = null;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "dereferenceControllable"); 
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPVirtualLinkControllable#getForeignBusControlAdapter()
   */
  public SIMPForeignBusControllable getForeignBusControllable()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
    {
      SibTr.entry(tc, "getForeignBusControlAdapter");
      SibTr.exit(tc, "getForeignBusControlAdapter");
    }    
    return (SIMPForeignBusControllable)_messageProcessor.getDestinationManager()
                                                        .getForeignBusIndex()
                                                        .findByName(_link.getBusName(), null)
                                                        .getControlAdapter(); 
  }
  
  public SIMPIterator getLinkReceiverControllableIterator() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getLinkReceiverControllableIterator"); 
    
    SIMPIterator itr =
      ((PtoPInputHandler)_link.getInputHandler()).
      getTargetStreamManager().
      getTargetStreamSetControlIterator();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getLinkReceiverControllableIterator", itr); 
    return itr;
  }

  public SIMPIterator getLinkRemoteQueuePointControllableIterator() {
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLinkRemoteQueuePointControllableIterator");
    
    SIMPIterator returnItr = new BasicSIMPIterator(_remotePoints.iterator());
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getLinkRemoteQueuePointControllableIterator", returnItr);
    return returnItr;
    
  } 

  public String getState()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getState"); 
    String state = _messageProcessor.getDestinationManager().getLinkIndex().getState(_link).toString();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getState", state); 
    return state;
  }

  public String getTargetBus() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getTargetBus"); 
    String bus = _link.getBusName();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getTargetBus", bus); 
    return bus;
  }
  
  public void addRemoteMessagePointControl(SIMPLinkRemoteMessagePointControllable adapter) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "addRemoteMessagePointControl", adapter); 
   
    if (_remotePoints == null)
      _remotePoints = new HashSet<SIMPLinkRemoteMessagePointControllable>();
    
    _remotePoints.add((SIMPLinkRemoteMessagePointControllable)adapter);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "addRemoteMessagePointControl"); 

  }
  
  public void removeRemoteMessagePointControl(ControlAdapter adapter) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "removeRemoteMessagePointControl", adapter); 
   
    if (_remotePoints != null)
      _remotePoints.remove((SIMPLinkRemoteMessagePointControllable)adapter);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "removeRemoteMessagePointControl"); 

  }
  
  public String getType() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getType"); 
    String type = _link.getType();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getType", type); 
    return type;
  }
  
  public void merge(VirtualLinkControl control) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "merge", control); 
    
    SIMPIterator it = control.getLinkRemoteQueuePointControllableIterator();
    while (it.hasNext())
      _remotePoints.add((SIMPLinkRemoteMessagePointControllable)it.next());
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "merge"); 
  }
}
