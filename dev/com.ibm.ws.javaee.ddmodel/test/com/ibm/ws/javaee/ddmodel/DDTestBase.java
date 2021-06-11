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

    protected <T, A> T parse(String xml, ContainerAdapter<T> adapter, String path, String... messages) throws Exception {
        return parse(xml, adapter, path, null, null, messages);
    }

    protected <T, A> T parse(String xml, ContainerAdapter<T> adapter, String path,
                             Class<A> extraAdaptClass, A extraAdapt,
                             String... messages) throws Exception {
        return parse(xml, adapter, path, extraAdaptClass, extraAdapt, null, null, messages);
    }

    protected <T, A, N> T parse(String xml, ContainerAdapter<T> adapter, String path,
                                Class<A> extraAdaptClass, A extraAdapt,
                                Class<N> npCacheClass, N npCache,
                                String... messages) throws Exception {

        OverlayContainer rootOverlay =
            mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        ArtifactContainer artifactContainer =
            mockery.mock(ArtifactContainer.class, "artifactContainer" + mockId++);

        Container root = mockery.mock(Container.class, "root" + mockId++);
        Entry entry = mockery.mock(Entry.class, "entry" + mockId++);

        Container container = mockery.mock(Container.class, "container" + mockId++);

        mockery.checking(new Expectations() {
            {
                if (npCacheClass != null) {
                    allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(npCacheClass));
                    will(returnValue(npCache));
                }
                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(any(Class.class)));
                will(returnValue(null));
                allowing(rootOverlay).addToNonPersistentCache(with(any(String.class)), with(any(Class.class)), with(any(Object.class)));

                allowing(artifactContainer).getPath();
                will(returnValue(null));

                allowing(container).getEntry(path);
                will(returnValue(entry));
                if (extraAdaptClass != null) {
                    allowing(container).adapt(extraAdaptClass);
                    will(returnValue(extraAdapt));
                }

                allowing(entry).getPath();
                will(returnValue('/' + path));
                allowing(entry).adapt(InputStream.class);
                will(returnValue(new ByteArrayInputStream(xml.getBytes("UTF-8"))));
            }
        });

        Exception boundException;

        try {
            T returnValue = adapter.adapt(root, rootOverlay, artifactContainer, container);
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

    protected <T, A> T parse(
        String xml, EntryAdapter<T> adapter, String path,
        String... messages) throws Exception {

        Container root = mockery.mock(Container.class, "root" + mockId++);
        OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        ArtifactEntry artifactEntry = mockery.mock(ArtifactEntry.class, "artifactEntry" + mockId++);
        Entry entry = mockery.mock(Entry.class, "entry" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(artifactEntry).getPath();
                will(returnValue('/' + path));

                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(any(Class.class)));
                will(returnValue(null));

                allowing(entry).getPath();
                will(returnValue('/' + path));

                allowing(entry).adapt(InputStream.class);
                will(returnValue(new ByteArrayInputStream(xml.getBytes("UTF-8"))));

                allowing(rootOverlay).addToNonPersistentCache(with(any(String.class)), with(any(Class.class)), with(any(Object.class)));
            }
        });

        Exception boundException;

        try {
            T returnValue = adapter.adapt(root, rootOverlay, artifactEntry, entry);
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
}
