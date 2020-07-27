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

import javax.enterprise.inject.spi.Extension;
import org.jboss.resteasy.cdi.ResteasyCdiExtension;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

@Component(service = WebSphereCDIExtension.class,
    configurationPolicy = ConfigurationPolicy.IGNORE,
    immediate = true,
    property = { "api.classes=" +
                "javax.ws.rs.Path;" +
                "javax.ws.rs.core.Application;" +
                "javax.ws.rs.ext.Provider",
             "bean.defining.annotations=" +
                "javax.ws.rs.Path;" +
                "javax.ws.rs.core.Application;" +
                "javax.ws.rs.ext.Provider",
             "service.vendor=IBM" })
public class LibertyResteasyCdiExtension extends ResteasyCdiExtension implements Extension, WebSphereCDIExtension {}
