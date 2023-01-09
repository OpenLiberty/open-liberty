/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;

import com.ibm.ws.javaee.dd.PlatformVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.adaptable.module.adapters.EntryAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 * Duplicated as com.ibm.ws.javaee.ddmodel.ws.DDTestBase_Webservices.
 *
 * Copied for simplicity. I'm not sure how to setup a
 * dependency of test code in a project on other test code
 * in another project.
 */
public class DDTestBase implements PlatformVersion {
    public static String getDottedVersionText(int version) {
        return PlatformVersion.getDottedVersionText(version);
    }

    public static String getVersionText(int version) {
        return PlatformVersion.getVersionText(version);
    }

    // Note: A version of the following which included the exception type
    //       as a type parameter was considered.  The resulting complexity
    //       was not sufficient, even though the resulting types would be
    //       much more usable.  Current use only uses 'Exception' as the
    //       exception type.

    @FunctionalInterface
    public interface FailableProducer<T> {
        T produce() throws Exception;
    }

    public class Failable<T> {
        private final FailableProducer<T> producer;

        private boolean didProduce;

        private T produced;

        private Exception failure;

        public Failable(FailableProducer<T> producer) {
            this.producer = producer;
        }

        public void reset() {
            didProduce = false;
            produced = null;
            failure = null;
        }

        public T get() throws Exception {
            if (didProduce) {
                if (failure != null) {
                    throw failure;
                } else {
                    return produced;
                }
            }

            try {
                produced = producer.produce();
                didProduce = true;
                return produced;

            } catch (Exception e) {
                failure = e;
                didProduce = true;
                throw e;
            }
        }
    }

    //

    public enum HeaderLine {
        NS(1),
        SI(2),
        SL(3),
        XSI(2, 3),
        V(4);

        public final int lineNo0;
        public final int lineNo1;

        private static final int UNUSED_LINE = -1;

        private HeaderLine(int lineNo0) {
            this.lineNo0 = lineNo0;
            this.lineNo1 = UNUSED_LINE;
        }

        private HeaderLine(int lineNo0, int lineNo1) {
            this.lineNo0 = lineNo0;
            this.lineNo1 = lineNo1;
        }

        public String adjust(String header) {
            int startOffset = -1;
            int endOffset = -1;

            for ( int lineNo = 0, lastOffset = 0, nextOffset = 0;
                  (nextOffset = header.indexOf('\n', lastOffset)) != -1;
                  lineNo++ ) {

                nextOffset++; // Include the end-of-line in the region.

                if (startOffset == -1) {
                    if (lineNo == lineNo0) {
                        startOffset = lastOffset; // Everything before and including the last end-of-line
                        if (lineNo1 == UNUSED_LINE) {
                            endOffset = nextOffset; // Everything after this end-of-line
                            break;
                        }
                    }

                } else {
                    if (lineNo == lineNo1) { // Can't get here if 'lineNo1 == UNUSED_LINE'
                        endOffset = nextOffset; // Everything after this end-of-line
                        break;
                    }
                }

                lastOffset = nextOffset;
            }

            if ((startOffset == -1) || (endOffset == -1)) {
                throw new IllegalArgumentException("Too few lines; [ " + lineNo0 + " : " + lineNo1 + " ]");
            }

            return header.substring(0, startOffset) + header.substring(endOffset);
        }
    }

    //

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

    protected static final List<Boolean[]> TEST_DATA;

    static {
        List<Boolean[]> testData = new ArrayList<Boolean[]>(2);
        testData.add(new Boolean[] { Boolean.FALSE });
        testData.add(new Boolean[] { Boolean.TRUE });
        TEST_DATA = testData;
    }

    //

    protected static final Mockery mockery = new Mockery();
    protected static int mockId;

    protected static int generateId() {
        return mockId++;
    }

    //

    protected static class ClassKeyedData {
        public final Class<?> classKey;
        public final Object value;

        public ClassKeyedData(Class<?> classKey, Object value) {
            this.classKey = classKey;
            this.value = value;
        }
    }

    protected static ClassKeyedData[] asDataArray(Class<?> adaptKey, Object adaptValue) {
        if (adaptKey == null) {
            return null;
        } else {
            return new ClassKeyedData[] { new ClassKeyedData(adaptKey, adaptValue) };
        }
    }

    // Parse using a container adapter.

    protected static <T> T parse(
                                 String appPath, String modulePath, String fragmentPath,
                                 String ddText, ContainerAdapter<T> ddAdapter, String ddPath) throws Exception {

        return parse(appPath, modulePath, fragmentPath,
                     ddText, ddAdapter, ddPath,
                     null);
    }

    protected static <T> T parse(
                                 String appPath, String modulePath, String fragmentPath,
                                 String ddText, ContainerAdapter<T> ddAdapter, String ddPath,
                                 String altMessage, String... messages) throws Exception {

        return parse(appPath, modulePath, fragmentPath,
                     ddText, ddAdapter, ddPath,
                     (Class<?>) null, null,
                     altMessage, messages);
    }

    protected static <T> T parse(
                                 String appPath, String modulePath, String fragmentPath,
                                 String ddText, ContainerAdapter<T> ddAdapter, String ddPath,
                                 Class<?> extraAdaptClass, Object extraAdaptValue) throws Exception {

        return parse(
                     appPath, modulePath, fragmentPath,
                     ddText, ddAdapter, ddPath,
                     extraAdaptClass, extraAdaptValue,
                     null);
    }

    protected static <T> T parse(
                                 String appPath, String modulePath, String fragmentPath,
                                 String ddText, ContainerAdapter<T> ddAdapter, String ddPath,
                                 Class<?> extraAdaptClass, Object extraAdaptValue,
                                 String altMessage, String... messages) throws Exception {

        return parse(
                     appPath, modulePath, fragmentPath,
                     ddText, ddAdapter, ddPath,
                     extraAdaptClass, extraAdaptValue,
                     null, null,
                     altMessage, messages);
    }

    protected static <T> T parse(
                                 String appPath, String modulePath, String fragmentPath,
                                 String ddText, ContainerAdapter<T> ddAdapter, String ddPath,
                                 Class<?> extraAdaptClass, Object extraAdaptValue,
                                 Class<?> npCacheKey, Object npCacheValue,
                                 String altMessage, String... messages) throws Exception {

        return parse(appPath, modulePath, fragmentPath,
                     ddText, ddAdapter, ddPath,
                     asDataArray(extraAdaptClass, extraAdaptValue),
                     asDataArray(npCacheKey, npCacheValue),
                     altMessage, messages);
    }

    protected static <T> T parse(
                                 String appPath, String modulePath, String fragmentPath,
                                 String ddText, ContainerAdapter<T> ddAdapter, String ddPath,
                                 ClassKeyedData[] adaptData,
                                 ClassKeyedData[] cacheData,
                                 String altMessage, String... messages) throws Exception {

        OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        ArtifactContainer artifactContainer = mockery.mock(ArtifactContainer.class, "artifactContainer" + mockId++);

        wire(rootOverlay, artifactContainer, null, null, cacheData);

        Container appRoot = ((appPath == null) ? null : mockery.mock(Container.class, "appRoot" + mockId++));

        Entry moduleEntry = ((appPath == null) ? null : mockery.mock(Entry.class, "moduleEntry" + mockId++));
        Container moduleRoot = mockery.mock(Container.class, "moduleRoot" + mockId++);

        Entry fragmentEntry = ((fragmentPath == null) ? null : mockery.mock(Entry.class, "fragmentEntry" + mockId++));
        Container fragmentRoot = ((fragmentPath == null) ? null : mockery.mock(Container.class, "fragmentRoot" + mockId++));

        Entry ddEntry = mockery.mock(Entry.class, "ddEntry" + mockId++);

        wire(appPath, modulePath, fragmentPath, ddPath,
             appRoot, moduleEntry, moduleRoot, fragmentEntry, fragmentRoot, ddEntry,
             ddText,
             adaptData, cacheData);

        Container ddRoot = ((fragmentRoot == null) ? moduleRoot : fragmentRoot);

        try {
            T returnValue = ddAdapter.adapt(ddRoot, rootOverlay, artifactContainer, ddRoot);
            verifySuccess(altMessage, messages);
            return returnValue;

        } catch (UnableToAdaptException e) {
            verifyFailure(getCause(e), altMessage, messages);
            return null;
        }
    }

    // Parse using an entry adapter.

    protected static <T> T parse(
                                 String appPath, String modulePath, String fragmentPath,
                                 String xmlText, EntryAdapter<T> adapter, String ddPath,
                                 String altMessage, String... messages) throws Exception {

        return parse(
                     appPath, modulePath, fragmentPath,
                     xmlText, adapter, ddPath,
                     null, null,
                     null, null,
                     altMessage, messages);
    }

    protected static <T> T parse(
                                 String appPath, String modulePath, String fragmentPath,
                                 String ddText, EntryAdapter<T> ddAdapter, String ddPath,
                                 Class<?> extraAdaptClass, Object extraAdapt,
                                 Class<?> npCacheClass, Object npCache,
                                 String altMessage, String... messages) throws Exception {
        return parse(appPath, modulePath, fragmentPath,
                     ddText, ddAdapter, ddPath,
                     asDataArray(extraAdaptClass, extraAdapt),
                     asDataArray(npCacheClass, npCache),
                     altMessage, messages);
    }

    protected static <T> T parse(
                                 String appPath, String modulePath, String fragmentPath,
                                 String ddText, EntryAdapter<T> ddAdapter, String ddPath,
                                 ClassKeyedData[] adaptData,
                                 ClassKeyedData[] cacheData,
                                 String altMessage, String... messages) throws Exception {

        OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        ArtifactEntry artifactEntry = mockery.mock(ArtifactEntry.class, "artifactEntry" + mockId++);

        wire(rootOverlay, null, artifactEntry, ddPath, cacheData);

        Container appRoot = ((appPath == null) ? null : mockery.mock(Container.class, "appRoot" + mockId++));
        Entry moduleEntry = ((appPath == null) ? null : mockery.mock(Entry.class, "moduleEntry" + mockId++));

        Container moduleRoot = mockery.mock(Container.class, "moduleRoot" + mockId++);

        Entry fragmentEntry = ((fragmentPath == null) ? null : mockery.mock(Entry.class, "fragmentEntry" + mockId++));
        Container fragmentRoot = ((fragmentPath == null) ? null : mockery.mock(Container.class, "fragmentRoot" + mockId++));

        Entry ddEntry = mockery.mock(Entry.class, "ddEntry" + mockId++);

        wire(appPath, modulePath, fragmentPath, ddPath,
             appRoot, moduleEntry, moduleRoot, fragmentEntry, fragmentRoot, ddEntry,
             ddText,
             adaptData, cacheData);

        try {
            T returnValue = ddAdapter.adapt(moduleRoot, rootOverlay, artifactEntry, ddEntry);
            verifySuccess(altMessage, messages);
            return returnValue;

        } catch (UnableToAdaptException e) {
            verifyFailure(getCause(e), altMessage, messages);
            return null;
        }
    }

    protected static Exception getCause(UnableToAdaptException e) {
        Throwable cause = e.getCause();
        return ((cause instanceof Exception) ? (Exception) cause : e);
    }

    protected static void wire(
                               OverlayContainer rootOverlay,
                               ArtifactContainer artifactContainer,
                               ArtifactEntry artifactEntry, String ddPath,
                               ClassKeyedData[] cacheData) {

        mockery.checking(new Expectations() {
            {
                if (cacheData != null) {
                    for (ClassKeyedData nextCacheData : cacheData) {
                        allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(nextCacheData.classKey));
                        will(returnValue(nextCacheData.value));
                    }
                }

                allowing(rootOverlay).addToNonPersistentCache(with(any(String.class)), with(any(Class.class)), with(any(Object.class)));
                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(any(Class.class)));
                will(returnValue(null));

                // An artifact container is provided when parsing
                // using container adapt.  An artifact entry is
                // provided when parsing using entry adapt.

                if (artifactContainer != null) {
                    allowing(artifactContainer).getPath();
                    will(returnValue("/"));
                }
                if (artifactEntry != null) {
                    allowing(artifactEntry).getPath();
                    will(returnValue('/' + ddPath));
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    protected static void wire(
                               String appPath, String modulePath, String fragmentPath, String ddPath,
                               Container appRoot, Entry moduleEntry,
                               Container moduleRoot, Entry fragmentEntry,
                               Container fragmentRoot, Entry ddEntry,
                               String ddText,
                               ClassKeyedData[] adaptData,
                               ClassKeyedData[] cacheData) throws UnsupportedEncodingException, UnableToAdaptException {

        mockery.checking(new Expectations() {
            {
                // Wire up the root-of-roots.
                // If the module root is not the root-of-roots,
                // wire the module root to the app root.

                Container rootOfRoots;
                String rootPath;

                if (appRoot != null) {
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

                if (cacheData != null) {
                    NonPersistentCache npCache = mockery.mock(NonPersistentCache.class, "npCache" + mockId++);

                    allowing(moduleRoot).adapt(NonPersistentCache.class);
                    will(returnValue(npCache));

                    for (ClassKeyedData nextCacheData : cacheData) {
                        allowing(npCache).getFromCache(nextCacheData.classKey);
                        will(returnValue(nextCacheData.value));
                    }
                    // allowing(npCache).getFromCache(with(any(Class.class)));
                    // will(returnValue(null));
                }

                allowing(moduleRoot).getPath();
                will(returnValue("/"));

                // Wire the module and fragment.
                //
                // When there is a fragment, wire it to the
                // module, and set it as the descriptor root.
                //
                // Otherwise, set the module as the descriptor root.

                Container ddRoot;

                if (fragmentEntry != null) {
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
                if (adaptData != null) {
                    for (ClassKeyedData nextAdaptData : adaptData) {
                        allowing(ddRoot).adapt(nextAdaptData.classKey);
                        will(returnValue(nextAdaptData.value));
                    }
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

    public static final String MISSING_DESCRIPTOR_NAMESPACE_ALT_MESSAGE =
            "missing.descriptor.namespace";
    public static final String[] MISSING_DESCRIPTOR_NAMESPACE_ALT_MESSAGES =
            { "CWWKC2264E" };    

    public static final String UNSUPPORTED_DESCRIPTOR_NAMESPACE_ALT_MESSAGE =
            "unsupported.descriptor.namespace";
    public static final String[] UNSUPPORTED_DESCRIPTOR_NAMESPACE_MESSAGES =
            { "CWWKC2262E" };

    public static final String UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE =
            "unsupported.descriptor.version";
    public static final String[] UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES =
            { "CWWKC2261E" };

    public static final String UNPROVISIONED_DESCRIPTOR_VERSION_ALT_MESSAGE =    
            "unprovisioned.descriptor.version";            
    public static final String[] UNPROVISIONED_DESCRIPTOR_VERSION_MESSAGES =
            { "CWWKC2263E" };

    public static final String XML_ERROR_ALT_MESSAGE =
            "xml.error";    
    public static final String[] XML_ERROR_MESSAGES =
            { "CWWKC2272E" };

    public static final String XML_ERROR_ALT_UNEXPECTED_CHILD =
            "unexpected.child.element";
    
    public static final String[] XML_ERROR_UNEXPECTED_CHILD =
            { "CWWKC2259E" };
    
    //

    public static void verifySuccess(String altMessage, String... requiredMessages) {
        if ((requiredMessages != null) && (requiredMessages.length > 0)) {
            Assert.fail("Obtained no exception; expected [ " + altMessage + " ] or all of [ " + Arrays.toString(requiredMessages) + " ]");
        }
    }

    public static void verifyFailure(Exception e, String altMessage, String... requiredMessages) throws Exception {
        if ((requiredMessages == null) || (requiredMessages.length == 0)) {
            System.out.println("Obtained incorrect exception [ " + e.getClass() + " ] [ " + e + " ]");
            throw e;
        }

        String errorMsg = e.getMessage();
        if (errorMsg == null) {
            System.out.println("Obtained exception with null message [ " + e.getClass() + " ] [ " + e + " ]");
            System.out.println("Either [ " + altMessage + " ] or all of [ " + Arrays.toString(requiredMessages) + " ] are required.");
            throw e;
        }

        if (errorMsg.contains(altMessage)) {
            return;
        }

        List<String> missingMessages = null;
        for (String requiredMessage : requiredMessages) {
            if (!errorMsg.contains(requiredMessage)) {
                if (missingMessages == null) {
                    missingMessages = new ArrayList<String>(1);
                }
                missingMessages.add(requiredMessage);
            }
        }
        if (missingMessages != null) {
            System.out.println("Missing exception message [ " + e.getClass() + " ] [ " + e + " ]: " + " [ " + missingMessages + " ]");
            System.out.println("Either [ " + altMessage + " ] or all of [ " + Arrays.toString(requiredMessages) + " ] are required.");
            throw e;
        }
    }
}
