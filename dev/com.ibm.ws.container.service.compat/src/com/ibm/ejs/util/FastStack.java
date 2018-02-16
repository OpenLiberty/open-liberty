/*******************************************************************************
 * Copyright (c) 1997, 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.util;

public class FastStack<T> {

    private T[] stack;
    private int topOfStack;
    private int currentCapacity;

    public FastStack() {
        this(11);
    }

    public FastStack(int initialCapacity) {
        stack = createArray(initialCapacity);
        topOfStack = -1;
        currentCapacity = stack.length;
    }

    @SuppressWarnings("unchecked")
    private T[] createArray(int size) {
        return (T[]) new Object[size];
    }

    public T peek() {
        if (topOfStack >= 0)
            return (stack[topOfStack]);
        else
            return null;
    }

    public T peek(Class clz) {
System.out.println("Toshi : class : " + clz);
        if (topOfStack >= 0) {
            for(int i = topOfStack; i >= 0; i--) {
System.out.println("Toshi ; stack [" + i + "] class : " + stack[i].getClass());
                if (clz.isAssignableFrom(stack[i].getClass())) {
System.out.println("Toshi ; return : " + stack[i]);
                    return (stack[i]);
                }
            }
        }
System.out.println("Toshi ; return : null");
        return null;
    }

    public T push(T o) {
        ensureCapacity(topOfStack + 1);
        stack[++topOfStack] = o;
        return stack[topOfStack];
    }

    public T pop() {
        if (topOfStack >= 0) {
            // defect 146239.4 : Arvind Srinivasan 
            // Item was being popped, by the reference still remained in the stack
            //
            T result = stack[topOfStack];
            stack[topOfStack--] = null;
            //
            return result;
        }
        return null;
    }

    /**
     * Resetting a stack empties the stack.
     */
    public void reset() {
        // defect 146239.4 : Arvind Srinivasan
        //
        while (topOfStack >= 0)
            stack[topOfStack--] = null;
        //
    }

    private void ensureCapacity(int newCapacity) {
        if (newCapacity >= currentCapacity) {
            T[] newStack = createArray(currentCapacity + (2 * currentCapacity));
            System.arraycopy(stack, 0, newStack, 0, stack.length);
            stack = newStack;
            currentCapacity = stack.length;
        }
    }

    public int getTopOfStackIndex() {
        return topOfStack;
    }
}
