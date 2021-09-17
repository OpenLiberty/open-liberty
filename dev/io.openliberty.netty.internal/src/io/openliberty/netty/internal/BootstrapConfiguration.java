/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.netty.internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;

/**
 * Bootstrap configuration object
 */
public interface BootstrapConfiguration {

    /**
     * Apply this configuration to the given ServerBootstrap
     * @param bootstrap
     */
    void applyConfiguration(ServerBootstrap bootstrap);

    /**
     * Apply this configuration to the given Bootstrap
     * @param bootstrap
     */
    void applyConfiguration(Bootstrap bootstrap);

}
