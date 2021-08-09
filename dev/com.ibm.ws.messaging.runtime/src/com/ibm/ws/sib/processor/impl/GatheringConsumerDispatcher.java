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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.am.Alarm;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport;
import com.ibm.ws.sib.processor.impl.exceptions.SIMPNoResponseException;
import com.ibm.ws.sib.processor.impl.interfaces.BrowseCursor;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerKey;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.DispatchableConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumableKey;
import com.ibm.ws.sib.processor.impl.interfaces.DispatchableKey;
import com.ibm.ws.sib.processor.impl.interfaces.JSConsumerManager;
import com.ibm.ws.sib.processor.impl.interfaces.JSKeyGroup;
import com.ibm.ws.sib.processor.impl.interfaces.MPDestinationChangeListener;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.items.AOValue;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.trm.dlm.Capability;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;

public class GatheringConsumerDispatcher extends AbstractConsumerManager implements JSConsumerManager
{
  /**
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      GatheringConsumerDispatcher.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);


  /** NLS for component */
  static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private JSConsumerManager localCD;
  private Map<SIBUuid8, JSConsumerManager> remoteCDs = new HashMap<SIBUuid8, JSConsumerManager>();
  private ConsumerDispatcherState consumerDispatcherState;

  /** The complete list of attached consumerPoints */
  protected HashMap<GatheringConsumerKey, AttachmentDetails> consumerPoints;

  class AttachmentDetails
  {
    private SIBUuid12 connectionUuid;
    private SelectionCriteria criteria;
    private boolean readAhead;
    private boolean forwardScanning;
    private JSConsumerSet consumerSet;

    AttachmentDetails(SelectionCriteria criteria,
        SIBUuid12 connectionUuid, boolean readAhead,
        boolean forwardScanning, JSConsumerSet consumerSet)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "AttachmentDetails",
            new Object[] {criteria, connectionUuid, Boolean.valueOf(readAhead), Boolean.valueOf(forwardScanning),consumerSet});

      this.criteria = criteria;
      this.connectionUuid = connectionUuid;
      this.readAhead = readAhead;
      this.forwardScanning = forwardScanning;
      this.consumerSet = consumerSet;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "AttachmentDetails");
    }
  }

  class GatheringChangeListener implements MPDestinationChangeListener, AlarmListener
  {
    private SIBUuid12 destUuid;
    private DestinationDefinition definition;
    private AbstractRemoteSupport support;
    private Set scopedMEs;
    private long retryInterval;
    private ArrayList<SIBUuid8> unreachableMEs;
    //@GuardedBy("this")
    private Alarm reattachAlarm;

    public GatheringChangeListener (Set scopedMEs, SIBUuid12 destUuid, DestinationDefinition definition, AbstractRemoteSupport support ) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "GatheringChangeListener", new Object[] {scopedMEs, destUuid, definition, support});

      this.destUuid = destUuid;
      this.definition = definition;
      this.support = support;
      this.scopedMEs = scopedMEs;
      this.unreachableMEs = new ArrayList<SIBUuid8>();
      this.retryInterval = _messageProcessor.getCustomProperties().get_gathering_reattach_interval();

      _messageProcessor.getDestinationChangeListener().addMPDestinationChangeListener(this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(this, tc, "GatheringChangeListener");
    }

    /**
     * Method required by the DestinationLocationChangeListener interface. We are only interested in changes in
     * our own destination with Get capability. If the change is of interest to us then we synchronize to ensure
     * that nothing (particularly deletions) gets missed during the initialisation phase in chooseConsumerManager.
     */
    public void destinationLocationChange (SIBUuid12 destId, Set additions, Set deletions, Capability capability)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(this, tc, "destinationLocationChange", new Object[] {destId, additions, deletions, capability});

      if (destUuid.equals(destId) && capability == Capability.GET)
      {

        Iterator it = additions.iterator();
        while(it.hasNext())
        {
          final SIBUuid8 meUuid = (SIBUuid8)it.next();
          if (!meUuid.equals(_messageProcessor.getMessagingEngineUuid()) &&
              (scopedMEs==null || (scopedMEs!=null && scopedMEs.contains(meUuid))))
          {

            // Force connection to be re-established before we attempt
            // to use the connection in a reattach
            _messageProcessor.getMPIO().forceConnect(meUuid);

            synchronized (this)
            {
              final AnycastInputHandler aih = support.getAnycastInputHandler(meUuid, null, definition, true);
              try
              {
                aih.getRCD().reachabilityChange(true);

                reattachRemoteCD(aih.getRCD());
                // If attach succeeded then add the CD back into the list
                addRemoteCD(meUuid, aih.getRCD());

                if (unreachableMEs.contains(meUuid))
                  unreachableMEs.remove(meUuid);

              }
              catch(SIException e)
              {
                // no FFDC code needed
                SibTr.exception(tc, e);

                // We were told the remote ME was now up but were unable to reconnect to it. We therefore
                // kick off a timer to retry the attach.
                unreachableMEs.add(meUuid);
                if (reattachAlarm == null) {
                  reattachAlarm = _messageProcessor.getAlarmManager().create(retryInterval,this);
                }

              }
            }
          }
        }

        it = deletions.iterator();
        while(it.hasNext())
        {
          final SIBUuid8 meUuid = (SIBUuid8)it.next();
          if (!meUuid.equals(_messageProcessor.getMessagingEngineUuid()) &&
              (scopedMEs==null || (scopedMEs!=null && scopedMEs.contains(meUuid))))
          {
            synchronized(this)
            {
              if (unreachableMEs.contains(meUuid))
                unreachableMEs.remove(meUuid);
            }
            removeRemoteCD(meUuid);
          }
        }
      }


      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(this, tc, "destinationLocationChange");
    }

    public void alarm(Object arg0)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "GatheringChangeListener.alarm", arg0);

      synchronized(this)
      {
        // Reset the alarm
        reattachAlarm = null;

        final Iterator it = unreachableMEs.iterator();
        while(it.hasNext())
        {
          final SIBUuid8 meUuid = (SIBUuid8)it.next();
          final AnycastInputHandler aih = support.getAnycastInputHandler(meUuid, null, definition, true);
          try
          {
            aih.getRCD().reachabilityChange(true);
            reattachRemoteCD(aih.getRCD());
            // If attach succeeded then add the CD back into the list
            addRemoteCD(meUuid, aih.getRCD());

            // Remove the uuid from the unreachable list
            it.remove();
          }
          catch(SIException e)
          {
            // no FFDC code needed
            SibTr.exception(tc, e);

            // We were told the remote ME was now up but were unable to reconnect to it. We therefore
            // kick off a timer to retry the attach.  Leave the uuid in the unreachable list.
            if (reattachAlarm == null) {
              reattachAlarm = _messageProcessor.getAlarmManager().create(retryInterval,this);
            }
          }
        }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "GatheringChangeListener.alarm", this);
    }
  }


  public GatheringConsumerDispatcher(BaseDestinationHandler destination, JSConsumerManager localCD)
  {
    super(destination);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "GatheringConsumerDispatcher", new Object[] {destination, localCD});

    this.localCD = localCD;
    this.consumerDispatcherState = new ConsumerDispatcherState();

    // Create the consumer point map
    consumerPoints = new HashMap<GatheringConsumerKey, AttachmentDetails>();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "GatheringConsumerDispatcher", this);
  }

  protected void reattachRemoteCD(RemoteConsumerDispatcher rcd)
    throws SINotPossibleInCurrentConfigurationException,
    SIDestinationLockedException,
    SISelectorSyntaxException,
    SIDiscriminatorSyntaxException,
    SIResourceException,
    SISessionDroppedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "reattachRemoteCD", new Object[]{rcd});

    synchronized(consumerPoints)
    {
      // re-attach all consumerpoints to the now available rcd
      Iterator<GatheringConsumerKey> it = consumerPoints.keySet().iterator();
      while (it.hasNext())
      {
        GatheringConsumerKey gck = it.next();
        if (!gck.isAttached(rcd.getUuid()))
        {
          AttachmentDetails details = consumerPoints.get(gck);
          ConsumerKey ck = rcd.attachConsumerPoint(gck.getConsumerPoint(),
                              details.criteria,
                              details.connectionUuid,
                              details.readAhead,
                              details.forwardScanning,
                              details.consumerSet);

          // Reinsert the new RemoteConsumerKey into the gathering consumer key
          ((GatheringConsumerKey)gck).
            reattachConsumer(rcd.getLocalisationUuid(), (ConsumableKey)ck );
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "reattachRemoteCD");
  }

  public void addRemoteCD (SIBUuid8 meUuid, JSConsumerManager rcd)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "addRemoteCD", new Object[]{meUuid, rcd});

    synchronized (remoteCDs)
    {
      remoteCDs.put(meUuid, rcd);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "addRemoteCD");
  }

  public void removeRemoteCD (SIBUuid8 meUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "removeRemoteCD", new Object[]{meUuid});

    synchronized (remoteCDs)
    {
      remoteCDs.remove(meUuid);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "removeRemoteCD");
  }

  public void attachBrowser(BrowserSessionImpl browserSession)
    throws SINotPossibleInCurrentConfigurationException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "attachBrowser", browserSession);

    synchronized (remoteCDs)
    {
      // Attach to the local consumer point
      if (localCD != null) localCD.attachBrowser(browserSession);

      // Attach to the remote consumer points
      Iterator<JSConsumerManager> it = remoteCDs.values().iterator();
      while (it.hasNext()) {
        it.next().attachBrowser(browserSession);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "attachBrowser");
  }

  public ConsumerKey attachConsumerPoint(ConsumerPoint consumerPoint, SelectionCriteria criteria,
                                          SIBUuid12 connectionUuid, boolean readAhead,
                                          boolean forwardScanning, JSConsumerSet consumerSet)
    throws SINotPossibleInCurrentConfigurationException,
    SIDestinationLockedException,
    SISelectorSyntaxException,
    SIDiscriminatorSyntaxException,
    SIResourceException,
    SISessionDroppedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "attachConsumerPoint", new Object[] {consumerPoint, criteria, connectionUuid, readAhead, forwardScanning, consumerSet});

    // Map to hold all gathered consumer keys = 1 local (if any) and N remote (if any)
    HashMap<SIBUuid8,ConsumableKey> consumerKeys = new HashMap<SIBUuid8,ConsumableKey>();
    GatheringConsumerKey consumerKey = null;
    ArrayList<RemoteConsumerDispatcher> retryRCDs = new ArrayList<RemoteConsumerDispatcher>();

    synchronized (consumerPoints)
    {
      synchronized (remoteCDs)
      {
        // Attach to the local consumer point & gather
        if (localCD != null)
        {
            // Get the consumer key for the local qpoint
            consumerKeys.put(_messageProcessor.getMessagingEngineUuid(),
                             (ConsumableKey)localCD.attachConsumerPoint(consumerPoint, criteria, connectionUuid, readAhead, forwardScanning, consumerSet));
        }

        // Attach the remote consumer points & gather
        Iterator<SIBUuid8> it = remoteCDs.keySet().iterator();
        while (it.hasNext())
        {
          SIBUuid8 meUuid = it.next();
          JSConsumerManager cm = remoteCDs.get(meUuid);
          try
          {
            consumerKeys.put(meUuid, (ConsumableKey)cm.attachConsumerPoint(consumerPoint, criteria, connectionUuid, readAhead, forwardScanning, consumerSet));
          }
          catch(SIMPNoResponseException e)
          {
            // No FFDC code needed
            // We get a SIMPNoResponseException if the DME was reachable but we didnt get a response
            // to a createStream or flushResponse in a suitable time. 
            // Defect 558352 : A deadlock can occur due to the response waiting on the RemoteMessageReceiver
            // thread which in turn is waiting on the AsyncUpdateThread which in turn is waiting on the
            // GatheringConsumerPoints lock which we currently hold!
            // Therefore this functionality should allow us to release the consumerpoints lock and retry.
            
            SibTr.exception(tc, e);
            
            it.remove();
            retryRCDs.add((RemoteConsumerDispatcher)cm);
            
          }
          catch(SIResourceException e) // DME unreachable
          {
            // No FFDC code needed
            // We get a ResourceException if the DME was unreachable, if we get anything else
            // then we throw it back to the consumer.

            SibTr.exception(tc, e);

            // We couldnt connect to the remote ME because it was unavailable. When it becomes available
            // we will reattach.
            it.remove();
          }
        }
      }

      // On an IME, an exception here will result in
      // a completed being sent to the RME which means the consumer just receives no msg.
      // On an RME the exception will be propogated to the app

      // Create a new gathered consumer key which represents the separate gathered consumer keys
      consumerKey =
        new GatheringConsumerKey((DispatchableConsumerPoint)consumerPoint,
                          this,
                          consumerKeys,
                          criteria,
                          connectionUuid,
                          forwardScanning);

      // Add consumerKey to list
      consumerPoints.put(consumerKey,
                         new AttachmentDetails(criteria,
                                               connectionUuid,
                                               readAhead,
                                               forwardScanning,
                                               consumerSet));
    } // Release consumerPoints lock
    
    // Defect 558352
    // If we didnt get responses back from DMEs then retry those DMEs now that we have released the lock
    // and given other threads a chance to take it and resolve their work.
    while(!retryRCDs.isEmpty())
    {
      // Wait a while to allow other threads to resolve their work under the lock
      try {
        Thread.sleep(SIMPConstants.GATHERING_NO_RESPONSE_ATTACH_INTERVAL);
      } 
      catch (InterruptedException e1) 
      {
        // No FFDC code needed
      }
      
      Iterator<RemoteConsumerDispatcher> it = retryRCDs.iterator();
      try
      {
        // Attempt to reattach the remaining failed DME attaches (under the lock)
        reattachRemoteCD(it.next());
        
        // Success - remove the failed attach so it wont get tried again
        it.remove();
      }
      catch (SIMPNoResponseException e)
      {
        // No FFDC code needed
        // Still no response so leave the entry in the list to be retried again.
      }
      catch (SIResourceException e)
      {
        // No FFDC code needed
        // ME is unreachable so this will get reattached when it comes back
        it.remove();
        
      } // Any other exceptions will propogate to the consumer    
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "attachConsumerPoint", consumerKey);

    return consumerKey;
  }

  public void checkInitialIndoubts(DispatchableConsumerPoint point)
    throws SIResourceException
  {
    // no-op
  }

  public void detachBrowser(BrowserSessionImpl browserSession)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "detachBrowser", browserSession);

    synchronized (remoteCDs)
    {
      // Attach to the local consumer point
      if (localCD != null) localCD.detachBrowser(browserSession);

      // Attach to the remote consumer points
      Iterator<JSConsumerManager> it = remoteCDs.values().iterator();
      while (it.hasNext()) {
        it.next().detachBrowser(browserSession);
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "detachBrowser");
  }

  /**
   * No-op
   */
  public int getConsumerCount()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getConsumerCount");

    int consumerCount;

    synchronized (consumerPoints)
    {
      consumerCount = consumerPoints.size();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getConsumerCount", Integer.valueOf(consumerCount));

    return consumerCount;
  }

  public ConsumerDispatcherState getConsumerDispatcherState()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getConsumerDispatcherState");
      SibTr.exit(tc, "getConsumerDispatcherState", consumerDispatcherState);
    }
    return consumerDispatcherState;
  }

  public boolean isLocked()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isLocked");

    boolean isLocked = true;

    synchronized (remoteCDs)
    {
      // Attach to the local consumer point
      if (localCD != null)
        isLocked &= localCD.isLocked();

      // Attach to the remote consumer points
      Iterator<JSConsumerManager> it = remoteCDs.values().iterator();
      while (it.hasNext())
        isLocked &= it.next().isLocked();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isLocked", Boolean.valueOf(isLocked));

    return isLocked;
  }

  public boolean isNewTransactionAllowed(TransactionCommon transaction)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isNewTransactionAllowed");
      SibTr.exit(tc, "isNewTransactionAllowed", Boolean.valueOf(false));
    }
    return false;
  }

  public BrowseCursor getBrowseCursor(SelectionCriteria selectionCriteria)
    throws SIResourceException, SISelectorSyntaxException, SIDiscriminatorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getBrowseCursor", selectionCriteria);

    BrowseCursor cursor = new GatheringBrowseCursor(selectionCriteria, localCD, remoteCDs);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getBrowseCursor", cursor);
    return cursor;
  }

  public SIMPMessage getMessageByValue(AOValue value)
  throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getMessageByValue", Long.valueOf(value.getMsgId()));

    SIMPMessage msgItem = null;
    SIBUuid8 sourceMEUuid = value.getSourceMEUuid();
    synchronized (remoteCDs)
    {
      if (sourceMEUuid.equals(_messageProcessor.getMessagingEngineUuid()))
        msgItem = localCD.getMessageByValue(value);
      else
      {
        AnycastInputHandler aih = _baseDestHandler.getAnycastInputHandler(sourceMEUuid, null, false);
        if (aih!=null)
          msgItem = aih.getRCD().getMessageByValue(value);
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMessageByValue", msgItem);
    return msgItem;
  }

  public boolean isPubSub()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isPubSub");
      SibTr.exit(tc, "isPubSub", Boolean.valueOf(false));
    }
    return false;
  }


  public void registerChangeListener(Set scopedMEs, SIBUuid12 uuid, DestinationDefinition definition, AbstractRemoteSupport support)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "registerChangeListener", new Object[] {uuid, definition, support});

    new GatheringChangeListener(scopedMEs, uuid, definition, support);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "registerChangeListener");

  }

  /**
   * Helper method to create a ConsumerKeyGroup.
   * @return
   */
  protected JSKeyGroup createConsumerKeyGroup(JSConsumerSet consumerSet)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createConsumerKeyGroup", consumerSet);

    // We do not implement keyGroups for gathering consumers.
    JSKeyGroup ckg = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createConsumerKeyGroup", ckg);

    return ckg;
  }

  public void detachConsumerPoint(ConsumerKey consumerKey)
    throws SIResourceException, SINotPossibleInCurrentConfigurationException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "detachConsumerPoint", consumerKey);

    synchronized(consumerPoints)
    {
      consumerPoints.remove(consumerKey);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "detachConsumerPoint");
  }

  /**
   * This list is cloned to stop illegal access to the ConsumerPoints
   * controlled by this ConsumerDispatcher
   * @return
   */
  public List<DispatchableKey> getConsumerPoints()
  {
    //no-op
    return null;
  }

}
