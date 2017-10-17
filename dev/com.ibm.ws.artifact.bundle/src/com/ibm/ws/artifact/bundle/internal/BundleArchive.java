/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.bundle.internal;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactNotifier;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 *
 */
public class BundleArchive implements ArtifactContainer {

    private final Bundle bundle;

    private final ArtifactContainer enclosingContainer;
    private final ArtifactEntry entryInEnclosingContainer;

    private final BundleContainerFactoryHelper containerFactoryHolder;

    private final File cacheDir;

    /**
     * @param cacheDir
     * @param bundle
     * @param enclosingContainer
     */
    public BundleArchive(File cacheDir, Bundle bundle, ArtifactContainer enclosingContainer, ArtifactEntry entryInEnclosingContainer,
                         BundleContainerFactoryHelper containerFactoryHolder) {
        super();
        this.cacheDir = cacheDir;
        this.bundle = bundle;
        this.enclosingContainer = enclosingContainer;
        this.containerFactoryHolder = containerFactoryHolder;
        this.entryInEnclosingContainer = entryInEnclosingContainer;
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactEntry getEntry(String pathAndName) {
        // Normalize the path to handle .. notation and check they are not trying to leave the archive
        pathAndName = PathUtils.checkAndNormalizeRootPath(pathAndName);

        // We may have bundle fragments so first test this bundle and if we don't find the entry test the fragments
        URL bundleEntry = this.getBundleEntry(this.bundle, pathAndName);
        if (bundleEntry == null) {
            /*
             * Fragments are loaded via bundle wirings and the parent bundle (that we are working with) "provides" the wires to it's fragments (see section 7.2.3 of the OSGi 4.3
             * spec). Iterate through each of them until we find the entry
             */
            BundleWiring bundleWiring = this.bundle.adapt(BundleWiring.class);
            // The bundleWiring can be null - in particular if this bundle has been stopped/uninstalled.
            if (bundleWiring != null) {
                List<BundleWire> wires = bundleWiring.getProvidedWires(BundleRevision.HOST_NAMESPACE);
                if (wires != null) {
                    Iterator<BundleWire> wireIterator = wires.iterator();
                    while (bundleEntry == null && wireIterator.hasNext()) {
                        BundleWire wire = wireIterator.next();
                        Bundle fragment = wire.getRequirerWiring().getBundle();
                        bundleEntry = this.getBundleEntry(fragment, pathAndName);
                    }
                }
            }

        }

        if (bundleEntry != null) {
            return this.createEntry(bundleEntry);
        }
        return null;
    }

    /**
     * This method will return a bundle entry URL for the supplied path, it will test for both a normal entry and a directory entry for it.
     *
     * @param pathAndName The path to the entry
     * @return The URL for the bundle entry
     */
    @FFDCIgnore(IllegalStateException.class)
    public URL getBundleEntry(Bundle bundleToTest, String pathAndName) {
        try {
            URL bundleEntry = bundleToTest.getEntry(pathAndName);

            /*
             * Defect 54588 discovered that if a directory does not have a zip entry then calling getEntry will return null unless the path has a "/" on the end so if we have null
             * still then add a "/" on the end of the path and retest
             */
            if (bundleEntry == null) {
                bundleEntry = bundleToTest.getEntry(pathAndName + "/");
            }
            return bundleEntry;
        } catch (IllegalStateException ise) {
            //bundle context was no longer valid, so we cannot use getEntry any more.
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(IllegalStateException.class)
    public Collection<URL> getURLs() {
        try {
            URL u = this.bundle.getEntry("/");
            return Collections.singleton(u);
        } catch (IllegalStateException ise) {
            return Collections.emptyList();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRoot() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void stopUsingFastMode() {
        // No-op

    }

    /** {@inheritDoc} */
    @Override
    public void useFastMode() {
        // No-op

    }

    /** {@inheritDoc} */
    @Override
    public Iterator<ArtifactEntry> iterator() {
        return this.iterator("/");
    }

    @FFDCIgnore(IllegalStateException.class)
    protected Iterator<ArtifactEntry> iterator(String path) {
        try {
            Enumeration<URL> entryUrls = this.bundle.findEntries(path, "*", false);
            if (entryUrls == null) {
                Set<ArtifactEntry> entries = Collections.emptySet();
                return entries.iterator();
            }
            Map<String, ArtifactEntry> entries = new LinkedHashMap<String, ArtifactEntry>();
            while (entryUrls.hasMoreElements()) {
                URL entryUrl = entryUrls.nextElement();
                ArtifactEntry potential = this.createEntry(entryUrl);
                String potPath = potential.getPath();
                //ignore dupes, AND remove "/" as iterator is only used for iterating Entries, and
                //there is no Entry for "/"
                if (!entries.containsKey(potPath) && !"/".equals(potPath)) {
                    entries.put(potential.getPath(), potential);
                }
            }
            return entries.values().iterator();
        } catch (IllegalStateException ise) {
            return Collections.<ArtifactEntry> emptyList().iterator();
        }
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactContainer getEnclosingContainer() {
        return this.enclosingContainer;
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactEntry getEntryInEnclosingContainer() {
        return this.entryInEnclosingContainer;
    }

    /** {@inheritDoc} */
    @Override
    public String getPath() {
        return "/";
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "/";
    }

    /** returns <code>null</code> */
    @Override
    public String getPhysicalPath() {
        // We can't get a physical path for a bundle so have to return null
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactContainer getRoot() {
        return this;
    }

    private ArtifactEntry createEntry(URL entryUrl) {
        String path = entryUrl.getPath();
        // A / at the end indicates a directory
        if ('/' == path.charAt(path.length() - 1)) {
            return new BundleContainer(entryUrl, this);
        } else {
            return new BundleEntry(entryUrl, this);
        }
    }

    /**
     * Method for other classes in this package to get hold of a container factory for creating containers.
     *
     * @return the ContainerFactory
     */
    ArtifactContainerFactory getContainerFactory() {
        return containerFactoryHolder.getContainerFactory();
    }

    //package protected method to get cacheDir
    File getCacheDir() {
        return this.cacheDir;
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactNotifier getArtifactNotifier() {
        //This will need to become a singleton, should we implement the notifier for bundles.
        return new BundleNotifier();
    }
}
