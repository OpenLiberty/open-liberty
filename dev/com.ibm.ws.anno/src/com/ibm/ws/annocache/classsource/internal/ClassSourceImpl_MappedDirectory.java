/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.annocache.classsource.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.jandex.Index;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.jandex.internal.Jandex_Utils;
import com.ibm.ws.annocache.jandex.internal.SparseIndex;
import com.ibm.ws.annocache.util.internal.UtilImpl_FileUtils;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts.ResultField;
import com.ibm.wsspi.annocache.classsource.ClassSource;
import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.classsource.ClassSource_MappedDirectory;
import com.ibm.wsspi.annocache.classsource.ClassSource_Streamer;
import com.ibm.wsspi.annocache.util.Util_InternMap;

public class ClassSourceImpl_MappedDirectory
    extends ClassSourceImpl implements ClassSource_MappedDirectory {

    @SuppressWarnings("hiding")
    public static final String CLASS_NAME = ClassSourceImpl_MappedDirectory.class.getSimpleName();

    // Top O' the world

    public ClassSourceImpl_MappedDirectory(
        ClassSourceImpl_Factory factory, Util_InternMap internMap,
        String name,
        String dirPath) throws ClassSource_Exception {

        this(factory, internMap, name, dirPath, NO_ENTRY_PREFIX); // throws ClassSource_Exception
    }

    @SuppressWarnings("unused")
    public ClassSourceImpl_MappedDirectory(
        ClassSourceImpl_Factory factory,
        Util_InternMap internMap,
        String name,
        String path, String entryPrefix) throws ClassSource_Exception {

        super(factory, internMap, entryPrefix, name, path); // throws ClassSource_Exception

        String methodName = "<init>";

        if ( path == null ) {
            throw new IllegalArgumentException("Null path is not allowed");
        } else if ( path.isEmpty() ) {
            throw new IllegalArgumentException("Empty path is not allowed");
        } else if ( path.charAt(path.length() - 1) != '/' ) {
            throw new IllegalArgumentException("Path [ " + path + " ] must have a trailing '/'");
        }
        this.rootPath = path;
        this.absRootPath = getAbsolutePath(path);

        // The entry prefix is validated in the superclass constructor.

        if ( entryPrefix == null ) {
            this.pathPrefix = null;
            this.path = this.rootPath;
            this.absPath = this.absRootPath;
        } else {
            this.pathPrefix = outconvert(entryPrefix);
            this.path = pathAppend(this.rootPath, this.pathPrefix);
            this.absPath = getAbsolutePath(this.path);
        }

        if ( logger.isLoggable(Level.FINER) ) {
            File rootFile = new File(this.rootPath);
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Root [ {1} ] Exists [ {2} ] IsDir [ {3} ]",
                new Object[] {
                    Integer.valueOf(this.hashCode()),
                    this.absRootPath,
                    Boolean.valueOf(rootFile.exists()),
                    Boolean.valueOf(rootFile.isDirectory()) });

            if ( entryPrefix != null ) {
                File file = new File(this.path);
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Path [ {1} ] Exists [ {2} ] IsDir [ {3} ]",
                    new Object[] {
                        Integer.valueOf(this.hashCode()),
                        this.absPath,
                        Boolean.valueOf(file.exists()),
                        Boolean.valueOf(file.isDirectory()) });
            }
        }
    }

    //

    /**
     * <p>Compute and return a time stamp for this class source.</p>
     *
     * <p>Time stamps are not available for directory class sources:
     * Answer the unrecorded stamp value {@link ClassSource#UNRECORDED_STAMP}.</p>
     *
     * @return The computed stamp for this class source.  This implementation always
     *         returns the unrecorded stamp value.
     */
    @Override
    protected String computeStamp() {
        return UNRECORDED_STAMP;
    }

    //

    @Override
    public void open() throws ClassSource_Exception {
        String methodName = "open";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER/RETURN", getHashText());
        }
    }

    @Override
    public void close() throws ClassSource_Exception {
        String methodName = "close";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER/RETURN", getHashText());
        }
    }

    //

    protected static final char FILE_SEPARATOR_CHAR = File.separatorChar;

    protected static final boolean CONVERT_PATHS = (FILE_SEPARATOR_CHAR == '\\');

    public static String pathAppend(String head, String tail) {
        if ( (head == null) || head.isEmpty() ) {
            return tail;
        } else {
            return ( head + tail );
        }
    }

    public static String pathAppend(String head, String middle, String tail) {
        if ( (head == null) || head.isEmpty() ) {
            if ( (middle == null) || middle.isEmpty() ) {
                return tail;
            } else {
                return ( middle + tail );
            }
        } else {
            if ( (middle == null) || middle.isEmpty() ) {
                return ( head + tail );
            } else {
                return ( head + middle + tail );
            }
        }
    }

    //

    public static boolean getConvertPaths() {
        return CONVERT_PATHS;
    }

    public static String inconvert(String resourcePath) {
        if ( CONVERT_PATHS ) {
            return resourcePath.replace(FILE_SEPARATOR_CHAR, ClassSource.RESOURCE_SEPARATOR_CHAR);
        } else {
            return resourcePath;
        }
    }

    public static String outconvert(String resourceName) {
        if ( CONVERT_PATHS ) {
            return resourceName.replace(ClassSource.RESOURCE_SEPARATOR_CHAR, FILE_SEPARATOR_CHAR);
        } else {
            return resourceName;
        }
    }

    //

    protected final String rootPath;
    protected final String absRootPath;

    @Trivial
    public String getRootPath() {
        return rootPath;
    }

    @Trivial
    public String getAbsRootPath() {
        return absRootPath;
    }

    public String getRootPath(String resourcePath) {
        return pathAppend( getRootPath(), resourcePath );
    }

    //

    protected final String pathPrefix;
    protected final String path;
    protected final String absPath;

    public String getPrefixPath() {
        return pathPrefix;
    }

    @Override
    public String getDirPath() {
        return path;
    }

    @Override
    public String getPath() {
        return path;
    }

    public String getAbsPath() {
        return absPath;
    }

    public String getPath(String resourcePath) {
        return pathAppend( getPath(), resourcePath );
    }

    //

    @Override    
    protected boolean basicHasJandexIndex() {
        String useJandexPath = outconvert( getJandexIndexPath() );
        String fullJandexPath = getRootPath(useJandexPath); // Does NOT use the prefix.

        File jandexIndexFile = new File(fullJandexPath);
        if ( !UtilImpl_FileUtils.exists(jandexIndexFile) ) {
            return false;
        } else if ( UtilImpl_FileUtils.isDirectory(jandexIndexFile) ) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * <p>Answer the JANDEX index for this directory class source.  Answer null if none
     * is available.</p>
     *
     * @return The JANDEX index for this directory class source.
     */
    protected Index basicGetJandexIndex() {
        String methodName = "basicGetJandexIndex";

        String useJandexPath = outconvert( getJandexIndexPath() );
        String fullJandexPath = getRootPath(useJandexPath); // Does NOT use the prefix.

        File jandexFile = new File(fullJandexPath);
        if ( !UtilImpl_FileUtils.exists(jandexFile) ) {
            return null;
        }

        try {
            Index jandexIndex = Jandex_Utils.basicReadIndex(fullJandexPath); // throws IOException
            if ( logger.isLoggable(Level.FINER) ) {
                String message = MessageFormat.format(
                    "[ {0} ] Read JANDEX index [ {1} ] from [ {2} ] Classes [ {3} ]", 
                    new Object[] { getHashText(), getAbsolutePath(fullJandexPath), getCanonicalName(), Integer.toString(jandexIndex.getKnownClasses().size()) });
                logger.logp(Level.FINER, CLASS_NAME, methodName, message);
            }
            return jandexIndex;

        } catch ( Exception e ) {
            // ANNO_CLASSSOURCE_FILE_JANDEX_READ_EXCEPTION=CWWKC0090W: 
            // [ {0} ] Read of Jandex index file [{1}] failed.  Error message: {2}

            logger.logp(Level.SEVERE, CLASS_NAME, methodName,
                "ANNO_CLASSSOURCE_FILE_JANDEX_READ_EXCEPTION",
                new Object[] { getHashText(), getAbsolutePath(fullJandexPath), e });

            return null;
        }
    }

    @Override
    protected SparseIndex basicGetSparseJandexIndex() {
        String methodName = "basicGetSparseJandexIndex";

        String useJandexPath = outconvert( getJandexIndexPath() );
        String fullJandexPath = getRootPath(useJandexPath); // Does NOT use the prefix.

        File jandexFile = new File(fullJandexPath);
        if ( !UtilImpl_FileUtils.exists(jandexFile) ) {
            return null;
        }

        try {
            SparseIndex jandexIndex = Jandex_Utils.basicReadSparseIndex(fullJandexPath); // throws IOException
            if ( logger.isLoggable(Level.FINER) ) {
                String message = MessageFormat.format(
                    "[ {0} ] Read sparse JANDEX index [ {1} ] from [ {2} ] Classes [ {3} ]", 
                    new Object[] { getHashText(), getAbsolutePath(fullJandexPath), getCanonicalName(), Integer.toString(jandexIndex.getKnownClasses().size()) });
                logger.logp(Level.FINER, CLASS_NAME, methodName, message);
            }
            return jandexIndex;

        } catch ( Exception e ) {
            // ANNO_CLASSSOURCE_FILE_JANDEX_READ_EXCEPTION=CWWKC0090W: 
            // [ {0} ] Read of Jandex index file [{1}] failed.  Error message: {2}

            logger.logp(Level.SEVERE, CLASS_NAME, methodName,
                "ANNO_CLASSSOURCE_FILE_JANDEX_READ_EXCEPTION",
                new Object[] { getHashText(), getAbsolutePath(fullJandexPath), e });

            return null;
        }
    }

    //

    @Override
    public InputStream openResourceStream(String className, String resourceName) throws ClassSource_Exception {
        String resourcePath = outconvert(resourceName);

        return openResourceStream(className, resourceName, resourcePath, DO_ADD_PREFIX);
        // throws ClassSource_Exception
    }

    protected byte[] readFully(String jandexPath) throws IOException {
        File file = new File(jandexPath);

        if ( !UtilImpl_FileUtils.exists(file) ) {
            return null;
        }

        return UtilImpl_FileUtils.readFully(file);
    }

    protected static final boolean DO_ADD_PREFIX = true;
    protected static final boolean DO_NOT_ADD_PREFIX = false;

    protected InputStream openResourceStream(
        String className, String resourceName, String resourcePath, boolean addPrefix)
        throws ClassSource_Exception {

        String methodName = "openResourceStream";

        String basePath;
        String filePath;
        if ( addPrefix ) {
            basePath = getPath();
            filePath = getPath(resourcePath);
        } else {
            basePath = getRootPath();
            filePath = getRootPath(resourcePath);
        }

        File file = new File(filePath);
        if ( !UtilImpl_FileUtils.exists(file) ) {
            return null;
        }

        if ( UtilImpl_FileUtils.isDirectory(file) ) {
            // defect 84235:we are generating multiple Warning/Error messages for each error due to each level reporting them.
            // Disable the following warning and defer message generation to a higher level,
            // preferably the ultimate consumer of the exception.
            // logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_CLASSSOURCE_NOT_FILE",
            //     new Object[] { getHashText(), externalResourceName, filePath, getPrefixPath(), className });
            String eMsg =
                "[ " + getHashText() + " ]" +
                " Found directory [ " + getAbsolutePath(filePath) + " ]" +
                " for resource [ " + resourcePath + " ]" +
                " under [ " + getAbsolutePath(basePath) + " ]";
            if ( className != null ) {
                eMsg += " for class [ " + className + " ]";
            }
            throw getFactory().newClassSourceException(eMsg);
        }

        InputStream result;

        try {
            result = UtilImpl_FileUtils.createFileInputStream(file); // throws IOException

        } catch ( IOException e ) {
            // defect 84235:we are generating multiple Warning/Error messages for each error due to each level reporting them.
            // Disable the following warning and defer message generation to a higher level,
            // preferably the ultimate consumer of the exception.
            //
            // logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_CLASSSOURCE_OPEN3_EXCEPTION",
            //     new Object[] { getHashText(), filePath, externalResourceName, getPrefixPath(), className });
            String eMsg =
                "[ " + getHashText() + " ]" +
                " Failed to open [ " + getAbsolutePath(filePath) + " ]" +
                " for resource [ " + resourcePath + " ]" +
                " under [ " + getAbsolutePath(basePath) + " ]";
            if ( className != null ) {
                eMsg += " for class [ " + className + " ]";
            }
            throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, e);
        }

        return result;
    }

    @Override
    public void closeResourceStream(String className, String resourceName, InputStream inputStream) {
        String resourcePath = outconvert(resourceName);

        closeResourceStream(className, resourceName, resourcePath, DO_ADD_PREFIX, inputStream);
    }

    public static final String NO_CLASS_NAME = null;

    protected void closeResourceStream(
        String className, String resourceName,
        String resourcePath, boolean doAddPrefix,
        InputStream inputStream) {

        String methodName = "closeResourceStream";

        try {
            inputStream.close(); // throws IOException

        } catch ( IOException e ) {
            // "[ {0} ] Failed to close resource [ {1} ] under root [ {2} ] for class [ {3} ]: [ {4} ]",
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_CLASSSOURCE_CLOSE4_EXCEPTION",
                new Object[] {
                    getHashText(),
                    resourcePath, (doAddPrefix ? getAbsPath() : getAbsRootPath()), className,
                    e });
        }
    }

    //

    @Override
    public int processFromScratch(ClassSource_Streamer streamer)
        throws ClassSource_Exception {

        File baseDir = new File( getPath() ); // DO start at the prefix path.
        return processDirectory(streamer, EMPTY_PREFIX, baseDir);
    }

    public static final String EMPTY_PREFIX = null;

    protected int processDirectory(ClassSource_Streamer streamer, String currentPrefix, File currentDir)
        throws ClassSource_Exception {

        String methodName = "processDirectory";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, 
                "[ {0} ] ENTER [ {1} ] of [ {2} ]",
                new Object[] { getHashText(), currentPrefix, currentDir.getName() });
        }

        File[] childFiles = UtilImpl_FileUtils.listFiles(currentDir);
        if ( childFiles == null ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN (empty)", getHashText());
            }
            return 0;
        }

        int classCount = 0;

        for ( File nextChildFile : childFiles ) {
            String nextChildName = nextChildFile.getName();
            String nextChildPrefix = pathAppend(currentPrefix, nextChildName);

            if ( UtilImpl_FileUtils.isDirectory(nextChildFile) ) {
                classCount += processDirectory(streamer, nextChildPrefix + FILE_SEPARATOR_CHAR, nextChildFile);

            } else {
                if ( isClassResource(nextChildPrefix) ) {
                    String nextResourceName = inconvert(nextChildPrefix);
                    String nextClassName = getClassNameFromResourceName(nextResourceName);

                    if ( ClassSourceImpl.isJava9PackageName(nextClassName) ) {  // PI89708
                        logger.logp(Level.FINER, CLASS_NAME, methodName, "Java9 class name [ {0} ]", nextClassName);
                        continue;
                    }

                    String i_nextClassName = internClassName(nextClassName);

                    try {
                        processClassResource(streamer, i_nextClassName, nextResourceName, nextChildPrefix);
                    } catch ( ClassSource_Exception e ) {
                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_SCAN_EXCEPTION [ {0} ]", e);
                    }

                    classCount++;
                }
            }
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN [ {1} ]",
                new Object[] { getHashText(), Integer.valueOf(classCount) });
        }
        return classCount;
    }

    protected void processClassResource(
        ClassSource_Streamer streamer,
        String i_className,
        String resourceName, String resourcePath) throws ClassSource_Exception {

        InputStream inputStream = openResourceStream(i_className, resourceName, resourcePath, DO_ADD_PREFIX);
        // throws ClassSource_Exception

        try {
            streamer.process(i_className, inputStream); // throws ClassSource_Exception

        } finally {
            closeResourceStream(i_className, resourceName, resourcePath, DO_ADD_PREFIX, inputStream);
        }
    }

    //

    @Override
    public void processSpecific(ClassSource_Streamer streamer, Set<String> i_classNames)
        throws ClassSource_Exception {
        String methodName = "processSpecific";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER [ {1} ]",
                new Object[] { getHashText(), Integer.valueOf(i_classNames.size()) });
        }

        long scanStart = System.nanoTime();

        for ( String i_className : i_classNames ) {
            String resourceName = getResourceNameFromClassName(i_className);
            String resourcePath = outconvert(resourceName);

            processClassResource(streamer, i_className, resourceName, resourcePath); // throws ClassSource_Exception
        }

        long scanTime = System.nanoTime() - scanStart;

        setProcessTime(scanTime);
        setProcessCount( i_classNames.size() );

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN", getHashText());
        }
    }

    //

    @Override
    public void log(Logger useLogger) {
        String methodName = "log";
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class Source [ {0} ]", getHashText());
    }

    // Obsolete methods ...

    @Override
    @Trivial
    public void log(TraceComponent tc) {
        Tr.debug(tc, MessageFormat.format("Class Source [ {0} ]", getHashText()));
    }

    //

    @Override
    public void setParentSource(com.ibm.wsspi.anno.classsource.ClassSource classSource) {
        // EMPTY
    }

    @Override
    public void scanClasses(com.ibm.wsspi.anno.classsource.ClassSource_Streamer streamer,
        Set<String> i_seedClassNamesSet, ScanPolicy scanPolicy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassSource_ScanCounts getScanResults() {
        return null;
    }

    @Override
    public int getResult(ResultField resultField) {
        return 0;
    }


    @Override
    public int getResourceExclusionCount() {
        return 0;
    }

    @Override
    public int getClassExclusionCount() {
        return 0;
    }

    @Override
    public int getClassInclusionCount() {
        return 0;
    }

    @Override
    public boolean scanSpecificSeedClass(String specificClassName,
            com.ibm.wsspi.anno.classsource.ClassSource_Streamer streamer)
            throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean scanReferencedClass(String referencedClassNam,
            com.ibm.wsspi.anno.classsource.ClassSource_Streamer streamer)
            throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getConvertResourceNames() {
        return false;
    }

    @Override
    public String inconvertResourceName(String externalResourceName) {
        return null;
    }

    @Override
    public String outconvertResourceName(String internalResourceName) {
        return null;
    }

    @Override
    public InputStream openClassStream(String className)
        throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
        return null;
    }

    @Override
    public void closeClassStream(String className, InputStream inputStream)
        throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
        // EMPTY
    }
}
