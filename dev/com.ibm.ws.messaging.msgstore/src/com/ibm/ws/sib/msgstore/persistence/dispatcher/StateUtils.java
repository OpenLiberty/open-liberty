/* ==============================================================================
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ==============================================================================
 */
package com.ibm.ws.sib.msgstore.persistence.dispatcher;

import java.util.concurrent.atomic.AtomicReference;

enum StateUtils {
    ;

    interface StateUpdater<T> {
        T update(T currentState);
    }
    interface UpdateCallback<T> {
        void updated(T newState);
    }

    public static <T> boolean updateState(AtomicReference<T> ref, StateUpdater<T> updater) {
        return updateState(ref, updater, null);
    }

    public static <T> boolean updateState(AtomicReference<T> ref, StateUpdater<T> updater, UpdateCallback<T> callback) {
        T curState, newState;
        do {
            curState = ref.get();
            newState = updater.update(curState);
            if (newState == curState) return false;
        } while (!ref.compareAndSet(curState, newState));
        if (null != callback) callback.updated(newState);
        return true;
    }
}
