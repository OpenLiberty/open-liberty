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
package io.openliberty.springboot.support.web.server.version40.container;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.autoconfigure.servlet.ServletWebServerConfiguration;
import org.springframework.boot.web.server.reactive.ReactiveWebServerFactory;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfiguration
@Configuration
@EnableConfigurationProperties
@ConditionalOnWebApplication
@ConditionalOnClass({ ServletWebServerFactory.class })
@Import(ServletWebServerConfiguration.class)
public class LibertyConfiguration {

    @Bean
    @ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
    public LibertyServletWebServerFactory libertyEmbeddedServletContainerFactory() {
        return new LibertyServletWebServerFactory();
    }

    @Bean
    @ConditionalOnMissingBean(value = ReactiveWebServerFactory.class, search = SearchStrategy.CURRENT)
    public LibertyReactiveWebServerFactory libertyReactiveWebServerFactory() {
        return new LibertyReactiveWebServerFactory();
    }
}
