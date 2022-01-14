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

import com.ibm.ws.cdi.internal.interfaces.BeanParser;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;

/**
 *
 */
@Component(name = "io.openliberty.cdi40.internal.weld.CDI40BeanParserImpl", service = { BeanParser.class }, property = { "service.vendor=IBM" })
public class CDI40BeanParserImpl implements BeanParser {

    /** {@inheritDoc} */
    @Override
    public BeansXml parse(WebSphereCDIDeployment cdiDeployment, URL beansXmlUrl) {
        //TODO: The BeanDiscoveryMode for an empty beans.xml needs to be configurable in order to maintain compatibility
        //Prior to CDI 4.0 an empty beans.xml meant an explicit archive (mode=ALL)
        //In CDI 4.0 the default becomes implicit (mode=ANNOTATED)
        return cdiDeployment.getBootstrap().parse(beansXmlUrl, BeanDiscoveryMode.ALL);
    }

}
