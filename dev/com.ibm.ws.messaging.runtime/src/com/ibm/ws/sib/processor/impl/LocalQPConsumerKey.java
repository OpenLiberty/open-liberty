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
// This class was previously called JSConsumerKey.java in CMVC

package com.ibm.ws.sib.processor.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.LockingCursor;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerKey;
import com.ibm.ws.sib.processor.impl.interfaces.DispatchableConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DispatchableKey;
import com.ibm.ws.sib.processor.impl.interfaces.JSConsumerKey;
import com.ibm.ws.sib.processor.impl.interfaces.JSConsumerManager;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class contains a reference to a ConsumerPoint along with some state
 * data and a getCursor to be used by that ConsumerPoint. It is a only a data
 * structure and thus does not have any setters and getters and does not do any
 * significant processing.
 *
 * @author tevans
 */
public class LocalQPConsumerKey extends AbstractConsumerKey
  implements DispatchableKey, Filter
{
  private static final TraceComponent tc =
    SibTr.register(
      LocalQPConsumerKey.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

   // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  protected boolean ready;
  protected boolean detached;
  protected final DispatchableConsumerPoint consumerPoint;
  protected long version;
  private Reliability unrecoverability = null;
  private final JSConsumerManager consumerDispatcher;
  private final SIBUuid12 connectionUuid;
  private boolean forwardScanning;

 
  private boolean started = false;
  private final ItemStream itemStream;
  
  /** Selector artefacts for matching */
  private String selectorString = null;
  private Selector selectorTree;
  private Selector discriminatorTree;
  private boolean specific;
  
  /** An array of filters. Unless XD has registered for message classification
   * the array will have just one member.
   */
  private LocalQPConsumerKeyFilter[] consumerKeyFilter = null;
  
  /** Flag whether classifications have been reset. If they have, then the filters
   * need to be reset also.
   */
  private boolean pendingFlowReset = false;
  
  /**
   * Create a new ConsumerKey
   */
  LocalQPConsumerKey(
    DispatchableConsumerPoint consumerPoint,
    JSConsumerManager consumerDispatcher,
    SelectionCriteria criteria,
    SIBUuid12 connectionUuid,
    boolean forwardScanning,
    JSConsumerSet consumerSet) throws SISelectorSyntaxException, SIDiscriminatorSyntaxException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "LocalQPConsumerKey",
        new Object[]{consumerPoint,
                     consumerDispatcher,
                     criteria,
                     connectionUuid,
                     Boolean.valueOf(forwardScanning),
                     consumerSet});

    this.consumerDispatcher = consumerDispatcher;
    this.consumerPoint = consumerPoint;
    this.connectionUuid = connectionUuid;
    this.detached = false;
    this.ready = false;
    this.forwardScanning = forwardScanning;
    this.keyGroup = null;
    this.consumerSet = consumerSet;
    
    if(consumerSet != null)
    {
      classifyingMessages = true;
    }
    
    // Create a new cursor
    try
    {
      itemStream = ((ConsumerDispatcher)consumerDispatcher).getItemStream();
      LockingCursor cursor = null;
      // If this is a forward scanning consumer the MS cursor needs its
      // jumpback capability disabled, the newLockingItemCursor method takes
      // a jumpback enabled parameter so we pass in !forwardScanning as we want
      // it enabled if it is not forward scanning.
      if(itemStream != null)
      {
        // Instantiate a set of pairs of filters and cursors
        createNewFiltersAndCursors(itemStream);
      }
      else
      {
        // Instantiate a single default filter and cursor pair.
        consumerKeyFilter = new LocalQPConsumerKeyFilter[1];
        consumerKeyFilter[0] = new LocalQPConsumerKeyFilter(this, 0, null);
        cursor = ((ConsumerDispatcher)consumerDispatcher).getReferenceStream().newLockingCursor(null, !forwardScanning); 

        consumerKeyFilter[0].setLockingCursor(cursor);
      }
    }
    catch (MessageStoreException e)
    {
      // MessageStoreException shouldn't occur so FFDC.
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.LocalQPConsumerKey.LocalQPConsumerKey",
        "1:198:1.23",
        this);

      SibTr.exception(tc, e);

      SIResourceException newE = new SIResourceException(
                                    nls.getFormattedMessage(
                                      "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                      new Object[] {
                                        "com.ibm.ws.sib.processor.impl.LocalQPConsumerKey",
                                        "1:208:1.23",
                                        e },
                                      null),
                                    e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "LocalQPConsumerKey", newE);

      throw newE;

    }

    // Parse the selector expression. We don't defer this cos we need to feed
    // back a selector syntax exception, if necessary, at this point.
    try
    {
      specific = false;
      if(criteria != null)
      {
        if((criteria.getSelectorString() != null) && !criteria.getSelectorString().equals(""))
        {
          selectorString = criteria.getSelectorString(); 
          selectorTree =
            consumerDispatcher.getMessageProcessor().getMessageProcessorMatching().parseSelector(
              criteria.getSelectorString(),
              criteria.getSelectorDomain());
          specific = true;
        }
        if((criteria.getDiscriminator() != null) && !criteria.getDiscriminator().equals(""))
        {
          discriminatorTree =
            consumerDispatcher.getMessageProcessor().getMessageProcessorMatching().parseDiscriminator(
              criteria.getDiscriminator());
          specific = true;
        }
      }
    }
    catch (SISelectorSyntaxException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.LocalQPConsumerKey.LocalQPConsumerKey",
        "1:251:1.23",
        this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "LocalQPConsumerKey", e);

      throw e;
    }
    catch (SIDiscriminatorSyntaxException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.LocalQPConsumerKey.LocalQPConsumerKey",
        "1:265:1.23",
        this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "LocalQPConsumerKey", e);

      throw e;
    }

    // Add the consumerPoint to the consumerSet if classifying messages
    if(classifyingMessages)
    {
      consumerSet.addConsumer(consumerPoint);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "LocalQPConsumerKey", this);
  }


  /**
   * Set the state of a ConsumerKey to ready and move it in to the ready list.
   * This causes the ready consumer list version, the specific ready consumer counter and the
   * specific ready consumer list version all to be incremented (if appropriate).
   *
   * This method only has any effect if the state was originally not ready.
   *
   * @param unrecoverability - type of unrecoverability required by the consumer
   */
  public void ready(Reliability unrecoverability) throws SINotPossibleInCurrentConfigurationException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "ready", unrecoverability);

    if (closedReason == ConsumerKey.CLOSED_DUE_TO_DELETE)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "ready", "SINotPossibleInCurrentConfigurationException - deleted");
      throw new SINotPossibleInCurrentConfigurationException(
        nls.getFormattedMessage(
          "DESTINATION_DELETED_ERROR_CWSIP00221",
          new Object[] {
            consumerDispatcher.getDestination().getName(),
            consumerDispatcher.getMessageProcessor().getMessagingEngineName() },
          null));
    }
    if (closedReason == ConsumerKey.CLOSED_DUE_TO_RECEIVE_EXCLUSIVE)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "ready", "SINotPossibleInCurrentConfigurationException - receive Exclusive");
      throw new SINotPossibleInCurrentConfigurationException(
        nls.getFormattedMessage(
            "DESTINATION_EXCLUSIVE_ERROR_CWSIP00222",
            new Object[] {
              consumerDispatcher.getDestination().getName(),
              consumerDispatcher.getMessageProcessor().getMessagingEngineName() },
            null));
    }
    if (closedReason == ConsumerKey.CLOSED_DUE_TO_ME_UNREACHABLE)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "ready", "SINotPossibleInCurrentConfigurationException - localisation unreachable");
      throw new SINotPossibleInCurrentConfigurationException(
        nls.getFormattedMessage(
              "DESTINATION_UNREACHABLE_ERROR_CWSIP00223",
              new Object[] {
                consumerDispatcher.getDestination().getName(),
                consumerDispatcher.getMessageProcessor().getMessagingEngineName() },
              null));
    }

    //get the readyConsumer list lock
    synchronized (consumerDispatcher.getDestination().getReadyConsumerPointLock())
    {
      // Remember the recoverability of this consumer
      this.unrecoverability = unrecoverability;

      // Let the ConsumerDispatcher know about this new ready consumer
      if(keyGroup == null)
      {
        if(!ready)
          version = consumerDispatcher.newReadyConsumer(this, specific);
      }
      else
        keyGroup.ready(null);

      this.ready = true;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "ready", Long.valueOf(version));
  }

  /**
   * Set the state of a ConsumerKey to not ready and remove it from the ready list.
   * or decrement the specific ready counter (if appropriate). Note that there is no need to
   * increment the specific consumer list version number.
   *
   * This method only has any effect if the state was originally ready.
   */
  public void notReady()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "notReady");

    //get the ready consumer list lock
    synchronized (consumerDispatcher.getDestination().getReadyConsumerPointLock())
    {
      if(keyGroup == null)
      {
        if(ready)
          consumerDispatcher.removeReadyConsumer(this, specific);
      }
      else
        keyGroup.notReady();

      //set the state to not ready
      ready = false;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "notReady");
  }

  /**
   * Mark this consumer as waiting for a message. This is only of interest
   * when consuming remotely. In this case we also reserve the right to
   * modify the suggested timeout.
   *
   * @param timeout  Supplied timeout
   * @param modifyTimeout unused in the local version
   */
  public long waiting(long timeout, boolean modifyTimeout)
  {
    // Add code when running remotely
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "waiting");
      SibTr.exit(tc, "waiting", Long.valueOf(timeout));
    }

    return timeout;
  }

  /**
   * Return the getCursor for this consumer. This method is only called in the
   * case where messages are not classified by XD.
   */
  public LockingCursor getDefaultGetCursor()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDefaultGetCursor");

    LockingCursor cursor = consumerKeyFilter[0].getGetCursor();
    if(keyGroup != null)
      cursor = keyGroup.getDefaultGetCursor();
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getDefaultGetCursor", cursor);

    return cursor;
  }

  public LockingCursor getGetCursor(SIMPMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getGetCursor", msg);

    // First we need to get the index for the appropriate cursor
    // The zeroth index is reserved for non-classified messages
    int classification = 0;
    LockingCursor cursor = null;
    if(classifyingMessages)
    {
      // Take the classifications read lock
      consumerSet.takeClassificationReadLock();
      
      // Classifications may change dynamically, so check the pendingReset
      // flag under this lock. If classifications have changed then we'll
      // set up the filters again
      if(pendingFlowReset)
      { 
        pendingFlowReset = false;
        // Reset the filters under the lock
        resetFlowProperties();
      }
      
      classification = consumerSet.getGetCursorIndex(msg);
      cursor = consumerKeyFilter[classification].getGetCursor();
      
      if(keyGroup != null)
        cursor = keyGroup.getGetCursor(classification);
      
      // Free the classifications read lock
      consumerSet.freeClassificationReadLock();
    }
    else
    {
      cursor = consumerKeyFilter[0].getGetCursor();
      if(keyGroup != null)
        cursor = keyGroup.getGetCursor(0);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getGetCursor", cursor);

    return cursor;
  }
  
  /**
   * Retrieves the GetCursor that is associated with the filter at the
   * specified classification index.
   * 
   * This method should be called under the classification readlock as it
   * references resources that are dependent on the current set of
   * classifications.
   *  
   * @param classification
   * @return
   */
  protected LockingCursor getGetCursor(int classification)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getGetCursor", Integer.valueOf(classification));
    
    // First we need to get the index for the appropriate cursor
    // The zeroth index is reserved for non-classified messages

    LockingCursor cursor = consumerKeyFilter[classification].getGetCursor();
    if(keyGroup != null)
      cursor = keyGroup.getGetCursor(classification);
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getGetCursor", cursor);

    return cursor;
  }

  /**
   * Determine the index of the getCursor to use based on the classifications defined
   * for the ConsumerSet that a consumer belongs to. If SIB is not registered with
   * XD and no classification is being performed then the default index is returned
   *
   * Delegates to the consumerSet if non-null, otherwise returns the
   * default zeroth index.
   * 
   * @param prevClassIndex index used previously
   * @return
   */
  private int chooseGetCursorIndex(int classification)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "chooseGetCursorIndex");
    
    int classIndex = 0;
    
    if(classifyingMessages)
      classIndex = consumerSet.chooseGetCursorIndex(classification);
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "chooseGetCursorIndex", classIndex);

    return classIndex;
  }      
  
  /**
   * Detach this consumer
   */
  public void detach() throws SIResourceException, SINotPossibleInCurrentConfigurationException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "detach");

    // Make sure we are not ready
    notReady();

    // Remove us from any group we are a member of
    if(keyGroup != null)
      keyGroup.removeMember(this);

    // Remove this consumer from the CD's knowledge
    consumerDispatcher.detachConsumerPoint(this);

    // Cleanly dispose of the getCursor
    if(classifyingMessages)
    {
      // Take the classifications read lock
      consumerSet.takeClassificationReadLock();
      
      int numFilters = consumerKeyFilter.length;
      for(int i=0;i<numFilters;i++)
        consumerKeyFilter[i].detach();
      
      // Free the classifications read lock
      consumerSet.freeClassificationReadLock();
    }
    else
      consumerKeyFilter[0].detach();

    synchronized (this)
    {
      detached = true;
    }
    
    // Remove the consumerPoint to the consumerSet if the latter has been specified
    if(classifyingMessages)
      consumerSet.removeConsumer(consumerPoint); 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "detach");
  }

  /**
   * Return the consumer's connection Uuid
   */
  public SIBUuid12 getConnectionUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getConnectionUuid");
      SibTr.exit(tc, "getConnectionUuid", connectionUuid);
    }
    return connectionUuid;
  }

  /**
   * Return the consumer's forward scanning setting
   */
  public boolean getForwardScanning()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getForwardScanning");
      SibTr.exit(tc, "getForwardScanning", Boolean.valueOf(forwardScanning));
    }
    return forwardScanning;
  }

  /**
   * Determine if this consumer will require this message to be recoverable
   * @param msg message to be delivered
   * @return true if recovery is required
   * @throws SIStoreException
   */
  public boolean requiresRecovery(SIMPMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "requiresRecovery", new Object[] { msg });

    boolean recoverable;

    Reliability msgReliability = msg.getReliability();
    recoverable = msgReliability.compareTo(unrecoverability) > 0;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "requiresRecovery", Boolean.valueOf(recoverable));

    return recoverable;
  }
  /**
   * Returns the consumerDispatcher.
   * @return ConsumerDispatcher
   */
  public JSConsumerManager getConsumerManager()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getConsumerManager");
      SibTr.exit(tc, "getConsumerManager", consumerDispatcher);
    }
    return consumerDispatcher;
  }
  
  /**
   * Method close.
   * <p>Called when the local consumers are being implicitly closed</p>
   */
  public boolean close(int closedReason, SIBUuid8 qpoint)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "close", 
          new Object[] { Integer.valueOf(closedReason), 
                         qpoint});

    /*
     * Indicate that the consumer is closed, due to the local destination
     * being deleted/receiveExclusive/unreachable
     */
    this.closedReason = closedReason;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "close", true);
    return true;
  }

  /**
   * Method notifyConsumerPointAboutException.
   * <p>Notify the consumerKeys consumerPoint about an exception
   * This method should not be called whilst holding the consumerPoint
   * lock from inside ConsumerDispatcher.
   * @param e - The exception to notify the consumerPoint about
   */
  public void notifyConsumerPointAboutException(SIException e)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "notifyConsumerPointAboutException", e);
    consumerPoint.notifyException(e);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "notifyConsumerPointAboutException");
  }

  /**
   * Method notifyReceiveAllowed
   * <p>Notify the consumerKeys consumerPoint about change of Receive Allowed state
   * @param isAllowed - New state of Receive Allowed for localization
   */
  public void notifyReceiveAllowed(boolean isAllowed, DestinationHandler destinationBeingModified)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "notifyReceiveAllowed", Boolean.valueOf(isAllowed));

    if (consumerPoint.destinationMatches(destinationBeingModified, consumerDispatcher))
    {
      consumerPoint.notifyReceiveAllowed(isAllowed);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "notifyReceiveAllowed");
  }

  /**
   * Determine if this key is ready
   */
  public boolean isKeyReady()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isKeyReady");
    }

    boolean returnValue = false;
    if(keyGroup == null)
    {
      returnValue = ready;
    }
    else
    {
      //check if the ordering context is ready
      returnValue = keyGroup.isKeyReady();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isKeyReady", Boolean.valueOf(returnValue));

    return returnValue;
  }

  /**
   * Make this key not ready
   */
  public void markNotReady()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "markNotReady");

    ready = false;
    if(keyGroup != null)
      keyGroup.markNotReady();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "markNotReady");

  }

  public long getVersion()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getVersion");
      SibTr.exit(tc, "getVersion", Long.valueOf(version));
    }

    return version;
  }

  public DispatchableConsumerPoint getConsumerPoint()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getConsumerPoint");
      SibTr.exit(tc, "getConsumerPoint", consumerPoint);
    }

    return consumerPoint;
  }

  public boolean isSpecific()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isSpecific");
      SibTr.exit(tc, "isSpecific", Boolean.valueOf(specific));
    }

    return specific;
  }

  /**
   * Return a true ConsumerKey (which we are so just return this)
   */
  public DispatchableKey resolvedKey()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "resolvedKey");
      SibTr.exit(tc, "resolvedKey", this);
    }

    return this;
  }

  /**
   * Called by MS when scanning an ItemStream for matching items
   */
  public boolean filterMatches(AbstractItem item)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "filterMatches", item);

    boolean result = true;

    if (selectorTree != null || discriminatorTree != null)
    {
      // Defect 382250, set the unlockCount from MsgStore into the message
      // in the case where the message is being redelivered.
      // 668676, get the message if available
      JsMessage msg = ((SIMPMessage)item).getMessageIfAvailable();
      if ( msg == null)
      {
    	  result = false;
      }
      else
      {
          int redelCount = ((SIMPMessage)item).guessRedeliveredCount();

          if (redelCount > 0)
          {
              if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  SibTr.debug(tc, "Set deliverycount into message: " + redelCount);
              msg.setDeliveryCount(redelCount);
          }

          // Evaluate message against selector tree
          result =
            consumerDispatcher.getMessageProcessor().getMessageProcessorMatching().evaluateMessage(
              selectorTree,
              discriminatorTree,
              msg);
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "filterMatches", Boolean.valueOf(result));

    return result;
  }

  /**
   * Return this key's parent if it is a member of a keyGroup
   */
  public JSConsumerKey getParent()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getParent");

    JSConsumerKey key = this;
    if(keyGroup != null)
      key = keyGroup;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getParent", key);

    return key;
  }

  /**
   * The ConsumerKey doesn't actually care if the consumer is started but its
   * ConsumerKey does
   */
  public void start()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "start");

    if(!started)
    {
      started = true;
      if(keyGroup != null)
        keyGroup.startMember();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "start");
  }

  /**
   * The ConsumerKey doesn't actually care if the consumer is stopped but its
   * ConsumerKey does
   */
  public void stop()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "stop");

    if(started)
    {
      started = false;
      if(keyGroup != null)
        keyGroup.stopMember();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "stop");
  }

  public String toString()
  {
    String returnStr = super.toString();
    if (consumerDispatcher != null)
    {
      returnStr = consumerDispatcher.toString() + returnStr + ", " + selectorString;
    }

    return returnStr;
  }

  public Selector getSelector()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getSelector");
      SibTr.exit(tc, "getSelector", selectorTree);
    }
    return selectorTree;
  }

  /**
   * Retrieve the next message using an appropriate cursor.
   *
   * @param classification
   * @return
   * @throws MessageStoreException
   */
  public SIMPMessage getMessageLocked() throws MessageStoreException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getMessageLocked");
    
    SIMPMessage msg = null;
    
    if(classifyingMessages)
    {
      // Take the classifications read lock
      consumerSet.takeClassificationReadLock();

      // Classifications may change dynamically, so check the pendingReset
      // flag under this lock. If classifications have changed then we'll
      // set up the filters again
      if(pendingFlowReset)
      {
        pendingFlowReset = false;
        resetFlowProperties();
      }
        
      // Determine which cursor to use. chooseGetCursorIndex() will 
      // randomly choose an index if XD has specified weightings.
      // The method can be called more than once. This supports the situation
      // where we've looked for but not found any messages of a particular 
      // classification. 
      // 
      // If chooseGetCursorIndex() returns 0, then either no classifications were 
      // specified or we've already tried all the classifications. An index
      // of 0 leads to the use of the default (unclassified) cursor.
      int classIndex = -1;
      while(classIndex != 0)
      {
        classIndex = chooseGetCursorIndex(classIndex);
     
        // Always use the cursor straight from the key
        msg = getMessageLocked(classIndex);

        if(msg != null)
          classIndex = 0;
      }

      // Free the classifications read lock
      consumerSet.freeClassificationReadLock();
    }
    else // Not classifying
    {
      // Use the zeroth index
      msg = getMessageLocked(0);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMessageLocked", msg);

    return msg;
  }

  /**
   * Retrieve the next message using an appropriate cursor.
   * 
   * if the classification parameter is 0 then the default (unclassified) cursor
   * will be used.
   * 
   * @param classification
   * @return
   * @throws MessageStoreException
   */
  protected SIMPMessage getMessageLocked(int classification) throws MessageStoreException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getMessageLocked", Integer.valueOf(classification));
    SIMPMessage msg = null;
    if(!classifyingMessages)
      msg = (SIMPMessage)getDefaultGetCursor().next();
    else
      msg = (SIMPMessage)getGetCursor(classification).next();
    
    if (msg != null)
      msg.setLocalisingME(consumerDispatcher.getMessageProcessor().getMessagingEngineUuid());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMessageLocked", msg);

    return msg;
  }

  public String getSelectorString()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
    {
      SibTr.entry(tc, "getSelectorString");
      SibTr.exit(tc, "getSelectorString", selectorString);  
    }    
    return selectorString;
  }
 
  
  /**
   * Create the filters and cursors for this Key. If XD has registered a
   * MessageController we'll need a cursor-filter pair for each classification.
   * 
   * @throws SIResourceException
   * @throws MessageStoreException
   */
  private void createNewFiltersAndCursors(ItemStream itemStream) 
    throws SIResourceException, MessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createNewFiltersAndCursors", itemStream);
    
    LockingCursor cursor = null;
 
    // Instantiate a new array of Filters and associated cursors. If there is
    // no message classification, then we'll instantiate a single filter and 
    // cursor pair.
    if(classifyingMessages)
    {
      // Classifying messages for XD.
      // this work should be done under the classifications readlock acquired higher
      // up in the stack
      JSConsumerClassifications classifications = consumerSet.getClassifications();
      int numClasses = classifications.getNumberOfClasses();
      consumerKeyFilter = new LocalQPConsumerKeyFilter[numClasses+1];
      for(int i=0;i<numClasses+1;i++)
      {
        String classificationName = null;
        // The zeroth filter belongs to the default classification, which has a 
        // null classification name
        if(i > 0)
          classificationName = classifications.getClassification(i);
        consumerKeyFilter[i] = new LocalQPConsumerKeyFilter(this, i, classificationName);
        cursor = itemStream.newLockingItemCursor(consumerKeyFilter[i], !forwardScanning);
        consumerKeyFilter[i].setLockingCursor(cursor);
      }
    }
    else
    {
      // No message classification
      consumerKeyFilter = new LocalQPConsumerKeyFilter[1];
      consumerKeyFilter[0] = new LocalQPConsumerKeyFilter(this, 0, null);
      cursor = itemStream.newLockingItemCursor(consumerKeyFilter[0], !forwardScanning);
      consumerKeyFilter[0].setLockingCursor(cursor);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createNewFiltersAndCursors");
  }

  /**
   * The Flow properties on a ConsumerSet may be reset dynamically. This
   * method supports the resetting of cursors and filters.
   * 
   */
  public void notifyResetFlowProperties() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "notifyResetFlowProperties");
 
    pendingFlowReset = true;
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "notifyResetFlowProperties"); 
  }  
  
  /**
   * The Flow properties on a ConsumerSet may be reset dynamically. This
   * method supports the resetting of cursors and filters.
   * 
   */
  private void resetFlowProperties() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "resetFlowProperties");
 
    // Tidy up what we had before
    int numFilters = consumerKeyFilter.length;    
    for(int i=0;i<numFilters;i++)
    {
      // Discard the cursor
      consumerKeyFilter[i].discard();
    }    
    
    // Recreate the filters and cursors
    try
    {
      ItemStream itemStream = ((ConsumerDispatcher)consumerDispatcher).getItemStream();
      createNewFiltersAndCursors(itemStream);
    } 
    catch (Exception ex)
    {
      // TODO Appropriate messaging?
      // FFDC
      FFDCFilter.processException(
          ex,
        "com.ibm.ws.sib.processor.impl.LocalQPConsumerKey.resetFlowProperties",
        "1:1101:1.23",
        this);

      SIErrorException finalE =
        new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] { "com.ibm.ws.sib.processor.impl.LocalQPConsumerKey", "1:1108:1.23", ex },
            null),
            ex);

      SibTr.exception(tc, finalE);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] { "com.ibm.ws.sib.processor.impl.LocalQPConsumerKey", "1:1114:1.23", SIMPUtils.getStackTrace(ex) });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "resetFlowProperties", finalE);

      throw finalE;        
    }
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "resetFlowProperties"); 
  }

  public void refillQueuePoint(boolean gathering) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
    {
      SibTr.entry(this, tc, "refillQueuePoint"); 
      SibTr.exit(this, tc, "refillQueuePoint");
    }
  }
  
  public void initiateRefill() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
    {
      SibTr.entry(this, tc, "initiateRefill", this);
      SibTr.exit(this, tc, "initiateRefill");
    }
  }


  public boolean isRefillAllowed() 
  {
    // no-op
    return false;
  }
}
