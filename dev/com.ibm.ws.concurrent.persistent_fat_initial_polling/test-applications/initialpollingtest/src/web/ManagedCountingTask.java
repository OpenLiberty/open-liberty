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

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;

import com.ibm.websphere.concurrent.persistent.TaskIdAccessor;

/**
 * A simple task that increments a counter each time it runs.
 */
public class ManagedCountingTask implements Callable<Integer>, ManagedTask, Serializable {
    private static final long serialVersionUID = 1873967162677728365L;

    private int counter;
    private final Map<String, String> execProps = new TreeMap<String, String>();

    @Override
    public Integer call() throws Exception {
        ++counter;
        System.out.println("Task " + TaskIdAccessor.get() + " execution attempt #" + counter);
        return counter;
    }

    @Override
    public Map<String, String> getExecutionProperties() {
        return execProps;
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return null;
    }
}
