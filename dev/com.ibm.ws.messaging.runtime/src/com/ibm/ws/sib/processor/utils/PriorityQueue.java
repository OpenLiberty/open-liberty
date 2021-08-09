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
 * A straightforward priority queue implementation.  Note that this class
 * is not thread safe.  Users must provide their own thread safety.
 * This class sorts priorities from highest to lowest, so the highest
 * priority element is always at the front of the queue.  Use negative
 * priorities to sort from lowest to highest.
 */
public class PriorityQueue {

  // Defaut starting size of the heap
  private static final int DEFAULT_START_SIZE = 16;

  // current heap size
  protected int size;

  // Heap elements
  protected PriorityQueueNode[] elements;

  // Standard debug/trace gunk
  private static final TraceComponent tc =
    SibTr.register(
      PriorityQueue.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  /**
   * Default constructor.
   */
  public PriorityQueue()
  {
    this(DEFAULT_START_SIZE);
  }

  /**
   * Use this constructor to preallocate a heap of a specific size.
   *
   * @param startSize The initial size of the heap.
   */
  public PriorityQueue(int startSize)
  {
//    if (tc.isEntryEnabled())
//      SibTr.entry(tc, "PriorityQueue.<ctor>", new Integer(startSize));
    if (startSize != 0)
      elements = new PriorityQueueNode[startSize];
    else elements = null;
    size = 0;
//    if (tc.isEntryEnabled())
//      SibTr.exit(tc, "PriorityQueue.<ctor>");
  }

  /**
   * Calculate the array position of the parent of node i.  It is half of
   * the child's position after subtracting 1 (round down).
   *
   * @param i node we wish to find the parent of.
   * @return the index of the parent of node i.
   */
  protected final int parent(int i)
  {
/*    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "parent", new Integer(i));
      SibTr.exit(tc, "parent", new Integer((i-1)/2));
    }
*/    return (i-1)/2;
  }

  /**
   * Calculate the array position of the left child of node i.  It is
   * double + 1 the parent's position.
   *
   * @param i node we wish to find left child of.
   * @return the index of the left child of node i.
   */
  protected final int left(int i)
  {
/*    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "left", new Integer(i));
      SibTr.exit(tc, "left", new Integer(i*2+1));
    }
*/    return i*2+1;
  }

  /**
   * Calculate the array position o fthe right child of node i.  It is
   * double + 2 the parent's position.
   *
   * @param i node we wish to find the right child of.
   * @return the index of the right child of node i.
   */
  protected final int right(int i)
  {
/*    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "right", new Integer(i));
      SibTr.exit(tc, "right", new Integer(i*2 + 2));
    }
*/    return i*2 + 2;
  }

  /**
   * Check if the queue is empty.
   *
   * @return true if the queue is empty, false otherwise.
   */
  public final boolean isEmpty()
  {
/*    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isEmpty");
      SibTr.exit(tc, "isEmpty", new Boolean(size==0));
    }
*/    return size == 0;
  }

  /**
   * Get the number of elements in the queue.
   *
   * @return the number of elements in the queue.
   */
  public final int size()
  {
/*    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "size");
      SibTr.exit(tc, "size", new Integer(size));
    }
*/    return size;
  }

  /**
   * Insert data with the given priority into the heap and heapify.
   *
   * @param priority the priority to associate with the new data.
   * @param value the date to enqueue.
   */
  public final void put(long priority, Object value)
  {
//    if (tc.isEntryEnabled())
//      SibTr.entry(tc, "put", new Object[] { new Long(priority), value});

    PriorityQueueNode node = new PriorityQueueNode(priority,value);

    // Resize the array (double it) if we are out of space.
    if (size == elements.length)
    {
      PriorityQueueNode[] tmp = new PriorityQueueNode[2*size];
      System.arraycopy(elements,0,tmp,0,size);
      elements = tmp;
    }

    int pos = size++;
    setElement(node, pos);

    moveUp(pos);

//    if (tc.isEntryEnabled())
//      SibTr.exit(tc, "put");
  }

  /**
   * Advance a node in the queue based on its priority.
   *
   * @param pos the index of the nod to advance.
   */
  protected void moveUp(int pos)
  {
//    if (tc.isEntryEnabled())
//      SibTr.entry(tc, "moveUp", new Integer(pos));

    PriorityQueueNode node = elements[pos];
    long priority = node.priority;

    while ((pos > 0) && (elements[parent(pos)].priority > priority))
    {
      setElement(elements[parent(pos)], pos);
      pos = parent(pos);
    }

    setElement(node, pos);

//    if (tc.isEntryEnabled())
//      SibTr.exit(tc, "moveUp");
  }

  /**
   * Dequeue the highest priority element from the queue.
   *
   * @return the highest priority object.
   * @throws NoSuchElementException if the queue is empty.
   */
  public final Object getMin()
    throws NoSuchElementException
  {
    PriorityQueueNode max = null;
    if (size == 0)
      throw new NoSuchElementException();
        
    max = elements[0];
    setElement(elements[--size], 0);
    heapify(0);

    return max.value;
  }

  /**
   * Set an element in the queue.
   *
   * @param node the element to place.
   * @param pos the position of the element.
   */
  protected final void setElement(PriorityQueueNode node, int pos)
  {
//    if (tc.isEntryEnabled())
//      SibTr.entry(tc, "setElement", new Object[] { node, new Integer(pos)});

    elements[pos] = node;
    node.pos      = pos;

//    if (tc.isEntryEnabled())
//      SibTr.exit(tc, "setElement");
  }

  /**
   * Reheap the queue.
   *
   * @param position The position from which to start reheaping.
   */
  protected void heapify(int position)
  {
//    if (tc.isEntryEnabled())
//      SibTr.entry(tc, "heapify", new Integer(position));

    // Heapify the remaining heap
    int i = -1;
    int l;
    int r;
    int smallest = position;

    // Heapify routine from CMR.
    // This was done without recursion.
    while (smallest != i)
    {
      i = smallest;
      l = left(i);
      r = right(i);
      if ((l < size) && (elements[l].priority < elements[i].priority))
        smallest = l;
      else smallest = i;
      if ((r < size) && (elements[r].priority < elements[smallest].priority))
        smallest = r;
      if (smallest != i)
      {
        PriorityQueueNode tmp = elements[smallest];
        setElement(elements[i], smallest);
        setElement(tmp, i);
      }
    }

//    if (tc.isEntryEnabled())
//      SibTr.exit(tc, "heapify");
  }

  /**
   * Non-destructively return the top element in the queue.
   *
   * @return the top element in the queue.
   * @throws NoSuchElementException if the queue is empty.
   */
  protected PriorityQueueNode minNode()
    throws NoSuchElementException
  {
    if (size > 0)
    {
      return elements[0];
    } 
    
    throw new NoSuchElementException();
  }

  /**
   * Non-destructively return the top data element in the queue.
   *
   * @return the top data element in the queue.
   * @throws NoSuchElementException if the queue is empty.
   */
  public Object minElement()
    throws NoSuchElementException
  {
/*    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "minElement");
      SibTr.exit(tc, "minElement", minNode().value);
    }
*/    return minNode().value;
  }

  /**
   * Non-destructively return the priority of the top data element in the
   * queu.
   *
   * @return the priority of the top data element in the queue.
   * @throws NoSuchElementException if the queue is empty.
   */
  public long minPriority()
    throws NoSuchElementException
  {
/*    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "minPriority");
      SibTr.exit(tc, "minPriority", new Long(minNode().priority));
    }
*/    return minNode().priority;
  }


  /////////////////////////////////////////////////////////
  // Inner classes
  /////////////////////////////////////////////////////////

  /**
   * Elements of this inner class represent heap entries.
   */
  class PriorityQueueNode {
    public long priority;
    public int  pos;
    public Object value;

    public PriorityQueueNode(long p, Object v)
    {
      priority = p;
      value    = v;
    }
  }

}
