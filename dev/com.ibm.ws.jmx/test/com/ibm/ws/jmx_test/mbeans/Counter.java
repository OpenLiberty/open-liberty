/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx_test.mbeans;

/**
 *
 */
public class Counter implements CounterMBean {

    private int i = 0;

    public Counter() {}

    @Override
    public int getValue() {
        return i;
    }

    @Override
    public void increment() {
        ++i;
    }

    @Override
    public void print() {
        System.out.println(i);
    }

    @Override
    public void reset() {
        i = 0;
    }
}
