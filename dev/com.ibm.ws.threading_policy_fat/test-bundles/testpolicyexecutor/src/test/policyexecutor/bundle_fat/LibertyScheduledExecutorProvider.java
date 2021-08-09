/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.policyexecutor.bundle_fat;

import java.util.concurrent.ScheduledExecutorService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

/**
 * A ResourceFactory implementation that makes the Liberty scheduled executor available to applications via JNDI
 * for convenience of writing tests.
 */
@Component(name = "LibertyScheduledExecutorProvider", //
           configurationPolicy = ConfigurationPolicy.IGNORE, //
           property = { "jndiName=test/LibertyScheduledExecutor", //
                        "creates.objectClass=java.util.concurrent.ScheduledExecutorService" //
           })
public class LibertyScheduledExecutorProvider implements ResourceFactory {

    @Reference(target = "(deferrable=false)")
    private ScheduledExecutorService scheduledExecutor;

    /**
     * @see com.ibm.wsspi.resource.ResourceFactory#createResource(com.ibm.wsspi.resource.ResourceInfo)
     */
    @Override
    public Object createResource(ResourceInfo ref) throws Exception {
        return scheduledExecutor;
    }

}
