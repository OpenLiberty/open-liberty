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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.ControllableType;
import com.ibm.ws.sib.admin.JsAdminUtils;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.msgstore.transactions.ExternalAutoCommitTransaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.gd.InternalOutputStreamManager;
import com.ibm.ws.sib.processor.impl.AnycastInputHandler;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.PubSubOutputHandler;
import com.ibm.ws.sib.processor.proxyhandler.NeighbourProxyListener;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPPubSubOutboundTransmitControllable;
import com.ibm.ws.sib.processor.runtime.SIMPRemoteTopicSpaceControllable;
import com.ibm.ws.sib.processor.runtime.SIMPTopicSpaceControllable;
import com.ibm.ws.sib.processor.runtime.anycast.AttachedRemoteSubscriberIterator;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * 
 */
public class RemoteTopicSpaceControl extends AbstractRegisteredControlAdapter implements SIMPRemoteTopicSpaceControllable
{
  private static final TraceComponent tc =
    SibTr.register(
      RemoteTopicSpaceControl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

 
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  /** This is ued if the remote topic space has been used for remote put*/
  protected PubSubOutputHandler _outputHandler;
  
  /** 
   * This is used if we are performing remote get against the 
   * remote topic space
   */
  private AnycastInputHandler _anycastInputHandler;
  
  private MessageProcessor _messageProcessor;

  /**
   * Creates a RemtoeTopicSpace control.
   * 
   * @param outputHandler if this me has published to the remote topic
   * space. This parameter can be null.
   * @param anycastIH if this me is performing remote get against
   * a remote topic space. This parameter can be null.
   * @param messageProcessor This parameter can not be null.
   */
  public RemoteTopicSpaceControl(PubSubOutputHandler outputHandler,
                                 AnycastInputHandler anycastIH,
                                 MessageProcessor messageProcessor)
  {
    super(messageProcessor, ControllableType.REMOTE_PUBLICATION_POINT);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "RemoteTopicSpaceControl",
        new Object[] { outputHandler, anycastIH, messageProcessor });
        
    _messageProcessor = messageProcessor;
    this._outputHandler = outputHandler;
    this._anycastInputHandler = anycastIH;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "RemoteTopicSpaceControl", this);   
  }
  
  public void setAnycastInputHandler(AnycastInputHandler anycastIH)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
    {
      SibTr.entry(tc, "setAnycastInputHandler", anycastIH);
      SibTr.exit(tc, "setAnycastInputHandler");
    }
    _anycastInputHandler = anycastIH;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteTopicSpaceControllable#getTopicSpace()
   */
  public SIMPTopicSpaceControllable getTopicSpace()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getTopicSpace");
    
    String topicSpaceUuidString = getUuid();
    SIBUuid12 topicSpaceUuid = new SIBUuid12(topicSpaceUuidString);  
    SIMPTopicSpaceControllable tsControl = 
      (SIMPTopicSpaceControllable) _messageProcessor
                                    .getDestinationManager()
                                    .getDestinationInternal(topicSpaceUuid, false).
                                    getControlAdapter();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getTopicSpace", tsControl);
      
    return tsControl;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteTopicSpaceControllable#getPubSubOutboundTransmit()
   */
  public SIMPIterator getPubSubOutboundTransmitIterator()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPubSubOutboundTransmitIterator");
    
    //at the moment there should be just one stream set in the manager:
    //therefore this method is misleading. For the moment it will remain
    //deprecated.
    
    SIMPIterator iterator = null;
    //there are only outbound transmitters if the PSOH is not null
    if(_outputHandler!=null)
    {
      InternalOutputStreamManager streamManager = _outputHandler.getInternalOutputStreamManager();
      iterator =
        new BasicSIMPIterator(streamManager.getStreamSetControlIterator(this)); 
    }

    if(iterator==null)
    {
      //return an empty iterator
      iterator =
              new BasicSIMPIterator(new ArrayList().iterator()); 
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPubSubOutboundTransmitIterator", iterator);
    return iterator;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteTopicSpaceControllable#getTopicNameIterator()
   */
  public SIMPIterator getTopicNameIterator()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getTopicNameIterator");
   
    BasicSIMPIterator itr = null;
    //the remote ME has only subscribed for topics
    //if the PSOH is not null
    if(_outputHandler!=null)
    {
      if (_outputHandler.getTopics() != null)    
        itr = new BasicSIMPIterator(Arrays.asList(_outputHandler.getTopics()).iterator());    
    }
    
    if(itr==null)
    {
      //Create an empty iterator
      itr = new BasicSIMPIterator(new ArrayList().iterator());   
    }
       
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getTopicNameIterator", itr);  
    return itr;

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId()
   */
  public String getId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getId");

    String id = this.getUuid();
   //we need something unique - therefore cannot use 
   //just topic space ID
   id += this.getRemoteEngineUuid();
    
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
    String name = null;
    if(_outputHandler!=null)
    {
      name = _outputHandler.getDestinationName();
    }
    else
    {
      name = _anycastInputHandler.getDestName();
    }
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
    
    //Must have a PSOH OR an AIH (or both)
    if(_outputHandler == null && _anycastInputHandler==null)
    {
      SIMPControllableNotFoundException e =         
        new SIMPControllableNotFoundException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {"RemoteTopicSpaceControl.assertValidControllable",
                        "1:274:1.34"},
          null));
          
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "assertValidControllable", e);
      throw e; 
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
    
    _outputHandler = null;
    _messageProcessor = null;
    _anycastInputHandler = null;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "dereferenceControllable");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#registerControlAdapterAsMBean()
   */
  public synchronized void registerControlAdapterAsMBean()
  {
    super.registerControlAdapterAsMBean();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#deregisterControlAdapterMBean()
   */
  public void deregisterControlAdapterMBean()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#runtimeEventOccurred(com.ibm.ws.sib.admin.RuntimeEvent)
   */
  public void runtimeEventOccurred(RuntimeEvent event)
  {
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteTopicSpaceControllable#getMessagingEngineName()
   */  
  public String getMessagingEngineName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getMessagingEngineName");
    String meUuid = this.getRemoteEngineUuid();
    String meName = 
      JsAdminUtils.getMENameByUuid(meUuid);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMessagingEngineName", meName);
    return meName;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControlAdapter#getUuid()
   */  
  public String getUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getUuid");
    String uuid = null;
    if(_outputHandler!=null)
    {
      uuid = _outputHandler.getTopicSpaceUuid().toString();
    }
    else
    {
      uuid = _anycastInputHandler.getBaseDestinationHandler().
              getUuid().toString();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getUuid", uuid);
    return uuid;
  }  
  
  public String getRemoteEngineUuid()
  {  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRemoteEngineUuid");
      
    String remoteME = null;
    if(_outputHandler!=null)
    {
      remoteME = _outputHandler.getTargetMEUuid().toString(); 
    }
    else
    {
      remoteME = _anycastInputHandler.getLocalisationUuid().toString();
    }
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getRemoteEngineUuid", remoteME);
    return remoteME;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteTopicSpaceControllable#getPubSubOutboundTransmitControllable()
   */
  public SIMPPubSubOutboundTransmitControllable getPubSubOutboundTransmitControllable()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPubSubOutboundTransmitControllable");
    
    SIMPPubSubOutboundTransmitControllable outbound = null;
    //there will only be a stream set if the PSOH is not null
    if(_outputHandler!=null)
    {
      //at the moment there should be just one stream set in the
      //iterator
      SIMPIterator iterator = getPubSubOutboundTransmitIterator();
      if(iterator.hasNext())
      {
        outbound = (SIMPPubSubOutboundTransmitControllable)iterator.next();
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPubSubOutboundTransmitControllable", outbound);
    return outbound;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteTopicSpaceControllable#clearTopics()
   * 
   * Deletes all topics from the PubSubOutputHandler/Proxy and Matchspace.
   */
  public void clearTopics()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "clearTopics");
    
    //we only clear the topics if the remote me
    //has subscribed to some topics on this me.
    if(_outputHandler!=null)
    {
      if (_outputHandler.getTopics() != null)
      {
        Iterator topics = 
          Arrays.asList(_outputHandler.getTopics()).iterator();

        LinkedList topicSpaces = new LinkedList();
        LinkedList topicSpaceMappings = new LinkedList();
        // Iterate through each of the topics and add the matching ts uuid and ts mapping
        for (int i = 0; i < _outputHandler.getTopics().length; i++)
        {
          topicSpaces.add(_outputHandler.getTopicSpaceUuid().toString());   
          topicSpaceMappings.add(_outputHandler.getTopicSpaceMapping());         
        }
  
        //get the NeighbourProxyListener
        NeighbourProxyListener listener = 
          _messageProcessor.getProxyHandler().getProxyListener();
        try
        {
          ExternalAutoCommitTransaction tran =
            _messageProcessor.getTXManager().createAutoCommitTransaction();
          listener.deleteProxySubscription(topics,
                                           topicSpaces.iterator(),
                                           topicSpaceMappings.iterator(),
                                           _outputHandler.getTargetMEUuid(),
                                           _outputHandler.getBusName(),
                                           tran);
        }
        catch(SIResourceException e)
        {
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.runtime.RemoteTopicSpaceControl.clearTopics",
            "1:456:1.34",
            this);
            
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] { "com.ibm.ws.sib.processor.runtime.RemoteTopicSpaceControl.clearTopics", 
                           "1:461:1.34", 
                           SIMPUtils.getStackTrace(e) });
          SibTr.exception(tc, e);
        }
      }
      else
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "No Topics to delete for topicspace with id " + _outputHandler.getDestinationName());
      }
    }//end if PSOH==null

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "clearTopics");        
  }

  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteTopicSpaceControllable#getRemoteSubscriptions()
   */  
  public SIMPIterator getRemoteSubscriptions()
  { 
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getRemoteSubscriptions");

    List durableConsumers = new LinkedList();
    if(_anycastInputHandler!=null)
    {
      //we have a durable consumer
      durableConsumers.add(_anycastInputHandler);
    }

    AttachedRemoteSubscriberIterator remoteSubscriptionItr = 
      new AttachedRemoteSubscriberIterator(durableConsumers);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getRemoteSubscriptions", remoteSubscriptionItr);
    return remoteSubscriptionItr;
  }
  
  public PubSubOutputHandler getOutputHandler()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
    {
      SibTr.entry(tc, "getOutputHandler");
      SibTr.exit(tc, "getOutputHandler", _outputHandler);
    }
    return _outputHandler;
  }
}
