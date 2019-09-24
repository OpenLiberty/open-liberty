/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.test.servlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;

import com.ibm.websphere.concurrent.persistent.AutoPurge;

public class SimpleTask implements Callable<String>, Serializable, ManagedTask {

    private static final long serialVersionUID = -2611538742435784871L;

    public static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    @Override
    public String call() {
        System.out.println("SimpleTask call method executing");
        return SUCCESS_MESSAGE;
    }

    // Setup autopurge values so the 
    // task is not auto purged
    @Override
    public Map<String, String> getExecutionProperties() {
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        return props;
    }

    // not using a ManagedTaskListener
    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return null;
    }

}
