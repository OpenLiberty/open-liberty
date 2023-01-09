/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS.client.internal;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.FieldOption;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import io.openliberty.restfulWS.client.ClientAsyncTaskWrapper;

/**
 * Tracks and applies {@link ClientAsyncTaskWrapper} services
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class ClientAsyncTaskWrapperComponent {
    
    private static AtomicReference<ClientAsyncTaskWrapperComponent> instance = new AtomicReference<>();
    
    @Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, fieldOption = FieldOption.REPLACE)
    private volatile List<ClientAsyncTaskWrapper> wrappers;
    
    @Activate
    protected void activate() {
        instance.set(this);
    }
    
    @Deactivate
    protected void deactivate() {
        instance.compareAndSet(this, null);
    }
    
    public static Runnable wrap(Runnable r) {
        ClientAsyncTaskWrapperComponent instance = ClientAsyncTaskWrapperComponent.instance.get();
        if (instance != null) {
            for (ClientAsyncTaskWrapper wrapper : instance.wrappers) {
                try {
                    Runnable wrapped = wrapper.wrap(r);
                    if (wrapped == null) {
                        throw new NullPointerException("ClientAsyncTaskWrapper " + wrapper + " returned null");
                    }
                    r = wrapped;
                } catch (Exception e) {
                    // FFDC
                }
            }
        }
        return r;
    }
    
    public static <T> Callable<T> wrap(Callable<T> c) {
        ClientAsyncTaskWrapperComponent instance = ClientAsyncTaskWrapperComponent.instance.get();
        if (instance != null) {
            for (ClientAsyncTaskWrapper wrapper : instance.wrappers) {
                try {
                    Callable<T> wrapped = wrapper.wrap(c);
                    if (wrapped == null) {
                        throw new NullPointerException("ClientAsyncTaskWrapper " + wrapper + " returned null");
                    }
                    c = wrapped;
                } catch (Exception e) {
                    // FFDC
                }
            }
        }
        return c;
    }
    

}
