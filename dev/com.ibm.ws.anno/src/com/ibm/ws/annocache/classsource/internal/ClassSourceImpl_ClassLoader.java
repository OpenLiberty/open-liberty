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

package com.ibm.ws.annocache.classsource.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Set;
import java.util.concurrent.Future;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts.ResultField;
import com.ibm.wsspi.annocache.classsource.ClassSource;
import com.ibm.wsspi.annocache.classsource.ClassSource_ClassLoader;
import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.classsource.ClassSource_Streamer;
import com.ibm.wsspi.annocache.util.Util_InternMap;

public class ClassSourceImpl_ClassLoader
    extends ClassSourceImpl
    implements ClassSource_ClassLoader {

    @SuppressWarnings("hiding")
    public static final String CLASS_NAME = ClassSourceImpl_ClassLoader.class.getSimpleName();

    // Top O' the world

    public ClassSourceImpl_ClassLoader(
        ClassSourceImpl_Factory factory,
        Util_InternMap internMap,
        String name,
        ClassLoader classLoader) {

        super(factory, internMap, NO_ENTRY_PREFIX, name, String.valueOf(classLoader));
        // throws ClassSource_Exception

        String methodName = "<init>";

        this.classLoader = classLoader;
        this.classLoaderThunk = null;

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, this.hashText);
        }
    }

    public ClassSourceImpl_ClassLoader(ClassSourceImpl_Factory factory,
                                       Util_InternMap internMap,
                                       String name,
                                       Future<ClassLoader> classLoaderThunk) {

        super(factory, internMap, NO_ENTRY_PREFIX, name, String.valueOf(classLoaderThunk));

        String methodName = "<init>";

        this.classLoader = null;
        this.classLoaderThunk = classLoaderThunk;
        
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, this.hashText);
        }
    }

    //

    /**
     * <p>Compute and return a time stamp for this class source.</p>
     *
     * <p>Time stamps are not available for class loader class sources:
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

    /**
     * <p>Open this class source.  This implementation does nothing.</p>
     * 
     * @throws ClassSource_Exception Thrown if the open failed.
     */
    @Override
    @Trivial
    public void open() throws ClassSource_Exception {
        String methodName = "open";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER/RETURN", getHashText());
        }
    }

    /**
     * <p>Close this class source.  This implementation does nothing.</p>
     * 
     * @throws ClassSource_Exception Thrown if the close failed.
     */
    @Override
    @Trivial
    public void close() throws ClassSource_Exception {
        String methodName = "close";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER/RETURN", getHashText());
        }
    }

    //

    protected volatile ClassLoader classLoader;

    protected final Future<ClassLoader> classLoaderThunk;

    @Override
    @Trivial
    public ClassLoader getClassLoader() {
        String methodName = "getClassLoader";

        if ( classLoader == null ) {
            synchronized(this) {
                if ( classLoader == null ) {
                    try {
                        classLoader = classLoaderThunk.get();
                        // throws InterruptedException, ExecutionException
                    } catch ( Exception e ) {
                        // Should never happen!
                    }
                    if ( logger.isLoggable(Level.FINER) ) {
                        logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Resolved [ {1} ]",
                            new Object[] { getHashText(), classLoader });
                    }
                }
            }
        }
        return classLoader;
    }

    //

    @Override
    public InputStream openResourceStream(String className, String resourceName)
        throws ClassSource_Exception {
        String methodName = "openResourceStream";

        ClassLoader useClassLoader = getClassLoader();

        URL url = useClassLoader.getResource(resourceName);
        if ( url == null ) {
            return null;
        }

        try {
            return url.openStream(); // throws IOException

        } catch ( IOException e ) {
            // defect 84235:we are generating multiple Warning/Error messages for each error due to each level reporting them.
            // Disable the following warning and defer message generation to a higher level,
            // preferably the ultimate consumer of the exception.
            // logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_CLASSSOURCE_OPEN1_EXCEPTION",
            //     new Object[] { getHashText(), resourceName, className });

            String eMsg =
                "[ " + getHashText() + " ]" +
                " Failed to open resource [ " + resourceName + " ]" +
                " for class [ " + className + " ]";
            throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, e);
        }
    }

    @Override
    public void closeResourceStream(String className, String resourceName, InputStream inputStream) {
        String methodName = "closeResourceStream";

        try {
            inputStream.close(); // throws IOException

        } catch ( IOException e ) {
            // String eMsg = "[ " + getHashText() + " ]" +
            //               " Failed to close resource [ " + resourceName + " ]" +
            //               " for class [ " + className + " ]";
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_CLASSSOURCE_CLOSE2_EXCEPTION",
                new Object[] { getHashText(), resourceName, className });
        }
    }

    //

    @Override
    public void process(ClassSource_Streamer streamer) throws ClassSource_Exception {
        throw new UnsupportedOperationException("ClassLoader; no class streaming");
    }

    @Override
    public int processFromScratch(ClassSource_Streamer streamer) throws ClassSource_Exception {
        throw new UnsupportedOperationException("ClasLloader; no class streaming");
    }

    @Override
    protected boolean processUsingJandex(ClassSource_Streamer streamer) {
        throw new UnsupportedOperationException("ClassLoader; no class streaming");
    }

    @Override
    @Trivial
    public void processSpecific(ClassSource_Streamer streamer, Set<String> i_classNames) throws ClassSource_Exception {
        String methodName = "processSpecific";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] ENTRY [ {1} ]",
                new Object[] { getHashText(), Integer.valueOf(i_classNames.size()) });
        }

        long scanStart = System.nanoTime();

        for ( String i_className : i_classNames ) {
            String resourceName = getResourceNameFromClassName(i_className);
            try {
                scan(streamer, i_className, resourceName); // throws ClassSource_Exception
            } catch ( ClassSource_Exception e ) {
                logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_SCAN_EXCEPTION", e);
            }
        }

        long scanTime = System.nanoTime() - scanStart;

        setProcessTime(scanTime);
        setProcessCount( i_classNames.size() );

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,  "[ {0} ] RETURN", getHashText());
        }
    }

    //

    @Trivial
    protected void scan(ClassSource_Streamer streamer, String i_className, String resourceName)
        throws ClassSource_Exception {

        String methodName = "scan";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Scan [ {1} ] as [ {2} ]",
                new Object[] { getHashText(), i_className, resourceName });
        }

        InputStream inputStream = openClassResourceStream(i_className, resourceName); // throws ClassSource_Exception
        if ( inputStream == null ) {
            return;
        }

        inputStream = new BufferedInputStream(inputStream, CLASS_BUFFER_SIZE);

        try {
            streamer.process(i_className, inputStream); // throws ClassSource_Exception

        } finally {
            closeResourceStream(i_className, resourceName, inputStream);
        }
    }

    //

    @Override
    @Trivial
    public void log(Logger useLogger) {
        String methodName = "log";

        if ( useLogger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "Class Source [ {0} ]", getHashText());
        }
    }

    // Obsolete ..

    @Override
    @Trivial
    public void log(TraceComponent tc) {
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, MessageFormat.format("Class Source [ {0} ]", getHashText()));
        }
    }

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
    public String inconvertResourceName(String externalResourceName) {
        return null;
    }

    @Override
    public String outconvertResourceName(String internalResourceName) {
        return null;
    }

    @Override
    public InputStream openClassStream(String className) throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
        return null;
    }

    @Override
    public void closeClassStream(String className, InputStream inputStream)
        throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
        // EMPTY
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
}
