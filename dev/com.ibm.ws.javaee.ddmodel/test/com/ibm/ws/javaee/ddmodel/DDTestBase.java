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
import org.junit.Assert;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.adaptable.module.adapters.EntryAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class DDTestBase {
    // 1.2
    // 1.3
    //
    // 1.4, http://java.sun.com/xml/ns/j2ee
    // 5,   http://java.sun.com/xml/ns/javaee
    // 6,   http://java.sun.com/xml/ns/javaee
    // 7,   http://xmlns.jcp.org/xml/ns/javaee
    // 8,   http://xmlns.jcp.org/xml/ns/javaee
    //
    // 9,   https://jakarta.ee/xml/ns/jakartaee    
    
    protected static final Mockery mockery = new Mockery();
    protected static int mockId;

    protected static int generateId() {
        return mockId++;
    }

    // Parse using a container adapter.    

    protected static <T, A> T parse(
            String appPath, String modulePath, String fragmentPath,
            String ddText, ContainerAdapter<T> ddAdapter, String ddPath) throws Exception {

        return parse(appPath, modulePath, fragmentPath,
                     ddText, ddAdapter, ddPath,
                     null);
    }

    protected static <T, A> T parse(
            String appPath, String modulePath, String fragmentPath,            
            String ddText, ContainerAdapter<T> ddAdapter, String ddPath,
            String altMessage, String... messages) throws Exception {

        return parse(appPath, modulePath, fragmentPath,
                     ddText, ddAdapter, ddPath,
                     null, null,
                     altMessage, messages);
    }

    protected static <T, A> T parse(
            String appPath, String modulePath, String fragmentPath,            
            String ddText, ContainerAdapter<T> ddAdapter, String ddPath,
            Class<A> extraAdaptClass, A extraAdaptValue) throws Exception {
        
        return parse(
                appPath, modulePath, fragmentPath,
                ddText, ddAdapter, ddPath,
                extraAdaptClass, extraAdaptValue,
                null);
    }

    protected static <T, A> T parse(
            String appPath, String modulePath, String fragmentPath,            
            String ddText, ContainerAdapter<T> ddAdapter, String ddPath,
            Class<A> extraAdaptClass, A extraAdaptValue,
            String altMessage, String... messages) throws Exception {

        return parse(
                appPath, modulePath, fragmentPath,
                ddText, ddAdapter, ddPath,
                extraAdaptClass, extraAdaptValue,
                null, null,
                altMessage, messages);
    }

    protected static <T, A, N> T parse(
            String appPath, String modulePath, String fragmentPath,            
            String ddText, ContainerAdapter<T> ddAdapter, String ddPath,
            Class<A> extraAdaptClass, A extraAdaptValue,
            Class<N> npCacheClass, N npCache) throws Exception {

        return parse(
                appPath, modulePath, fragmentPath,
                ddText, ddAdapter, ddPath,
                extraAdaptClass, extraAdaptValue,
                npCacheClass, npCache,
                null);
    }

    protected static <T, A, N> T parse(
        String appPath, String modulePath, String fragmentPath,                        
        String ddText, ContainerAdapter<T> ddAdapter, String ddPath,
        Class<A> extraAdaptClass, A extraAdaptValue,
        Class<N> npCacheClass, N npCache,
        String altMessage, String... messages) throws Exception {

        OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        ArtifactContainer artifactContainer = mockery.mock(ArtifactContainer.class, "artifactContainer" + mockId++);

        wire(rootOverlay, artifactContainer, null, null,
             npCacheClass, npCache);

        Container appRoot = ((appPath == null) ? null : mockery.mock(Container.class, "appRoot" + mockId++));

        Entry moduleEntry = ((appPath == null) ? null : mockery.mock(Entry.class, "moduleEntry" + mockId++));
        Container moduleRoot = mockery.mock(Container.class, "moduleRoot" + mockId++);

        Entry fragmentEntry = ((fragmentPath == null) ? null : mockery.mock(Entry.class, "fragmentEntry" + mockId++));
        Container fragmentRoot = ((fragmentPath == null) ? null : mockery.mock(Container.class, "fragmentRoot" + mockId++));

        Entry ddEntry = mockery.mock(Entry.class, "ddEntry" + mockId++);
        
        wire(appPath, modulePath, fragmentPath, ddPath,
             appRoot, moduleEntry, moduleRoot, fragmentEntry, fragmentRoot, ddEntry,
             ddText,
             extraAdaptClass, extraAdaptValue);

        try {
            T returnValue = ddAdapter.adapt(moduleRoot, rootOverlay, artifactContainer, moduleRoot);
            verifySuccess(altMessage, messages);
            return returnValue;

        } catch ( UnableToAdaptException e ) {
            verifyFailure( getCause(e), altMessage, messages );
            return null;
        }
    }            

    // Parse using an entry adapter.
    
    protected static <T, A> T parse(
            String appPath, String modulePath, String fragmentPath,            
            String xmlText, EntryAdapter<T> adapter, String ddPath) throws Exception {

        return parse(
                appPath, modulePath, fragmentPath,
                xmlText, adapter, ddPath,
                null);
    }

    protected static <T, A> T parse(
            String appPath, String modulePath, String fragmentPath,
            String ddText, EntryAdapter<T> ddAdapter, String ddPath,
            String altMessage, String... messages) throws Exception {

        OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        ArtifactEntry artifactEntry = mockery.mock(ArtifactEntry.class, "artifactEntry" + mockId++);

        wire(rootOverlay, null, artifactEntry, ddPath, null, null);

        Container appRoot = ((appPath == null) ? null : mockery.mock(Container.class, "appRoot" + mockId++));

        Entry moduleEntry = ((appPath == null) ? null : mockery.mock(Entry.class, "moduleEntry" + mockId++));
        Container moduleRoot = mockery.mock(Container.class, "moduleRoot" + mockId++);

        Entry fragmentEntry = ((fragmentPath == null) ? null : mockery.mock(Entry.class, "fragmentEntry" + mockId++));
        Container fragmentRoot = ((fragmentPath == null) ? null : mockery.mock(Container.class, "fragmentRoot" + mockId++));        

        Entry ddEntry = mockery.mock(Entry.class, "ddEntry" + mockId++);

        wire(appPath, modulePath, fragmentPath, ddPath,
             appRoot, moduleEntry, moduleRoot, fragmentEntry, fragmentRoot, ddEntry,
             ddText,
             null, null);

        try {
            T returnValue = ddAdapter.adapt(moduleRoot, rootOverlay, artifactEntry, ddEntry);
            verifySuccess(altMessage, messages);
            return returnValue;

        } catch ( UnableToAdaptException e ) {
            verifyFailure( getCause(e), altMessage, messages );
            return null;
        }
    }

    protected static Exception getCause(UnableToAdaptException e ) {
        Throwable cause = e.getCause();
        return ( (cause instanceof Exception) ? (Exception) cause : e );
    }
    
    protected static <N> void wire(
            OverlayContainer rootOverlay,
            ArtifactContainer artifactContainer,
            ArtifactEntry artifactEntry, String ddPath,
            Class<N> npCacheClass, N npCache) {

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
            }
        });
    }

    @SuppressWarnings("deprecation")
    protected static <T, A, N> void wire(
            String appPath, String modulePath, String fragmentPath, String ddPath,
            Container appRoot, Entry moduleEntry,
            Container moduleRoot, Entry fragmentEntry,
            Container fragmentRoot, Entry ddEntry,
            String ddText,
            Class<A> extraAdaptClass, A extraAdapt) throws UnsupportedEncodingException, UnableToAdaptException {
        
        mockery.checking(new Expectations() {
            {        
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
                will(returnValue(new ByteArrayInputStream(ddText.getBytes("UTF-8"))));
            }
        });
    }

    //

    public static final String UNSUPPORTED_DESCRIPTOR_NAMESPACE_ALT_MESSAGE =
            "unsupported.descriptor.namespace";
    public static final String[] UNSUPPORTED_DESCRIPTOR_NAMESPACE_MESSAGES =
            { "unknown" };

    public static final String UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE =
            "unsupported.descriptor.version";
    public static final String[] UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES =
            { "unknown" };
            
    public static final String UNPROVISIONED_DESCRIPTOR_VERSION_ALT_MESSAGE =    
            "unprovisioned.descriptor.version";            
    public static final String[] UNPROVISIONED_DESCRIPTOR_VERSION_MESSAGES =
            { "CWWKC2262E" };

    public static final String XML_ERROR_ALT_MESSAGE =
            "xml.error";    
    public static final String[] XML_ERROR_MESSAGES =
            { "CWWKC2272E" };

    //

    public static void verifySuccess(String altMessage, String... requiredMessages) {
        if ( (requiredMessages != null) && (requiredMessages.length > 0) ) {
            Assert.fail("Expected failure did not occur." +
                        " Either [ " + altMessage + " ] or all of [ " + Arrays.toString(requiredMessages) + " ] were expected.");
        }
    }
    
    public static void verifyFailure(Exception e, String altMessage, String... requiredMessages) {
        if ( (requiredMessages == null) || (requiredMessages.length == 0) ) {        
            Assert.fail("Unexpected exception [ " + e.getClass() + " ] [ " + e + " ]");
            return; // Never reached.
        }

        String errorMsg = e.getMessage();
        if ( errorMsg == null ) {
            Assert.fail("Exception [ " + e.getClass() + " ] [ " + e + " ] has a null message." +
                        "Either [ " + altMessage + " ] or all of [ " + Arrays.toString(requiredMessages) + " ] are required.");
            return; // Never reached.
        }

        if ( errorMsg.contains(altMessage) ) {
            return;
        }

        for ( String requiredMessage : requiredMessages ) {
            if ( !errorMsg.contains(requiredMessage) ) {
                Assert.fail("Exception [ " + e.getClass() + " ] [ " + e + " ] does not contain [ " + requiredMessage + " ]." +
                            " Either [ " + altMessage + " ] or all of [ " + Arrays.toString(requiredMessages) + " ] are required.");
                return; // Never reached.
            }
        }
    }    
}
