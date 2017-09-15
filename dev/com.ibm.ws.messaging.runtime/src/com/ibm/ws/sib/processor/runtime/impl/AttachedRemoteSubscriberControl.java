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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.sib.admin.ControllableType;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.gd.AIStream;
import com.ibm.ws.sib.processor.impl.AnycastInputHandler;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.exceptions.InvalidOperationException;
import com.ibm.ws.sib.processor.runtime.SIMPAttachedRemoteSubscriberControllable;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPRemoteConsumerReceiverControllable;
import com.ibm.ws.sib.processor.runtime.SIMPTopicSpaceControllable;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *
 */
public class AttachedRemoteSubscriberControl extends AbstractRegisteredControlAdapter implements SIMPAttachedRemoteSubscriberControllable
{
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  
  private static final TraceComponent tc =
    SibTr.register(
      AttachedRemoteSubscriberControl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private static final String HANDLER_NAME = "AttachedRemoteSubscriber";
  
  private String _remoteDurableName;
  private BaseDestinationHandler _destinationHandler;
  private MessageProcessor _messageProcessor;
  private AnycastInputHandler _anycastInputHandler;
  private Topicspace _topicspace;
  private RemoteTopicSpaceControl _remoteTopicSpaceControl;
  
  public AttachedRemoteSubscriberControl(String remoteDurableName,
                                         AnycastInputHandler anycastInputHandler,
                                         MessageProcessor messageProcessor,
                                         RemoteTopicSpaceControl remoteTopicSpace)
  {
    super(messageProcessor, ControllableType.REMOTE_SUBSCRIPTION_POINT);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "AttachedRemoteSubscriberControl",
        new Object[] { remoteDurableName, anycastInputHandler, messageProcessor, remoteTopicSpace});

    _remoteDurableName = remoteDurableName;
    _anycastInputHandler = anycastInputHandler;
    _destinationHandler = anycastInputHandler.getBaseDestinationHandler();
    _messageProcessor = messageProcessor;
    _remoteTopicSpaceControl = remoteTopicSpace;
    
    registerControlAdapterAsMBean();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AttachedRemoteSubscriberControl", this);     
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPAttachedRemoteSubscriberControllable#getTopicSpace()
   */
  public SIMPTopicSpaceControllable getTopicSpace()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getTopicSpace");
      
    //need to check that we don't hav it already
    if (_topicspace == null)
    {
      _topicspace = new Topicspace(_messageProcessor, _destinationHandler);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getTopicSpace", _topicspace);
    return _topicspace;
  }
  
  public RemoteTopicSpaceControl getRemoteTopicSpaceControl()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
    {
      SibTr.entry(tc, "getRemoteTopicSpaceControl");
      SibTr.exit(tc, "getRemoteTopicSpaceControl", _remoteTopicSpaceControl);
    }
    return _remoteTopicSpaceControl;
  }
  
  /**
   * Return the remote engine uuid
   */
  public String getRemoteEngineUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRemoteEngineUuid");
    
    String engineUUID = _anycastInputHandler.getLocalisationUuid().toString(); 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getRemoteEngineUuid", engineUUID);
    return engineUUID;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPAttachedRemoteSubscriberControllable#getRemoteConsumerReceiver()
   */
  public SIMPRemoteConsumerReceiverControllable getRemoteConsumerReceiver()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRemoteConsumerReceiver");
      
    AIStream aiStream = _anycastInputHandler.getAIStream();                 
    SIMPRemoteConsumerReceiverControllable remoteConsumerReceiverControl = null;

    if(aiStream!=null)
      remoteConsumerReceiverControl = (SIMPRemoteConsumerReceiverControllable)aiStream.getControlAdapter();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getRemoteConsumerReceiver", remoteConsumerReceiverControl);
    return remoteConsumerReceiverControl;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPAttachedRemoteSubscriberControllable#getConsumerIterator()
   */
  public SIMPIterator getConsumerIterator()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getConsumerIterator");

    InvalidOperationException finalE =
      new InvalidOperationException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0005",
          new Object[] {"AttachedRemoteSubscriberControl.getConsumerIterator",
              "1:188:1.32",
                        this},
          null));

    SibTr.exception(tc, finalE);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getConsumerIterator");
    throw finalE;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#checkValidControllable()
   */
  public void assertValidControllable()throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "assertValidControllable");
      
    if( _destinationHandler==null ||
        _messageProcessor==null ||
        _anycastInputHandler==null ||
        _remoteTopicSpaceControl==null
    )
    {
      SIMPControllableNotFoundException e =         
        new SIMPControllableNotFoundException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {"AttachedRemoteSubscriberControl.assertValidControllable",
                        "1:218:1.32"},
          null));
          
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "assertValidControllable", e);
      throw e; 
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "assertValidControllable");
      
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
    _remoteDurableName = null;
    _anycastInputHandler = null;
    _destinationHandler = null;
    _messageProcessor = null;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "dereferenceControllable");    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#registerControlAdapterAsMBean()
   */
  public synchronized void registerControlAdapterAsMBean()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "registerControlAdapterAsMBean");
    
    if(!isRegistered()) 
    {
      super.registerControlAdapterAsMBean();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "registerControlAdapterAsMBean");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#deregisterControlAdapterMBean()
   */
  public void deregisterControlAdapterMBean()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "deregisterControlAdapterMBean");
    super.deregisterControlAdapterMBean();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "deregisterControlAdapterMBean");    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#runtimeEventOccurred(com.ibm.ws.sib.admin.RuntimeEvent)
   */
  public void runtimeEventOccurred(RuntimeEvent event)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
      SibTr.debug(tc, "runtimeEventOccurred", event);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getName()
   */
  public String getName()
  {
    return HANDLER_NAME + "." + _remoteDurableName;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getId()
   */
  public String getId()
  { 
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
    {
      SibTr.entry(tc, "getId");
      SibTr.exit(tc, "getId", _remoteDurableName);
    }
    return _remoteDurableName; //guarnteed to be unique
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPAttachedRemoteSubscriberControllable#getTopicNameIterator()
   */
  public SIMPIterator getTopicNameIterator()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getTopicNameIterator");
    //if the stream is flushed then we have not subscribed
    //to any topics, so we return an empty list
    List topicList = null;
    if(_anycastInputHandler.getAIStream()!=null)
    {
      String[] topics =
            _anycastInputHandler.getRCD().getConsumerDispatcherState().getTopics();
       topicList = Arrays.asList(topics);
    }
    else
    {
      //we are flushed, so we are not currently subscribing for any
      //topics
      topicList = new LinkedList(); //create an empty collection
    }
    
    BasicSIMPIterator iterator = new BasicSIMPIterator(topicList.iterator());
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getTopicNameIterator", iterator);
    return iterator;                                   
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPAttachedRemoteSubscriberControllable#clearAllTopics()
   */  
  public void clearAllTopics()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "clearAllTopics");
    //stop the consumption from this remote topic space
  	_anycastInputHandler.forceFlushAtTarget(); 	 
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "clearAllTopics"); 
  }
}
