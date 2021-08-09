/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

import com.ibm.ws.threading.PolicyExecutorProvider;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

/**
 * A ResourceFactory implementation that makes PolicyExecutorProvider available to applications via JNDI for convenience of writing tests.
 */
@Component(name = "TestPolicyExecutorProvider", configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "jndiName=test/TestPolicyExecutorProvider", "creates.objectClass=com.ibm.ws.threading.PolicyExecutorProvider" })
public class PolicyExecutorProviderFactory implements ResourceFactory {

    @Reference
    private PolicyExecutorProvider provider;

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.resource.ResourceFactory#createResource(com.ibm.wsspi.resource.ResourceInfo)
     * //
     */
    @Override
    public Object createResource(ResourceInfo arg0) throws Exception {
        return provider;
    }

}
