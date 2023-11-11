/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package com.ibm.ws.kernel.boot.utils;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
public final class KeyBasedLockStore<Key, Lock> {

    private final ReferenceQueue<Lock> refQueue = new ReferenceQueue<>();
    private final ConcurrentHashMap<Key, LockWeakRef> lockMap = new ConcurrentHashMap<>();
    private final Supplier<Lock> lockCreator;

    public KeyBasedLockStore(Supplier<Lock> lockCreator) {
        this.lockCreator = lockCreator;
    }

    private final class LockWeakRef extends WeakReference<Lock> {
        final Key key;

        @Trivial
        public LockWeakRef(Lock referent, Key keyValue) {
            super(referent, refQueue);
            key = keyValue;
        }
    }

    public final Lock getLock(Key key) {
        poll();
        LockWeakRef lockRef = lockMap.get(key);
        Lock lock = lockRef != null ? lockRef.get() : null;
        if (lock != null) {
            return lock;
        }

        lock = lockCreator.get();

        while (true) {
            LockWeakRef retVal = lockMap.putIfAbsent(key, new LockWeakRef(lock, key));
            if (retVal == null) {
                return lock;
            }

            Lock retLock = retVal.get();
            if (retLock != null) {
                return retLock;
            }
            lockMap.remove(key, retVal);
        }
    }

    @SuppressWarnings("unchecked")
    private final void poll() {
        LockWeakRef lockRef;
        while ((lockRef = (LockWeakRef) refQueue.poll()) != null) {
            lockMap.remove(lockRef.key, lockRef);
        }
    }
}
