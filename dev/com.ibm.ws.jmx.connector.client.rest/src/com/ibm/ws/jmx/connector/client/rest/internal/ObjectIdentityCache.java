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
package com.ibm.ws.jmx.connector.client.rest.internal;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ObjectIdentityCache {

    private final Map<ObjectReference, Integer> identityMap = new HashMap<ObjectReference, Integer>();
    private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<Object>();
    private int nextIdentity = Integer.MIN_VALUE + 1;

    int getObjectIdentity(Object o) {
        // Clean up cleared references
        ObjectReference clearedRef = (ObjectReference) referenceQueue.poll();
        while (clearedRef != null) {
            identityMap.remove(clearedRef);
            clearedRef = (ObjectReference) referenceQueue.poll();
        }

        if (o == null)
            return Integer.MIN_VALUE;

        ObjectReference ref = new ObjectReference(o, referenceQueue);
        if (identityMap.containsKey(ref)) {
            return identityMap.get(ref);
        } else {
            if (nextIdentity == Integer.MAX_VALUE)
                throw new IllegalStateException();
            final int identity = nextIdentity++;
            identityMap.put(ref, identity);
            return identity;
        }
    }

    private static class ObjectReference extends WeakReference<Object> {
        private final int hashCode;

        ObjectReference(Object referent, ReferenceQueue<Object> queue) {
            super(referent, queue);
            hashCode = referent.hashCode();
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof ObjectReference) {
                ObjectReference other = (ObjectReference) o;
                return other.get() == get();
            } else {
                return false;
            }
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
