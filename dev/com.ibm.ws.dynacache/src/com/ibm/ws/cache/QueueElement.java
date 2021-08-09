/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

  /**
   *  A <code>QueueElement</code> provides a base class for elements that
   *  are stored in a <code>Queue</code>. <p>
   */

  public class QueueElement {

      /**
       *  Pointer to previous and next elements in the queue. 
       */

      protected QueueElement previous;
      protected QueueElement next;

      /**
       *  The <code>Queue</code> this element is currently an element of.
       *
       *  Note, a <code>QueueElement</code> can belong to at most one queue
       *  at a time.
       */

      protected Queue queue;

      /**
       *  Remove this <code>QueueElement</code> from its associated queue.
       *
       *  It is an error to attempt to remove a queue element that is not
       *  currently associated with any queue. 
       */

      public void removeFromQueue() {

	  if (queue == null) {
	      throw new RuntimeException("Queue element is not " +
					 "member of a queue");
	  }

	  queue.remove(this);

      }				// removeFromQueue

  }				// QueueElement

