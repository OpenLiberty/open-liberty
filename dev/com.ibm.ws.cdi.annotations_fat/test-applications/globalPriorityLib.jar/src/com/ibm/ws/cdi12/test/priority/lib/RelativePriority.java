/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.priority.lib;

public class RelativePriority {

    /*
     * Alternatives have the concept of high vs. low priority.
     * The highest priority alternative will be used.
     */
    public static final int HIGH_PRIORITY = 100;
    public static final int LOW_PRIORITY = 10;

    /*
     * Interceptors and Decorators use priority for order.
     * Lower priorities are called first.
     */
    public static final int FIRST = 1;

    private RelativePriority() {}
}
