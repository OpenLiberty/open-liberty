/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.zip.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.artifact.contributor.ArtifactContainerFactoryHelper;
import com.ibm.ws.artifact.zip.cache.ZipCachingService;
import com.ibm.ws.classloading.configuration.GlobalClassloadingConfiguration;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;

/**
 * Factory to create Containers for Zip structured data.
 */
public class ZipFileContainerFactory implements ArtifactContainerFactoryHelper, ContainerFactoryHolder {

    static final TraceComponent tc = Tr.register(ZipFileContainerFactory.class);

    private boolean hasZipExtension(String name) {
        return name.matches("(?i:(.*)\\.(ZIP|[SEJRW]AR|E[BS]A))");
    }

    /**
     * Returns a Container if the Object is a File, and if the filename has an extension indicating Zip data. <p> {@inheritDoc}
     */
    @Override
    public ArtifactContainer createContainer(File cacheDir, Object o) {
        ArtifactContainer zfc = null;
        if (o instanceof File && Utils.isFile(((File) o))) {
            File f = (File) o;
            if (isZip(f)) {
                zfc = new ZipFileContainer(cacheDir, f, this);
            }
        }
        return zfc;
    }

    /**
     * Returns a Container if
     * <li>the Object is a File, and if the filename has an extension indicating Zip data.
     * <li>the Entry name has an extension indicating Zip data, and the inputstream for the Entry is able to be opened as a ZipInputStream <p> {@inheritDoc}
     */
    @Override
    public ArtifactContainer createContainer(File cacheDir, ArtifactContainer parent, ArtifactEntry e, Object o) {
        ArtifactContainer zfc = null;

        if (o instanceof File && Utils.isFile(((File) o))) {
            File f = (File) o;
            if (isZip(f)) {
                zfc = new ZipFileContainer(cacheDir, parent, e, f, this);
            }
        } else {
            if (isZip(e)) {
                zfc = new ZipFileContainer(cacheDir, parent, e, null, this);
            }
        }
        return zfc;
    }

    private boolean isZip(ArtifactEntry e) {
        boolean validZip = false;

        if (hasZipExtension(e.getName())) {
            InputStream is = null;
            try {
                is = e.getInputStream();
                if (is == null) {
                    return false;
                }
                if (!(is instanceof BufferedInputStream)) {
                    is = new BufferedInputStream(is);
                }

                ZipInputStream zis = new ZipInputStream(is);
                //test if its actually a zip ?
                try {
                    // we call getNextEntry to ensure we have a valid zip, will fail if not.
                    // we don't care about the first entry so we can ignore it.
                    zis.getNextEntry();
                    validZip = true;
                } catch (IOException io) {
                    //if we caught an exception.. it's not a zip
                    //or its a broken zip
                    //or the disk is failing

                    //no need to report the error, we were only attempting to load the file.
                    //if we fail, someone else might not.
                    //update: we now report the error, as a user aid to diagnosing issues.

                    // If we get an exception it isn't a valid zip.
                    // build a path for the message.. this isn't too straightforward.
                    // note: the artifact api impls are allowed to use getPhysicalPath =)
                    String path = e.getPath();
                    if (e.getPhysicalPath() != null) {
                        //entry has a real path
                        path = e.getPhysicalPath();
                    } else {
                        if (e.getRoot().getPhysicalPath() != null) {
                            //entry didnt have a path, but it's root did.. 
                            path = e.getRoot().getPhysicalPath() + "!" + path;
                        } else {
                            //entry and it's root had no physical path..
                            //can we go up above that?
                            boolean found = false;
                            ArtifactEntry parent = e.getRoot().getEntryInEnclosingContainer();
                            while (parent != null) {
                                if (parent.getPhysicalPath() != null) {
                                    path = parent.getPhysicalPath() + "!" + path;
                                } else {
                                    path = parent.getPath() + "!" + path;
                                }
                                parent = parent.getRoot().getEntryInEnclosingContainer();
                            }
                            //path is now either prefixed by a physical path, or is as 
                            //good as we can get.. (eg, if this is a zip in loose.. )
                        }
                    }
                    Tr.error(tc, "bad.zip.data", path);
                }
                try {
                    // attempt to close the zip, ignoring any error because we can't recover.
                    zis.close();
                } catch (IOException ioe) {
                    //ignore errors closing.
                }
            } catch (IOException e1) {
                //IOException just means we couldn't verify it was a zip, so return false.
                //IOException must have come from getInputStream, so we have nothing to close.
                //(all others are caught above)
                return false;
            }
        }

        return validZip;
    }

    @FFDCIgnore(IOException.class)
    private boolean isZip(File f) {
        boolean validZip = false;
        if (hasZipExtension(f.getName())) {
            // Opening the file as a zip to ensure it really is a valid zip.
            ZipFile zf;
            try {
                zf = Utils.newZipFile(f);
                validZip = true;
                zf.close();
            } catch (IOException e) {
                // If we get an exception it isn't a valid zip.
                Tr.error(tc, "bad.zip.data", f.getAbsolutePath());
            }

        }

        return validZip;
    }

    private ArtifactContainerFactory containerFactory = null;
    private ZipCachingService zipCachingService = null;
    private BundleContext ctx = null;
    private GlobalClassloadingConfiguration classLoadingConfiguration;

    protected synchronized void activate(ComponentContext ctx) {
        //need to get this into containers for the notifier.. 
        this.ctx = ctx.getBundleContext();
    }

    protected synchronized void deactivate(ComponentContext ctx) {
        this.containerFactory = null;
        this.ctx = null;
    }

    protected synchronized void setContainerFactory(ArtifactContainerFactory cf) {
        this.containerFactory = cf;
    }

    protected synchronized void unsetContainerFactory(ArtifactContainerFactory cf) {
        if (this.containerFactory == cf)
            this.containerFactory = null;
    }

    protected void setGlobalClassloadingConfiguration(GlobalClassloadingConfiguration globalClassloadingConfiguration) {
        this.classLoadingConfiguration = globalClassloadingConfiguration;
    }

    protected void setZipCachingService(ZipCachingService zcs) {
        this.zipCachingService = zcs;
    }

    @Override
    public synchronized ArtifactContainerFactory getContainerFactory() {
        if (containerFactory == null) {
            throw new IllegalStateException();
        }
        return containerFactory;
    }

    @Override
    public synchronized BundleContext getBundleContext() {
        if (ctx == null) {
            throw new IllegalStateException();
        }
        return ctx;
    }

    @Override
    public ZipCachingService getZipCachingService() {
        if (zipCachingService == null) {
            throw new IllegalStateException();
        }
        return this.zipCachingService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.artifact.zip.internal.ContainerFactoryHolder#useJarUrls()
     */
    @Override
    public boolean useJarUrls() {
        // The classloadingConfiguration is optional, so don't assume it's there
        if (classLoadingConfiguration != null) {
            return classLoadingConfiguration.useJarUrls();
        } else {
            return false;
        }
    }

}
