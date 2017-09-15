/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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

    protected <T, A> T parse(String xml, ContainerAdapter<T> adapter, String path) throws Exception {
        return parse(xml, adapter, path, null, null);
    }

    protected <T, A> T parse(final String xml, ContainerAdapter<T> adapter, final String path,
                             final Class<A> extraAdaptClass, final A extraAdapt) throws Exception {
        return parse(xml, adapter, path, extraAdaptClass, extraAdapt, null, null);
    }

    protected <T, A, N> T parse(final String xml, ContainerAdapter<T> adapter, final String path,
                                final Class<A> extraAdaptClass, final A extraAdapt,
                                final Class<N> npCacheClass, final N npCache) throws Exception {
        final Container root = mockery.mock(Container.class, "root" + mockId++);
        final Entry entry = mockery.mock(Entry.class, "entry" + mockId++);
        final OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        final ArtifactContainer artifactContainer = mockery.mock(ArtifactContainer.class, "artifactContainer" + mockId++);
        final Container container = mockery.mock(Container.class, "container" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(artifactContainer).getPath();
                will(returnValue(null));

                if (npCacheClass != null) {
                    allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(npCacheClass));
                    will(returnValue(npCache));
                }

                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(any(Class.class)));
                will(returnValue(null));

                if (extraAdaptClass != null) {
                    allowing(container).adapt(extraAdaptClass);
                    will(returnValue(extraAdapt));
                }

                allowing(container).getEntry(path);
                will(returnValue(entry));

                allowing(entry).getPath();
                will(returnValue('/' + path));

                allowing(entry).adapt(InputStream.class);
                will(returnValue(new ByteArrayInputStream(xml.getBytes("UTF-8"))));

                allowing(rootOverlay).addToNonPersistentCache(with(any(String.class)), with(any(Class.class)), with(any(Object.class)));
            }
        });

        try {
            return adapter.adapt(root, rootOverlay, artifactContainer, container);
        } catch (UnableToAdaptException e) {
            Throwable cause = e.getCause();
            throw cause instanceof Exception ? (Exception) cause : e;
        }
    }

    protected <T, A> T parse(final String xml, EntryAdapter<T> adapter, final String path) throws Exception {
        final Container root = mockery.mock(Container.class, "root" + mockId++);
        final OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        final ArtifactEntry artifactEntry = mockery.mock(ArtifactEntry.class, "artifactEntry" + mockId++);
        final Entry entry = mockery.mock(Entry.class, "entry" + mockId++);

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

        try {
            return adapter.adapt(root, rootOverlay, artifactEntry, entry);
        } catch (UnableToAdaptException e) {
            Throwable cause = e.getCause();
            throw cause instanceof Exception ? (Exception) cause : e;
        }
    }
}
