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

package com.ibm.wsspi.sib.pacing;

public interface MessagePacing {

  // ***** MDB dispatch hook points *****

  /**
   * Hook for negotiating max message batch size of MDB dispatcher. Called once
   * at MDB dispatcher startup.
   * 
   * @param bus
   *          The name of the bus.
   * @param destination
   *          The name of the destination.
   * @param currentBatchSize
   *          Configured maximum message batch size.
   * 
   * @return The overridden maximum message batch size.
   */
  int overrideMaxBatchSize(String bus, String destination, int currentBatchSize);

  /**
   * This method is implemented by XD and called by the MDB dispatcher once a
   * batch of messages have been allocated to this dispatcher but before they
   * have been processed by an MDB.
   * <p>
   * It returns an object that implements the MessagePacerContext interface
   * (below). This object support the suspendAsynchDispatcher() method that is
   * called by the RA and will return true if XD wishes the dispatcher to stop
   * processing messages or false if it wishes the RA to continue.
   * 
   * A suspended RA is resumed by a call to resume() (see below)
   * 
   * @param bus
   *          The name of the bus.
   * @param destination
   *          The name of the destination.
   * @param dispatcherContext
   *          Opaque object that it passed in on the MessagePacingController
   *          method resumeAsynchDispatcher() method to resume the MDB
   *          dispatcher after it has been suspended.
   * 
   * @return MessagePacerContext. See above.
   */
  AsynchDispatchScheduler preAsynchDispatch(String bus, String destination, Object dispatcherContext);

  /**
   * This method is implemented by XD and called by the MDB dispatcher just
   * prior to entering the MDB's onMessage() method (a matching exit method,
   * postMdbInvoke() is also supported).
   * 
   * @param correlator
   *          An opaque object, unique at in the system at this point in time
   *          (although may be re-used in future, once postMdbInvoke() has been
   *          called), used to correlate between this call and the matching exit
   *          call, postMdbInvoke(). This object supports the equals() method to
   *          allow equality checking with the matching post call.
   * @param messagePacerContext
   *          The object returned by XD on the preAsynchDispatch() method.
   */
  void preMdbInvoke(Object correlator, AsynchDispatchScheduler messagePacerContext);

  /**
   * This method is implemented by XD and called by the MDB dispatcher just
   * after exiting the MDB's onMessage() method (a matching entry method,
   * preMdbInvoke() is also supported).
   * 
   * @param correlator
   *          An opaque object, unique at in the system at this point in time
   *          (although may be re-used in future, once postMdbInvoke() has been
   *          called), used to correlate between this call and the matching
   *          entry call, preMdbInvoke(). This object supports the equals()
   *          method to allow equality checking with the matching pre call.
   * @param messagePacerContext
   *          The object returned by XD on the preAsynchDispatch() method.
   */
  void postMdbInvoke(Object correlator, AsynchDispatchScheduler messagePacerContext);

  // ***** Synchronous JMS receive hook points *****

  /**
   * This method is implemented by XD and called by the SIB JMS layer as part of
   * a receive() request from a JMS MessageConsumer. This method is called prior
   * to requesting a message from the ME. If the implementer wishes to delay
   * requesting the message it is their responsibility to block this method for
   * the required period of time.
   * 
   * @param bus
   *          The name of the bus.
   * @param destination
   *          The name of the destination.
   * @param correlator
   *          An opaque object, unique in the system at this point in time
   *          (although may be re-used in future, once postSynchReceive() has
   *          been called), used to correlate between this call and the matching
   *          post call, postSynchReceive(). This object supports the equals()
   *          method to allow equality checking with the matching post call.
   * @param timeout
   *          The timeout, in milliseconds, specified by the JMS application
   *          when issuing the JMS receive call. 0: Indefinite wait, -1: No wait
   * 
   * @return The time in milliseconds that the receive should wait for after
   *         returning from this method.
   */
  long preSynchReceive(String bus, String destination, Object correlator, long timeout);

  /**
   * This method is implemented by XD and called by the SIB JMS layer as part of
   * a receive() request from a JMS MessageConsumer. This method is called after
   * a request for a message from the ME has completed. If the implementer
   * wishes to delay return the message to the application it is their
   * responsibility to block this method for the required period of time.
   * 
   * @param bus
   *          The name of the bus
   * @param destination
   *          The name of the destination
   * @param correlator
   *          An opaque object, unique at in the system at this point in time
   *          (although may be re-used in future, once this method has been
   *          called), used to correlate between this call and the matching pre
   *          call, preSynchReceive(). This object supports the equals() method
   *          to allow equality checking with the matching post call.
   * @param message
   *          A boolean that indicates if a message was successfully received
   *          (true: message, false: no message)
   */
  void postSynchReceive(String bus, String destination, Object correlator, boolean message);

}
