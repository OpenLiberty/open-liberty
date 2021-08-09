/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.util;

import java.util.concurrent.atomic.AtomicReference;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Concurrent Stack class with index embedded. It is based on the algorithm
 * proposed by Kent Treiber.
 * <p>
 * Basic idea is to avoid locks by using compare-and-swap (CAS) operations
 * instead. The <code>push</code> and <code>pop</code> operations use a CAS in a
 * while loop.
 * </p>
 * 
 * @author Zhi Gan (ganzhi@cn.ibm.com)
 */
@Trivial
public class LockFreeIndexedStack<E> {
    final AtomicReference<StackNode<E>> top;

    public LockFreeIndexedStack() {
        top = new AtomicReference<StackNode<E>>(null);
    }

    /**
     * Remove all nodes from stack and return the old top node.
     * 
     * @return the old top node, which can be used to iterate over all cleaned
     *         stack elements.
     */
    public StackNode<E> clean() {
        do {
            final StackNode<E> oldTop = top.get();
            if (top.compareAndSet(oldTop, null))
                return oldTop;

        } while (true);
    }

    // Node definition for stack
    public static class StackNode<E> {
        final E data;
        StackNode<E> next;
        int index;

        public StackNode(E d) {
            super();
            this.data = d;
        }

        public StackNode<E> getNext() {
            return next;
        }

        public E getValue() {
            return data;
        }
    }

    /**
     * Pop data from the Stack.
     * 
     * @return topmost element of the stack, or null if stack is empty.
     */
    public E pop() {
        StackNode<E> oldTop, newTop;

        while (true) {
            oldTop = top.get();
            if (oldTop == null)
                return null;
            newTop = oldTop.next;
            if (top.compareAndSet(oldTop, newTop))
                break;
        }

        return oldTop.data;
    }

    /**
     * Pop data from the Stack if its size is larger than <code>minSize</code>
     * 
     * @param minSize
     *            Lower bound of the stack size when poping
     * 
     * @return topmost element of the stack, or null if stack is empty or size
     *         is not larger than <code>minSize</code>.
     */
    public E popWithLimit(int minSize) {
        StackNode<E> oldTop, newTop;

        while (true) {
            oldTop = top.get();
            if (oldTop == null)
                return null;

            if (oldTop.index + 1 <= minSize)
                return null;

            newTop = oldTop.next;
            if (top.compareAndSet(oldTop, newTop))
                break;
        }

        return oldTop.data;
    }

    /**
     * Push data onto Stack.
     * 
     * @param d
     *            data to be pushed onto the stack.
     */
    public void push(E d) {
        StackNode<E> oldTop, newTop;

        newTop = new StackNode<E>(d);

        while (true) {
            oldTop = top.get();
            newTop.next = oldTop;
            if (oldTop != null)
                newTop.index = oldTop.index + 1;
            else
                newTop.index = 0;
            if (top.compareAndSet(oldTop, newTop))
                return;
        }
    }

    /**
     * Push data onto Stack while keeping size of the stack under
     * <code>maxSize</code>
     * 
     * @param d
     *            data to be pushed onto the stack.
     * @param maxSize
     *            Maximal size of the stack.
     * 
     * @return <code>True</code> if succeed. False if the size limitation has
     *         been reached
     */
    public boolean pushWithLimit(E d, int maxSize) {
        StackNode<E> oldTop, newTop;

        newTop = new StackNode<E>(d);

        while (true) {
            oldTop = top.get();

            newTop.next = oldTop;
            if (oldTop != null) {
                newTop.index = oldTop.index + 1;
                if (newTop.index >= maxSize)
                    return false;
            } else {
                if (maxSize == 0)
                    return false;
                newTop.index = 0;
            }

            if (top.compareAndSet(oldTop, newTop))
                return true;
        }
    }

    /**
     * Check to see if Stack is empty.
     * 
     * @return true if stack is empty.
     */
    public boolean isEmpty() {
        if (top.get() == null) {
            return true;
        } else {
            return false;
        }
    }

    public int size() {
        final StackNode<E> oldTop = top.get();
        if (oldTop == null)
            return 0;
        else
            return oldTop.index + 1;
    }

    /**
     * Return copy of the top data on the Stack
     * 
     * @return copy of top of stack, or null if empty.
     */
    public E peek() {
        final StackNode<E> oldTop = top.get();
        if (oldTop == null) {
            return null;
        } else {
            return oldTop.data;
        }
    }
}
