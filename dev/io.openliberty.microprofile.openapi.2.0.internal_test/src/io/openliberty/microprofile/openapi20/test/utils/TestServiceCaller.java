/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.test.utils;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.ibm.ws.kernel.service.util.ServiceCaller;

/**
 * ServiceCaller implementation for unit tests which always uses a manually provided instance.
 *
 * @param <S> the service type
 */
public class TestServiceCaller<S> extends ServiceCaller<S> {

    private S instance;

    public TestServiceCaller(Class<S> service, S testInstance) {
        super(TestServiceCaller.class, service);
        this.instance = testInstance;
    }

    @Override
    public boolean call(Consumer<S> consumer) {
        consumer.accept(instance);
        return true;
    }

    @Override
    public <R> Optional<R> run(Function<S, R> function) {
        return Optional.ofNullable(function.apply(instance));
    }

    @Override
    public Optional<S> current() {
        return Optional.of(instance);
    }

    @Override
    public void unget() {}

}
