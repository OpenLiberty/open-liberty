/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.jboss.resteasy.common.component;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Extension;

import org.jboss.resteasy.cdi.ResteasyCdiExtension;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.openliberty.cdi.spi.CDIExtensionMetadata;

@Component(service = CDIExtensionMetadata.class,
    configurationPolicy = ConfigurationPolicy.IGNORE,
    immediate = true,
    property = { "api.classes=" +
                    "javax.ws.rs.Path;" +
                    "javax.ws.rs.core.Application;" +
                    "javax.ws.rs.ext.Provider",
                 "bean.defining.annotations=" +
                    "javax.ws.rs.Path;" +
                    "javax.ws.rs.core.Application;" +
                    "javax.ws.rs.ApplicationPath;" +
                    "javax.ws.rs.ext.Provider",
                 "service.vendor=IBM" })
public class ResteasyCDIExtensionMetadata implements CDIExtensionMetadata {
    
    // TODO
    // This class is currently "unhooked", meaning that it is not listed in -dsannotations in the bnd file.
    // This is because the CDIExtensionMetadata SPI doesn't currently support bean.defining.annotations.
    // Until bean.defining.annotations is supported, we have to use the "old" WebSphereCDIExtension API.
    // When bean.defining.annotations is suppoerted, we will need to delete LibertyResteasyCdiExtension
    // and replace it's -dsannotations with this class in the bnd file (and delete this comment).
    
    @Override
    public Set<Class<? extends Extension>> getExtensions() {
        Set<Class<? extends Extension>> extensions = new HashSet<Class<? extends Extension>>();
        extensions.add(ResteasyCdiExtension.class);
        return extensions;
    }
}
