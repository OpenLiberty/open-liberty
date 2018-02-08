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
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jboss.jandex.Index;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.jandex.internal.Jandex_Utils;
import com.ibm.ws.anno.util.internal.UtilImpl_FileUtils;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_MappedJar;
import com.ibm.wsspi.anno.classsource.ClassSource_Options;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts;
import com.ibm.wsspi.anno.classsource.ClassSource_Streamer;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.util.Util_InternMap;

public class ClassSourceImpl_MappedJar
    extends ClassSourceImpl
    implements ClassSource_MappedJar {

    public static final String CLASS_NAME = ClassSourceImpl_MappedJar.class.getName();
    private static final TraceComponent tc = Tr.register(ClassSourceImpl_MappedJar.class);

    // Top O' the world

    @Trivial
    public ClassSourceImpl_MappedJar(
        ClassSourceImpl_Factory factory, Util_InternMap internMap,
        String name, ClassSource_Options options,
        String jarPath) throws ClassSource_Exception {

        super(factory, internMap, name, options, jarPath);

        this.jarPath = jarPath;
    }

    //

    /**
     * <p>Counter to keep track of the number of active users. Incremented for each 'open'
     * and decremented for each 'close'. The underlying ZipFile will be closed when the
     * count goes to 0.</p>
     */
    protected int opens;

    /**
     * <p>Open the ClassSource for processing. If this is the first open, the underlying jar
     * file will be opened.</p>
     * 
     * @throws ClassSource_Exception Thrown if the open failed.
     */
    @Override
    @Trivial
    @FFDCIgnore({ IOException.class })
    public void open() throws ClassSource_Exception {
        String methodName = "open";
        if ( tc.isEntryEnabled() ) {
            String msg = MessageFormat.format(
                "[ {0} ] State [ {1} ]",
                new Object[] { getHashText(), Integer.valueOf(opens) });
            Tr.entry(tc, methodName, msg);
        }

        if ( (opens < 0) ||
             ((opens == 0) && (jarFile != null)) ||
             ((opens > 0) && (jarFile == null)) ) {

            // [ {0} ]: The Jar file [{1}] open counter [{2}] is in an incorrect state for close.
            Tr.warning(tc, "ANNO_CLASSSOURCE_JAR_STATE_BAD",
                getHashText(), getJarPath(), Integer.valueOf(opens));

            String eMsg = "[ " + getHashText() + " ]" +
                " Failed to open [ " + getJarPath() + " ]" +
                " Count of opens [ " + opens + " ]" +
                " Jar state [ " + jarFile + " ]";
            throw getFactory().newClassSourceException(eMsg);
        }

        opens++;

        if ( jarFile == null ) {
            try {
                jarFile = UtilImpl_FileUtils.createJarFile(jarPath); // throws IOException
            } catch ( IOException e ) {
                // do NOT process with FFDC
                String eMsg = "[ " + getHashText() + " ] Failed to open [ " + jarPath + " ]";
                throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, e);
            }

            if ( tc.isEntryEnabled() ) {
                Tr.exit(tc, methodName, MessageFormat.format("[ {0} ] RETURN (new open)", getHashText()));
            }

        } else {
            if ( tc.isEntryEnabled() ) {
                Tr.exit(tc, methodName, MessageFormat.format("[ {0} ] RETURN (already open)", getHashText()));
            }
        }
    }

    /**
     * <p>Close the class source.</p>
     * 
     * <p>If the open counter goes to zero, then close and release the jar file.</p>
     * 
     * @throws ClassSource_Exception Thrown if the close fails.
     */
    @SuppressWarnings("resource")
    @Override
    @Trivial
    @FFDCIgnore({ IOException.class })
    public void close() throws ClassSource_Exception {
        String methodName = "close";
        if ( tc.isEntryEnabled() ) {
            Tr.entry(tc, methodName, getHashText());
        }

        JarFile useJarFile = getJarFile();
        if ( (opens < 0) ||
             ((opens == 0) && (useJarFile != null)) ||
             ((opens > 0) && (useJarFile == null)) ) {

            // [ {0} ]: The Jar file [{1}] open counter [{2}] is in an incorrect state for close.
            Tr.warning(tc, "ANNO_CLASSSOURCE_JAR_STATE_BAD",
               getHashText(), getJarPath(), Integer.valueOf(opens));

            opens = 0;
            useJarFile = clearJarFile();
            if ( useJarFile != null ) {
                try {
                    useJarFile.close(); // throws IOException

                } catch ( IOException e ) {
                    // do NOT process with FFDC

                    // defect 84235:we are generating multiple Warning/Error messages for each error due to each level reporting them.
                    // Disable the following warning and defer message generation to a higher level, 
                    // preferably the ultimate consumer of the exception.
                    //Tr.warning(tc, "ANNO_CLASSSOURCE_CLOSE5_EXCEPTION", getHashText(), getJarPath());

                    String eMsg = "[ " + getHashText() + " ] Failed to close [ " + getJarPath() + " ]";
                    throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, e);
                }
            }

            String eMsg = "[ " + getHashText() + " ] Close with open count [ " + opens + " ]";
            throw getFactory().newClassSourceException(eMsg);
        }

        if ( --opens == 0 ) {
            useJarFile = clearJarFile();

            try {
                useJarFile.close(); // throws IOException

            } catch ( IOException e ) {
                // do NOT process with FFDC

                // defect 84235:we are generating multiple Warning/Error messages for each error due to each level reporting them.
                // Disable the following warning and defer message generation to a higher level, 
                // preferably the ultimate consumer of the exception.
                //Tr.warning(tc, "ANNO_CLASSSOURCE_CLOSE5_EXCEPTION", getHashText(), getJarPath());

                String eMsg = "[ " + getHashText() + " ] Failed to close [ " + getJarPath() + " ]";
                throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, e);
            }
        }

        if ( tc.isEntryEnabled() ) {
            Tr.exit(tc, methodName, getHashText());
        }
    }

    //

    protected final String jarPath;

    @Override
    @Trivial
    public String getJarPath() {
        return jarPath;
    }

    protected JarFile jarFile;

    @Trivial    
    public JarFile getJarFile() {
        return jarFile;
    }

    protected JarFile clearJarFile() {
        JarFile useJarFile = jarFile;
        jarFile = null;
        return useJarFile;
    }

    //

    @Override
    @Trivial
    public int getResult(ClassSource_ScanCounts.ResultField resultField) {
        return scanCounts.getResult(resultField);
    }

    //

    @Override
    protected void processFromScratch(
        ClassSource_Streamer streamer,
        Set<String> i_seedClassNames,
        ScanPolicy scanPolicy) {

        JarFile useJarFile = getJarFile();

        Enumeration<JarEntry> jarEntries = useJarFile.entries();
        while ( jarEntries.hasMoreElements() ) {
            JarEntry nextEntry = jarEntries.nextElement();
            String nextEntryName = nextEntry.getName();

            if ( isDirectoryResource(nextEntryName) ) {
                incrementResourceExclusionCount();

                // Mark all directories as non-root containers;
                // Note that root containers are not currently possible for jar elements.

                markResult(ClassSource_ScanCounts.ResultField.CONTAINER);
                markResult(ClassSource_ScanCounts.ResultField.NON_ROOT_CONTAINER);

            } else {
                if ( !isClassResource(nextEntryName) ) {
                    incrementResourceExclusionCount();

                    markResult(ClassSource_ScanCounts.ResultField.NON_CLASS);

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

                    String nextClassName = getClassNameFromResourceName(nextEntryName);

                    String i_nextClassName = internClassName(nextClassName);
                    boolean didAdd = i_maybeAdd(i_nextClassName, i_seedClassNames);

                    if ( !didAdd ) {
                        incrementClassExclusionCount();

                        markResult(ClassSource_ScanCounts.ResultField.DUPLICATE_CLASS);

                    } else {
                        incrementClassInclusionCount();

                        boolean didProcess;

                        try {
                            didProcess = process(streamer, nextClassName, nextEntryName, scanPolicy); // throws ClassSource_Exception

                        } catch ( ClassSource_Exception e ) {
                            didProcess = false;

                            // autoFFDC will display the stack trace
                            // [ {0} ] The processing of JAR entry [{1}] for class [{2}] caused an exception. The message is: {3}
                            Tr.warning(tc, "ANNO_CLASSSOURCE_JARENTRY_SCAN_EXCEPTION",
                                getHashText(), nextEntryName, nextClassName, e.getMessage());
                        }

                        if ( didProcess ) {
                            markResult(ClassSource_ScanCounts.ResultField.PROCESSED_CLASS);

                        } else {
                            markResult(ClassSource_ScanCounts.ResultField.UNPROCESSED_CLASS);
                        }
                    }

                    markResult(ClassSource_ScanCounts.ResultField.CLASS);
                }

                markResult(ClassSource_ScanCounts.ResultField.NON_CONTAINER);
            }

            // Mark in the totals count for every entry.
            markResult(ClassSource_ScanCounts.ResultField.ENTRY);
        }
    }

    //

    protected boolean process(
        ClassSource_Streamer streamer,
        String className, String resourceName,
        ScanPolicy scanPolicy) throws ClassSource_Exception {

        if ( streamer == null ) {
            return true;
        } else if ( !streamer.doProcess(className, scanPolicy) ) {
            return false;
        }

        InputStream inputStream = openResourceStream(className, resourceName); // throws ClassSource_Exception
        try {
            streamer.process(getCanonicalName(), className, inputStream, scanPolicy);
            // 'process' throws ClassSourceException
        } finally {
            closeResourceStream(className, resourceName, inputStream); // throws ClassSource_Exception
        }

        return true;
    }

    //

    @Override
    protected Index getJandexIndex() {
        String useJandexIndexPath = getJandexIndexPath();

        InputStream jandexStream;

        try {
            jandexStream = openResourceStream(null, useJandexIndexPath);
            // throws ClassSource_Exception

        } catch ( ClassSource_Exception e ) {
            // autoFFDC will display the stack trace
            // [ {0} ] Open of Jandex index resource [{1}] caused an exception.  The message is: {2}.
            Tr.warning(tc, "ANNO_CLASSSOURCE_ENTRY_JANDEX_OPEN_EXCEPTION",
                getHashText(), useJandexIndexPath, e.getMessage());

            return null;
        }

        if ( jandexStream == null ) {
            return null;
        }

        try {
            Index jandexIndex = Jandex_Utils.basicReadIndex(jandexStream); // throws IOException

            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] Read JANDEX index [ {1} ] from [ {2} ] Classes  [ {3} ]", 
                         new Object[] { getHashText(),
                                        useJandexIndexPath,
                                        getCanonicalName(),
                                        Integer.toString(jandexIndex.getKnownClasses().size()) } ));
            }
            return jandexIndex;

        } catch ( IOException e ) {
            // autoFFDC will display the stack trace
            // [ {0} ] Read of Jandex index resource [{1}] failed with an exception.  The message is: {2}
            Tr.warning(tc, "ANNO_CLASSSOURCE_ENTRY_JANDEX_OPEN_EXCEPTION",
                getHashText(), useJandexIndexPath, e.getMessage());

            return null;
        }
    }

    //

    @Override
    @FFDCIgnore({ IOException.class })
    public InputStream openResourceStream(String className, String resourceName)
        throws ClassSource_Exception {

        String methodName = "openResourceStream";

        JarFile useJarFile = getJarFile();

        JarEntry jarEntry = useJarFile.getJarEntry(resourceName);
        if ( jarEntry == null ) {
            return null;
        }

        InputStream inputStream;

        try {
            inputStream = useJarFile.getInputStream(jarEntry); // throws IOException

        } catch ( IOException e ) {
            // do NOT process with FFDC

            // defect 84235:we are generating multiple Warning/Error messages for each error due to each level reporting them.
            // Disable the following warning and defer message generation to a higher level, 
            // preferably the ultimate consumer of the exception.
            //Tr.warning(tc, "ANNO_CLASSSOURCE_OPEN5_EXCEPTION", getHashText(), resourceName, className, getJarPath());
            String eMsg =
                "[ " + getHashText() + " ]" +
                " Failed to open [ " + resourceName + " ]" +
                " in [ " + getJarPath() + " ]";
            if ( className != null ) {
                eMsg += " for class [ " + className + " ]";
            }
            throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, e);
        }

        return inputStream;
    }

    @Override
    public void closeResourceStream(String className, String resourceName, InputStream inputStream) {
        try {
            inputStream.close(); // throws IOException

        } catch ( IOException e ) {
            // autoFFDC will display the stack trace
            // [ {0} ]: The close of JAR entry [{1}] for class [{2}] in [{3}] failed with an exception. The message is {4}
            Tr.warning(tc, "ANNO_CLASSSOURCE_JARENTRY_CLOSE_EXCEPTION",
                getHashText(), resourceName, className, getJarPath(), e.getMessage());
        }
    }

    //

    @Override
    @Trivial
    public void log(TraceComponent logger) {
        Tr.debug(logger, MessageFormat.format("Class Source [ {0} ]", getHashText()));
        logCounts(logger);
    }
}
