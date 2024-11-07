/*
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jndi.iiop;

import java.util.BitSet;

final class ImmutableBitSet {
    private final long[] longArray;
    public ImmutableBitSet(BitSet bitSet) {
        longArray = bitSet.toLongArray();
    }

    public boolean get(int index) {
        if (index < 0) throw new IndexOutOfBoundsException("bitIndex < 0: " + index);
        final int aIndex = index / Long.SIZE;
        if (aIndex >= longArray.length) return false;
        final int bIndex = index % Long.SIZE;
        return (0x1 == ((longArray[aIndex] >> bIndex) & 0x1));
    }
}
