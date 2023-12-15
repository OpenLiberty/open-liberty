/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.streams.operators30.spi.impl;

import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsEngineResolver;
import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsFactoryImpl;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsFactoryResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import io.smallrye.mutiny.jakarta.streams.Engine;

@Component(name = "io.openliberty.microprofile.reactive.streams.operators30.spi.impl.LibertyReactiveStreamsEngineProvider", property = { "service.vendor=IBM" }, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class LibertyReactiveStreamsEngineProvider {

    @Activate
    public void activate() {
        ReactiveStreamsFactoryResolver.setInstance(new ReactiveStreamsFactoryImpl());
        ReactiveStreamsEngineResolver.setInstance(new Engine());
    }

    @Deactivate
    public void deactivate() {
        ReactiveStreamsEngineResolver.setInstance(null);
        ReactiveStreamsFactoryResolver.setInstance(null);
    }

}
