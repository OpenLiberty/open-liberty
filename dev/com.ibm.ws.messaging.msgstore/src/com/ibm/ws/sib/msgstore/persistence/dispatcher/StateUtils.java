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

    public static final <T> boolean updateState(AtomicReference<T> ref, StateUpdater<T> updater) {
        return updateState(ref, updater, null);
    }

    public static final <T> boolean updateState(AtomicReference<T> ref, StateUpdater<T> updater, UpdateCallback<T> callback) {
        T curState, newState;
        do {
            curState = ref.get();
            newState = updater.update(curState);
            if (newState == curState) return false;
        } while (false == ref.compareAndSet(curState, newState));
        if (null != callback) callback.updated(newState);
        return true;
    }
}
