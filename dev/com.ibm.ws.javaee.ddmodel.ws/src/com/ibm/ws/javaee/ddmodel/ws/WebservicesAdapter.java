/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
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

public final class WebservicesAdapter implements ContainerAdapter<Webservices> {
    @FFDCIgnore(ParseException.class)
    @Override
    public Webservices adapt(
        Container ddRoot,
        OverlayContainer rootOverlay, ArtifactContainer artifactContainer,
        Container ddRootAdapt) throws UnableToAdaptException {
        
        // TODO: The adapt path should always be "/".  Obtaining the path
        //       in this manner is strange.
        String adaptPath = artifactContainer.getPath();

        Webservices webServices = (Webservices)
            rootOverlay.getFromNonPersistentCache( adaptPath, Webservices.class );
        if ( webServices != null ) {
            return webServices;
        }

        Entry ddEntry;
        if ( rootOverlay.getFromNonPersistentCache(adaptPath, WebModuleInfo.class) != null ) {
            ddEntry = ddRootAdapt.getEntry(Webservices.WEB_DD_NAME);
        } else if ( rootOverlay.getFromNonPersistentCache(adaptPath, EJBModuleInfo.class) != null ) {
            ddEntry = ddRootAdapt.getEntry(Webservices.EJB_DD_NAME);
        } else {
            ddEntry = null;
        }
        if ( ddEntry == null ) {
            return null;
        }

        try {
            WebServicesDDParser ddParser = new WebServicesDDParser(ddRootAdapt, ddEntry);
            webServices = ddParser.parse();
        } catch ( ParseException e ) {
            throw new UnableToAdaptException(e);
        }

        rootOverlay.addToNonPersistentCache(adaptPath, Webservices.class, webServices);

        return webServices;
    }

    private static final class WebServicesDDParser extends DDParser {
        public WebServicesDDParser(Container ddRootContainer, Entry ddEntry) throws ParseException {
            super(ddRootContainer, ddEntry, "webservices");
        }

        @Override
        public WebservicesType parse() throws ParseException {
            super.parseRootElement();
            return (WebservicesType) rootParsable;
        }

        /**
         * Override: WebServices doesn't check the namespace.
         * 
         * @return The parsed root webservices element.
         * 
         * @throws ParseException Thrown in case of a non-valid root
         *     element name, or a non-valid version attribute.
         */
        @Override
        protected WebservicesType createRootParsable() throws ParseException {
            validateRootElementName();

            int ddVersion;
            String versAttr = getAttributeValue("", "version");
            if ( versAttr == null ) {
                ddVersion = 20;
            } else if ( versAttr.equals("2.0") ) {
                ddVersion = 20;
            } else if ( versAttr.equals("1.4") ) {
                ddVersion = 14;
            } else if ( versAttr.equals("1.3") ) {
                ddVersion = 13;
            } else if ( versAttr.equals("1.2") ) {
                ddVersion = 12;
            } else if ( versAttr.equals("1.1") ) {
                ddVersion = 11;
            } else {
                throw new ParseException( unsupportedDescriptorVersion(versAttr) );
            }
            version = ddVersion;

            // The webservices namespace is completely ignored.

            return new WebservicesType( getDeploymentDescriptorPath() ); 
        }
    }
}
