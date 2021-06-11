/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.adaptable.module.adapters.EntryAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class DDTestBase {
    protected final Mockery mockery = new Mockery();
    protected int mockId;

    protected <T, A> T parse(
        String xml,
        ContainerAdapter<T> adapter,
        String path, String... messages) throws Exception {

        return parse(xml, adapter, path, null, null, messages);
    }

    protected <T, A> T parse(
        String xmlText,
        ContainerAdapter<T> adapter,
        String ddPath,
        Class<A> extraAdaptClass, A extraAdaptValue,
        String... messages) throws Exception {

        return parse(xmlText, adapter, ddPath, extraAdaptClass, extraAdaptValue, null, null, messages);
    }

    // Parse using a container adapter.    
    protected <T, A, N> T parse(
        String xmlText,
        ContainerAdapter<T> adapter,
        String ddPath,
        Class<A> extraAdaptClass, A extraAdaptValue,
        Class<N> npCacheClass, N npCache,
        String... messages) throws Exception {

        String appPath = "/parent/wlp/usr/servers/server1/apps/someApp.ear";
        String modulePath = "web.war";
        String fragmentPath = null;

        OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        ArtifactContainer artifactContainer = mockery.mock(ArtifactContainer.class, "artifactContainer" + mockId++);

        Container appRoot = ((appPath == null) ? null : mockery.mock(Container.class, "appRoot" + mockId++));

        Entry moduleEntry = ((appPath == null) ? null : mockery.mock(Entry.class, "moduleEntry" + mockId++));
        Container moduleRoot = mockery.mock(Container.class, "moduleRoot" + mockId++);

        Entry fragmentEntry = ((fragmentPath == null) ? null : mockery.mock(Entry.class, "fragmentEntry" + mockId++));
        Container fragmentRoot = ((fragmentPath == null) ? null : mockery.mock(Container.class, "fragmentRoot" + mockId++));

        Entry ddEntry = mockery.mock(Entry.class, "ddEntry" + mockId++);
        
        wire(appPath, modulePath, fragmentPath, ddPath,
             rootOverlay, artifactContainer, null,
             appRoot, moduleEntry, moduleRoot, fragmentEntry, fragmentRoot, ddEntry,
             xmlText,
             extraAdaptClass, extraAdaptValue,
             npCacheClass, npCache);

        Exception boundException;

        try {
            T returnValue = adapter.adapt(moduleRoot, rootOverlay, artifactContainer, moduleRoot);
            if ( (messages != null) && (messages.length != 0) ) {
                throw new Exception("Expected exception text [ " + Arrays.toString(messages) + " ]");
            }            
            return returnValue;

        } catch ( UnableToAdaptException e ) {
            Throwable cause = e.getCause();
            if ( cause instanceof Exception ) {
                boundException = (Exception) cause;
            } else {
                boundException = e;
            }
        }

        if ( (messages != null) && (messages.length != 0) ) {
            String message = boundException.getMessage();
            if ( message != null ) {
                for ( String expected : messages ) {
                    if ( message.contains(expected) ) {
                        return null;
                    }
                }
            }
        }
        throw boundException;
    }            

    // Parse using an entry adapter.
    protected <T, A> T parse(
        String xmlText, EntryAdapter<T> adapter, String ddPath,
        String... messages) throws Exception {

        String appPath = "/parent/wlp/usr/servers/server1/apps/someApp.ear";
        String modulePath = "web.war";
        String fragmentPath = null;

        OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        ArtifactEntry artifactEntry = mockery.mock(ArtifactEntry.class, "artifactEntry" + mockId++);

        Container appRoot = ((appPath == null) ? null : mockery.mock(Container.class, "appRoot" + mockId++));

        Entry moduleEntry = ((appPath == null) ? null : mockery.mock(Entry.class, "moduleEntry" + mockId++));
        Container moduleRoot = mockery.mock(Container.class, "moduleRoot" + mockId++);

        Entry fragmentEntry = ((fragmentPath == null) ? null : mockery.mock(Entry.class, "fragmentEntry" + mockId++));
        Container fragmentRoot = ((fragmentPath == null) ? null : mockery.mock(Container.class, "fragmentRoot" + mockId++));        

        Entry ddEntry = mockery.mock(Entry.class, "ddEntry" + mockId++);

        wire(appPath, modulePath, fragmentPath, ddPath,
             rootOverlay, null, artifactEntry,
             appRoot, moduleEntry, moduleRoot, fragmentEntry, fragmentRoot, ddEntry,
             xmlText);

        Exception boundException;

        try {
            T returnValue = adapter.adapt(moduleRoot, rootOverlay, artifactEntry, ddEntry);
            if ( (messages != null) && (messages.length != 0) ) {
                throw new Exception("Expected exception text [ " + Arrays.toString(messages) + " ]");
            }            
            return returnValue;

        } catch ( UnableToAdaptException e ) {
            Throwable cause = e.getCause();
            if ( cause instanceof Exception ) {
                boundException = (Exception) cause;
            } else {
                boundException = e;
            }
        }

        if ( (messages != null) && (messages.length != 0) ) {
            String message = boundException.getMessage();
            if ( message != null ) {
                for ( String expected : messages ) {
                    if ( message.contains(expected) ) {
                        return null;
                    }
                }
            }
        }
        throw boundException;        
    }

    protected void wire(
            String appPath, String modulePath, String fragmentPath, String ddPath,
            OverlayContainer rootOverlay,
            ArtifactContainer artifactContainer, ArtifactEntry artifactEntry,
            Container appRoot, Entry moduleEntry,
            Container moduleRoot, Entry fragmentEntry,
            Container fragmentRoot, Entry ddEntry,
            String xmlText) throws UnsupportedEncodingException, UnableToAdaptException {

        wire(appPath, modulePath, fragmentPath, ddPath,
             rootOverlay, artifactContainer, artifactEntry,
             appRoot, moduleEntry, moduleRoot, fragmentEntry, fragmentRoot, ddEntry,
             xmlText,
             null, null, null, null);
    }

    @SuppressWarnings("deprecation")
    protected <T, A, N> void wire(
            String appPath, String modulePath, String fragmentPath, String ddPath,
            OverlayContainer rootOverlay,
            ArtifactContainer artifactContainer, ArtifactEntry artifactEntry,
            Container appRoot, Entry moduleEntry,
            Container moduleRoot, Entry fragmentEntry,
            Container fragmentRoot, Entry ddEntry,
            String xmlText,
            Class<A> extraAdaptClass, A extraAdapt,
            Class<N> npCacheClass, N npCache) throws UnsupportedEncodingException, UnableToAdaptException {

        mockery.checking(new Expectations() {
            {
                if ( npCacheClass != null ) {
                    allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(npCacheClass));
                    will(returnValue(npCache));
                }

                allowing(rootOverlay).addToNonPersistentCache(with(any(String.class)), with(any(Class.class)), with(any(Object.class)));
                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(any(Class.class)));
                will(returnValue(null));

                // An artifact container is provided when parsing
                // using container adapt.  An artifact entry is
                // provided when parsing using entry adapt.

                if ( artifactContainer != null ) {
                    allowing(artifactContainer).getPath();
                    will(returnValue("/"));
                }
                if ( artifactEntry != null ) {
                    allowing(artifactEntry).getPath();
                    will(returnValue('/' + ddPath));
                }

                // Wire up the root-of-roots.
                // If the module root is not the root-of-roots,
                // wire the module root to the app root.

                Container rootOfRoots;
                String rootPath;

                if ( appRoot != null ) {
                    rootOfRoots = appRoot;
                    rootPath = appPath;

                    allowing(appRoot).getPath();
                    will(returnValue("/"));
                    allowing(appRoot).getEntry(modulePath);
                    will(returnValue(moduleEntry));

                    allowing(moduleEntry).getRoot();
                    will(returnValue(appRoot));
                    allowing(moduleEntry).getPath();
                    will(returnValue(modulePath));

                    allowing(moduleRoot).adapt(Entry.class);
                    will(returnValue(moduleEntry));
                    
                } else {
                    rootOfRoots = moduleRoot;
                    rootPath = modulePath;
                }
                
                allowing(rootOfRoots).getPhysicalPath();
                will(returnValue(rootPath));                
                allowing(rootOfRoots).adapt(Entry.class);
                will(returnValue(null));

                allowing(moduleRoot).getPath();
                will(returnValue("/"));

                // Wire the module and fragment.
                //
                // When there is a fragment, wire it to the
                // module, and set it as the descriptor root.
                //
                // Otherwise, set the module as the descriptor root.

                Container ddRoot;
                
                if ( fragmentEntry != null ) {
                    ddRoot = fragmentRoot;

                    allowing(moduleRoot).getEntry(fragmentPath);
                    will(returnValue(fragmentEntry));
                    
                    allowing(fragmentEntry).getRoot();
                    will(returnValue(moduleRoot));
                    allowing(fragmentEntry).getPath();
                    will(returnValue(fragmentPath));

                    allowing(fragmentRoot).adapt(Entry.class);
                    will(returnValue(fragmentEntry));
                    allowing(fragmentRoot).getPath();
                    will(returnValue("/"));
                    
                } else {
                    ddRoot = moduleRoot;
                }

                // Wire the descriptor entry.

                allowing(ddRoot).getEntry(ddPath);
                will(returnValue(ddEntry));
                if ( extraAdaptClass != null ) {
                    allowing(ddRoot).adapt(extraAdaptClass);
                    will(returnValue(extraAdapt));
                }

                allowing(ddEntry).getRoot();
                will(returnValue(ddRoot));
                allowing(ddEntry).getPath();
                will(returnValue('/' + ddPath));
                allowing(ddEntry).adapt(InputStream.class);
                will(returnValue(new ByteArrayInputStream(xmlText.getBytes("UTF-8"))));
            }
        });
    }
}
