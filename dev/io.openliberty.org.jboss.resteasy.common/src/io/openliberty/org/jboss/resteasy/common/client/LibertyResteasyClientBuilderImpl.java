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
package io.openliberty.org.jboss.resteasy.common.client;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import io.openliberty.jaxrs.client.ClientBuilderListener;
import org.osgi.framework.ServiceReference;

public class LibertyResteasyClientBuilderImpl extends ResteasyClientBuilderImpl {

    @SuppressWarnings("unchecked")
    @Override
    public ResteasyClient build() {
        BundleContext ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference<ClientBuilderListener>[] refs;
        try {
            refs = (ServiceReference<ClientBuilderListener>[]) 
                            ctx.getServiceReferences(ClientBuilderListener.class.getName(), null);
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
        for (ServiceReference<ClientBuilderListener> ref : refs) {
            ClientBuilderListener listener = ctx.getService(ref);
            listener.building(this);
        }

        ResteasyClient client = super.build();

        for (ServiceReference<ClientBuilderListener> ref : refs) {
            ClientBuilderListener listener = ctx.getService(ref);
            listener.built(client);
        }
        return client;
    }
}
