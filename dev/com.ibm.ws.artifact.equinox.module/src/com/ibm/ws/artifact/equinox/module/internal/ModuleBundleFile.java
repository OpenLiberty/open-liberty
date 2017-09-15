/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.equinox.module.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.BundleFileWrapper;
import org.osgi.framework.BundleContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * A BundleFile implementation that is backed by a {@link Container container}.
 */
public class ModuleBundleFile extends BundleFileWrapper {

    private static final TraceComponent tc = Tr.register(ModuleBundleFile.class);

    private final Container container;
    private final BundleFile wrappedBundleFile;
    private final Map<File, Container> nestedFiles;
    private final BundleContext context;

    public ModuleBundleFile(Container container, BundleFile wrappedBundleFile, Map<File, Container> nestedFiles, BundleContext context) {
        super(wrappedBundleFile);
        this.container = container;
        this.wrappedBundleFile = wrappedBundleFile;
        this.nestedFiles = nestedFiles;
        this.context = context;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.osgi.storage.bundlefile.BundleFileWrapper#close()
     */
    @Override
    public void close() throws IOException {
    //nothing.
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.osgi.storage.bundlefile.BundleFileWrapper#open()
     */
    @Override
    public void open() throws IOException {
    //nothing.
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleFile#getFile(java.lang.String, boolean)
     */
    @SuppressWarnings("deprecation")
    @Override
    public File getFile(String path, boolean nativeCode) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, " asked to getFile for path " + path);

        path = preSlashify(path);
        if (path.equals("/")) {
            // return a file for "/" .. for normal bundles this would be the jar
            // or directory. For loose, we might have a dir mapped to root, or may not
            // if we do, we'll try using it.
            if (container.getPhysicalPath() != null)
                return new File(container.getPhysicalPath());
            else
                return null;
        }
        final Entry entry = container.getEntry(path);
        if (entry == null) {
            //no entry found for path, bundle has been asked for a file/path that doesn't exist.
            //can happen via bundle-classpath entries for paths that don't exist in the bundle.
            return null;
        }
        Container entryContainer = getContainer(entry);
        if (entryContainer == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, " returning file for entry at " + path + " @ " + entry.getPhysicalPath());

            // this is a 'normal entry' we are going to assume we can get a real file
            // we are allowed to use getPhysicalPath, as this layer has to see through the abstraction for
            // leaf nodes that are files.
            final String physical = entry.getPhysicalPath();
            if (physical != null) {
                return AccessController.doPrivileged(new PrivilegedAction<File>() {
                    @Override
                    public File run() {
                        return new File(physical);
                    }

                });
            }
            // TODO Going to have to extract the content out to a file on disk

            // This would be a case where an entry in a container has no physical path. 
            // If loose config allowed jars to be mapped as directories, this would occur there.

            // Jars mapped as files are already handled by equinox natively, so the lack of 
            // physical paths there will not cause issues.

            // The other common source of imaginary physical paths is intermediate directory 
            // creation..  
            // (eg, mapping /etc/fred.txt as /etc/fish/goat/horse/fred.txt and asking for physical path of 'goat')

            // Current artifact implementations always have a physical path, but this may not remain true 
            // forever, so this case DOES need to be handled, else we'll have some very entertaining 
            // bugs to handle for future deployment types. 
            return null;
        }

        if (nativeCode) {
            // native code is not supported as a container
            return null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, " entry was a container.. but was it root? " + entryContainer.isRoot());
        if (!entryContainer.isRoot()) {
            // If the container is not a root then it is 
            // a simple directory in an archive; just return null.
            // The framework will handle nested directories in archives
            return null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, " leaf node was a root container, so use marker file for equinox");

        // This is a root container and may be on the bundle classpath.
        // Need to create a temp file to return so we can find this container later when a bundle file is created
        final File tempBase = context.getDataFile("nestedTempFiles");
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, " creating a temp dir, request was for " + path + " the nested path");

        // create the temp dir, if needed.. not clear
        if (!AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return tempBase.exists() || tempBase.mkdirs();
            }
        })) {
            //no chained exception will mean mkdirs failed.
            throw new IllegalStateException();
        }
        try {
            File t = null;
            try {
                t = AccessController.doPrivileged(new PrivilegedExceptionAction<File>() {
                    @Override
                    public File run() throws IOException {
                        return File.createTempFile(entry.getName(), null, tempBase);
                    }
                });
            } catch (PrivilegedActionException e) {
                Exception e2 = e.getException();
                if (e2 instanceof IOException)
                    throw (IOException) e2;
                if (e2 instanceof RuntimeException)
                    throw (RuntimeException) e2;
                throw new UndeclaredThrowableException(e);
            }
            final File tempKey = t;

            // Add it to the nestedFiles map so that the ModuleBundleFileFactory impl can
            // find the container if the framework attempts to create a bundle file from it.
            nestedFiles.put(tempKey, entryContainer);
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    tempKey.deleteOnExit();
                    return null;
                }
            });

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, " returning a tempkey file.. " + tempKey.getAbsolutePath());
            return tempKey;
        } catch (IOException e) {
            //chained exception will mean the temp create failed.
            throw new IllegalStateException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleFile#getEntry(java.lang.String)
     */
    @Override
    public BundleEntry getEntry(String path) {
        path = preSlashify(path);
        if (path.equals("/")) {
            return new RootModuleEntry(container);
        }
        Entry entry = container.getEntry(path);
        return entry == null ? null : new ModuleEntry(entry, postSlashify(path, entry));
    }

    private String preSlashify(String path) {
        // Our use of the Container API requires all paths begin with '/'
        // (The Container API will accept non / prefixed paths as being relative to the current container)
        // We also remove trailing slash if it exists, as the Container API does not use this convention.
        path = (path.isEmpty() || path.charAt(0) != '/') ? ('/' + path) : path;
        path = (path.length() > 1 && path.charAt(path.length() - 1) == '/') ? path.substring(0, path.length() - 1) : path;
        return path;
    }

    private String postSlashify(String path, Entry entry) {
        // Except for the root path '/' we don't want to have
        // paths that begin with '/' for the bundle entries.
        // We also need to have the path end in '/' if the path
        // is for a container (that is not root).
        // HACK!!  we pass entry as null when we want a trailing '/' from getEntryPaths
        Container c = null;
        if (entry == null || ((c = getContainer(entry)) != null) && !c.isRoot()) {
            path = path + '/';
        }
        return (path.length() > 1 && path.charAt(0) == '/') ? path.substring(1) : path;
    }

    static Container getContainer(Entry entry) {
        // This is just a wrapper method that returns null if UnableToAdaptException
        // occurs. We will treat that as there is no container for the entry.
        try {
            return entry.adapt(Container.class);
        } catch (UnableToAdaptException e) {
            // nothing
            e.hashCode(); //dummy invoke for findbugs.
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.osgi.storage.bundlefile.BundleFileWrapper#getEntryPaths(java.lang.String, boolean)
     */
    @Override
    public Enumeration<String> getEntryPaths(String path, boolean recurse) {
        path = preSlashify(path);
        Iterator<Entry> entries;
        if (path.equals("/")) {
            // asking for entries of the root container
            entries = container.iterator();
        } else {
            Entry entry = container.getEntry(path);
            Container entryContainer = entry == null ? null : getContainer(entry);
            // Check if the entry is a container; 
            // if so only iterate over its entries if the container is not a root
            entries = entryContainer == null || entryContainer.isRoot() ? null : entryContainer.iterator();
            path = postSlashify(path, null);
        }
        return entries == null ? null : new ContainerEnumeration(path, entries, recurse);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleFile#getEntryPaths(java.lang.String)
     */
    @Override
    public Enumeration<String> getEntryPaths(String path) {
        return getEntryPaths(path, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleFile#containsDir(java.lang.String)
     */
    @Override
    public boolean containsDir(String dir) {
        // TODO not super efficient
        // Should not be used much to cause performance issues
        return getEntryPaths(dir) != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleFile#getBaseFile()
     */
    @Override
    public File getBaseFile() {
        // This could cause some issues, but we have no real good options
        // but to return the original File object for which this bundle file is for
        return wrappedBundleFile.getBaseFile();
    }

    /*
     * Enumerates an iterator of entries that have a specific path context.
     * For example: path=foo/ entries=bar,baz then the paths returned by
     * this enumeration will be foo/bar, foo/baz.
     * Also checks if the entries are containers and appends the appropriate '/' if so.
     */
    static class ContainerEnumeration implements Enumeration<String> {
        private final Stack<String> path;
        private final Stack<Iterator<Entry>> iEntries;
        private final boolean recurse;

        /**
         * @param path
         * @param iEntries
         */
        public ContainerEnumeration(String path, Iterator<Entry> iEntries, boolean recurse) {
            this.path = new Stack<String>();
            this.path.push(path);
            this.iEntries = new Stack<Iterator<Entry>>();
            this.iEntries.push(iEntries);
            this.recurse = recurse;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Enumeration#hasMoreElements()
         */
        @Override
        public boolean hasMoreElements() {
            //as long as an element in the stack has more.. we do.
            for (int idx = 0; idx < iEntries.size(); idx++) {
                if (iEntries.get(idx).hasNext()) {
                    return true;
                }
            }
            return false;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Enumeration#nextElement()
         */
        @Override
        public String nextElement() {
            //if we're done with the current element.. pop stack.
            //keep popping till we find an entry with next =)
            while (!iEntries.peek().hasNext()) {
                iEntries.pop();
                path.pop();
            }
            //did we just blow the stack?
            if (iEntries.isEmpty()) {
                throw new IllegalStateException();
            }

            Entry next = iEntries.peek().next();

            Container nextContainer = getContainer(next);
            // here we assume path ends in '/'
            String result = path.peek() + next.getName();
            if (nextContainer != null) {
                // if the entry is a container then we need a trailing '/'
                result += "/";
            }

            if (recurse && nextContainer != null && !nextContainer.isRoot()) {
                iEntries.push(nextContainer.iterator());
                path.push(path.peek() + next.getName() + "/");
            }

            return result;
        }

    }
}
