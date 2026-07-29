/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.microprofile.config.internal.serverxml;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.ibm.ws.kernel.service.util.ServiceCaller;

/**
 * A no-op {@link ServiceCaller} for use in unit tests where no OSGi framework or real
 * service instance is available. All operations return empty / false.
 *
 * @param <S> the service type
 */
public class TestServiceCaller<S> extends ServiceCaller<S> {

    public TestServiceCaller(Class<S> serviceType) {
        super(TestServiceCaller.class, serviceType);
    }

    @Override
    public boolean call(Consumer<S> consumer) {
        return false;
    }

    @Override
    public <R> Optional<R> run(Function<S, R> function) {
        return Optional.empty();
    }

    @Override
    public Optional<S> current() {
        return Optional.empty();
    }

    @Override
    public void unget() {}

}
