/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;

import com.ibm.websphere.concurrent.persistent.AutoPurge;

/**
 * Callable that returns a result which is not serializable.
 */
public class NonSerializableResultTask implements Callable<ThreadGroup>, ManagedTask {
    private static final Map<String, String> EXEC_PROPS = Collections.singletonMap(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());

    @Override
    public ThreadGroup call() {
        return Thread.currentThread().getThreadGroup();
    }

    @Override
    public Map<String, String> getExecutionProperties() {
        return EXEC_PROPS;
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return null;
    }
}
