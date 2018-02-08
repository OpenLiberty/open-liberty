/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
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

import org.jboss.jandex.Index;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.jandex.internal.Jandex_Utils;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.FastModeControl;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_MappedContainer;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts;
import com.ibm.wsspi.anno.classsource.ClassSource_Streamer;
import com.ibm.wsspi.anno.util.Util_InternMap;

public class ClassSourceImpl_MappedContainer
    extends ClassSourceImpl
    implements ClassSource_MappedContainer {

    @SuppressWarnings("hiding")
    public static final String CLASS_NAME = ClassSourceImpl_MappedContainer.class.getName();
    private static final TraceComponent tc = Tr.register(ClassSourceImpl_MappedContainer.class);

    // Top O' the world

    @SuppressWarnings("unused")
    @Trivial
    public ClassSourceImpl_MappedContainer(
        ClassSourceImpl_Factory factory, Util_InternMap internMap,
        String name, Container container) throws ClassSource_Exception {

        super(factory, internMap, name, String.valueOf(container));

        this.container = container;
    }

    //

    /**
     * <p>Open this class source.</p>
     * 
     * @throws ClassSource_Excewption Thrown if the open failed.
     */
    @Override
    @Trivial
    @FFDCIgnore({ UnableToAdaptException.class })
    public void open() throws ClassSource_Exception {
        String methodName = "open";

        // Force the container to stay open.  That means that iteration
        // across the container elements will open the container's JAR
        // just once.  The JAR will be kept open for iteration, and will
        // be closed after the iteration is complete.

        try {
            FastModeControl fastMode = getContainer().adapt(FastModeControl.class);
            // 'adapt' throws UnableToAdaptException
            fastMode.useFastMode();

        } catch ( UnableToAdaptException e ) {
            // do NOT process with FFDC
            String eMsg =
                "[ " + getHashText() + " ]" +
                " Failed to adapt [ " + getCanonicalName() + " ]" +
                " to [ " + FastModeControl.class.getName() + " ]";
            throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, e);
        }

        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] ENTER/RETURN", getHashText()));
        }
    }

    /**
     * <p>Close this class source.</p>
     * 
     * @throws ClassSource_Excewption Thrown if the close failed.
     */
    @Override
    @Trivial
    public void close() throws ClassSource_Exception {
        try {
            FastModeControl fastMode = getContainer().adapt(FastModeControl.class);
            // 'adapt' throws UnableToAdaptException
            fastMode.stopUsingFastMode();

        } catch ( UnableToAdaptException e ) {
            // autoFFDC will display the stack trace
            // [ {0} ]: The container of this class source failed to convert to [{1}]. The message is {2}
            Tr.warning(tc, "ANNO_CLASSSOURCE_MODE_ADAPT_EXCEPTION",
                getHashText(), FastModeControl.class.getName(), e.getMessage());
        }

        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] ENTER/RETURN", getHashText()));
        }
    }

    //

    protected final Container container;

    @Override
    @Trivial
    public Container getContainer() {
        return container;
    }

    //

    @Override
    protected void processFromScratch(
        ClassSource_Streamer streamer,
        Set<String> i_seedClassNames,
        ScanPolicy scanPolicy) {

        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            startTimings();
        }

        processClasses(
            getContainer(), EMPTY_PREFIX,
            streamer,
            i_seedClassNames,
            getScanResults(),
            scanPolicy);

        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            endTimings();
        }
    }

    //

    protected long startTime;
    protected long endTime;

    protected long streamTime;
    protected long jandexTime;

    protected void startTimings() {
        startTime = getTime();
        streamTime = 0L;
        jandexTime = 0L;
    }

    protected void addStreamTime(long additionalTime) {
        streamTime += additionalTime;
    }

    protected void addJandexTime(long additionalTime) {
        jandexTime += additionalTime;
    }

    @Trivial
    protected void endTimings() {
        endTime = getTime();

        Tr.debug(tc, MessageFormat.format("Start time:  [ {0} ]", Long.valueOf(startTime)));
        Tr.debug(tc, MessageFormat.format("End time:    [ {0} ]", Long.valueOf(endTime)));
        Tr.debug(tc, MessageFormat.format("Delta time:  [ {0} ]", Long.valueOf(endTime - startTime)));

        Tr.debug(tc, MessageFormat.format("Stream time: [ {0} ]", Long.valueOf(streamTime)));
        Tr.debug(tc, MessageFormat.format("Jandex time: [ {0} ]", Long.valueOf(jandexTime)));
    }

    //

    public static final String EMPTY_PREFIX = "";

    @Trivial
    protected void processClasses(
        Container targetContainer, String prefix,
        ClassSource_Streamer streamer,
        Set<String> i_seedClassNames,
        ClassSourceImpl_ScanCounts localScanCounts,
        ScanPolicy scanPolicy) {

        String methodName = "processClasses";
        if ( tc.isEntryEnabled() ) {
            String msg = MessageFormat.format(
                "[ {0} ] ENTER [ {1} ] of [ {2} ]",
                new Object[] { getHashText(), prefix, targetContainer.getName() });
            Tr.entry(tc, methodName, msg);
        }

        int initialResources = i_seedClassNames.size();

        for ( Entry nextEntry : targetContainer ) {
            String nextChildName = nextEntry.getName();
            String nextPrefix = resourceAppend(prefix, nextChildName);

            Container nextChildContainer;
            try {
                nextChildContainer = nextEntry.adapt(Container.class);

            } catch ( Throwable th ) {
                nextChildContainer = null;

                // autoFFDC will display the stack trace
                // [ {0} ]: The conversion of [{1}] as [{2}] under root [{3}] for prefix [{4}] failed. The message is {5}
                Tr.warning(tc, "ANNO_CLASSSOURCE_ENTRY_ADAPT_EXCEPTION",
                    getHashText(), nextChildName, nextEntry, targetContainer, prefix, th.getMessage());
            }

            if ( nextChildContainer != null ) {
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

                if ( nextChildContainer.isRoot() ) {
                    incrementResourceExclusionCount();

                    localScanCounts.increment(ClassSource_ScanCounts.ResultField.ROOT_CONTAINER);

                } else {
                    ClassSourceImpl_ScanCounts childScanCounts = new ClassSourceImpl_ScanCounts();

                    processClasses(nextChildContainer, nextPrefix,
                                streamer,
                                i_seedClassNames,
                                childScanCounts,
                                scanPolicy);
                    // throws ClassSource_Exception

                    localScanCounts.addResults(childScanCounts);

                    localScanCounts.increment(ClassSource_ScanCounts.ResultField.ROOT_CONTAINER);
                }

            } else {
                if ( !isClassResource(nextPrefix) ) {
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

                    if ( !didAdd ) {
                        incrementClassExclusionCount();

                        localScanCounts.increment(ClassSource_ScanCounts.ResultField.DUPLICATE_CLASS);

                    } else {
                        incrementClassInclusionCount();

                        boolean didProcess;

                        try {
                            didProcess = process(streamer, nextClassName, nextPrefix, nextEntry, scanPolicy);

                        } catch ( ClassSource_Exception e ) {
                            didProcess = false;

                            // autoFFDC will display the stack trace
                            // "[ {0} ] The processing of entry [ {1} ] for class [{1}] caused an exception. The message is: {2}"
                            Tr.warning(tc, "ANNO_CLASSSOURCE_ENTRY_SCAN_EXCEPTION",
                                nextEntry.getName(), nextClassName, getHashText(), e);
                        }

                        if ( didProcess ) {
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

        if ( tc.isDebugEnabled() ) {
            Object[] logParms = new Object[] { getHashText(), null, null };

            logParms[1] = Integer.valueOf(finalResources - initialResources);
            Tr.debug(tc, MessageFormat.format("[ {0} ] RETURN [ {1} ] Added classes", logParms));

            for ( ClassSource_ScanCounts.ResultField resultField : ClassSource_ScanCounts.ResultField.values() ) {
                int nextResult = localScanCounts.getResult(resultField);
                String nextResultTag = resultField.getTag();

                logParms[1] = Integer.valueOf(nextResult);
                logParms[2] = nextResultTag;

                Tr.debug(tc, MessageFormat.format("[ {0} ]  [ {1} ] {0}", logParms));
            }
        }

        if ( tc.isEntryEnabled() ) {
            Tr.exit(tc, methodName, getHashText());
        }
    }

    protected boolean process(
        ClassSource_Streamer streamer,
        String className, String resourceName, Entry entry, ScanPolicy scanPolicy)
        throws ClassSource_Exception {

        if ( streamer == null ) {
            return true;
        } else if ( !streamer.doProcess(className, scanPolicy) ) {
            return false;
        }

        InputStream inputStream = openResourceStream(className, resourceName, entry);
        // throws ClassSource_Exception
        try {
            streamer.process( getCanonicalName(), className, inputStream, scanPolicy );
            // 'process' throws ClassSourceException
        } finally {
            closeResourceStream(className, resourceName, entry, inputStream);
        }

        return true;
    }

    //

    /**
     * <p>Answer the path to JANDEX index files.</p>
     *
     * <p>The default implementation answers <code>"META-INF/jandex.ndx"</code>.
     * This implementation accounts for the possibility that the target container
     * might be "/WEB-INF/classes", in which case the path is adjusted
     * to "../../META_INF/jandex.ndx".</p>
     *
     * @return The relative path to JANDEX index files.
     */
    @Trivial
    @Override
    public String getJandexIndexPath() {
        String jandexIndexPath = super.getJandexIndexPath();

        Container useContainer = getContainer();
        if ( !useContainer.isRoot() && useContainer.getPath().equals("/WEB-INF/classes") ) {
            jandexIndexPath = "../../" + jandexIndexPath;
        }

        return jandexIndexPath;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Index getJandexIndex() {
        String useJandexIndexPath = getJandexIndexPath();

        System.out.println("Looking for JANDEX [ " + useJandexIndexPath + " ]" +
                           " in [ " + getContainer().getPhysicalPath() + " ]");

        InputStream jandexStream;

        try {
            jandexStream = openResourceStream(null, useJandexIndexPath); // throws ClassSource_Exception
        } catch ( ClassSource_Exception e ) {
            // autoFFDC will display the stack trace
            // [ {0} ] Open of Jandex index resource [{1}] caused an exception.  The message is: {2}.
            Tr.warning(tc, "ANNO_CLASSSOURCE_ENTRY_JANDEX_OPEN_EXCEPTION",
               getHashText(), useJandexIndexPath, e.getMessage());
            return null;
        }

        if ( jandexStream == null ) {
            System.out.println("No JANDEX index was found");
            return null;
        }

        System.out.println("Located JANDEX index");

        long startJandexTime = getTime();

        try {
            Index jandexIndex = Jandex_Utils.basicReadIndex(jandexStream); // throws IOException
            System.out.println(
                "Read JANDEX index [ " + useJandexIndexPath + " ] from [ " + getCanonicalName() + " ]:" +
                " Classes [ " + Integer.toString(jandexIndex.getKnownClasses().size()) + " ]");
            return jandexIndex;

        } catch ( Exception e ) {
            // autoFFDC will display the stack trace
            // [ {0} ] Read of Jandex index resource [{1}] failed with an exception.  The message is: {2}.
            Tr.warning(tc, "ANNO_CLASSSOURCE_ENTRY_JANDEX_READ_EXCEPTION",
               getHashText(), useJandexIndexPath, e.getMessage());

            return null;

        } finally {
            closeResourceStream(null,  useJandexIndexPath, jandexStream);

            long endJandexTime = getTime();
            addJandexTime(endJandexTime - startJandexTime);
        }
    }

    //

    @Override
    public InputStream openResourceStream(String className, String resourceName)
        throws ClassSource_Exception {

        Entry entry = getContainer().getEntry(resourceName);
        if ( entry == null ) {
            return null;
        }

        return openResourceStream(className, resourceName, entry);
        // throws ClassSource_Exception
    }

    @FFDCIgnore({ Throwable.class })
    public InputStream openResourceStream(
        String className, String resourceName, Entry entry)
        throws ClassSource_Exception {

        String methodName = "openResourceStream";

        InputStream result;

        try {
            long initialTime = 0;
            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                initialTime = getTime();
            }

            result = entry.adapt(InputStream.class);

            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                long finalTime = getTime();
                addStreamTime(finalTime - initialTime);
            }

        } catch ( Throwable th ) {
            // do NOT process with FFDC

            // defect 84235:we are generating multiple Warning/Error messages for each error due to each level reporting them.
            // Disable the following warning and defer message generation to a higher level, 
            // preferably the ultimate consumer of the exception.
            // Tr.warning(tc, "ANNO_CLASSSOURCE_OPEN2_EXCEPTION",
            //           getHashText(), resourceName, entry, getContainer(), className);

            String eMsg = "[ " + getHashText() + " ]" +
                          " Failed to open [ " + resourceName + " ]" +
                          " as [ " + entry + " ]" +
                          " under root [ " + getContainer() + " ]";
            if ( className != null ) {
                eMsg += " for class [ " + className + " ]";
            }
            throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, th);
        }

        if ( result == null ) {
            // defect 84235:we are generating multiple Warning/Error messages for each error due to each level reporting them.
            // Disable the following warning and defer message generation to a higher level, 
            // preferably the ultimate consumer of the exception.
            // Tr.warning(tc, "ANNO_CLASSSOURCE_OPEN2_EXCEPTION",
            //           getHashText(), resourceName, entry, getContainer(), className);

            String eMsg = "[ " + getHashText() + " ]" +
                          " Failed to open [ " + resourceName + " ]" +
                          " as [ " + entry + " ]" +
                          " under root [ " + getContainer() + " ]";
            if ( className != null ) {
                eMsg += " for class [ " + className + " ]";
            }
            throw getFactory().newClassSourceException(eMsg);
        }

        return result;
    }

    @Override
    public void closeResourceStream(String className, String resourceName, InputStream inputStream) {
        Entry entry = getContainer().getEntry(resourceName);
        if ( entry == null ) {
            // [ {0} ]: The entry [{1}] could not be located under root [{2}] for class [{3}].
            Tr.warning(tc, "ANNO_CLASSSOURCE_RESOURCE_NOTFOUND",
                getHashText(), resourceName, getContainer(), className);
        } else {
            closeResourceStream(className, resourceName, entry, inputStream);
        }
    }

    protected void closeResourceStream(
        String className,
        String resourceName,
        Entry entry,
        InputStream inputStream) {

        try {
            inputStream.close(); // throws IOException
        } catch ( IOException e ) {
            // autoFFDC will display the stack trace
            // [ {0} ]: The close of resource [{1}] for class [{2}] failed with an exception. The message is {3}
            Tr.warning(tc, "ANNO_CLASSSOURCE_RESOURCE_CLOSE_EXCEPTION",
                getHashText(), resourceName, className, e.getMessage());
        }
    }

    //

    @Override
    @Trivial
    public void log(TraceComponent logger) {
        Tr.debug(logger, MessageFormat.format("Class Source [ {0} ]", getHashText()));
    }
}
