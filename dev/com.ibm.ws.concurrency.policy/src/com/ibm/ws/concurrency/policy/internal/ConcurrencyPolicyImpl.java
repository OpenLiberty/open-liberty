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
package com.ibm.ws.concurrency.policy.internal;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.concurrency.policy.ConcurrencyPolicy;
import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.ws.threading.PolicyExecutorProvider;

/**
 * Corresponds to a <code>concurrencyPolicy</code> configuration element.
 * Allows you to obtain the policy executor that implements the configured policy.
 */
@Component(name = "com.ibm.ws.concurrency.policy.concurrencyPolicy",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { ConcurrencyPolicy.class },
           property = { "service.pid=com.ibm.ws.concurrency.policy.concurrencyPolicy" })
public class ConcurrencyPolicyImpl implements ConcurrencyPolicy {
    @Reference
    protected PolicyExecutorProvider provider;

    private PolicyExecutor executor;

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> props) {
        executor = provider.create((String) props.get("config.displayId"));
        executor.updateConfig(props);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        executor.shutdownNow();
    }

    /**
     * @see com.ibm.ws.concurrency.policy.ConcurrencyPolicy#getExecutor()
     */
    @Override
    public PolicyExecutor getExecutor() {
        return executor;
    }

    @Modified
    protected void modified(ComponentContext context, Map<String, Object> props) {
        executor.updateConfig(props);
    }
}