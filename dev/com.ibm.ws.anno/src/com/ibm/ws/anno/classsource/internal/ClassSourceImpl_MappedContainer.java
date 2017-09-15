/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.classsource.internal;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.FastModeControl;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.classsource.ClassSource;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_MappedContainer;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts;
import com.ibm.wsspi.anno.classsource.ClassSource_Streamer;
import com.ibm.wsspi.anno.util.Util_InternMap;

public class ClassSourceImpl_MappedContainer
                extends ClassSourceImpl
                implements ClassSource, ClassSource_MappedContainer {

    public static final String CLASS_NAME = ClassSourceImpl_MappedContainer.class.getName();
    private static final TraceComponent tc = Tr.register(ClassSourceImpl_MappedContainer.class);

    // Top O' the world

    public ClassSourceImpl_MappedContainer(ClassSourceImpl_Factory factory, Util_InternMap internMap,
                                           String name, Container container)
        throws ClassSource_Exception {

        super(factory, internMap, name, String.valueOf(container));

        this.container = container;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.hashText);
        }
    }

    //

    /*
     * Open the ClassSource for processing.
     */
    @Override
    public void open() throws ClassSource_Exception {
        String methodName = "open";

        // enable FastMode on the container. this will keep any underlying jars open for additional
        // processing to prevent the performance degradation from continuous open/close operations.

        try {
            FastModeControl fastMode = getContainer().adapt(FastModeControl.class);
            fastMode.useFastMode();
        } catch (UnableToAdaptException e) {
            String eMsg = "[ " + getHashText() + " ]" +
                          " Failed to adapt [ " + container + " ]" +
                          " to FastModeControl";
            throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, e);
            // Throw this ... let callers know not to use this class source.
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] ENTER/RETURN", getHashText()));
        }
    }

    /*
     * Close the ClassSource for processing.
     * 
     * Disable FastMode for the container and return.
     */
    @Override
    @Trivial
    public void close() throws ClassSource_Exception {

        // tell the container to stop FastMode

        try {
            FastModeControl fastMode = getContainer().adapt(FastModeControl.class);
            fastMode.stopUsingFastMode();
        } catch (UnableToAdaptException e) {
            // String eMsg = "[ " + getHashText() + " ]" +
            //               " Failed to adapt [ " + container + " ]" +
            //               " to [ " + FastModeControl.class.getName() + " ]";
            // CWWKC0001W: [ {0} ]: The close of source [{1}] [{2}] failed with an exception
            Tr.warning(tc, "ANNO_CLASSSOURCE_CLOSE1_EXCEPTION", getHashText(), getHashText(), container);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] ENTER/RETURN", getHashText()));
        }
    }

    //

    protected final Container container;

    @Override
    public Container getContainer() {
        return container;
    }

    //

    @Override
    public void scanClasses(ClassSource_Streamer streamer, Set<String> i_seedClassNames, ScanPolicy scanPolicy) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            startTimings();
        }

        scanClasses(getContainer(), EMPTY_PREFIX, streamer, i_seedClassNames, getScanResults(), scanPolicy);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            endTimings();
        }
    }

    protected long startTime;
    protected long endTime;

    protected long streamTime;

    protected void startTimings() {
        startTime = getTime();

        streamTime = 0L;
    }

    protected void addStreamTime(long additionalTime) {
        streamTime += additionalTime;
    }

    protected void endTimings() {

        endTime = getTime();

        // the caller is responsible for calling isDebugEnabled.
        Tr.debug(tc, MessageFormat.format("Start time:  [ {0} ]", Long.valueOf(startTime)));
        Tr.debug(tc, MessageFormat.format("End time:    [ {0} ]", Long.valueOf(endTime)));
        Tr.debug(tc, MessageFormat.format("Delta time:  [ {0} ]", Long.valueOf(endTime - startTime)));
        Tr.debug(tc, MessageFormat.format("Stream time: [ {0} ]", Long.valueOf(streamTime)));

        // System.out.println("Start time:  [ " + startTime + " ]");
        // System.out.println("End time:    [ " + endTime + " ]");
        //
        // System.out.println("Delta time:  [ " + (endTime - startTime)+ " ]");
        // System.out.println("Stream time: [ " + streamTime + " ]");
    }

    protected long getTime() {
        return System.currentTimeMillis();
    }

    public static final String EMPTY_PREFIX = "";

    protected void scanClasses(Container targetContainer, String prefix,
                               ClassSource_Streamer streamer,
                               Set<String> i_seedClassNames,
                               ClassSourceImpl_ScanCounts localScanCounts,
                               ScanPolicy scanPolicy) {

        String methodName = "scanClasses";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, MessageFormat.format("[ {0} ] ENTER [ {1} ] of [ {2} ]",
                                                          new Object[] { getHashText(), prefix, targetContainer.getName() }));
        }

        int initialResources = i_seedClassNames.size();

        for (Entry nextEntry : targetContainer) {
            String nextChildName = nextEntry.getName();
            String nextPrefix = resourceAppend(prefix, nextChildName);

            Container nextChildContainer;
            try {
                nextChildContainer = nextEntry.adapt(Container.class);

            } catch (Throwable th) {
                nextChildContainer = null;

                // String eMsg = "[ " + getHashText() + " ]" +
                //               " Failed to adapt [ " + nextChildName + " ]" +
                //               " as [ " + nextEntry + " ]" +
                //               " under root [ " + targetContainer + " ]" +
                //               " for prefix [ " + prefix + " ]";
                Tr.warning(tc, "ANNO_CLASSSOURCE_ADAPT_EXCEPTION",
                           getHashText(), nextChildName, nextEntry, targetContainer, prefix);
            }

            if (nextChildContainer != null) {
                // TODO: Is this exclusion correct??
                //
                // There is a difference of handling truly new structural units
                // (moving from an EAR to a child WAR, or from a WAR to a child JAR,
                // and of handling child elements which are simply packaged in a new
                // root (say, if a JAR was mapped to a directory, with child folders of the
                // JAR mapped to child jars).
                //
                // That is:
                //
                // aWAR.asADir
                //   WEB-INF/lib (as aDir)
                //     aJar.asAJar
                //
                // Compared with:
                //
                // aJar.asADir
                //   aSubFolder.asADir
                //     aSubJar.asAJar

                if (nextChildContainer.isRoot()) {
                    incrementResourceExclusionCount();

                    localScanCounts.increment(ClassSource_ScanCounts.ResultField.ROOT_CONTAINER);

                } else {
                    ClassSourceImpl_ScanCounts childScanCounts = new ClassSourceImpl_ScanCounts();

                    scanClasses(nextChildContainer, nextPrefix,
                                streamer,
                                i_seedClassNames,
                                childScanCounts,
                                scanPolicy);
                    // throws ClassSource_Exception

                    localScanCounts.addResults(childScanCounts);

                    localScanCounts.increment(ClassSource_ScanCounts.ResultField.ROOT_CONTAINER);
                }

            } else {
                if (!isClassResource(nextPrefix)) {
                    incrementResourceExclusionCount();

                    localScanCounts.increment(ClassSource_ScanCounts.ResultField.NON_CLASS);

                } else {
                    // Processing notes:
                    //
                    // Make sure to record the class before attempting processing.
                    //
                    // Only one version of the class is to be processed, even if processing
                    // fails on that one version.
                    //
                    // That is, if two child class sources have versions of a class, and
                    // the version from the first class source is non-valid, the version
                    // of the class in the second class source is still masked by the
                    // version in the first class source.

                    String nextClassName = getClassNameFromResourceName(nextPrefix);
                    String i_nextClassName = internClassName(nextClassName);

                    boolean didAdd = i_maybeAdd(i_nextClassName, i_seedClassNames);

                    if (!didAdd) {
                        incrementClassExclusionCount();

                        localScanCounts.increment(ClassSource_ScanCounts.ResultField.DUPLICATE_CLASS);

                    } else {
                        incrementClassInclusionCount();

                        boolean didProcess;

                        try {
                            didProcess = process(streamer, nextClassName, nextPrefix, nextEntry, scanPolicy);
                        } catch (ClassSource_Exception e) {
                            didProcess = false;

                            // TODO: NEW_MESSAGE: Need a new message here.

                            // String eMsg = "[ " + getHashText() + " ]" +
                            //               " Failed to process class [ " + nextClassName + " ]" +
                            //               " under root [ " + getContainer() + " ]";
                            // CWWKC0044W: An exception occurred while scanning class and annotation data.
                            Tr.warning(tc, "ANNO_TARGETS_SCAN_EXCEPTION", e);
                        }

                        if (didProcess) {
                            localScanCounts.increment(ClassSource_ScanCounts.ResultField.PROCESSED_CLASS);

                        } else {
                            localScanCounts.increment(ClassSource_ScanCounts.ResultField.UNPROCESSED_CLASS);
                        }
                    }

                    localScanCounts.increment(ClassSource_ScanCounts.ResultField.CLASS);
                }

                localScanCounts.increment(ClassSource_ScanCounts.ResultField.NON_CONTAINER);
            }

            localScanCounts.increment(ClassSource_ScanCounts.ResultField.ENTRY);
        }

        int finalResources = i_seedClassNames.size();

        if (tc.isDebugEnabled()) {
            Object[] logParms = new Object[] { getHashText(), null, null };

            logParms[1] = Integer.valueOf(finalResources - initialResources);
            Tr.debug(tc, MessageFormat.format("[ {0} ] RETURN [ {1} ] Added classes", logParms));

            for (ClassSource_ScanCounts.ResultField resultField : ClassSource_ScanCounts.ResultField.values()) {
                int nextResult = localScanCounts.getResult(resultField);
                String nextResultTag = resultField.getTag();

                logParms[1] = Integer.valueOf(nextResult);
                logParms[2] = nextResultTag;

                Tr.debug(tc, MessageFormat.format("[ {0} ]  [ {1} ] {0}", logParms));
            }
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, getHashText());
        }
    }

    protected boolean process(ClassSource_Streamer streamer, String className, String resourceName, Entry entry, ScanPolicy scanPolicy)
                    throws ClassSource_Exception {

        if (streamer == null) {
            return true;
        }

        if (!streamer.doProcess(className, scanPolicy)) {
            return false;
        }

        InputStream inputStream = openResourceStream(className, resourceName, entry); // throws ClassSource_Exception

        try {
            streamer.process(getCanonicalName(), className, inputStream, scanPolicy);
            // 'process' throws ClassSourceException
        } finally {
            closeResourceStream(className, resourceName, entry, inputStream);
        }

        return true;
    }

    //

    @Override
    public InputStream openResourceStream(String className, String resourceName)
                    throws ClassSource_Exception {

        // String methodName = "openResourceStream";

        Entry entry = getContainer().getEntry(resourceName);
        if (entry == null) {
            return null;
        }

        return openResourceStream(className, resourceName, entry); // throws ClassSource_Exception
    }

    public InputStream openResourceStream(String className, String resourceName, Entry entry)
                    throws ClassSource_Exception {

        String methodName = "openResourceStream";

        InputStream result;

        try {
            long initialTime = 0;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                initialTime = getTime();
            }

            result = entry.adapt(InputStream.class);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                long finalTime = getTime();
                addStreamTime(finalTime - initialTime);
            }
        } catch (Throwable th) {
            // defect 84235:we are generating multiple Warning/Error messages for each error due to each level reporting them.
            // Disable the following warning and defer message generation to a higher level, 
            // preferably the ultimate consumer of the exception.
            //Tr.warning(tc, "ANNO_CLASSSOURCE_OPEN2_EXCEPTION",
            //           getHashText(), resourceName, entry, getContainer(), className);

            String eMsg = "[ " + getHashText() + " ]" +
                          " Failed to open [ " + resourceName + " ]" +
                          " as [ " + entry + " ]" +
                          " under root [ " + getContainer() + " ]" +
                          " for class [ " + className + " ]";
            throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, th);
        }

        if (result == null) {
            // defect 84235:we are generating multiple Warning/Error messages for each error due to each level reporting them.
            // Disable the following warning and defer message generation to a higher level, 
            // preferably the ultimate consumer of the exception.
            //Tr.warning(tc, "ANNO_CLASSSOURCE_OPEN2_EXCEPTION",
            //           getHashText(), resourceName, entry, getContainer(), className);

            String eMsg = "[ " + getHashText() + " ]" +
                          " Failed to open [ " + resourceName + " ]" +
                          " as [ " + entry + " ]" +
                          " under root [ " + getContainer() + " ]" +
                          " for class [ " + className + " ]";
            throw getFactory().newClassSourceException(eMsg);
        }

        return result;
    }

    @Override
    public void closeResourceStream(String className, String resourceName, InputStream inputStream) {
        Entry entry = getContainer().getEntry(resourceName);
        if (entry == null) {
            // String eMsg = "[ " + getHashText() + " ]" +
            //               " Failed to locate entry [ " + resourceName + " ]" +
            //               " under root [ " + getContainer() + " ]" +
            //               " for class [ " + className + " ]";
            Tr.warning(tc, "ANNO_CLASSSOURCE_RESOURCE_NOTFOUND",
                       getHashText(), resourceName, getContainer(), className);

        } else {
            closeResourceStream(className, resourceName, entry, inputStream);
        }
    }

    protected void closeResourceStream(String className, String resourceName, Entry entry, InputStream inputStream) {
        try {
            inputStream.close(); // throws IOException

        } catch (IOException e) {
            // String eMsg = "[ " + getHashText() + " ]" +
            //               " Failed to close [ " + resourceName + " ]" +
            //               " as [ " + entry + " ]" +
            //               " under root [ " + getContainer() + " ]" +
            //               " for class [ " + className + " ]";
            Tr.warning(tc, "ANNO_CLASSSOURCE_CLOSE3_EXCEPTION",
                       getHashText(), resourceName, entry, getContainer(), className);
        }
    }

    //

    @Override
    @Trivial
    public void log(TraceComponent logger) {
        Tr.debug(logger, MessageFormat.format("Class Source [ {0} ]", getHashText()));
    }
}
