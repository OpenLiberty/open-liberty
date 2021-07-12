/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.callback;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.ibm.ws.jpa.fvt.callback.CallbackRecord.CallbackLifeCycle;

public abstract class AbstractCallbackListener {
    public enum ProtectionType {
        PT_PUBLIC,
        PT_PRIVATE,
        PT_PACKAGE,
        PT_PROTECTED,
        ALL;
    }

    private final static transient ArrayList<CallbackRecord> _globalCallbackEventList = new ArrayList<CallbackRecord>();
    private static transient CallbackLifeCycle _globalCallbackEventFilter = null;
    private static transient ProtectionType _globalCallbackProtectionTypeFilter = null;

    public static void resetGlobalCallbackEventList() {
        synchronized (_globalCallbackEventList) {
            _globalCallbackEventList.clear();
            _globalCallbackEventFilter = null;
            _globalCallbackProtectionTypeFilter = null;
        }
    }

    public static void setGlobalCallbackEventFilter(CallbackLifeCycle callbackLifeCycle) {
        synchronized (_globalCallbackEventList) {
            _globalCallbackEventFilter = callbackLifeCycle;
        }
    }

    public static void setGlobalCallbackProtectionTypeFilter(ProtectionType protectionType) {
        synchronized (_globalCallbackEventList) {
            _globalCallbackProtectionTypeFilter = protectionType;
        }
    }

    private static void recordGlobalCallbackEvent(CallbackRecord cr, ProtectionType pt) {
        if (_globalCallbackProtectionTypeFilter != null && pt != _globalCallbackProtectionTypeFilter) {
            return;
        }

        synchronized (_globalCallbackEventList) {
            if (_globalCallbackEventFilter == null || _globalCallbackEventFilter == CallbackLifeCycle.All) {
                _globalCallbackEventList.add(cr);
            } else if (_globalCallbackEventFilter == cr.getLifecycleType()) {
                _globalCallbackEventList.add(cr);
            }
        }
    }

    public static List<CallbackRecord> getGlobalCallbackEventList() {
        List<CallbackRecord> globalEventList = null;

        synchronized (_globalCallbackEventList) {
            globalEventList = new ArrayList<CallbackRecord>(_globalCallbackEventList);
        }

        return globalEventList;
    }

    private volatile transient ArrayList<CallbackRecord> callbackEventList = new ArrayList<CallbackRecord>();
    private volatile transient HashSet<CallbackLifeCycle> firedLifeCycleSet = new HashSet<CallbackLifeCycle>();
    private transient CallbackLifeCycle runtimeExceptionLifecycleTarget = null;
    private static transient ProtectionType targetPostLoadLifeCycleWithRuntimeException = null;

    public final static ProtectionType getTargetPostLoadLifeCycleWithRuntimeException() {
        return targetPostLoadLifeCycleWithRuntimeException;
    }

    public final static void setTargetPostLoadLifeCycleWithRuntimeException(ProtectionType triggerPT) {
        AbstractCallbackListener.targetPostLoadLifeCycleWithRuntimeException = triggerPT;
    }

    public AbstractCallbackListener() {

    }

    private final void recordCallbackEvent(CallbackLifeCycle lifecycleType, ProtectionType pt) {
        StackTraceElement[] ste = Thread.currentThread().getStackTrace();

        String callerClassName = "";
        String callerMethodName = "";

        for (StackTraceElement element : ste) {
            String className = element.getClassName();
            String methodName = element.getMethodName();

            if (className != null &&
                (className.startsWith("com.ibm.ws.jpa.fvt.callback.entities.") ||
                 className.startsWith("com.ibm.ws.jpa.fvt.callback.listeners."))) {
                callerClassName = className;
                callerMethodName = methodName;
                break;
            }
        }

        CallbackRecord callbackRecord = new CallbackRecord(lifecycleType, callerClassName, callerMethodName);
        getCallbackEventList().add(callbackRecord);
        AbstractCallbackListener.recordGlobalCallbackEvent(callbackRecord, pt);
        getFiredLifeCycleSet().add(lifecycleType);
    }

    public void resetCallbackData() {
        getCallbackEventList().clear(); // = new ArrayList<CallbackRecord>();
        getFiredLifeCycleSet().clear(); // = new HashSet<CallbackLifeCycle>();
        runtimeExceptionLifecycleTarget = null;
        targetPostLoadLifeCycleWithRuntimeException = null;
    }

    private final void throwRuntimeExceptionIfTargetLifeCycleType(CallbackLifeCycle lifecycleType) {
        if (lifecycleType == runtimeExceptionLifecycleTarget) {
            throw new CallbackRuntimeException("Throwing RuntimeException on Callback Type " +
                                               runtimeExceptionLifecycleTarget.toString());
        }
    }

    protected final void doPrePersist(ProtectionType pt) {
        recordCallbackEvent(CallbackLifeCycle.PrePersist, pt);
        throwRuntimeExceptionIfTargetLifeCycleType(CallbackLifeCycle.PrePersist);
    }

    protected final void doPostPersist(ProtectionType pt) {
        recordCallbackEvent(CallbackLifeCycle.PostPersist, pt);
        throwRuntimeExceptionIfTargetLifeCycleType(CallbackLifeCycle.PostPersist);
    }

    protected final void doPreRemove(ProtectionType pt) {
        recordCallbackEvent(CallbackLifeCycle.PreRemove, pt);
        throwRuntimeExceptionIfTargetLifeCycleType(CallbackLifeCycle.PreRemove);
    }

    protected final void doPostRemove(ProtectionType pt) {
        recordCallbackEvent(CallbackLifeCycle.PostRemove, pt);
        throwRuntimeExceptionIfTargetLifeCycleType(CallbackLifeCycle.PostRemove);
    }

    protected final void doPreUpdate(ProtectionType pt) {
        recordCallbackEvent(CallbackLifeCycle.PreUpdate, pt);
        throwRuntimeExceptionIfTargetLifeCycleType(CallbackLifeCycle.PreUpdate);
    }

    protected final void doPostUpdate(ProtectionType pt) {
        recordCallbackEvent(CallbackLifeCycle.PostUpdate, pt);
        throwRuntimeExceptionIfTargetLifeCycleType(CallbackLifeCycle.PostUpdate);
    }

    protected final void doPostLoad(ProtectionType pt) {
        recordCallbackEvent(CallbackLifeCycle.PostLoad, pt);

        if (targetPostLoadLifeCycleWithRuntimeException == ProtectionType.ALL ||
            pt == targetPostLoadLifeCycleWithRuntimeException) {
            throw new CallbackRuntimeException("Throwing RuntimeException on Callback Type POSTLOAD");
        }
    }

    public ArrayList<CallbackRecord> getCallbackEventList() {
        if (callbackEventList == null) {
            callbackEventList = new ArrayList<CallbackRecord>();
        }
        return callbackEventList;
    }

    public HashSet<CallbackLifeCycle> getFiredLifeCycleSet() {
        if (firedLifeCycleSet == null) {
            firedLifeCycleSet = new HashSet<CallbackLifeCycle>();
        }
        return firedLifeCycleSet;
    }

    public CallbackLifeCycle getRuntimeExceptionLifecycleTarget() {
        return runtimeExceptionLifecycleTarget;
    }

    public void setRuntimeExceptionLifecycleTarget(CallbackLifeCycle runtimeExceptionLifecycleTarget) {
        this.runtimeExceptionLifecycleTarget = runtimeExceptionLifecycleTarget;
    }
}
