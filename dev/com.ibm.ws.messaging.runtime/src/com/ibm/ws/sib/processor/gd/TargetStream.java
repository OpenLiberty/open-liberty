/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.gd;

import java.util.List;

import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.mfp.control.ControlSilence;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.runtime.SIMPDeliveryReceiverControllable;
import com.ibm.ws.sib.processor.runtime.impl.TargetStreamControl;

/**
 * An output stream that handles delivery of messages to all local consumers
 * This stream exists if and only if this ME hosts local-consumers.
 *
 * Synchronization:
 *  - Most read/writes to data-structures in this class are synchronized on 'this'.
 *    Sending messages upstream is done outside 'synchronized (this)' since the
 *    response to a nack sent upstream can occur on the same thread if the
 *    producer is local.
 *    If the nacks were sent within a synchronized block, this can cause a deadlock
 *    due to nested synchronization attempts with different orders across multiple
 *    OutputStream objects.
 *  - Messages from local producers will go direct to local consumers where possible
 *    and will therefore not be seen by th TargetStream
 *  - The Nack Response Timer estimates are updated by synchronizing on nrtLock
 *
 */
public interface TargetStream extends Stream
{
  /**
   * An enumeration for the different states of a target stream
   * @author tpm100
   */
  public static abstract class TargetStreamState 
    implements SIMPDeliveryReceiverControllable.StreamState
  {
    protected String name;
    protected int id;
    
    protected TargetStreamState(String _name, int _id)
    {
      name = _name;
      id = _id;  
    }
    public String toString()
    {
      return name;
    }
    
    public int getValue()
    {
      return id;
    }
  }
  
  /**
   * @return a TargetStreamState representing the current state of the
   * target stream
   * @author tpm
   */
  public TargetStreamState getStreamState();
  
  
  public void setCompletedPrefix( long prefix ) 
   throws SIResourceException;
   
  public long reconstituteCompletedPrefix( long newPrefix ); 
   
 /**
   * Writes a Value tick into the stream
   *
   * @exception Thrown from writeRange and handleNewGap
   */ 
  public void writeValue( MessageItem m );
  
  /**
   * Writes a range of Completed ticks into the stream
   *
   * @exception Thrown from writeRange 
   */
  public void writeSilence(ControlSilence m);
  
  /**
    * Writes a Silence into the stream for a filtered out Value message
    *
    * @exception Thrown from writeRange and handleNewGap
    */ 
   public void writeSilence( MessageItem m );
   
  /**
   * This method will walk the oststream from the doubtHorizon to the stamp
   * and send Nacks for any Unknown or Request ticks it finds.
   * It will also change Unknown ticks to Reuqested.
   *
   * @exception GDException  Thrown from the writeRange method
   */
  public void processAckExpected(long stamp );
  
  /**
   * Flush this stream by discarding any nacks we may be waiting on
   * (all such ticks automatically become finality).  When this
   * process is complete, any persistent state for the stream may be
   * discarded.
   *
   * @throws GDException if an error occurs in writeRange.
   */
  public void flush();
  
 /**
  * This method returns the last tick in the stream 
  * which is not in Unknown state 
  * 
  * @return long lastKnownTick
  */ 
  public long getLastKnownTick();
  
  /**
   * @return TargetStreamControl the control adapter for 
   * this target stream
   */
  public TargetStreamControl getControlAdapter();
  
  /**
   * @return an unmodifiable list of all of the messages in the value
   * state on this stream. The list contains objects of type MessageItem.
   * If there are no messages then an empty list is returned.
   */
  public List<MessageItem> getAllMessagesOnStream();
  
  /**
   * @return a long for the number of messages received since reboot
   */
  public long getNumberOfMessagesReceived();
  
  /**
   * Count the number of active messages on the stream
   * @return
   * @author tpm
   */
  public long countAllMessagesOnStream();
  
  /**
   * The time the most recent message arrived on this targetstream.
   * @return Time (ms)
   */
  public long getLastMsgReceivedTimestamp();
}
