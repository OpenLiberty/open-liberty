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
package com.ibm.ws.sib.processor.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.ForeignBusDefinition;
import com.ibm.ws.sib.admin.ForeignDestinationDefault;
import com.ibm.ws.sib.admin.SIBExceptionBusNotFound;
import com.ibm.ws.sib.admin.SIBExceptionNoLinkExists;
import com.ibm.ws.sib.admin.SIBExceptionObjectNotFound;
import com.ibm.ws.sib.admin.VirtualLinkDefinition;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.exceptions.InvalidOperationException;
import com.ibm.ws.sib.processor.impl.interfaces.ControlHandler;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream;
import com.ibm.ws.sib.processor.runtime.impl.ForeignBus;
import com.ibm.ws.sib.security.auth.OperationType;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;

/**
 * Class to represent Bus Destinations
 */ 
public class BusHandler extends AliasDestinationHandler
{
  /** Trace for the component */
  private static final TraceComponent tc =
    SibTr.register(
      BusHandler.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
   
  
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  
  //The definition of the bus
  private ForeignBusDefinition _foreignBusDefinition = null;
 
  //Default destination attributes for the foreign bus definition
  private ForeignDestinationDefault _foreignDestinationDefault = null;
  
  /**
   * <p>Create a new instance of a destination, passing in the name of the 
   * destination and its definition.  A destination represents a topicspace in 
   * pub/sub or a queue in point to point.</p>
   * 
   * @param destinationName
   * @param destinationDefinition
   * @param messageProcessor
   * @param parentStream  The Itemstream this DestinationHandler should be
   *         added into. 
   * @param durableSubscriptionsTable  Required only by topicspace 
   *         destinations.  Can be null if point to point (local or remote).
   */
  public BusHandler(
    ForeignBusDefinition foreignBusDefinition,
    MessageProcessor messageProcessor,
    SIMPItemStream parentItemStream,   
    HashMap durableSubscriptionsTable,
    LinkHandler resolvedDestinationHandler) throws SINotPossibleInCurrentConfigurationException
  {
    super(null
         ,messageProcessor
         ,parentItemStream
         ,resolvedDestinationHandler
         ,messageProcessor.getMessagingEngineBus());
         
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "BusHandler",
        new Object[] { foreignBusDefinition, 
          messageProcessor, parentItemStream, 
          durableSubscriptionsTable, resolvedDestinationHandler});

    _foreignBusDefinition = foreignBusDefinition;
    
    // Set the send allowed attribute for the BusHandler
    if(foreignBusDefinition.getSendAllowed())
      _sendAllowedOnTargetForeignBus = Boolean.TRUE;
    else
      _sendAllowedOnTargetForeignBus = Boolean.FALSE;
    
    try
    {
      _foreignDestinationDefault = foreignBusDefinition.getDestinationDefault();
    }
    catch (SIBExceptionObjectNotFound e)
    {
      // MessageStoreException shouldn't occur so FFDC.
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.BusHandler.BusHandler",
          "1:177:1.54",
          this);
        
      SibTr.exception(tc, e);

      throw new SINotPossibleInCurrentConfigurationException(e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "BusHandler", this);        
  }

  // ------------------------------------------------------------------------------------

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#updateDefinition(com.ibm.ws.sib.admin.DestinationDefinition)
   */
  public void updateDefinition(DestinationDefinition destinationDefinition)
  {
    //unsupported protocol type
    throw new InvalidOperationException(
      nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0005",
          new Object[] {
            "com.ibm.ws.sib.processor.proxyhandler.BusGroup",
            "1:202:1.54",
            getName() },
          null));
  }    

  /**
   * Dynamically update the foreignBusDefinition
   * 
   * @param foreignBusDefinition
   * @throws SIResourceException 
   */
  public void updateDefinition(ForeignBusDefinition foreignBusDefinition)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "updateDefinition", foreignBusDefinition);
      
    ForeignDestinationDefault newForeignDefault = null;

    // If sendAllowed has changed on the BusDefinition, then
    // update it.
    updateSendAllowed(_foreignBusDefinition.getSendAllowed(),
                      foreignBusDefinition.getSendAllowed());
 
    try
    {
      // Update the the VLD - including inbound and outbound userids if appropriate
      VirtualLinkDefinition vld = foreignBusDefinition.getLinkForNextHop();
      updateVirtualLinkDefinition(vld);
      
      newForeignDefault = foreignBusDefinition.getDestinationDefault();
      _foreignDestinationDefault = newForeignDefault;
      _foreignBusDefinition = foreignBusDefinition;
    }
    catch (SIBExceptionObjectNotFound e)
    {
      // Shouldn't occur so FFDC.
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.BusHandler.updateDefinition",
          "1:242:1.54",
          this);
        
      SibTr.exception(tc, e);
      // Allow processing to continue
    }      
    catch (SIBExceptionNoLinkExists e)
    {
      // Shouldn't occur so FFDC.
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.BusHandler.updateDefinition",
          "1:254:1.54",
          this);
        
      SibTr.exception(tc, e);
      // Allow processing to continue
    }      
    catch (SIIncorrectCallException e)
    {
      // Shouldn't occur so FFDC.
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.BusHandler.updateDefinition",
          "1:266:1.54",
          this);
        
      SibTr.exception(tc, e);
      // Allow processing to continue
    } 
    catch (SIBExceptionBusNotFound e)
    {
      // Shouldn't occur so FFDC.
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.BusHandler.updateDefinition",
          "1:278:1.54",
          this);
        
      SibTr.exception(tc, e);
      // Allow processing to continue
    }      
 
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateDefinition");
 
  }      

  /**
   * Update sendAllowed setting for this bus and any targetting Handlers.
   * 
   * @param foreignBusDefinition
   */
  public void updateSendAllowed(boolean oldSendAllowed, boolean newSendAllowed)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc,
                  "updateSendAllowed", 
                  new Object[] {Boolean.valueOf(oldSendAllowed), Boolean.valueOf(newSendAllowed)});
      
    if(oldSendAllowed && !newSendAllowed)
      setForeignBusSendAllowed(false);
    else if(!oldSendAllowed && newSendAllowed)
      setForeignBusSendAllowed(true);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateSendAllowed");
 
  }        

  /**
   * Update VirtualLinkDefinition attributes such as inbound or outbound userid settings for this bus and 
   * any targetting Handlers.
   * 
   * @param newVirtualLinkDefinition
   * @throws SIIncorrectCallException 
   * @throws SIResourceException 
   */
  public void updateVirtualLinkDefinition(VirtualLinkDefinition newVirtualLinkDefinition) 
    throws SIIncorrectCallException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc,
                  "updateVirtualLinkDefinition", 
                  new Object[] {newVirtualLinkDefinition});
   
    // Get hold of the associated LinkHandler
    LinkHandler lh = (LinkHandler)_targetDestinationHandler;
    
    // We'll do this under a tranaction. This work is not coordinated with the Admin component.
    //
    // Create a local UOW
    LocalTransaction transaction = txManager.createLocalTransaction(true);

    try
    {
      lh.updateLinkDefinition(newVirtualLinkDefinition, transaction);

      // If the update was successful then commit the unit of work
      transaction.commit();
    }
    catch (SIResourceException e)
    {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "updateVirtualLinkDefinition", e);

      try
      {
        transaction.rollback();
      }
      catch (Throwable et)
      {
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.BusHandler.updateVirtualLinkDefinition",
          "1:359:1.54",
          this);

        SibTr.exception(tc, et);
      }
      
      throw e;
    }
    catch (RuntimeException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.BusHandler.updateVirtualLinkDefinition",
        "1:373:1.54",
        this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.exception(tc, e);
        SibTr.exit(tc, "updateVirtualLinkDefinition", e);
      }

      try
      {
        transaction.rollback();
      }
      catch (Throwable et)
      {
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.BusHandler.updateVirtualLinkDefinition",
          "1:392:1.54",
          this);

        SibTr.exception(tc, et);
      }
      throw e;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateVirtualLinkDefinition");
 
  }          

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isPubSub()
   */
  public boolean isPubSub()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isPubSub");
      SibTr.exit(tc, "isPubSub", Boolean.FALSE);
    }
    return false;
  }  

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDefaultReliability()
   */
  public Reliability getDefaultReliability()
  {
    return _foreignDestinationDefault.getDefaultReliability();
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getMaxReliability()
   */
  public Reliability getMaxReliability()
  {
    return _foreignDestinationDefault.getMaxReliability();
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDefaultPriority()
   */
  public int getDefaultPriority()
  {
    return _foreignDestinationDefault.getDefaultPriority();
  }
  
  /* (non-Javadoc)
  * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getAlterationTime()
  */
  public long getAlterationTime()
  {
    //unsupported
    throw new InvalidOperationException(
      nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0005",
          new Object[] {
            "com.ibm.ws.sib.processor.proxyhandler.BusGroup",
            "1:453:1.54",
            getName() },
          null));
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDescription()
   */
  public String getDescription()
  {
    return _foreignBusDefinition.getDescription();                              
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDestinationType()
   */
  public DestinationType getDestinationType()
  {
    return DestinationType.UNKNOWN;         
  }  
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDefaultForwardRoutingPath()
   */
  public SIDestinationAddress[] getDefaultForwardRoutingPath()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getDefaultForwardRoutingPath");
      SibTr.exit(tc, "getDefaultForwardRoutingPath", null);
    }
    // No op for foreign destinations
    // TODO: It is possible that a slight rearrangement of the class hierachy 
    // could reduce the risk of defects like 301491. We should bear this in mind.
    return null;
  }  
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isSendAllowed()
   */
  public boolean isSendAllowed()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isSendAllowed");
    
    boolean sendAllowed;
    if(_sendAllowedOnTargetForeignBus.equals(Boolean.FALSE))
      sendAllowed =  false;
    else
      sendAllowed = _foreignDestinationDefault.isSendAllowed();     
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isSendAllowed", sendAllowed);
    
    return sendAllowed;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isReceiveAllowed()
   */
  public boolean isReceiveAllowed()
  {
    // Never called 
    return false;                           
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isInQuiesceMode()
   */
  public boolean isInQuiesceMode()
  {
    throw new InvalidOperationException(
      nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0005",
          new Object[] {
            "com.ibm.ws.sib.processor.proxyhandler.BusGroup",
            "1:529:1.54",
            getName() },
          null));
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isTemporary()
   */
  public boolean isTemporary()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isTemporary");   

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isTemporary", Boolean.FALSE); 
      
    return false;      
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isSystem()
   */
  public boolean isSystem()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isSystem");
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isSystem", Boolean.FALSE);
    
    return false;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getName()
   */
  public String getName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled() && _foreignBusDefinition == null)
      return "ForeignDestNullName";
    
    return _foreignBusDefinition.getName();
  } 

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getUuid()
   */
  public SIBUuid12 getUuid()
  {
    return _foreignBusDefinition.getUuid();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getLocalisation(com.ibm.ws.sib.utils.SIBUuid8)
   */
  public PtoPMessageItemStream getLocalisation(SIBUuid8 meUuid)
  {
    throw new InvalidOperationException(
      nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0005",
          new Object[] {
            "com.ibm.ws.sib.processor.proxyhandler.BusGroup",
            "1:591:1.54",
            getName() },
          null));
  }


  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isReconciled()
   */
  public boolean isReconciled()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isReconciled");
      SibTr.exit(tc, "isReconciled", Boolean.TRUE);
    }
      
    return true;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getControlHandler(com.ibm.ws.sib.common.ProtocolType, com.ibm.ws.sib.trm.topology.Cellule)
   */
  public ControlHandler getControlHandler(ProtocolType type, SIBUuid8 sourceCellule)
  {
    throw new InvalidOperationException(
      nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0005",
          new Object[] {
            "com.ibm.ws.sib.processor.proxyhandler.BusGroup",
            "1:651:1.54",
            getName() },
          null));
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getSubscriptionList()
   */
  public List getSubscriptionList()
  {
    throw new InvalidOperationException(
      nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0005",
          new Object[] {
            "com.ibm.ws.sib.processor.proxyhandler.BusGroup",
            "1:666:1.54",
            getName() },
          null));
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#createControlAdapter()
   */
  public void createControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "createControlAdapter");
    controlAdapter = new ForeignBus(messageProcessor, this);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "createControlAdapter", controlAdapter);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#registerControlAdapterAsMBean()
   */
  public void registerControlAdapterAsMBean()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#deregisterControlAdapterMBean()
   */
  public void deregisterControlAdapterMBean()
  {
  }

  public boolean isOverrideOfQOSByProducerAllowed()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "isOverrideOfQOSByProducerAllowed");
    boolean override = _foreignDestinationDefault.isOverrideOfQOSByProducerAllowed();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "isOverrideOfQOSByProducerAllowed", Boolean.valueOf(override));
    return override;
  }

  public Object getContextValue(String keyName)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getContextValue", keyName);
      
    Map context = _foreignDestinationDefault.getDestinationContext();
    
    Object property = context.get(keyName);
    
    if (null == property)
      property = getTarget().getContextValue(keyName);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getContextValue", property);
      
    return property; 
  }  
  
  Map getDestinationContext()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDestinationContext");

    Map context = _foreignDestinationDefault.getDestinationContext();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getDestinationContext", context);
    
    return context;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getForwardRoutingPath()
   */
  public List getForwardRoutingPath()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getForwardRoutingPath");
      SibTr.exit(tc, "getForwardRoutingPath", null);
    }
    // No op for foreign destinations
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getReplyDestination()
   */
  public JsDestinationAddress getReplyDestination()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getReplyDestination");
      SibTr.exit(tc, "getReplyDestination", null);
    }
    // No op for foreign destinations
    return null;
  }

  public JsDestinationAddress getRoutingDestinationAddr(JsDestinationAddress inAddress,
                                                        boolean fixedMessagePoint)
  throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRoutingDestinationAddr", new Object[] {this,
                                                                 inAddress,
                                                                 Boolean.valueOf(fixedMessagePoint)});

    // As we're a foreign bus all we know is that a routing destination
    // is required and it must be the address that we've just been given
    // (which is the address of the foreign destination) so return it.
    JsDestinationAddress outAddress = inAddress;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getRoutingDestinationAddr", outAddress);

    return outAddress;
  }

  /**
   * Check permission to access a Destination
   * 
   * @param secContext
   * @param operation
   * @return
   * @throws SICoreException
   */   
  public boolean checkDestinationAccess(
    SecurityContext secContext,
    OperationType operation) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "checkDestinationAccess",
        new Object[] { secContext, operation });

    boolean allow = false;

    // This style of access check is against a bus only.
    if(accessChecker.checkForeignBusAccess(secContext,
                              getName(),
                              operation))
    {
      allow = true;
    }
       
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "checkDestinationAccess", Boolean.valueOf(allow));

    return allow; 
  }   
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isTopicAccessCheckRequired()
   */
  public boolean isTopicAccessCheckRequired()
  {
    // Not applicable to bus handlers
    return false;
  } 
  
  /**
   * Check permission to access a Discriminator
   * 
   * @param secContext
   * @param operation
   * @return
   * @throws SICoreException
   */   
  public boolean checkDiscriminatorAccess(
    SecurityContext secContext,
    OperationType operation) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "checkDiscriminatorAccess",
        new Object[] { secContext, operation });

    // We don't apply discriminator access checks to foreign destinations
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "checkDiscriminatorAccess", Boolean.valueOf(true));

    return true; 
  }       
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isForeignBus()
   */
  public boolean isForeignBus()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isForeignBus");

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isForeignBus",Boolean.TRUE);

    return true;
  }
  
  public String toString()
  {
    return "BusHandler to [" 
      + _foreignBusDefinition 
      + "] with dest default [" 
      + _foreignDestinationDefault
      + "]";
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isAlias()
   */
  public boolean isAlias()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isAlias");

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isAlias", Boolean.FALSE);

    return false;
  }
}
