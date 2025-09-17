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

import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Looks if jsp template is available
 */
public class LibertyJspTemplateProvider implements TemplateAvailabilityProvider {

    @Override
    public boolean isTemplateAvailable(String view, Environment environment, ClassLoader classLoader, ResourceLoader resourceLoader) {
        try {
            Class.forName("jakarta.servlet.jsp.HttpJspPage", false, classLoader);
            String prefix = environment.getProperty("spring.mvc.view.prefix", WebMvcAutoConfiguration.DEFAULT_PREFIX);
            String suffix = environment.getProperty("spring.mvc.view.suffix", WebMvcAutoConfiguration.DEFAULT_SUFFIX);
            Resource resource = resourceLoader.getResource(prefix + view + suffix);
            return resource.exists();
        } catch (ClassNotFoundException e) {
            //ignore
        }
        return false;
    }
}
