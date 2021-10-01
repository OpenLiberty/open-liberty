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

import io.netty.bootstrap.ServerBootstrap;

/**
 * Extends {@link ServerBootstrap} to allow configuration via a {@link BootstrapConfiguration} 
 */
public class ServerBootstrapExtended extends ServerBootstrap {

    private BootstrapConfiguration config;
    private ChannelInitializerWrapper initializer;

    /**
     * Apply the given {@link BootstrapConfiguration} to this {@link ServerBootstrap}
     * Note that most props are implemented via handlers, see {@link TCPChannelInitializerImpl}
     * 
     * @param config
     */
    public void applyConfiguration(BootstrapConfiguration config) {
        this.config = config;
        config.applyConfiguration(this);
    }

    /**
     * Get the ServerBootstrapConfiguration associated with this bootstrap
     * @return ServerBootstrapConfiguration
     */
    public BootstrapConfiguration getConfiguration() {
        return config;
    }

    /**
     * Set the base {@link ChannelInitializerWrapper} to use with this bootstrap
     */
    public void setBaseInitializer(ChannelInitializerWrapper initializer) {
        this.initializer = initializer;
    }

    /**
     * Get the base {@link ChannelInitializerWrapper} used by this bootstrap
     * @return ChannelInitializerWrapper
     */
    public ChannelInitializerWrapper getBaseInitializer() {
        return this.initializer;
    }
}
