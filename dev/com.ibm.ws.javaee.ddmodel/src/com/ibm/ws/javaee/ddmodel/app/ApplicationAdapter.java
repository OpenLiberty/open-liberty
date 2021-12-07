/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
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
    private Version platformVersion = JavaEEVersion.DEFAULT_VERSION;

    public synchronized void setVersion(ServiceReference<JavaEEVersion> referenceRef) {
        this.versionRef = referenceRef;
        this.platformVersion = Version.parseVersion((String) referenceRef.getProperty("version"));

        if ( this.platformVersion == null ) {
            this.platformVersion = JavaEEVersion.DEFAULT_VERSION;
        }
    }

    public synchronized void unsetVersion(ServiceReference<JavaEEVersion> versionRef) {
        if ( versionRef == this.versionRef ) {
            this.versionRef = null;
            this.platformVersion = JavaEEVersion.DEFAULT_VERSION;
        }
    }

    public synchronized Version getVersion() {
        return platformVersion;
    }

    public int getVersionInt() {
        Version usePlatformVersion = getVersion();

        return ( usePlatformVersion.getMajor() * 10 +
                 usePlatformVersion.getMinor() );
    }

    public static final boolean FAILED = true;

    public static class ApplicationRef {
        public final Application application;
        public final String failure;

        public ApplicationRef(Application application) {
            this.application = application;
            this.failure = null;
        }

        public ApplicationRef(String failure) {
            this.application = null;
            this.failure = failure;
        }        

        public Application getApplication () {
            return application;
        }

        public String getFailure() {
            return failure;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T cacheGet(OverlayContainer rootOverlay, String targetPath, Class<T> targetType) {
        return (T) rootOverlay.getFromNonPersistentCache(targetPath, targetType);
    }

    private <T> void cachePut(OverlayContainer rootOverlay, String targetPath, Class<T> targetType, T value) {
        rootOverlay.addToNonPersistentCache(targetPath, targetType, value);
    }    

    
    @FFDCIgnore(ParseException.class)
    @Override
    public Application adapt(Container root,
                             OverlayContainer rootOverlay,
                             ArtifactContainer artifactContainer,
                             Container containerToAdapt) throws UnableToAdaptException {
        
        String containerPath = artifactContainer.getPath();
        
        Application appDD = cacheGet(rootOverlay, containerPath, Application.class);
        if ( appDD != null ) {
            return appDD;
        }

        ApplicationRef appDDRef = cacheGet(rootOverlay, containerPath, ApplicationRef.class);
        if ( appDDRef != null ) {
            String failure = appDDRef.getFailure();
            if ( failure != null ) {
                throw new UnableToAdaptException(failure);
            } else {
                return appDDRef.getApplication();
            }
        }

        ParseException pe = null;

        Entry ddEntry = containerToAdapt.getEntry(Application.DD_NAME);
        if ( ddEntry != null ) {
            try {
                ApplicationDDParser ddParser =
                    new ApplicationDDParser( containerToAdapt, ddEntry, getVersionInt() );
                appDD = ddParser.parse();
            } catch ( ParseException e ) {
                pe = e;
            }
        }

        if ( pe != null ) {
            appDDRef = new ApplicationRef(pe.getMessage());
        } else {
            appDDRef = new ApplicationRef(appDD);
        }

        if ( appDD != null ) {
            cachePut(rootOverlay, containerPath, Application.class, appDD);
        }
        cachePut(rootOverlay, containerPath, ApplicationRef.class, appDDRef);

        if ( pe != null ) {
            throw new UnableToAdaptException(pe);
        } else {
            return appDD;
        }
    }
}
