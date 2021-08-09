/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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
import java.text.MessageFormat;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.jboss.jandex.Index;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.jandex.internal.Jandex_Utils;
import com.ibm.ws.annocache.jandex.internal.SparseIndex;
import com.ibm.ws.annocache.util.internal.UtilImpl_FileStamp;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.FastModeControl;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts.ResultField;
import com.ibm.wsspi.annocache.classsource.ClassSource;
import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.classsource.ClassSource_MappedContainer;
import com.ibm.wsspi.annocache.classsource.ClassSource_Streamer;
import com.ibm.wsspi.annocache.util.Util_InternMap;

public class ClassSourceImpl_MappedContainer
    extends ClassSourceImpl implements ClassSource_MappedContainer {

    @SuppressWarnings("hiding")
    public static final String CLASS_NAME = ClassSourceImpl_MappedContainer.class.getSimpleName();

    // Top O' the world

    public ClassSourceImpl_MappedContainer(
        ClassSourceImpl_Factory factory, Util_InternMap internMap,
        String name,
        Container container,
        URLConverter bundleEntryUrlConverter) throws ClassSource_Exception {

        this(factory, internMap, name, container, NO_ENTRY_PREFIX, bundleEntryUrlConverter); // throws ClassSource_Exception
    }

    @SuppressWarnings("unused")
	public ClassSourceImpl_MappedContainer(
        ClassSourceImpl_Factory factory, Util_InternMap internMap,
        String name,
        Container container, String entryPrefix, URLConverter bundleEntryUrlConverter) throws ClassSource_Exception {

        super(factory, internMap, entryPrefix, name, String.valueOf(container));
        // throws ClassSource_Exception

        this.bundleEntryUrlConverter = bundleEntryUrlConverter;
        this.rootContainer = container;
        this.container = navigateFrom(container, entryPrefix);
    }

    private Container navigateFrom(Container useContainer, String prefix) {
        String methodName = "navigateFrom";

        if ( prefix == null ) {
            return useContainer;
        }

        Entry prefixEntry = useContainer.getEntry(prefix);
        if ( prefixEntry == null ) {
            return null;
        }
        
        try {
            return prefixEntry.adapt(Container.class);
        } catch ( UnableToAdaptException e ) {
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_CLASSSOURCE_ADAPT_EXCEPTION",
                new Object[] { getHashText(), prefixEntry.getName(), prefixEntry, useContainer, prefix });
            return null;
        }
    }

    //

    /**
     * <p>Open this class source.</p>
     *
     * @throws ClassSource_Exception Thrown if the open failed.
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
            String eMsg =
                "[ " + getHashText() + " ]" +
                " Failed to adapt [ " + getCanonicalName() + " ]" +
                " to [ " + FastModeControl.class.getName() + " ]";
            throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, e);
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER/RETURN", getHashText());
        }
    }

    /**
     * <p>Close this class source.</p>
     *
     * @throws ClassSource_Exception Thrown if the close failed.
     */
    @Override
    @Trivial
    @FFDCIgnore({ UnableToAdaptException.class })
    public void close() throws ClassSource_Exception {
        String methodName = "close";
        try {
            FastModeControl fastMode = getContainer().adapt(FastModeControl.class);
            // 'adapt' throws UnableToAdaptException
            fastMode.stopUsingFastMode();

        } catch ( UnableToAdaptException e ) {
            String eMsg =
                "[ " + getHashText() + " ]" +
                " Failed to close [ " + getCanonicalName() + " ]" +
                " to [ " + FastModeControl.class.getName() + " ]";
            throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, e);
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER/RETURN", getHashText());
        }
    }

    //

    protected final Container rootContainer;

    @Trivial
    public Container getRootContainer() {
        return rootContainer;
    }

    protected final Container container;
    private final URLConverter bundleEntryUrlConverter;

    @Override
    @Trivial
    public Container getContainer() {
        return container;
    }

    //

    @Override
    public int processFromScratch(ClassSource_Streamer streamer) throws ClassSource_Exception {
        Container useContainer = getContainer();
        if ( useContainer != null ) {
            return processContainer(EMPTY_PREFIX, useContainer, streamer);
        } else {
            // System.out.println("Strange: Null container!");
            return 0;
        }
    }

    //

    /**
     * <p>Answer a time stamp for the mapped container.</p>
     *
     * <p>See {@link UtilImpl_FileStamp#computeStamp(Container)}.</p>
     *
     * @return The time stamp of the mapped container.
     */
    @Override
    protected String computeStamp() {
        String methodName = "computeStamp";

        Container useRootContainer = getRootContainer();

        String useStamp = UtilImpl_FileStamp.computeStamp(bundleEntryUrlConverter, useRootContainer);
        if ( useStamp == null ) {
            useStamp = ClassSource.UNAVAILABLE_STAMP;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                MessageFormat.format("[ {0} ] Container [ {1} ] Stamp [ {2} ]", getHashText(), useRootContainer, useStamp));
        }
        return useStamp;
    }

    //

    public static final String EMPTY_PREFIX = "";

    @Trivial
    protected int processContainer(
        String prefix, Container targetContainer,
        ClassSource_Streamer streamer) throws ClassSource_Exception {

        String methodName = "processContainer";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER [ {1} ] of [ {2} ]",
                new Object[] { getHashText(), prefix, targetContainer.getName() });
        }

        int classCount = 0;

        int initialResources = getInternMap().getSize();

        for ( Entry nextEntry : targetContainer ) {
            String nextChildName = nextEntry.getName();
            String nextPrefix = resourceAppend(prefix, nextChildName);

            Container nextChildContainer;
            try {
                nextChildContainer = nextEntry.adapt(Container.class);

            } catch ( Throwable th ) {
                nextChildContainer = null;

                // String eMsg = "[ " + getHashText() + " ]" +
                //               " Failed to adapt [ " + nextChildName + " ]" +
                //               " as [ " + nextEntry + " ]" +
                //               " under root [ " + targetContainer + " ]" +
                //               " for prefix [ " + prefix + " ]";
                logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_CLASSSOURCE_ADAPT_EXCEPTION",
                    new Object[] { getHashText(), nextChildName, nextEntry, targetContainer, prefix });
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
                    // Ignore
                } else {
                    classCount += processContainer(nextPrefix, nextChildContainer, streamer);
                    // throws ClassSource_Exception
                }

            } else {
                if ( !isClassResource(nextPrefix) ) {
                    // Ignore
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
                    if ( ClassSourceImpl.isJava9PackageName(nextClassName) ) {
                        logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Java9 class name [ {1} ]", 
                            new Object[] { getHashText(), nextClassName });
                        continue;
                    }

                    String i_nextClassName = internClassName(nextClassName);

                    try {
                        @SuppressWarnings("unused")
                        boolean didProcess = processEntry(streamer, i_nextClassName, nextPrefix, nextEntry);
                    } catch ( ClassSource_Exception e ) {
                        // CWWKC0068W: An exception occurred while processing class [ {0} ] in container [ {1} ]
                        // identified by [ {2} ]. The exception was {3}.
                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_SCAN_EXCEPTION",
                            new Object[] { nextClassName, targetContainer, nextPrefix, e });
                    }

                    classCount++;
                }
            }
        }

        int finalResources = getInternMap().getSize();

        if ( logger.isLoggable(Level.FINER) ) {
            Object[] logParms = new Object[] { getHashText(), null, null };

            logParms[1] = Integer.valueOf(finalResources - initialResources);
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN [ {1} ] Added classes", logParms);
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN [ {1} ]",
                new Object[] { getHashText(), Integer.valueOf(classCount) });
        }
        
        return classCount;
    }

    protected boolean processEntry(
        ClassSource_Streamer streamer,
        String i_className, String resourceName, Entry entry) throws ClassSource_Exception {

        if ( streamer == null ) {
            return true;
        } else if ( !streamer.doProcess(i_className) ) {
            return false;
        }

        InputStream inputStream = openResourceStream(
            i_className, resourceName,
            getContainer(), entry ); // throws ClassSource_Exception

        try {
            streamer.process(i_className, inputStream); // 'process' throws ClassSourceException
        } finally {
            closeResourceStream(i_className, resourceName, entry, inputStream);
        }

        return true;
    }

    @Override
    @Trivial
    public void processSpecific(ClassSource_Streamer streamer, Set<String> i_classNames)
        throws ClassSource_Exception {

        String methodName = "processSpecific";

    	boolean doLog = logger.isLoggable(Level.FINER);
        if ( doLog ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] [ {1} ]",
                new Object[] { getHashText(), Integer.valueOf(i_classNames.size()) });
        }

        Container useContainer = getContainer();
        if ( useContainer == null ) {
            if ( doLog ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN", getHashText());
            }
            return;
        }

        long scanStart = System.nanoTime();

        for ( String i_className : i_classNames ) {
            String resourceName = getResourceNameFromClassName(i_className);

            Entry nextEntry;
            try {
                nextEntry = useContainer.getEntry(resourceName);
            } catch ( Throwable th ) {
                nextEntry = null;
                // CWWKC0044W: An exception occurred while scanning class and annotation data.
                logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_SCAN_EXCEPTION", th);
            }

            if ( nextEntry == null ) {
                if ( doLog ) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "Skip [ {0} ] as [ {1} ]",
                        new Object[] { i_className, resourceName });
                }
                continue;
            }

            try {
                processEntry(streamer, i_className, resourceName, nextEntry);
            } catch ( ClassSource_Exception e ) {
                // CWWKC0044W: An exception occurred while scanning class and annotation data.
                logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_SCAN_EXCEPTION", e);
            }
        }

        long scanTime = System.nanoTime() - scanStart;

        setProcessTime(scanTime);
        setProcessCount( i_classNames.size() );

        if ( doLog ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN", getHashText());
        }
    }

    //

    @Override
    protected boolean basicHasJandexIndex() {
        return ( getRootContainer().getEntry( getJandexIndexPath() ) != null );
    }

    @SuppressWarnings("deprecation")
    @Override
    @Trivial
    protected Index basicGetJandexIndex() {
        String methodName = "basicGetJandexIndex";

        String useJandexIndexPath = getJandexIndexPath();

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Looking for JANDEX [ {1} ] in [ {2} ]",
                    new Object[] {  getHashText(), useJandexIndexPath, getContainer().getPhysicalPath() } );
        }

        InputStream jandexStream;

        try {
            jandexStream = openRootResourceStream(null, useJandexIndexPath, JANDEX_BUFFER_SIZE); // throws ClassSource_Exception
        } catch ( ClassSource_Exception e ) {
            // CWWKC0066E: An exception occurred while attempting to open Jandex index file [ {0} ].
            // The identifier for the class source is [ {1} ].
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "JANDEX_INDEX_OPEN_EXCEPTION",
                new Object[] { useJandexIndexPath, getCanonicalName() });
            return null;
        }

        if ( jandexStream == null ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] No JANDEX index was found", getHashText());
            }
            return null;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Located JANDEX index", getHashText());
        }

        try {
            Index jandexIndex = Jandex_Utils.basicReadIndex(jandexStream); // throws IOException
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Read JANDEX index [ {1} ] from [ {2} ]: Classes [ {3} ]",
                    new Object[] {  getHashText(),
                                    useJandexIndexPath,
                                    getCanonicalName(),
                                    Integer.toString(jandexIndex.getKnownClasses().size()) } );
            }
            return jandexIndex;

        } catch ( Throwable th ) {
            // CWWKC0067E: An exception occurred while reading Jandex index file [ {0} ] from resource [ {1} ].
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "JANDEX_INDEX_READ_EXCEPTION",
                new Object[] { useJandexIndexPath, getCanonicalName() });
            return null;

        } finally {
            closeRootResourceStream(null,  useJandexIndexPath, jandexStream);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected SparseIndex basicGetSparseJandexIndex() {
        String methodName = "basicGetSparseJandexIndex";

        String useJandexIndexPath = getJandexIndexPath();
    
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Looking for JANDEX [ {1} ] in [ {2} ]",
                    new Object[] {  getHashText(), useJandexIndexPath, getRootContainer().getPhysicalPath() } );
        }

        InputStream jandexStream;
        try {
            jandexStream = openRootResourceStream(null, useJandexIndexPath, JANDEX_BUFFER_SIZE); // throws ClassSource_Exception
        } catch ( ClassSource_Exception e ) {
            // CWWKC0067E: An exception occurred while reading Jandex index file [ {0} ] from resource [ {1} ].
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "JANDEX_INDEX_READ_EXCEPTION",
                new Object[] { useJandexIndexPath, getCanonicalName() });
            return null;
        }
    
        if ( jandexStream == null ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] No JANDEX index was found", getHashText());
            }
            return null;
        }
    
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Located JANDEX index", getHashText());
        }

        try {
            SparseIndex jandexIndex = Jandex_Utils.basicReadSparseIndex(jandexStream); // throws IOException
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Read JANDEX index [ {1} ] from [ {2} ]: Classes [ {3} ]",
                    new Object[] {  getHashText(),
                                    useJandexIndexPath,
                                    getCanonicalName(),
                                    Integer.toString(jandexIndex.getKnownClasses().size()) } );
            }
            return jandexIndex;
    
        } catch ( Throwable th ) {
            // CWWKC0067E: An exception occurred while reading Jandex index file [ {0} ] from resource [ {1} ].
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "JANDEX_INDEX_READ_EXCEPTION",
                new Object[] { useJandexIndexPath, getCanonicalName() });
            return null;
    
        } finally {
            closeRootResourceStream(null,  useJandexIndexPath, jandexStream);
        }
    }

    //

    public InputStream openRootResourceStream(String className, String resourceName, int bufferSize)
        throws ClassSource_Exception {

        InputStream inputStream = openRootResourceStream(className, resourceName);
        return ( (inputStream == null) ? null : new BufferedInputStream(inputStream, bufferSize) );
    }

    public InputStream openRootResourceStream(String className, String resourceName)
        throws ClassSource_Exception {

        Container useContainer = getRootContainer();

        Entry entry = useContainer.getEntry(resourceName);
        if ( entry == null ) {
            return null;
        }

        return openResourceStream(className, resourceName, useContainer, entry);
        // throws ClassSource_Exception
    }

    @Override
    public InputStream openResourceStream(String className, String resourceName)
        throws ClassSource_Exception {

        Container useContainer = getContainer();
        if ( useContainer == null ) {
            return null;
        }

        Entry entry = useContainer.getEntry(resourceName);
        if ( entry == null ) {
            return null;
        }

        return openResourceStream(className, resourceName, useContainer, entry);
        // throws ClassSource_Exception
    }

    public InputStream openResourceStream(
        String className, String resourceName,
        Container useContainer, Entry entry)
        throws ClassSource_Exception {

        String methodName = "openResourceStream";

        InputStream result;

        try {
            result = entry.adapt(InputStream.class);

        } catch ( Throwable th ) {
            // defect 84235:we are generating multiple Warning/Error messages for each error due
            // to each level reporting them.
            // Disable the following warning and defer message generation to a higher level,
            // preferably the ultimate consumer of the exception.
            // logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_CLASSSOURCE_OPEN2_EXCEPTION",
            //     new Object[] { getHashText(), resourceName, entry, getContainer(), className });

            String eMsg = "[ " + getHashText() + " ]" +
                          " Failed to open [ " + resourceName + " ]" +
                          " as [ " + entry + " ]" +
                          " under [ " + useContainer + " ]";
            if ( className != null ) {
                eMsg += " for class [ " + className + " ]";
            }
            throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, th);
        }

        if ( result == null ) {
            // defect 84235:we are generating multiple Warning/Error messages for each
            // error due to each level reporting them.
            // Disable the following warning and defer message generation to a higher level,
            // preferably the ultimate consumer of the exception.
            // logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_CLASSSOURCE_OPEN2_EXCEPTION",
            //     new Object[] { getHashText(), resourceName, entry, getContainer(), className });

            String eMsg = "[ " + getHashText() + " ]" +
                          " Failed to open [ " + resourceName + " ]" +
                          " as [ " + entry + " ]" +
                          " under [ " + useContainer + " ]";
            if ( className != null ) {
                eMsg += " for class [ " + className + " ]";
            }
            throw getFactory().newClassSourceException(eMsg);
        }

        return result;
    }

    @Override
    public void closeResourceStream(String className, String resourceName, InputStream inputStream) {
        String methodName = "closeResourceStream";

        Entry entry = getContainer().getEntry(resourceName);
        if ( entry == null ) {
            // String eMsg = "[ " + getHashText() + " ]" +
            //               " Failed to locate entry [ " + resourceName + " ]" +
            //               " under root [ " + getContainer() + " ]" +
            //               " for class [ " + className + " ]";
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_CLASSSOURCE_RESOURCE_NOTFOUND",
                new Object[] { getHashText(), resourceName, getRootContainer(), className });
        } else {
            closeResourceStream(className, resourceName, entry, inputStream);
        }
    }

    protected void closeResourceStream(
        String className, String resourceName,
        Entry entry, InputStream inputStream) {

        String methodName = "closeResourceStream";

        try {
            inputStream.close(); // throws IOException

        } catch ( IOException e ) {
            // TODO:   pull from properties file
            String eMsg =
                "[ " + getHashText() + " ]" +
                " Failed to close [ " + resourceName + " ]" +
                " in [ " + getCanonicalName() + " ]";
            if ( className != null ) {
                eMsg += " for class [ " + className + " ]";
            }
            logger.logp(Level.WARNING, CLASS_NAME, methodName, eMsg);
        }
    }

    public void closeRootResourceStream(String className, String resourceName, InputStream inputStream) {
        String methodName = "closeResourceStream";

        Entry entry = getRootContainer().getEntry(resourceName);
        if ( entry == null ) {
            // String eMsg = "[ " + getHashText() + " ]" +
            //               " Failed to locate entry [ " + resourceName + " ]" +
            //               " under root [ " + getContainer() + " ]" +
            //               " for class [ " + className + " ]";
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_CLASSSOURCE_RESOURCE_NOTFOUND",
                new Object[] { getHashText(), resourceName, getRootContainer(), className });
        } else {
            closeResourceStream(className, resourceName, entry, inputStream);
        }
    }

    //

    @Override
    @Trivial
    public void log(Logger useLogger) {
        String methodName = "log";
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class Source [ {0} ]", getHashText());
    }

    // Obsolete ...

    @Override
    @Trivial
    public void log(TraceComponent tc) {
        Tr.debug(tc, MessageFormat.format("Class Source [ {0} ]", getHashText()));
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
