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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import jakarta.enterprise.concurrent.ManagedTask;
import jakarta.enterprise.concurrent.ManagedTaskListener;

/**
 * Callable that increments and returns a counter each time it runs.
 */
public class MBeanCounterCallable implements Callable<Integer>, ManagedTask, Serializable {
    private static final long serialVersionUID = -3880167073113971050L;

    int counter;
    private final Map<String, String> execProps = new HashMap<String, String>();

    @Override
    public Integer call() throws Exception {
        return ++counter;
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
