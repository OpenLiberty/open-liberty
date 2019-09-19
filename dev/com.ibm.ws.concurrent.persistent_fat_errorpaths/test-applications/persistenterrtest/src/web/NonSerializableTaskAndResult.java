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
 * Non-serializable task returns a non-serializable result, which means the result cannot
 * be persisted and will result in an error when attempt is made to obtain it.
 */
public class NonSerializableTaskAndResult implements Callable<Object>, ManagedTask {
    static Object resultOverride;

    @Override
    public Object call() throws Exception {
        if (resultOverride == null)
            return this;
        else
            return resultOverride;
    }

    @Override
    public Map<String, String> getExecutionProperties() {
        return Collections.singletonMap(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return null;
    }
}
