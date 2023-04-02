/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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

package com.ibm.ws.event.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class ReservedKeys {

    private final static Map<String, Integer> reservedKeys = new HashMap<String, Integer>();
    private final static AtomicInteger reservedSlotCount = new AtomicInteger();

    public static synchronized int reserveSlot(final String keyName) {
        Integer slot = reservedKeys.get(keyName);
        if (slot == null) {
            slot = reservedSlotCount.getAndIncrement();
            reservedKeys.put(keyName, slot);
        }
        return slot;
    }

}
