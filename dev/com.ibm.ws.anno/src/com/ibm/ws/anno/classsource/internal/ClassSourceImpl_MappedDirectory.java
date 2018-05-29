/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.classsource.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Set;

import org.jboss.jandex.Index;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.jandex.internal.Jandex_Utils;
import com.ibm.ws.anno.util.internal.UtilImpl_FileUtils;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_MappedDirectory;
import com.ibm.wsspi.anno.classsource.ClassSource_Options;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts;
import com.ibm.wsspi.anno.classsource.ClassSource_Streamer;
import com.ibm.wsspi.anno.util.Util_InternMap;

public class ClassSourceImpl_MappedDirectory
    extends ClassSourceImpl
    implements ClassSource_MappedDirectory {

    public static final String CLASS_NAME = ClassSourceImpl_MappedDirectory.class.getName();
    private static final TraceComponent tc = Tr.register(ClassSourceImpl_MappedDirectory.class);

    // Top O' the world

    @Trivial
    public ClassSourceImpl_MappedDirectory(
        ClassSourceImpl_Factory factory, Util_InternMap internMap,
        String name, ClassSource_Options options,
        String dirPath) throws ClassSource_Exception {

        super(factory, internMap, name, options, dirPath);

        this.dirPath = dirPath; // TODO: verify the path?
    }

    //

    /**
     * <p>Open this class source.  This implementation does nothing.</p>
     * 
     * @throws ClassSource_Exception Thrown if the open failed.
     */
    @Override
    @Trivial
    public void open() throws ClassSource_Exception {
        String methodName = "open";
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] [{1}] ENTER/RETURN", getHashText(), methodName));
        }
    }

    /*
     * <p>Close this class source.  This implementation does nothing.</p>
     * 
     * @throws ClassSource_Exception Thrown if the close failed.
     */
    @Override
    @Trivial
    public void close() throws ClassSource_Exception {
        String methodName = "close";
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] [{1}] ENTER/RETURN", getHashText(), methodName));
        }
    }

    //

    protected static final char FILE_SEPARATOR_CHAR = File.separatorChar;
    protected static final boolean CONVERT_SEPARATORS = (FILE_SEPARATOR_CHAR == '\\');

    @Trivial
    protected String pathAppend(String prefix, String tail) {
        if ( prefix.isEmpty() ) {
            return tail;
        } else {
            return (prefix + FILE_SEPARATOR_CHAR + tail);
        }
    }

    @Override
    @Trivial
    public boolean getConvertResourceNames() {
        return CONVERT_SEPARATORS;
    }

    @Override
    public String inconvertResourceName(String externalResourceName) {
        if ( CONVERT_SEPARATORS ) {
            return externalResourceName.replace(FILE_SEPARATOR_CHAR, RESOURCE_SEPARATOR_CHAR);
        } else {
            return externalResourceName;
        }
    }

    @Override
    public String outconvertResourceName(String internalResourceName) {
        if ( CONVERT_SEPARATORS ) {
            return internalResourceName.replace(RESOURCE_SEPARATOR_CHAR, FILE_SEPARATOR_CHAR);
        } else {
            return internalResourceName;
        }
    }

    //

    protected final String dirPath;

    @Override
    @Trivial
    public String getDirPath() {
        return dirPath;
    }

    public String getFilePath(String resourcePath) {
        return pathAppend( getDirPath(), resourcePath );
    }

    //

    @Override
    @Trivial
    protected void processFromScratch(
        ClassSource_Streamer streamer,
        Set<String> i_seedClassNamesSet,
        ScanPolicy scanPolicy) {

        File useDir = new File( getDirPath() );

        processClasses(
            useDir, EMPTY_PREFIX,
            streamer,
            i_seedClassNamesSet,
            getScanResults(),
            scanPolicy);
    }

    public static final String EMPTY_PREFIX = "";

    @Trivial
    protected void processClasses(
        File targetDir, String dirPrefix,
        ClassSource_Streamer streamer,
        Set<String> i_seedClassNames,
        ClassSourceImpl_ScanCounts localScanCounts,
        ScanPolicy scanPolicy) {

        String methodName = "scanClasses";

        if ( tc.isEntryEnabled() ) {
            Tr.entry(tc, methodName, MessageFormat.format("[ {0} ] ENTER [ {1} ] of [ {2} ]",
                                                          new Object[] { getHashText(), dirPrefix, targetDir.getName() }));
        }

        File[] childFiles = UtilImpl_FileUtils.listFiles(targetDir);
        if ( childFiles == null ) {
            Tr.warning(tc, "ANNO_CLASSSOURCE_EMPTY_DIR", getHashText(), targetDir, getDirPath());

            if ( tc.isEntryEnabled() ) {
                Tr.exit(tc, methodName, getHashText());
            }
            return;
        }

        int initialResources = i_seedClassNames.size();

        for ( File nextChildFile : childFiles ) {
            String nextChildName = nextChildFile.getName();
            String nextDirPrefix = pathAppend(dirPrefix, nextChildName);

            if ( UtilImpl_FileUtils.isDirectory(nextChildFile).booleanValue() ) {
                ClassSourceImpl_ScanCounts childCounts = new ClassSourceImpl_ScanCounts();

                processClasses(nextChildFile, nextDirPrefix,
                               streamer,
                               i_seedClassNames,
                               childCounts,
                               scanPolicy);

                localScanCounts.addResults(childCounts);

                localScanCounts.increment(ClassSource_ScanCounts.ResultField.NON_ROOT_CONTAINER);

            } else {
                if ( !isClassResource(nextDirPrefix) ) {
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

                    String nextResourceName = inconvertResourceName(nextDirPrefix);
                    String nextClassName = getClassNameFromResourceName(nextResourceName);
                    if ( isJava9SpecificClass(nextClassName) ) {
                        Tr.debug(tc, MessageFormat.format("[ {0} ] Unsupported class; skipping [ {1} ]", 
                                                          new Object[] { getHashText(), nextClassName }));
                        continue;
                    }
                    
                    String i_nextClassName = internClassName(nextClassName);

                    boolean didAdd = i_maybeAdd(i_nextClassName, i_seedClassNames);

                    if ( !didAdd ) {
                        incrementClassExclusionCount();

                        localScanCounts.increment(ClassSource_ScanCounts.ResultField.DUPLICATE_CLASS);

                    } else {
                        incrementClassInclusionCount();

                        boolean didProcess;

                        try {
                            didProcess = process(streamer, nextClassName, nextResourceName, nextDirPrefix, scanPolicy);

                        } catch (ClassSource_Exception e) {
                            didProcess = false;

                            // TODO: NEW_MESSAGE: Need a new message here.

                            // String eMsg = "[ " + getHashText() + " ]" +
                            //               " Failed to process resource [ " + nextResourceName + " ]" +
                            //               " under root [ " + getDirPath() + " ]" +
                            //               " for class [ " + nextClassName + " ]";
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

        if ( tc.isDebugEnabled() ) {
            Object[] logParms = new Object[] { getHashText(), null, null };

            logParms[1] = Integer.valueOf(finalResources - initialResources);
            Tr.debug(tc, MessageFormat.format("[ {0} ] RETURN [ {1} ] Added classes", logParms));

            for (ClassSource_ScanCounts.ResultField resultField : ClassSource_ScanCounts.ResultField.values()) {
                int nextResult = localScanCounts.getResult(resultField);
                String nextResultTag = resultField.getTag();

                logParms[1] = Integer.valueOf(nextResult);
                logParms[2] = nextResultTag;

                Tr.debug(tc, MessageFormat.format("[ {0} ]  [ {1} ] {2}", logParms));
            }
        }
        if ( tc.isEntryEnabled() ) {
            Tr.exit(tc, methodName, MessageFormat.format("[ {0} ] RETURN [ {1} ] Added classes",
                                                         getHashText(),
                                                         Integer.valueOf(finalResources - initialResources)));
        }
    }

    protected boolean process(
        ClassSource_Streamer streamer,
        String className, String resourceName, String externalResourceName,
        ScanPolicy scanPolicy) throws ClassSource_Exception {

        if ( streamer == null ) {
            return true;
        } else if ( !streamer.doProcess(className, scanPolicy) ) {
            return false;
        }

        InputStream inputStream = openResourceStream(className, resourceName, externalResourceName);
        // throws ClassSource_Exception
        if ( inputStream == null ) {
            return false;
        }

        try {
            streamer.process(getCanonicalName(), className, inputStream, scanPolicy);
            // 'process' throws ClassSourceException
        } finally {
            closeResourceStream(className, resourceName, externalResourceName, inputStream);
        }

        return true;
    }

    //

    @Override    
    protected boolean basicHasJandexIndex() {
        String useJandexPath = getJandexIndexPath();
        String fullJandexPath = getFilePath(useJandexPath);

        File file = new File(fullJandexPath);
        if ( !UtilImpl_FileUtils.exists(file) ) {
            return false;
        } else if ( UtilImpl_FileUtils.isDirectory(file).booleanValue() ) {
        	return false;
        } else {
        	return true;
        }
    }
    
    @Override
    protected Index basicGetJandexIndex() {
        String useJandexPath = getJandexIndexPath();
        String fullJandexPath = getFilePath(useJandexPath);

        InputStream jandexStream;
        try {
            jandexStream = openResourceStream(null, useJandexPath, fullJandexPath);
        } catch ( ClassSource_Exception e ) {
            String errorMessage = "Failed to read [ " + fullJandexPath + " ] as JANDEX index";
            Tr.error(tc, errorMessage);
            return null;
        }

        if ( jandexStream == null ) {
            return null;
        }

        try {
            Index jandexIndex = Jandex_Utils.basicReadIndex(jandexStream); // throws IOException

            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] Read JANDEX index [ {1} ] from [ {2} ] Classes  [ {3} ]", 
                         new Object[] { getHashText(), fullJandexPath, getCanonicalName(), Integer.toString(jandexIndex.getKnownClasses().size()) } ));
            }            
            return jandexIndex;

        } catch ( Exception e ) {
            // TODO:
            String errorMessage = "Failed to read [ " + fullJandexPath + " ] as JANDEX index";
            Tr.error(tc, errorMessage);
            return null;

        } finally {
            closeResourceStream(null, useJandexPath, fullJandexPath, jandexStream);
        }
    }

    //

    @Override
    public InputStream openClassStream(String className) throws ClassSource_Exception {
        String resourceName = getResourceNameFromClassName(className);
        String externalResourceName = outconvertResourceName(resourceName);

        return openResourceStream(className, resourceName, externalResourceName);
        // throws ClassSource_Exception
    }

    @Override
    public InputStream openResourceStream(String className, String resourceName)
        throws ClassSource_Exception {

        String externalResourceName = outconvertResourceName(resourceName);

        return openResourceStream(className, resourceName, externalResourceName);
        // throws ClassSource_Exception
    }

    protected InputStream openResourceStream(
        String className, String resourceName, String externalResourceName)
        throws ClassSource_Exception {

        String methodName = "openResourceStream";

        String filePath = getFilePath(externalResourceName);

        File file = new File(filePath);
        if ( !UtilImpl_FileUtils.exists(file) ) {
            return null;
        }

        if ( UtilImpl_FileUtils.isDirectory(file).booleanValue() ) {
            // defect 84235:we are generating multiple Warning/Error messages for each error due to each level reporting them.
            // Disable the following warning and defer message generation to a higher level, 
            // preferably the ultimate consumer of the exception.
            //Tr.warning(tc, "ANNO_CLASSSOURCE_NOT_FILE",
            //           getHashText(), externalResourceName, filePath, getDirPath(), className);
            String eMsg =
                "[ " + getHashText() + " ]" +
                " Found directory [ " + filePath + " ]" +
                " for resource [ " + externalResourceName + " ]" +
                " under root [ " + getDirPath() + " ]";
            if ( className != null ) {
                eMsg += " for class [ " + className + " ]";
            }
            throw getFactory().newClassSourceException(eMsg);
        }

        InputStream inputStream;
        try {
            inputStream = UtilImpl_FileUtils.createFileInputStream(file); // throws IOException
        } catch (IOException e) {
            // defect 84235:we are generating multiple Warning/Error messages for each error due to each level reporting them.
            // Disable the following warning and defer message generation to a higher level, 
            // preferably the ultimate consumer of the exception.
            //Tr.warning(tc, "ANNO_CLASSSOURCE_OPEN3_EXCEPTION",
            //           getHashText(), filePath, externalResourceName, getDirPath(), className);
            String eMsg =
                "[ " + getHashText() + " ]" +
                " Failed to open [ " + filePath + " ]" +
                " for resource [ " + externalResourceName + " ]" +
                " under root [ " + getDirPath() + " ]";
            if ( className != null ) {
                eMsg += " for class [ " + className + " ]";
            }
            throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, e);
        }

        return inputStream;
    }

    @Override
    public void closeClassStream(String className, InputStream inputStream) {
        String resourceName = getResourceNameFromClassName(className);
        String externalResourceName = outconvertResourceName(resourceName);

        closeResourceStream(className, resourceName, externalResourceName, inputStream);
    }

    @Override
    public void closeResourceStream(String className, String resourceName, InputStream inputStream) {
        String externalResourceName = outconvertResourceName(resourceName);

        closeResourceStream(className, resourceName, externalResourceName, inputStream);
    }

    protected void closeResourceStream(String className,
                                       String resourceName,
                                       String externalResourceName,
                                       InputStream inputStream) {

        try {
            inputStream.close(); // throws IOException

        } catch ( IOException e ) {
            // String eMsg = "[ " + getHashText() + " ]" +
            //               " Failed to close resource [ " + externalResourceName + " ]" +
            //               " under root [ " + getDirPath() + " ]" +
            //               " for class [ " + className + " ]";
            Tr.warning(tc,
                "ANNO_CLASSSOURCE_CLOSE4_EXCEPTION",
                getHashText(), externalResourceName, getDirPath(), className);
        }
    }

    //

    @Override
    @Trivial
    public void log(TraceComponent logger) {
        Tr.debug(logger, MessageFormat.format("Class Source [ {0} ]", getHashText()));
    }
}
