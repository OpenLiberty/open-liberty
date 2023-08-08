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
package io.openliberty.cdi40.internal.weld;

import java.net.URL;

import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.internal.config.CDIConfiguration;
import com.ibm.ws.cdi.internal.interfaces.BeansXmlParser;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;

/**
 *
 */
@Component(name = "io.openliberty.cdi40.internal.weld.CDI40BeanParserImpl", service = { BeansXmlParser.class })
public class CDI40BeansXmlParserImpl implements BeansXmlParser {

    private static final TraceComponent tc = Tr.register(CDI40BeansXmlParserImpl.class);

    @Reference(name = "cdiContainerConfig", service = CDIConfiguration.class)
    private CDIConfiguration cdiContainerConfig;

    /** {@inheritDoc} */
    @Override
    public BeansXml parse(WebSphereCDIDeployment cdiDeployment, URL beansXmlUrl) {
        //Prior to CDI 4.0 an empty beans.xml meant an explicit archive (mode=ALL)
        //In CDI 4.0 the default becomes implicit (mode=ANNOTATED) but we provide a configuration option to switch it back to ALL
        boolean emptyBeansXmlCDI3Compatibility = cdiContainerConfig.emptyBeansXmlCDI3Compatibility();
        BeanDiscoveryMode emptyBeansXMLMode = emptyBeansXmlCDI3Compatibility ? BeanDiscoveryMode.ALL : BeanDiscoveryMode.ANNOTATED;
        BeansXml beansXml = cdiDeployment.getBootstrap().parse(beansXmlUrl, emptyBeansXMLMode);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            URL parsedURL = beansXml.getUrl();
            if (parsedURL != null && beansXml != null && beansXml.getVersion() == null) { //if the beansXml has no version
                //if the parsedURL is not null then it wasn't an empty beans xml file
                //if it was null and therefore the file was empty, only warn if the legacy config property was not set
                Tr.debug(tc, "Archive contains a non-empty beans.xml file without a version defined: " + beansXmlUrl);
            }
        }
        return beansXml;
    }

}
