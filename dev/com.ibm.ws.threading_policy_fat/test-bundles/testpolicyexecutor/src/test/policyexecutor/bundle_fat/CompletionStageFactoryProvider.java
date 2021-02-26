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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.threading.CompletionStageFactory;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

/**
 * A ResourceFactory implementation that makes PolicyExecutorProvider available to applications via JNDI for convenience of writing tests.
 */
@Component(name = "CompletionStageFactoryProvider", configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "jndiName=test/CompletionStageFactory", "creates.objectClass=com.ibm.ws.threading.CompletionStageFactory" })
public class CompletionStageFactoryProvider implements ResourceFactory {

    @Reference
    private CompletionStageFactory factory;

    @Override
    public Object createResource(ResourceInfo arg0) throws Exception {
        return factory;
    }

}
