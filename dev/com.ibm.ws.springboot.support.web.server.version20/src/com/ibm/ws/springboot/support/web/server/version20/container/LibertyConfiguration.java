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
package com.ibm.ws.springboot.support.web.server.version20.container;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Configuration
@EnableConfigurationProperties
@ConditionalOnWebApplication
@ConditionalOnClass({ ServletWebServerFactory.class })
@Import(BeanPostProcessorsRegistrar.class)
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
