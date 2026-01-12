/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package concurrent.rar;

import static org.junit.Assert.fail;

import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.Connector;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

import javax.transaction.xa.XAResource;

/**
 * A resource adapter that is also an application scoped bean.
 * Ensure we intentionally avoid creating a default managed thread factory
 */
@Connector
@ApplicationScoped
public class ConcurrentResourceAdapater implements ResourceAdapter {
    final ConcurrentHashMap<ActivationSpec, MessageEndpointFactory> endpointFactories = new ConcurrentHashMap<ActivationSpec, MessageEndpointFactory>();

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec) throws ResourceException {
        endpointFactories.putIfAbsent(activationSpec, endpointFactory);
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec) {
        endpointFactories.remove(activationSpec, endpointFactory);
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] activationSpecs) throws ResourceException {
        return null;
    }

    @Override
    public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
        try {
            ManagedThreadFactory defaultLookupMTF = CDI.current().select(ManagedThreadFactory.class, Default.Literal.INSTANCE).get();
            fail("Concurrent resource extension should not have provided a default "
                 + "managed thread factory for a RAR application. But instead got: " + defaultLookupMTF);
        } catch (UnsatisfiedResolutionException e) {
            //expected
        } catch (Throwable t) {
            fail("Caught unexpected throwable: " + t.getMessage());
        }
    }

    @Override
    public void stop() {
    }
}
