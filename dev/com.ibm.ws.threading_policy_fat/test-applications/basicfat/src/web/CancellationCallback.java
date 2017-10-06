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

import java.util.concurrent.Future;

import com.ibm.ws.threading.PolicyTaskCallback;

/**
 * Callback that cancels tasks on submit or on start.
 */
public class CancellationCallback extends PolicyTaskCallback {
    private String whenToCancel;

    /**
     * @param whenToCancel supported values: onSubmit, onStart
     */
    public CancellationCallback(String whenToCancel) {
        this.whenToCancel = whenToCancel;
    }

    @Override
    public Object onStart(Object task, Future<?> future) {
        if ("onStart".equals(whenToCancel))
            System.out.println("CancellationCallback.onStart " + task.toString() + " canceled? " + future.cancel(true));
        return null;
    }

    @Override
    public void onSubmit(Object task, Future<?> future, int invokeAnyCount) {
        if ("onSubmit".equals(whenToCancel))
            System.out.println("CancellationCallback.onSubmit " + task.toString() + " canceled? " + future.cancel(false));
    }
}
