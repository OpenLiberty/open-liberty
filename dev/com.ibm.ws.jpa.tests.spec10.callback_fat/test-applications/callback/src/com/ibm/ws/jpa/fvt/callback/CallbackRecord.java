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

public class CallbackRecord implements java.io.Serializable {
    private static final long serialVersionUID = -5270959307552041511L;

    public enum CallbackLifeCycle {
        PrePersist,
        PostPersist,
        PreRemove,
        PostRemove,
        PreUpdate,
        PostUpdate,
        PostLoad,
        All;
    }

    private CallbackLifeCycle lifecycleType;
    private long timestamp;

    private String callerClassName = null;
    private String callerMethodName = null;

    private RuntimeException callbackException = null;

    public CallbackRecord(CallbackLifeCycle lifecycleType, String callerClassName, String callerMethodName) {
        timestamp = System.currentTimeMillis();

        // Save Arguments
        this.lifecycleType = lifecycleType;
        this.callerClassName = callerClassName;
        this.callerMethodName = callerMethodName;
    }

    public RuntimeException getCallbackException() {
        return callbackException;
    }

    public void setCallbackException(RuntimeException callbackException) {
        this.callbackException = callbackException;
    }

    public CallbackLifeCycle getLifecycleType() {
        return lifecycleType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getCallerClassName() {
        return callerClassName;
    }

    public String getCallerMethodName() {
        return callerMethodName;
    }

    @Override
    public String toString() {
        return "CallbackRecord [lifecycleType=" + lifecycleType + ", callerClassName="
               + callerClassName + ", callerMethodName=" + callerMethodName + "]";
    }
}
