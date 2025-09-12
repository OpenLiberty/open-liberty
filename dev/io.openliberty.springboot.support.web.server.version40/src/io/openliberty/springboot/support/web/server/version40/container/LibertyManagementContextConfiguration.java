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

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextFactory;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(ManagementContextFactory.class)
@ConditionalOnManagementPort(ManagementPortType.DIFFERENT)
public class LibertyManagementContextConfiguration {

    @Bean
    @ConditionalOnWebApplication(type = Type.SERVLET)
    public static ManagementContextFactory libertyServletManagementContextFactory() {
        return new ManagementContextFactory(WebApplicationType.SERVLET, LibertyServletWebServerFactory.class, LibertyConfiguration.class);
    }

    @Bean
    @ConditionalOnWebApplication(type = Type.REACTIVE)
    public static ManagementContextFactory libertyReactiveManagementContextFactory() {
        return new ManagementContextFactory(WebApplicationType.REACTIVE, LibertyReactiveWebServerFactory.class, LibertyConfiguration.class);
    }
}
