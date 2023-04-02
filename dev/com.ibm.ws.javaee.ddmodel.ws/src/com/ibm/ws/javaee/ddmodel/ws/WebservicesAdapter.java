/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmodel.ws;

import com.ibm.ws.container.service.app.deploy.EJBModuleInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.PlatformVersion;
import com.ibm.ws.javaee.dd.ws.Webservices;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.DDParser.VersionData;
import com.ibm.ws.javaee.ddmodel.DDParserSpec;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public final class WebservicesAdapter implements ContainerAdapter<Webservices>, PlatformVersion {

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

    public static VersionData[] VERSION_DATA = {
            new VersionData(
                    Webservices.VERSION_1_1_STR,
                    null, DDParser.NAMESPACE_SUN_J2EE,
                    Webservices.VERSION_1_1, VERSION_1_4_INT),
            new VersionData(
                    Webservices.VERSION_1_2_STR,
                    null, DDParser.NAMESPACE_SUN_JAVAEE,
                    Webservices.VERSION_1_2, VERSION_5_0_INT),            
            new VersionData(
                    Webservices.VERSION_1_3_STR,
                    null, DDParser.NAMESPACE_SUN_JAVAEE,
                    Webservices.VERSION_1_3, VERSION_6_0_INT),                        
            new VersionData(
                    Webservices.VERSION_1_4_STR,
                    null, DDParser.NAMESPACE_JCP_JAVAEE,
                    Webservices.VERSION_1_4, VERSION_7_0_INT),                                    
            new VersionData(
                    Webservices.VERSION_2_0_STR,
                    null, DDParser.NAMESPACE_JAKARTA,
                    Webservices.VERSION_2_0, VERSION_9_0_INT),
            // No new data for Jakarta 10
        };

    public static int getMaxTolerated() {
        return Webservices.VERSION_2_0;
    }
    
    public static int getMaxImplemented() {
        return Webservices.VERSION_2_0;
    }
    
    private static final class WebServicesDDParser extends DDParserSpec {
        public WebServicesDDParser(Container ddRootContainer, Entry ddEntry) throws ParseException {
            super(ddRootContainer, ddEntry, Webservices.VERSION_2_0, "webservices");
        }

        @Override
        protected VersionData[] getVersionData() {
            return VERSION_DATA;
        }

        @Override
        public WebservicesType parse() throws ParseException {
            return (WebservicesType) super.parse();
        }

        @Override
        protected WebservicesType createRootElement() {
            return new WebservicesType( getDeploymentDescriptorPath() );
        }
    }
}
