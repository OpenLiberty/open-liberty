/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import com.ibm.ws.cdi.internal.config.CDIConfiguration;
import com.ibm.ws.cdi.internal.interfaces.BeansXmlParser;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;

/**
 *
 */
@Component(name = "io.openliberty.cdi40.internal.weld.CDI40BeanParserImpl", service = { BeansXmlParser.class }, property = { "service.vendor=IBM" })
public class CDI40BeansXmlParserImpl implements BeansXmlParser {

    @Reference(name = "cdiContainerConfig", service = CDIConfiguration.class)
    private CDIConfiguration cdiContainerConfig;

    /** {@inheritDoc} */
    @Override
    public BeansXml parse(WebSphereCDIDeployment cdiDeployment, URL beansXmlUrl) {
        //Prior to CDI 4.0 an empty beans.xml meant an explicit archive (mode=ALL)
        //In CDI 4.0 the default becomes implicit (mode=ANNOTATED) but we provide a configuration option to switch it back to ALL
        BeanDiscoveryMode emptyBeansXMLMode = cdiContainerConfig.emptyBeansXMLExplicitArchive() ? BeanDiscoveryMode.ALL : BeanDiscoveryMode.ANNOTATED;
        return cdiDeployment.getBootstrap().parse(beansXmlUrl, emptyBeansXMLMode);
    }

}
