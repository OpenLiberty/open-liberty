/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel;

public interface ConnectionInterface
{
  /*
   * Actually invalidates this connection.  For every day operation this should
   * never be called.  When invoked, the implementation should:
   * <ul>
   * <li>Attempt to notify it's peer (depending on the argument)</li>
   * <li>Purge the connection from any tracker with which it is
   *     registered.</li>
   * <li>Wake up any outstanding exchanges with an exception.</li>
   * <li>Send an exception to each conversation receive listener.</li>
   * <li>Mark all the conversations that use the connection as
   *     invalid.</li>
   * <li>Close the underlying physical socket.</li>
   * </ul>
   * @param notifyPeer When set to true, an attempt is made to notify
   * our peer that we are about to close the socket.
   * @param throwable The exception to link to the "JFapConnectionBrokenException"
   * passed to outstanding exchanges and conversation receive listeners.
   * @param debugReason The reason for the invalidate to be called
   */
  void invalidate(boolean notifyPeer, Throwable throwable, String debugReason);
  
  /*
   * This method will just check that the state of the connection is not in 
   * open state. Any other state is defined as closed by this method.
   */
  boolean isClosed();
}
