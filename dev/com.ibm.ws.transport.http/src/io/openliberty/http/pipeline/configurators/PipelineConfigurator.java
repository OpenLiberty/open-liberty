/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.pipeline.configurators;

import io.netty.channel.ChannelPipeline;
import io.openliberty.netty.internal.exception.NettyException;

 /**
 * PipelineConfigurator defines a contract for configuring a ChannelPipeline for a specific protocol scenario.
 * Implementations of this interface are responsible for adding and arranging handlers 
 * to meet the requirements of that particular pipeline configuration (HTTP/1.1, HTTPS, HTTP/2, etc.).
 */
public interface PipelineConfigurator {

    /**
     * Configures the given ChannelPipeline with the necessary handlers for the protocol scenario.
     *
     * @param pipeline the ChannelPipeline to configure
     * @throws NettyException if there is an error during pipeline configuration
     */
    void configure(ChannelPipeline pipeline) throws NettyException;
}
