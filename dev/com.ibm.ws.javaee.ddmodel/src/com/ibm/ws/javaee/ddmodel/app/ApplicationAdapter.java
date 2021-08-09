/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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
    // TODO: Why is this volatile?
    private volatile Version platformVersion = JavaEEVersion.DEFAULT_VERSION;

    public synchronized void setVersion(ServiceReference<JavaEEVersion> referenceRef) {
        this.versionRef = referenceRef;
        this.platformVersion = Version.parseVersion((String) referenceRef.getProperty("version"));
    }

    public synchronized void unsetVersion(ServiceReference<JavaEEVersion> versionRef) {
        if (versionRef == this.versionRef) {
            this.versionRef = null;
            this.platformVersion = JavaEEVersion.DEFAULT_VERSION;
        }
    }

    @FFDCIgnore(ParseException.class)
    @Override
    public Application adapt(
                             Container root,
                             OverlayContainer rootOverlay,
                             ArtifactContainer artifactContainer,
                             Container containerToAdapt) throws UnableToAdaptException {

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

            // Old DTD based descriptor.

            if (namespace == null) {
                if (dtdPublicId != null) {
                    if (APPLICATION_DTD_PUBLIC_ID_12.equals(dtdPublicId)) {
                        version = Application.VERSION_1_2;
                        eePlatformVersion = Application.VERSION_1_2;
                        return new ApplicationType(getDeploymentDescriptorPath());
                    }
                    if (APPLICATION_DTD_PUBLIC_ID_13.equals(dtdPublicId)) {
                        version = Application.VERSION_1_3;
                        eePlatformVersion = Application.VERSION_1_3;
                        return new ApplicationType(getDeploymentDescriptorPath());
                    }
                }
                throw new ParseException(unknownDeploymentDescriptorVersion());
            }

            // Schema based descriptor.

            // A version must always be specified.
            String vers = getAttributeValue("", "version");

            if (vers == null) {
                throw new ParseException(missingDeploymentDescriptorVersion());
            }

            if ("1.4".equals(vers)) {
                // Always supported. The namespace must be correct for the version.
                if ("http://java.sun.com/xml/ns/j2ee".equals(namespace)) {
                    version = Application.VERSION_1_4;
                    eePlatformVersion = Application.VERSION_1_4;
                    return new ApplicationType(getDeploymentDescriptorPath());
                }
            } else if ("5".equals(vers)) {
                // Always supported. The namespace must be correct for the version.
                if ("http://java.sun.com/xml/ns/javaee".equals(namespace)) {
                    version = Application.VERSION_5;
                    eePlatformVersion = Application.VERSION_5;
                    return new ApplicationType(getDeploymentDescriptorPath());
                }
            } else if ("6".equals(vers)) {
                // Always supported. The namespace must be correct for the version.
                if ("http://java.sun.com/xml/ns/javaee".equals(namespace)) {
                    version = Application.VERSION_6;
                    eePlatformVersion = Application.VERSION_6;
                    return new ApplicationType(getDeploymentDescriptorPath());
                }
            } else if ("7".equals(vers)) {
                // Supported only when provisioned for java 7 or higher.
                // The namespace must still be correctly set.
                if (eeVersion.compareTo(JavaEEVersion.VERSION_7_0) >= 0) {
                    if ("http://xmlns.jcp.org/xml/ns/javaee".equals(namespace)) {
                        version = Application.VERSION_7;
                        eePlatformVersion = Application.VERSION_7;
                        return new ApplicationType(getDeploymentDescriptorPath());
                    }
                }
            } else if ("8".equals(vers)) {
                // Supported only when provisioned for java 8 or higher.
                // The namespace must still be correctly set.
                if (eeVersion.compareTo(JavaEEVersion.VERSION_8_0) >= 0) {
                    if ("http://xmlns.jcp.org/xml/ns/javaee".equals(namespace)) {
                        version = Application.VERSION_8;
                        eePlatformVersion = Application.VERSION_8;
                        return new ApplicationType(getDeploymentDescriptorPath());
                    }
                }
            } else if ("9".equals(vers)) {
                // Supported only when provisioned for java 9 or higher.
                // The namespace must still be correctly set.
                if (eeVersion.compareTo(JavaEEVersion.VERSION_9_0) >= 0) {
                    if ("https://jakarta.ee/xml/ns/jakartaee".equals(namespace)) {
                        version = Application.VERSION_9;
                        eePlatformVersion = Application.VERSION_9;
                        return new ApplicationType(getDeploymentDescriptorPath());
                    }
                }
            }

            throw new ParseException(invalidDeploymentDescriptorNamespace(vers));
        }
    }
}
