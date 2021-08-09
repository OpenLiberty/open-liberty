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
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;

import com.ibm.websphere.concurrent.persistent.TaskIdAccessor;

/**
 * Task that runs for 10 seconds.
 */
public class TenSecondTask implements Callable<Long>, ManagedTask, Serializable {
    private static final long serialVersionUID = 2182542133175570710L;

    private final Map<String, String> execProps = new TreeMap<String, String>();

    @Override
    public Long call() throws Exception {
        long taskId = TaskIdAccessor.get();
        String name = execProps.get(ManagedTask.IDENTITY_NAME) + " (" + taskId + ")";
        System.out.println("Started task " + name);
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(10));
        } finally {
            System.out.println("Completed task " + name);
        }
        return taskId;
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
