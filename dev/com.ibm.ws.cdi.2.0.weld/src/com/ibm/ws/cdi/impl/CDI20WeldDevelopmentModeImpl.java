/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.impl;

import javax.enterprise.inject.spi.Extension;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration.Dynamic;

import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.probe.ProbeExtension;
import org.jboss.weld.probe.ProbeFilter;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;
import com.ibm.ws.cdi.internal.interfaces.WeldDevelopmentMode;

@Component(name = "com.ibm.ws.cdi.impl.CDI20WeldDevelopmentModeImpl",
                service = { WeldDevelopmentMode.class },
                property = { "service.vendor=IBM" })
public class CDI20WeldDevelopmentModeImpl implements WeldDevelopmentMode {
    private ExtensionArchive probeExtensionArchive;

    @Override
    public ExtensionArchive getProbeExtensionArchive(CDIRuntime cdiRuntime) {
        synchronized (this) {
            if (this.probeExtensionArchive == null) {
                this.probeExtensionArchive = new ProbeExtensionArchive(cdiRuntime, null);
            }
        }
        return this.probeExtensionArchive;
    }

    @Override
    public Metadata<Extension> getProbeExtension() {
        Metadata<Extension> extension = CDIUtils.loadExtension(ProbeExtension.class.getName(), ProbeExtension.class.getClassLoader());
        return extension;
    }

    @Override
    public WebSphereBeanDeploymentArchive getProbeBDA(WebSphereCDIDeployment deployment) {
        WebSphereBeanDeploymentArchive bda = deployment.getBeanDeploymentArchive(ProbeExtension.class);
        return bda;
    }

    @Override
    public void addProbeFilter(ServletContext ctx) {
        //dynamically register a dummy servlet to ensure that the ProbeFilter will be triggered
        Dynamic servletDynamic = ctx.addServlet("ProbeServlet", ProbeDummyServlet.class);
        servletDynamic.addMapping("/weld-probe/*");
        //Add the Probe Filter for all URLs
        javax.servlet.FilterRegistration.Dynamic filterDynamic = ctx.addFilter("ProbeFilter", ProbeFilter.class);
        filterDynamic.addMappingForUrlPatterns(null, false, "/*");
    }
}
