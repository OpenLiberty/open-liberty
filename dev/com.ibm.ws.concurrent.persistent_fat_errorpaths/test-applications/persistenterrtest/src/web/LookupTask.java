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
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.websphere.concurrent.persistent.AutoPurge;

public class LookupTask implements Callable<String>, ManagedTask {

    @Override
    public String call() throws NamingException {
        return new InitialContext().lookup("java:comp/env/concurrent/mySchedulerRef").toString();
    }

    @Override
    public Map<String, String> getExecutionProperties() {
        return Collections.singletonMap(AutoPurge.PROPERTY_NAME, AutoPurge.ON_SUCCESS.toString());
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return null;
    }
}
