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
package com.ibm.ws.sib.processor.utils;

import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;

import java.util.NoSuchElementException;

/**
 * A standard queue class which allows objects to be enqueued,
 * dequeued or "popped".  Note that this class is not thread safe.
 * Make sure your own data structures are thread safe, or you'll have
 * problems.
 */
public class Queue 
{

  // Where everything gets stored
  private volatile Object[] m_array;

  // Head and tail refs
  private volatile int m_head = 0;
  private volatile int m_tail = 0;

  // Standard debug/trace gunk
  private static final TraceComponent tc =
    SibTr.register(
      Queue.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
      
  /**
   * Default constructor.  Initial size is 4.
   */
  public Queue()
  {
    m_array = new Object[4];
  }

  /**
   * Constructor which allows one to select the initial size.
   *
   * @param initial_size The initial size of the queue.
   */
  public Queue(int initial_size)
  {
    if (initial_size < 1)
      initial_size = 3;
    m_array = new Object[initial_size+1]; // +1 to allow for full Q of initial size
  }

  /**
   * Check if the queue is empty.
   *
   * @return true if the queue is empty, false otherwise.
   */
  public final boolean isEmpty()
  {
    return m_head == m_tail;
  }

  /**
   * Remove all elements from the queue.
   */
  public final void makeEmpty()
  { 
//    if (tc.isEntryEnabled())
//      SibTr.entry(tc, "makeEmpty");
    m_head = m_tail;
//    if (tc.isEntryEnabled())
//      SibTr.exit(tc, "makeEmpty");
  }

  /**
   * Empty the queue, and also set every element of the internal
   * array to "null" so that the removed elements can be GC'd.
   */
  public final void makeEmptyAndClean()
  {
//    if (tc.isEntryEnabled())
//      SibTr.entry(tc, "makeEmptyAndClean");
    makeEmpty();
    for (int i=0;i<m_array.length;++i) m_array[i]=null;
//   if (tc.isEntryEnabled())
//      SibTr.entry(tc, "makeEmptyAndClean");
  }

  /**
   * Return the number of elements in the queue.
   *
   * @return the number of elements in the queue.
   */
  public final int size()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "size");
    int result = (m_tail >= m_head) ? (m_tail - m_head) : (m_array.length - m_head + m_tail);
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "size", new Integer(result));
    return result;
  }

  /**
   * Store an object in the queue.
   *
   * @param obj the object to be stored.
   */
  public final void enqueue(Object obj)
  {
//    if (tc.isEntryEnabled())
//      SibTr.entry(tc, "enqueue", obj);
    // m_array has at least one position in it that is free.
    m_array[m_tail++] = obj;
    if (m_tail == m_array.length)
      m_tail = 0;
    if (m_head == m_tail)
      expand_array();
//    if (tc.isEntryEnabled())
//      SibTr.exit(tc, "enqueue");
  }

  /**
   * Return the first element on the queue.
   *
   * @return the first element on the queue.
   * @throws NoSuchElementException if the queue is empty.
   */
  public final Object dequeue()
    throws NoSuchElementException
  {
//    if (tc.isEntryEnabled())
//      SibTr.entry(tc, "dequeue");

    if (m_head == m_tail)
      throw new NoSuchElementException();
    Object obj = m_array[m_head];
    m_array[m_head++] = null;
    if (m_head == m_array.length)
      m_head = 0;

//    if (tc.isEntryEnabled())
//      SibTr.exit(tc, "dequeue", obj);
    return obj;
  }

  /**
   * Return the first element on the queue without removing it.
   *
   * @return the first element on the queue.
   * @throws NoSuchElementException if the queue is emtpy.
   */
  public final Object peek()
    throws NoSuchElementException
  {
//    if (tc.isEntryEnabled())
//      SibTr.entry(tc, "peek");

    if (m_head == m_tail)
      throw new NoSuchElementException();

//    if (tc.isEntryEnabled())
//      SibTr.exit(tc, "peek", m_array[m_head]);
    return m_array[m_head];
  }

  /**
   * Increase the size of the internal array to accomodate more
   * queue elements.
   */
  private final void expand_array()
  {
//    if (tc.isEntryEnabled())
//      SibTr.entry(tc, "expand_array");

    int length = m_array.length;
    Object[] m_new = new Object[length*2];
    System.arraycopy(m_array, m_head, m_new, m_head, length-m_head);
    System.arraycopy(m_array, 0, m_new, length, m_tail);
    m_tail += length;
    m_array = m_new;

//    if (tc.isEntryEnabled())
//      SibTr.exit(tc, "expand_array");
  }

}
