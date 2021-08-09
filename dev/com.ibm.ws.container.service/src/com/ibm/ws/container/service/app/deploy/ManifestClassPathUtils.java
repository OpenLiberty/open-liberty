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
package com.ibm.ws.container.service.app.deploy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * Utilities to assist with processing manifest classpaths
 */
public class ManifestClassPathUtils {
    static final TraceComponent tc = Tr.register(ManifestClassPathUtils.class);

    /**
     * create an Entry Identity that can identify an entry in an ear/war archive
     * This is used to avoid the cross reference in jar files' Class-Path causing the non-stopping recursion
     * 
     * @param entry
     * @return
     * @throws UnableToAdaptException
     */
    public static String createEntryIdentity(Entry entry) throws UnableToAdaptException {
        String result = "";
        while (entry != null && !entry.getPath().isEmpty()) {
            result = entry.getPath() + result;
            entry = entry.getRoot().adapt(Entry.class);
        }

        return result;
    }

    public static void processMFClasspath(Entry jarEntry, List<ContainerInfo> containers, Collection<String> resolved) throws UnableToAdaptException {
        processMFClasspath(jarEntry, containers, resolved, false);
    }

    @SuppressWarnings("deprecation")
    public static void processMFClasspath(Entry jarEntry, List<ContainerInfo> containers, Collection<String> resolved, boolean addRoot) throws UnableToAdaptException {
        String mfClassPath = null;
        if (jarEntry != null) {
            Container jarContainer = jarEntry.adapt(Container.class);
            if (jarContainer != null) {

                Entry manifestEntry = jarContainer.getEntry("/META-INF/MANIFEST.MF");
                if (manifestEntry != null) {
                    InputStream is = null;
                    try {
                        is = manifestEntry.adapt(InputStream.class);
                        Manifest manifest = new Manifest(is);
                        if (manifest != null) {
                            mfClassPath = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
                        }
                    } catch (IOException e) {
                        throw new UnableToAdaptException(e);
                    } finally {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException io) {
                            }
                        }
                    }
                }

            }//else no probs.. this jarEntry isn't usable as a container.. 
        }

        if (mfClassPath != null) {
            for (StringTokenizer tokenizer = new StringTokenizer(mfClassPath, " "); tokenizer.hasMoreTokens();) {
                String path = tokenizer.nextToken();

                if (path.equals(".")) {
                    if (addRoot) {
                        //add the root of the container holding the ear, if there was one..
                        final Container rootContainer = jarEntry.getRoot();
                        containers.add(new ContainerInfo() {
                            @Override
                            public Type getType() {
                                return Type.MANIFEST_CLASSPATH;
                            }

                            @Override
                            public String getName() {
                                return "/";
                            }

                            @Override
                            public Container getContainer() {
                                return rootContainer;
                            }
                        });
                    }
                } else {
                    URI pathUri;
                    try {
                        pathUri = new URI(path);
                    } catch (URISyntaxException e) {
                        Tr.warning(tc, "WARN_INVALID_MANIFEST_CLASSPATH_DEFINITION", path, jarEntry.getResource());
                        continue;
                    }

                    if (pathUri.isAbsolute()) {
                        Tr.warning(tc, "WARN_INVALID_MANIFEST_CLASSPATH_DEFINITION", path, jarEntry.getResource());
                        continue;
                    }

                    try {
                        Entry classPathEntry = findClassPathEntry(jarEntry, pathUri); // <----
                        if (classPathEntry == null) {
                            Tr.warning(tc, "WARN_MANIFEST_CLASSPATH_NOT_FOUND", path, jarEntry.getResource());
                            continue;
                        }

                        //does the entry convert to a container?
                        final String classPathName = createEntryIdentity(classPathEntry);
                        final Container classPathContainer = classPathEntry.adapt(Container.class);
                        if (classPathContainer != null) {

                            containers.add(new ContainerInfo() {
                                @Override
                                public Type getType() {
                                    return Type.MANIFEST_CLASSPATH;
                                }

                                @Override
                                public String getName() {
                                    return classPathName;
                                }

                                @Override
                                public Container getContainer() {
                                    return classPathContainer;
                                }
                            });
                        }

                        //TODO: toLowerCase?
                        if (classPathEntry.getName().endsWith(".jar")) {
                            addCompleteJarEntryUrls(containers, classPathEntry, resolved);
                        }
                    } catch (URISyntaxException e) {
                        //could not make sense of manifest entry..
                        throw new UnableToAdaptException(e);
                    }
                }
            }
        }
    }

    /**
     * Add the jar entry URLs and its class path URLs.
     * We need deal with all the thrown exceptions so that it won't interrupt the caller's processing.
     * 
     * @param urls
     * @param jarEntry
     */
    public static void addCompleteJarEntryUrls(List<ContainerInfo> containers, Entry jarEntry, Collection<String> resolved) throws UnableToAdaptException {
        String entryIdentity = createEntryIdentity(jarEntry);
        if (!entryIdentity.isEmpty() && !resolved.contains(entryIdentity)) {
            resolved.add(entryIdentity);
            processMFClasspath(jarEntry, containers, resolved);
        }
    }

    /**
     * calculate the class path entry based on the jar entry
     * 
     * @param jarEntry
     * @param pathUri
     * @return
     * @throws URISyntaxException
     * @throws UnableToAdaptException
     */
    private static Entry findClassPathEntry(Entry jarEntry, URI pathUri) throws URISyntaxException, UnableToAdaptException {
        URI relativeJarUri = new URI("/").relativize(new URI(jarEntry.getPath()));
        URI targetUri = null;
        targetUri = relativeJarUri.resolve(pathUri);

        if (targetUri.toString().startsWith("..")) {
            Entry rootEntry = jarEntry.getRoot().adapt(Entry.class);
            if (rootEntry == null || rootEntry.getPath().isEmpty()) { //already at the outermost
                return null;
            }
            return findClassPathEntry(rootEntry, new URI("..").relativize(targetUri)); // <----
        } else {
            return jarEntry.getRoot().getEntry(targetUri.toString());
        }
    }
}
