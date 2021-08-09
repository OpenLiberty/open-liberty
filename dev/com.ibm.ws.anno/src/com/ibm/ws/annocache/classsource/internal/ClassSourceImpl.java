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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.jandex.internal.Jandex_Utils;
import com.ibm.ws.annocache.jandex.internal.SparseClassInfo;
import com.ibm.ws.annocache.jandex.internal.SparseIndex;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.util.internal.UtilImpl_FileStamp;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.classsource.ClassSource;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.classsource.ClassSource_Options;
import com.ibm.wsspi.annocache.classsource.ClassSource_Streamer;
import com.ibm.wsspi.annocache.util.Util_InternMap;

public abstract class ClassSourceImpl implements ClassSource {
    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;
    protected static final Logger stateLogger = AnnotationCacheServiceImpl_Logging.ANNO_STATE_LOGGER;
    protected static final Logger jandexLogger = AnnotationCacheServiceImpl_Logging.ANNO_JANDEX_LOGGER;

    public static final String CLASS_NAME = ClassSourceImpl.class.getSimpleName();

    @Trivial
    protected static long getTime() {
        return System.currentTimeMillis();
    }

    protected static String getAbsolutePath(String path) {
        return (new File(path)).getAbsolutePath();
    }

    //

    /**
     * <p>Perform a resource append operation: This places
     * a resource separator between the supplied values.
     *
     * @param head The first value to put into the concatenated value.
     * @param tail The second value to put into the concatenated value.
     *
     * @return The first value concatenated with a resource separator
     *         and with the second value.
     */
    @Trivial
    public String resourceAppend(String head, String tail) {
        if ( head.isEmpty() ) {
            return tail;
        } else {
            return (head + ClassSource.RESOURCE_SEPARATOR_CHAR + tail);
        }
    }    

    /**
     * <p>Tell if a specified resource is a directory resource.
     * Resources which end with the resource separator are directory
     * resources.</p>
     *
     * @param resourceName The resource name to test.
     *
     * @return True if the resource is a directory resource. Otherwise,
     *         false.
     */
    @Trivial
    public boolean isDirectoryResource(String resourceName) {
        return resourceName.endsWith(ClassSource.RESOURCE_SEPARATOR_STRING);
    }

    /**
     * <p>Tell if a specified resource is a class resource. A resource
     * is a class resource if and only if it has the class extension.</p>
     *
     * @param resourceName The resource which is to be tested.
     *
     * @return True if the resource is a class resource. Otherwise, false.
     */
    @Trivial
    public boolean isClassResource(String resourceName) {
        return resourceName.endsWith(CLASS_EXTENSION);
    }

    /**
     * <p>Convert a resource name to a class name. Conversion strips
     * the class extension and converts all resource separators to
     * class separators. Note that the inner class separator is not
     * changed by the conversion.
     *
     * @param resourceName The resource to convert to a class name.
     *
     * @return The class name for the resource.
     */
    @Trivial
    public String getClassNameFromResourceName(String resourceName) {
        int endingOffset = resourceName.length() - ClassSource.CLASS_EXTENSION.length();
        String className = resourceName.substring(0, endingOffset);
        className = className.replace(RESOURCE_SEPARATOR_CHAR, ClassSource.CLASS_SEPARATOR_CHAR);

        return className;
    }

    /**
     * <p>Convert a class name to a resource name. Conversion
     * changes the class separator to the resource separator and
     * adds the class extension. The inner class separator is not
     * changed by the conversion.</p>
     *
     * @param className The class name to convert to a resource.
     *
     * @return The resource for the class name.
     */
    @Trivial
    public String getResourceNameFromClassName(String className) {
        return ClassSourceImpl.resourceNameFromClassName(className);
    }

    /**
     * <p>Convert a class name to a resource name. Conversion
     * changes the class separator to the resource separator and
     * adds the class extension. The inner class separator is not
     * changed by the conversion.</p>
     *
     * @param className The class name to convert to a resource.
     *
     * @return The resource for the class name.
     */
    @Trivial
    public static String resourceNameFromClassName(String className) {
        return
            className.replace(ClassSource.CLASS_SEPARATOR_CHAR, RESOURCE_SEPARATOR_CHAR) +
            ClassSource.CLASS_EXTENSION;
    }

    /**
     * Tell if a class is a forbidden java9 type class.  These are not
     * currently processed.
     * 
     * There are two cases: multi-release classes, which are beneath the META-INF folder,
     * and module classes, which are named "module-info.class".
     *
     * @param packageName A package name to test.
     *
     * @return True or false telling if the the name is a forbidden java9 name.
     */
    public static boolean isJava9PackageName(String packageName) {
        if ( packageName.endsWith("module-info") ) {
            return true;
        } else if ( packageName.contains("META-INF") ) {
            return true;
        } else {
            return false;
            // if ( SourceVersion.isName(packageName) ) {
        }
    }

    //

    protected final String hashText;

    @Override
    @Trivial
    public String getHashText() {
        return hashText;
    }

    @Override
    @Trivial
    public String toString() {
        return hashText;
    }

    //

    @Trivial
    protected ClassSourceImpl(
        ClassSourceImpl_Factory factory, Util_InternMap internMap,
        String entryPrefix,
        String name, String hashTextSuffix) {

        super();

        String methodName = "<init>";

        this.internMap = internMap;
        this.factory = factory;

        if ( entryPrefix != null ) {
            if ( entryPrefix.isEmpty() ) {
                throw new IllegalArgumentException("Prefix cannot be empty");
            } else if ( entryPrefix.charAt(entryPrefix.length() - 1) != '/' ) {
                throw new IllegalArgumentException("Prefix [ " + entryPrefix + " ] must have a trailing '/'");
            }
        }
        this.entryPrefix = entryPrefix;

        this.name = name;
        this.canonicalName = factory.getCanonicalName(this.name);

        this.parentSource = null;

        String useHashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
        useHashText += "(" + this.canonicalName;
        if ( hashTextSuffix != null ) {
            useHashText += ", " + hashTextSuffix;
        }
        useHashText += ")";
        this.hashText = useHashText;

        this.processedUsingJandex = false;
        this.processTime = 0L;
        this.processCount = 0;

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] InternMap [ {1} ]",
                new Object[] { this.hashText, this.internMap.getHashText() });
            if ( this.entryPrefix != null ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Prefix [ {1} ]",
                    new Object[] { this.hashText, this.entryPrefix });
            }
        }
    }

    //

    protected final ClassSourceImpl_Factory factory;

    @Override
    @Trivial
    public ClassSourceImpl_Factory getFactory() {
        return factory;
    }

    //

    private final String entryPrefix;

    @Trivial
    @Override
    public String getEntryPrefix() {
        return entryPrefix;
    }

    //

    protected final String name;

    @Override
    @Trivial
    public String getName() {
        return name;
    }

    protected final String canonicalName;

    @Override
    @Trivial
    public String getCanonicalName() {
        return canonicalName;
    }

    //

    protected ClassSource_Aggregate parentSource;

    @Override
    @Trivial
    public ClassSource_Aggregate getParentSource() {
        return parentSource;
    }

    @Override
    public void setParentSource(ClassSource_Aggregate parentSource) {
        this.parentSource = parentSource;
    }

    //

    @Override
    @Trivial
    public ClassSource_Options getOptions() {
        return getParentSource().getOptions();
    }

    @Trivial
    public boolean getUseJandex() {
        return getOptions().getUseJandex();
    }

    /**
     * <p>Answer the path to JANDEX index files.</p>
     *
     * <p>The default implementation answers <code>"META-INF/jandex.ndx"</code>.</p>
     *
     * @return The relative path to JANDEX index files.
     */
    @Trivial
    public String getJandexIndexPath() {
        return getOptions().getJandexPath();
    }

    // Sparse Jandex Index Methods ...

    private static final int NS_IN_MS = 1000000;

    /**
     * Attempt to read the Jandex index.
     *
     * If Jandex is not enabled, immediately answer null.
     *
     * If no Jandex index is available, or if it cannot be read, answer null.
     *
     * @return The read Jandex index.
     */
    protected SparseIndex getSparseJandexIndex() {
        String methodName = "getSparseJandexIndex";

        long startTime = System.nanoTime();
        SparseIndex jandexIndex = basicGetSparseJandexIndex();
        long readTime = System.nanoTime() - startTime;

        if ( jandexIndex != null ) {
            setProcessTime(readTime);
            setProcessCount(jandexIndex.getKnownClasses().size());

            // System.out.println("Sparse jandex read [ " + readTime + " ]");
        }

        boolean doLog = logger.isLoggable(Level.FINER);
        boolean doJandexLog = jandexLogger.isLoggable(Level.FINER);
        if ( doLog || doJandexLog ) {
            String msg;
            if ( jandexIndex != null ) {
                msg = MessageFormat.format(
                    "[ {0} ] Index [ {1} ] found; [ {2} (ms) ]",
                    getHashText(), getJandexIndexPath(), Long.valueOf(readTime / NS_IN_MS));
            } else {
                msg = MessageFormat.format(
                    "[ {0} ] Index [ {1} ] not found",
                    getHashText(), getJandexIndexPath());
            }
            if ( doLog ) {
                logger.logp(Level.FINER, CLASS_NAME,  methodName, msg);
            }
            if ( doJandexLog ) {
                jandexLogger.logp(Level.FINER, CLASS_NAME,  methodName, msg);
            }
        }

        return jandexIndex;
    }

    protected SparseIndex basicGetSparseJandexIndex() {
        return null;
    }

    //

    protected String stamp;

    @Override
    @Trivial
    public String getStamp() {
        if ( stamp == null ) {
            stamp = computeStamp();
        }
        return stamp;
    }

    /**
     * <p>Compute and return a stamp for this class source.</p>
     *
     * Stamps are available only for mapped jar and for mapped container
     * file sources.  Stamps for mapped container file sources are only available
     * if the container maps to a single simple file.
     * 
     * See {@link UtilImpl_FileStamp#computeStamp(File)} for more information.
     *
     * @return The stamp for the class source.
     */
    protected abstract String computeStamp();

    //

    protected final Util_InternMap internMap;

    @Override
    @Trivial
    public Util_InternMap getInternMap() {
        return internMap;
    }

    @Trivial
    protected String internClassName(String className) {
        return getInternMap().intern(className);
    }

    @Trivial
    protected String internClassName(String className, boolean doForce) {
        return getInternMap().intern(className, doForce);
    }

    @Trivial
    protected boolean i_maybeAdd(String i_resourceName, Set<String> i_seedClassNamesSet) {
        String methodName = "i_maybeAdd";

        boolean didAdd;
        if ( didAdd = !i_seedClassNamesSet.contains(i_resourceName) ) {
            i_seedClassNamesSet.add(i_resourceName);
        }

        // Explicit trace: We want to trace additions, but don't
        // want to trace the seed class names parameter.  The
        // seed class names can be large, and displaying it to trace
        // rather bloats the trace.

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Resource [ {1} ]: [ {2} ]",
                new Object[] { getHashText(), i_resourceName, Boolean.valueOf(didAdd) });
        }

        return didAdd;
    }

    //

    @Override
    public abstract void open() throws ClassSource_Exception;

    @Override
    public abstract void close() throws ClassSource_Exception;

    //

    @Override
    @Trivial
    public BufferedInputStream openClassResourceStream(String className, String resourceName)
        throws ClassSource_Exception {
        return openResourceStream(className, resourceName, CLASS_BUFFER_SIZE);
    }

    @Override
    public abstract InputStream openResourceStream(String className, String resourceName)
        throws ClassSource_Exception;

    @Override
    public BufferedInputStream openResourceStream(String className, String resourceName, int bufferSize)
        throws ClassSource_Exception {
        InputStream inputStream = openResourceStream(className, resourceName); // throws ClassSource_Exception
        return ( (inputStream == null) ? null : new BufferedInputStream(inputStream, bufferSize) );
    }

    @Override
    public abstract void closeResourceStream(String className, String resourceName, InputStream inputStream);

    //

    @Override
    @Trivial
    public void logState() {
        if ( stateLogger.isLoggable(Level.FINER) ) {
            log(stateLogger);
        }
    }

    @Override
    public abstract void log(Logger useLogger);

    //

    @Override
    public abstract void log(TraceComponent tc);

    @Trivial
    protected void logCounts(TraceComponent tc) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("  Included classes: [ {0} ]",
                                                  Integer.valueOf(getClassInclusionCount())));

            Tr.debug(tc, MessageFormat.format("  Excluded classes: [ {0} ]",
                                                  Integer.valueOf(getClassExclusionCount())));
        }
    }

    //

    /**
     * Process the contents of this class source using a class information consumer.
     * 
     * Processing has three options:
     * 
     * If a jandex index is available, read it and provide the classes to the streamer.
     * 
     * If jandex index creation is specified, use the class resources of this class
     * source to create a jandex index, then provide the new classes to the streamer.
     *
     * If jandex processing is not done, provide the class resources of this class source
     * to the streamer.
     
     * @param streamer A consumer of class information.
     *
     * @throws ClassSource_Exception Thrown if the processing failed.
     */
    @Override
    public void process(ClassSource_Streamer streamer) throws ClassSource_Exception {
        String methodName = "process";
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", getHashText());
        }

        int initialClasses = getInternMap().getSize();
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Processing [ {1} ] Initial classes [ {2} ]",
                new Object[] { getHashText(), getCanonicalName(), Integer.valueOf(initialClasses) });
        }

        String scanTag;

        if ( processUsingJandex(streamer) ) {
            scanTag = "Existing jandex index";

        } else if ( processJandexFromScratch(streamer) ) {
            scanTag = "New jandex index";

        } else {
            long startScan = System.nanoTime();
            setProcessCount( processFromScratch(streamer) );
            long scanTime = System.nanoTime() - startScan;
            setProcessTime(scanTime);

            scanTag = "New scan";
        }

        int finalClasses = getInternMap().getSize();

//        System.out.println(
//            "[ " + getHashText() + " ]" +
//             " Processing [ " + getCanonicalName() +" ]: " + scanTag +
//             ": Final classes [ " + finalClasses + " ]");
//
//        System.out.println("[ Use jandex ]: " + getUseJandex());
//        System.out.println("[ Use jandex full ]: " + getUseJandexFull());
//        System.out.println("[ Create jandex ]: " + streamer.createJandex());

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Processing [ {1} ] {2}; Final classes [ {3} ]",
                new Object[] { getHashText(),
                               getCanonicalName(),
                               scanTag,  Integer.valueOf(finalClasses) } );

            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] RETURN [ {1} ] Added classes",
                new Object[] { getHashText(),
                               Integer.valueOf(finalClasses - initialClasses) } );
        }

        if ( jandexLogger.isLoggable(Level.FINER) ) {
            String useHashText = getHashText();

            jandexLogger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Processing [ {1} ] {2}; Final classes [ {3} ]",
                new Object[] { useHashText,
                              getCanonicalName(),
                              scanTag, Integer.valueOf(finalClasses) } );

            jandexLogger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Added classes [ {1} ]",
                new Object[] { useHashText,
                               Integer.valueOf(finalClasses - initialClasses) });
        }
    }

    protected abstract int processFromScratch(ClassSource_Streamer streamer)
        throws ClassSource_Exception;

    /**
     * Attempt to process using a new jandex index.
     *
     * @param streamer The streamer which will process class information.
     *
     * @return True or false telling if the information was processed using
     *     a new Jandex index.
     *
     * @throws ClassSource_Exception Thrown if processing failed.
     */
    protected boolean processJandexFromScratch(ClassSource_Streamer streamer)
        throws ClassSource_Exception {

        if ( !streamer.createJandex() ) {
            return false;
        }

        long startScan = System.nanoTime();

        Index jandexIndex = createIndex();

        Collection<ClassInfo> jandexClasses = jandexIndex.getKnownClasses(); 
        for ( ClassInfo jandexClassInfo : jandexClasses ) {
            streamer.processJandex(jandexClassInfo);
        }

        // Need to keep the index: It will be saved as cache data.
        streamer.storeJandex(jandexIndex);

        setProcessCount( jandexClasses.size() );

        long scanTime = System.nanoTime() - startScan;
        setProcessTime(scanTime);

        return true;
    }

    /**
     * Create a jandex index for this class source.
     *
     * @return A new jandex index.
     *
     * @throws ClassSource_Exception Thrown if the scan failed.
     */
    protected Index createIndex()
        throws ClassSource_Exception {

        final Indexer indexer = Jandex_Utils.createIndexer();

        ClassSource_Streamer jandexStreamer = new ClassSource_Streamer() {
            @Override
            public boolean process(String i_className, InputStream inputStream) throws ClassSource_Exception {
                String methodName = "process";
                try {
                    indexer.index(inputStream);
                } catch ( IOException e ) {
                    throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName,
                        "Failed to scan class [ " + i_className + " ] of class source [ " + getCanonicalName() + " ]", e);
                }
                return true;
            }

            @Override
            public boolean doProcess(String className) {
                return true;
            }

            // Unused

            public boolean supportsJandex() {
                return false; // Unused
            }

            @Override
            public boolean createJandex() {
                return false; // Unused
            }

            @Override
            public void storeJandex(Object jandexIndex) {
                // Unused
            }

            @Override
            public void processJandex() {
                // Unused
            }

            @Override
            public boolean processJandex(Object classInfo) throws ClassSource_Exception {
                return false; // Unused
            }

            @Override
            public boolean processSparseJandex(Object sparseClassInfo) throws ClassSource_Exception {
                return false; // Unused
            }

            // Obsolete

            @Override
            public boolean doProcess(String className, ScanPolicy scanPolicy) {
                return false;
            }

            @Override
            public boolean process(String sourceName, String className, InputStream inputStream,
                ScanPolicy scanPolicy) throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
                return false;
            }

            @Override
            public boolean process(String sourceName, Object jandexClassInfo, ScanPolicy scanPolicy)
                throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
                return false;
            }
        };

        @SuppressWarnings("unused")
        int classCount = processFromScratch(jandexStreamer);

        return indexer.complete();
    }

    //

    /**
     * Attempt to read the Jandex index.
     *
     * If Jandex is not enabled, immediately answer null.
     *
     * If no Jandex index is available, or if it cannot be read, answer null.
     *
     * @return The read Jandex index.
     */
    protected Index getJandexIndex() {
        String methodName = "getJandexIndex";

        long startTime = System.nanoTime();
        Index jandexIndex = basicGetJandexIndex();
        long readTime = System.nanoTime() - startTime;
        if ( jandexIndex != null ) {
            setProcessTime(readTime);
            setProcessCount(jandexIndex.getKnownClasses().size());

            // System.out.println("Jandex read [ " + readTime + " ]");
        }

        boolean doLog = logger.isLoggable(Level.FINER);
        boolean doJandexLog = jandexLogger.isLoggable(Level.FINER);
        if ( doLog || doJandexLog ) {
            String msg;
            if ( jandexIndex != null ) {
                msg = MessageFormat.format("[ {0} ] Index [ {1} ] found [ {2} (ms) ]",
                    getHashText(), getJandexIndexPath(), Long.valueOf(readTime / NS_IN_MS));
            } else {
                msg = MessageFormat.format("[ {0} ] Index [ {1} ] not found",
                    getHashText(), getJandexIndexPath());
            }
            if ( doLog ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, msg);
            }
            if ( doJandexLog ) {
                jandexLogger.logp(Level.FINER, CLASS_NAME,  methodName, msg);
            }
        }

        return jandexIndex;
    }

    /**
     * <p>Answer the JANDEX index for this class source.  Answer null if none
     * is available.</p>
     *
     * @return The JANDEX index for this class source.  This default implementation
     *     always answers null.
     */
    protected Index basicGetJandexIndex() {
        return null;
    }

    /**
     * <p>Tell if a Jandex index is available.</p>
     *
     * @return Whether a Jandex index is available.  This implementation always
     *     answers false.
     */
    @Trivial
    protected boolean basicHasJandexIndex() {
        return false;
    }

    protected boolean processedUsingJandex;

    @Trivial
    @Override
    public boolean isProcessedUsingJandex() {
        return processedUsingJandex;
    }

    protected boolean readFromCache;

    @Override
    public boolean isReadFromCache() {
        return readFromCache;
    }

    /**
     * Set that this class source was processed using cache data.
     *
     * @param processTime The time spent reading cache data.
     * @param processCount The count of classes read from the cache.
     */
    @Override
    public void setReadFromCache(long processTime, int processCount) {
        this.readFromCache = true;

        this.processTime = processTime;
        this.processCount = processCount;
    }

    //

    protected long processTime;

    @Trivial
    @Override
    public long getProcessTime() {
        return processTime;
    }

    protected void setProcessTime(long processTime) {
        this.processTime = processTime;
    }

    protected long addProcessTime(long addProcessTime) {
        this.processTime += addProcessTime;
        return this.processTime;
    }

    protected int processCount;

    @Trivial
    @Override
    public int getProcessCount() {
        return processCount;
    }

    @Trivial
    protected void setProcessCount(int processCount) {
        this.processCount = processCount;
        // (new Throwable("Process count [ " + processCount + " ] [ " + getName() + " ] [ " + getClass().getName() + " ]")).printStackTrace(System.out);
    }

    //


//  } else if ( processJandexFull(streamer) ) {
//      processedUsingJandex = true;
//      if ( logger.isLoggable(Level.FINER) ) {
//          logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER / RETURN [ true ]: using full index");
//      }
//      return true;

    /**
     * <p>Attempt to process this class source using cache data.</p>
     *
     * @param streamer The streamer used to process the class source.
     *
     * @return True or false telling if the class was successfully
     *     processed using cache data.
     */
    protected boolean processUsingJandex(ClassSource_Streamer streamer) {
        String methodName = "processUsingJandex";

        if ( streamer == null ) {
            return false;
        }

        if ( getUseJandex() ) {
            if ( processJandexSparse(streamer) ) {
                processedUsingJandex = true;
                return true;
            } else {
                return false;
            }

        } else {
            boolean doLog = logger.isLoggable(Level.FINER);
            boolean doJandexLog = jandexLogger.isLoggable(Level.FINER);
            if ( doLog || doJandexLog ) {
                if ( basicHasJandexIndex() ) {
                    String msg = MessageFormat.format(
                        "[ {0} ] Jandex disabled; Jandex index [ {1} ] found",
                        getHashText(), getJandexIndexPath());
                    if ( doLog ) {
                        logger.logp(Level.FINER, CLASS_NAME,  methodName, msg);
                    }
                    if ( doJandexLog ) {
                        jandexLogger.logp(Level.FINER, CLASS_NAME,  methodName, msg);
                    }
                }
            }
            return false;
        }
    }

    @Trivial
    protected boolean processJandexFull(ClassSource_Streamer streamer) {
        String methodName = "processJandexFull";

        if ( !getUseJandex() ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER / RETURN [ false ]: jandex is not enabled");
            }
            return false;
        }

        Index index = getJandexIndex();
        if ( index == null ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER / RETURN [ false ]: no index or read failure");
            }
            return false;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER Classes [ {0} ]",
                Integer.valueOf(index.getKnownClasses().size()));
        }

        long startTime = System.nanoTime();

        streamer.processJandex(); // Mark to the streamer that Jandex was used.

        for ( ClassInfo classInfo : index.getKnownClasses() ) {
            DotName classDotName = classInfo.name();
            String className = classDotName.toString();

            if ( streamer.doProcess(className) ) {
                try {
                    @SuppressWarnings("unused")
                    boolean didProcess = streamer.processJandex(classInfo);
                } catch ( ClassSource_Exception e ) {
                    // ANNO_CLASSSOURCE_JANDEX_SCAN_EXCEPTION=CWWKC0077W:
                    // [ {0} ] The processing of Jandex information for class [{1}] caused an exception.
                    // Error message: {2}
                    logger.logp(Level.WARNING, CLASS_NAME, methodName,
                        "ANNO_CLASSSOURCE_JANDEX_SCAN_EXCEPTION",
                        new Object[] { getHashText(), className, e });
                }
            }
        }

        @SuppressWarnings("unused")
        long addedTime = addProcessTime( System.nanoTime() - startTime );

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "RETURN [ true ]");
        }
        return true;
    }

    protected boolean processJandexSparse(ClassSource_Streamer streamer) {
        String methodName = "processJandexSparse";

        SparseIndex sparseIndex = getSparseJandexIndex();
        if ( sparseIndex == null ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER / RETURN [ false ]: no index or read failure");
            }
            return false;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER Classes [ {0} ]",
                Integer.valueOf(sparseIndex.getKnownClasses().size()));
        }

        long startTime = System.nanoTime();

        streamer.processJandex(); // Mark to the streamer that Jandex was used.

        for ( SparseClassInfo classInfo : sparseIndex.getKnownClasses() ) {
            com.ibm.ws.annocache.jandex.internal.SparseDotName classDotName = classInfo.name();
            String className = classDotName.toString();

            if ( streamer.doProcess(className) ) {
                try {
                    @SuppressWarnings("unused")
                    boolean didProcess = streamer.processSparseJandex(classInfo);
                } catch ( ClassSource_Exception e ) {
                    // ANNO_CLASSSOURCE_JANDEX_SCAN_EXCEPTION=CWWKC0077W:
                    // [ {0} ] The processing of Jandex information for class [{1}] caused an exception.
                    // Error message: {2}
                    logger.logp(Level.WARNING, CLASS_NAME, methodName,
                        "ANNO_CLASSSOURCE_JANDEX_SCAN_EXCEPTION",
                        new Object[] { getHashText(), className, e });
                }
            }
        }

        @SuppressWarnings("unused")
        long addedTime = addProcessTime( System.nanoTime() - startTime );

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "RETURN [ true ]");
        }
        return true;
    }

    //

    @Override
    public abstract void processSpecific(ClassSource_Streamer streamer, Set<String> i_classNames)
        throws ClassSource_Exception;
}
