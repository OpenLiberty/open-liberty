/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import com.ibm.ws.threading.PolicyTaskFuture;

/**
 * Callback that intentionally raises RuntimeException subclasses.
 */
public class FailingCallback extends ParameterInfoCallback {
    public final Class<?>[] failureClass = new Class<?>[NUM_CALLBACKS];

    private RuntimeException newRuntimeException(Class<?> c) {
        try {
            return (RuntimeException) failureClass[START].newInstance();
        } catch (IllegalAccessException x) {
            return new RuntimeException(x);
        } catch (InstantiationException x) {
            return new RuntimeException(x);
        }
    }

    @Override
    public void onCancel(Object task, PolicyTaskFuture<?> future, boolean whileRunning) {
        super.onCancel(task, future, whileRunning);
        if (failureClass[CANCEL] != null)
            throw newRuntimeException(failureClass[CANCEL]);
    }

    @Override
    public void onEnd(Object task, PolicyTaskFuture<?> future, Object startObj, boolean aborted, int pending, Throwable failure) {
        super.onEnd(task, future, startObj, aborted, pending, failure);
        if (failureClass[END] != null)
            throw newRuntimeException(failureClass[END]);
    }

    @Override
    public Object onStart(Object task, PolicyTaskFuture<?> future) {
        Object o = super.onStart(task, future);
        if (failureClass[START] != null)
            throw newRuntimeException(failureClass[START]);
        return o;
    }

    @Override
    public void onSubmit(Object task, PolicyTaskFuture<?> future, int invokeAnyCount) {
        super.onSubmit(task, future, invokeAnyCount);
        if (failureClass[SUBMIT] != null)
            throw newRuntimeException(failureClass[SUBMIT]);
    }
}
