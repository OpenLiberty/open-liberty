/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.app;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public final class ApplicationAdapter implements ContainerAdapter<Application> {

    private ServiceReference<JavaEEVersion> versionRef;
    private volatile Version platformVersion = JavaEEVersion.DEFAULT_VERSION;

    public synchronized void setVersion(ServiceReference<JavaEEVersion> reference) {
        versionRef = reference;
        platformVersion = Version.parseVersion((String) reference.getProperty("version"));
    }

    public synchronized void unsetVersion(ServiceReference<JavaEEVersion> reference) {
        if (reference == this.versionRef) {
            versionRef = null;
            platformVersion = JavaEEVersion.DEFAULT_VERSION;
        }
    }

    @FFDCIgnore(ParseException.class)
    @Override
    public Application adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        Application appDD = (Application) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), Application.class);
        if (appDD != null) {
            return appDD;
        }
        Entry ddEntry = containerToAdapt.getEntry(Application.DD_NAME);
        if (ddEntry != null) {
            try {
                ApplicationDDParser ddParser = new ApplicationDDParser(containerToAdapt, ddEntry, platformVersion);
                appDD = ddParser.parse();
                rootOverlay.addToNonPersistentCache(artifactContainer.getPath(), Application.class, appDD);
                return appDD;
            } catch (ParseException e) {
                throw new UnableToAdaptException(e);
            }
        }
        return null;
    }

    private static final class ApplicationDDParser extends DDParser {
        private final Version eeVersion;

        public ApplicationDDParser(Container ddRootContainer, Entry ddEntry, Version platformVersion) throws ParseException {
            super(ddRootContainer, ddEntry);
            eeVersion = platformVersion;
        }

        Application parse() throws ParseException {
            super.parseRootElement();
            return (Application) rootParsable;
        }

        private static final String APPLICATION_DTD_PUBLIC_ID_12 = "-//Sun Microsystems, Inc.//DTD J2EE Application 1.2//EN";
        private static final String APPLICATION_DTD_PUBLIC_ID_13 = "-//Sun Microsystems, Inc.//DTD J2EE Application 1.3//EN";

        @Override
        protected DDParser.ParsableElement createRootParsable() throws ParseException {
            if (!"application".equals(rootElementLocalName)) {
                throw new ParseException(invalidRootElement());
            }
            if (namespace == null) {
                if (dtdPublicId != null) {
                    if (APPLICATION_DTD_PUBLIC_ID_12.equals(dtdPublicId)) {
                        version = 12;
                        eePlatformVersion = 12;
                        return new ApplicationType(getDeploymentDescriptorPath());
                    }
                    if (APPLICATION_DTD_PUBLIC_ID_13.equals(dtdPublicId)) {
                        version = 13;
                        eePlatformVersion = 13;
                        return new ApplicationType(getDeploymentDescriptorPath());
                    }
                }
                throw new ParseException(unknownDeploymentDescriptorVersion());
            }
            String vers = getAttributeValue("", "version");
            if (vers == null) {
                throw new ParseException(missingDeploymentDescriptorVersion());
            }

            if ("http://java.sun.com/xml/ns/j2ee".equals(namespace)) {
                if ("1.4".equals(vers)) {
                    version = 14;
                    eePlatformVersion = 14;
                    return new ApplicationType(getDeploymentDescriptorPath());
                }
            }
            else if ("http://java.sun.com/xml/ns/javaee".equals(namespace)) {
                if ("5".equals(vers)) {
                    version = 50;
                    eePlatformVersion = 50;
                    return new ApplicationType(getDeploymentDescriptorPath());
                }
                if ("6".equals(vers)) {
                    version = 60;
                    eePlatformVersion = 60;
                    return new ApplicationType(getDeploymentDescriptorPath());
                }
            }
            else if (eeVersion.compareTo(JavaEEVersion.VERSION_7_0) >= 0 && "http://xmlns.jcp.org/xml/ns/javaee".equals(namespace)) {
                if ("7".equals(vers)) {
                    version = 70;
                    eePlatformVersion = 70;
                    return new ApplicationType(getDeploymentDescriptorPath());
                }
            }

            throw new ParseException(invalidDeploymentDescriptorNamespace(vers));
        }
    }
}
