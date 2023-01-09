/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.context.list;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Example third-party thread context.
 */
public class ListContext {
    public static final String CONTEXT_NAME = "List";

    static ThreadLocal<ConcurrentLinkedQueue<Integer>> local = new ThreadLocal<ConcurrentLinkedQueue<Integer>>();

    // API methods:

    public static void add(int value) {
        ConcurrentLinkedQueue<Integer> list = local.get();
        if (list == null)
            throw new IllegalStateException();
        else
            list.add(value);
    }

    public static String asString() {
        ConcurrentLinkedQueue<Integer> list = local.get();
        return list == null ? "null" : list.toString();
    }

    public static void clear() {
        local.remove();
    }

    public static int count() {
        ConcurrentLinkedQueue<Integer> list = local.get();
        if (list == null)
            throw new IllegalStateException();
        else
            return list.size();
    }

    public static void newList() {
        local.set(new ConcurrentLinkedQueue<Integer>());
    }

    public static int sum() {
        ConcurrentLinkedQueue<Integer> list = local.get();
        if (list == null)
            throw new IllegalStateException();
        else
            return list.stream().reduce(0, Integer::sum);
    }
}
