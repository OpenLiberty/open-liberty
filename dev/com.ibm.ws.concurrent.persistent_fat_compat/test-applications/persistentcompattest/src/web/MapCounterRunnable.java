/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;

/**
 * Runnable that increments and updates a counter stored in a static map by task name each time it runs.
 */
public class MapCounterRunnable implements ManagedTask, Runnable, Serializable {
    private static final long serialVersionUID = -6225953773556435355L;

    private int counter;
    private final Map<String, String> execProps = new HashMap<String, String>();

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
        try {
            String identityName = execProps.get(ManagedTask.IDENTITY_NAME);
            if (identityName == null) {
                // If instance was serialized using different Java EE/Jakarta,
                // then it will be using the other constant value.
                if (ManagedTask.IDENTITY_NAME.startsWith("jakarta."))
                    identityName = execProps.get(ManagedTask.IDENTITY_NAME.replace("jakarta.", "javax."));
                else if (ManagedTask.IDENTITY_NAME.startsWith("javax."))
                    identityName = execProps.get(ManagedTask.IDENTITY_NAME.replace("javax.", "jakarta."));
            }
            @SuppressWarnings("unchecked")
            Map<String, Integer> counters = (Map<String, Integer>) Thread.currentThread().getContextClassLoader().loadClass(web.MapCounter.class.getName()).getField("counters").get(null);
            counters.put(identityName, ++counter);
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }
}
