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
package fat.persistra.resourceadapter;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.resource.spi.AdministeredObject;

import com.ibm.websphere.concurrent.persistent.AutoPurge;

/**
 * Non-serializable task that is provided by a resource adapter.
 */
@AdministeredObject(adminObjectInterfaces = { Callable.class, ManagedTask.class })
public class RATask implements Callable<RAResult>, ManagedTask {
    @Override
    public RAResult call() {
        return new RAResult();
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
