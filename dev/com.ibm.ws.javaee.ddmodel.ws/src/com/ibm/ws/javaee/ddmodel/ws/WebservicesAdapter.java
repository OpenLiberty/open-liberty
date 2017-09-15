/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ws;

import com.ibm.ws.container.service.app.deploy.EJBModuleInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.ws.Webservices;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 *
 */
public final class WebservicesAdapter implements ContainerAdapter<Webservices> {

    @FFDCIgnore(ParseException.class)
    @Override
    public Webservices adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {

        //try to find cache
        Webservices wsxml = (Webservices) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), Webservices.class);

        if (wsxml != null) {
            return wsxml;
        }

        Entry ddEntry = null;

        if (rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), WebModuleInfo.class) != null) {
            ddEntry = containerToAdapt.getEntry(Webservices.WEB_DD_NAME);
        } else if (rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), EJBModuleInfo.class) != null) {
            ddEntry = containerToAdapt.getEntry(Webservices.EJB_DD_NAME);
        }

        if (ddEntry != null) {

            try {
                WebServicesDDParser ddParser = new WebServicesDDParser(containerToAdapt, ddEntry);
                wsxml = ddParser.parse();
                //cache it
                rootOverlay.addToNonPersistentCache(artifactContainer.getPath(), Webservices.class, wsxml);
                return wsxml;
            } catch (ParseException e) {
                throw new UnableToAdaptException(e);
            }

        }

        return null;
    }

    /**
     * DDParser for webservices.xml
     */
    private static final class WebServicesDDParser extends DDParser {

        /**
         * @param ddRootContainer
         * @param ddEntry
         * @throws ParseException
         */
        public WebServicesDDParser(Container ddRootContainer, Entry ddEntry) throws ParseException {
            super(ddRootContainer, ddEntry);
        }

        Webservices parse() throws ParseException {
            super.parseRootElement();
            return (Webservices) rootParsable;
        }

        @Override
        protected DDParser.ParsableElement createRootParsable() throws ParseException {
            if (!"webservices".equals(rootElementLocalName)) {
                return null;
            }
            String vers = getAttributeValue("", "version");
            if (vers == null) {
                throw new ParseException(unknownDeploymentDescriptorVersion());
            }
            if ("1.4".equals(vers) || "1.3".equals(vers) || "1.2".equals(vers) || "1.1".equals(vers)) {
                return new WebservicesType(getDeploymentDescriptorPath());
            }
            throw new ParseException(invalidDeploymentDescriptorVersion(vers));
        }
    }
}
