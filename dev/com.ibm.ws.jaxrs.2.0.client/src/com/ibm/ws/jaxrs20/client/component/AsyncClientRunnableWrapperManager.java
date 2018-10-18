/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.cxf.message.Message;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.jaxrs20.client.AsyncClientRunnableWrapper;

@Component(name = "com.ibm.ws.jaxrs20.client.component.AsyncClientRunnableWrapperManager",
           immediate = true,
           property = { "service.vendor=IBM" })
public class AsyncClientRunnableWrapperManager {

    private static final List<AsyncClientRunnableWrapper> wrappers = new CopyOnWriteArrayList<>();

    public static void prepare(Message message) {
        for (AsyncClientRunnableWrapper wrapper : wrappers) {
            try {
                wrapper.prepare(message);
            } catch (Throwable t) {
                // auto FFDC - process FFDC and then continue
            }
        }
    }

    public static Runnable wrap(Message message, Runnable runnable) {
        Runnable ret = runnable;
        for (AsyncClientRunnableWrapper wrapper : wrappers) {
            try {
                ret = wrapper.wrap(message, ret);
            } catch (Throwable t) {
                // auto FFDC - process FFDC and then continue
            }
        }
        return ret;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void addWrapper(AsyncClientRunnableWrapper wrapper) {
        wrappers.add(wrapper);
    }

    protected void removeWrapper(AsyncClientRunnableWrapper wrapper) {
        wrappers.remove(wrapper);
    }
}
