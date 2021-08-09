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
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;

/**
 * Task that increments a static counter and intentionally fails upon specified executions.
 * Any test that uses this task must ensure that execProps and failOn are cleared upon test completion
 * (successful or otherwise) by invoking the clear method so as not to interfere with other tests.
 */
public class SharedFailingTask extends SharedCounterTask implements ManagedTask {
    static final Map<String, String> execProps = new TreeMap<String, String>();
    static final Set<Long> failOn = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());

    @Override
    public Long call() {
        Long result = super.call();
        if (failOn.contains(result))
            throw new IllegalStateException("Intentionally failing execution #" + result);
        return result;
    }

    static void clear() {
        counter.set(0);
        execProps.clear();
        failOn.clear();
    }

    @Override
    public Map<String, String> getExecutionProperties() {
        return execProps;
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return null;
    }

    @Override
    public void run() {
        Long result = super.call();
        if (failOn.contains(result))
            throw new IllegalStateException("Intentionally failing execution #" + result);
    }
}
