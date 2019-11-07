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

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;

/**
 * Task that adds its name to a static list when it runs.
 */
public class NamedTask implements Callable<String>, ManagedTask, Runnable, Serializable {
    private static final long serialVersionUID = 3665663730995246259L;

    final static ConcurrentLinkedQueue<String> namesOfExecutedTasks = new ConcurrentLinkedQueue<String>();

    private final Map<String, String> execProps = new TreeMap<String, String>();

    public NamedTask(String name) {
        execProps.put(ManagedTask.IDENTITY_NAME, name);
    }

    @Override
    public String call() {
        String name = execProps.get(ManagedTask.IDENTITY_NAME);
        namesOfExecutedTasks.add(name);
        return name;
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
        call();
    }
}
