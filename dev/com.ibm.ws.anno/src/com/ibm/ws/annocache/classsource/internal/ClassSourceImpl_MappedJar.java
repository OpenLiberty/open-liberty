/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
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
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.jandex.Index;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.jandex.internal.Jandex_Utils;
import com.ibm.ws.annocache.jandex.internal.SparseIndex;
import com.ibm.ws.annocache.util.internal.UtilImpl_FileStamp;
import com.ibm.ws.annocache.util.internal.UtilImpl_FileUtils;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts.ResultField;
import com.ibm.wsspi.annocache.classsource.ClassSource;
import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.classsource.ClassSource_MappedJar;
import com.ibm.wsspi.annocache.classsource.ClassSource_Streamer;
import com.ibm.wsspi.annocache.util.Util_InternMap;

public class ClassSourceImpl_MappedJar
    extends ClassSourceImpl implements ClassSource_MappedJar {

    @SuppressWarnings("hiding")
    public static final String CLASS_NAME = ClassSourceImpl_MappedJar.class.getSimpleName();

    // Top O' the world

    public ClassSourceImpl_MappedJar(
            ClassSourceImpl_Factory factory, Util_InternMap internMap,
            String name,
            String jarPath) {
        this(factory, internMap, name, jarPath, NO_ENTRY_PREFIX);
         // throws ClassSource_Exception
    }

    public ClassSourceImpl_MappedJar(
        ClassSourceImpl_Factory factory, Util_InternMap internMap,
        String name,
        String jarPath, String entryPrefix) {

        super(factory, internMap, entryPrefix, name, jarPath);
        // throws ClassSource_Exception

        String methodName = "<init>";

        this.jarPath = jarPath;
        this.absJarPath = getAbsolutePath(jarPath);
        this.rawJarFile = new File(jarPath);

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] [ {1} ] Exists [ {2} ] IsDir [ {3} ]",
                new Object[] {
                    this.hashText, this.jarPath,
                    Boolean.valueOf(this.rawJarFile.exists()),
                    Boolean.valueOf(this.rawJarFile.isFile()) });
        }
    }

    //

    /**
     * <p>Answer a time stamp for the mapped JAR file.</p>
     *
     * <p>Answer the last modified time of the mapped jar.
     * See {@link File#lastModified()}.</p>
     *
     * <p>From {@link File#lastModified()}:</p>
     *
     * "A <code>long</code> value representing the time the file was
     * last modified, measured in milliseconds since the epoch
     * (00:00:00 GMT, January 1, 1970), or <code>0L</code> if the
     * file does not exist or if an I/O error occurs."
     *
     * @return The time stamp of the mapped JAR.
     */
    @Override
    protected String computeStamp() {
        String methodName = "computeStamp";

        File useJarFile = getRawJarFile();
    
        String useStamp = UtilImpl_FileStamp.computeStamp(useJarFile);
        if ( useStamp == null ) {
            useStamp = ClassSource.UNAVAILABLE_STAMP;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                MessageFormat.format("[ {0} ] File [ {1} ] Stamp [ {2} ]", getHashText(), useJarFile, useStamp));
        }
        return useStamp;
    }

    /*
     * Counter to keep track of the number of active users. Incremented for each 'open' and decremented for each
     * 'close'. The underlying ZipFile will be closed when the count goes to 0.
     */
    protected int opens;

    /*
     * Open this class source for processing. If this is the first open,
     * the underlying jar file will be opened.
     * 
     * @throws ClassSourceException Thrown if the JAR file could not be opened.
     */
    @Override
    public void open() throws ClassSource_Exception {
        String methodName = "open";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER [ {1} ]",
                new Object[] { getHashText(), Integer.valueOf(opens) });
        }

        if ( opens == 0 ) {
            try {
                jarFile = UtilImpl_FileUtils.createJarFile(jarPath); // throws IOException
            } catch ( IOException e ) {
                String eMsg = "[ " + getHashText() + " ] Failed to open [ " + getAbsJarPath() + " ]";
                throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, e);
            }
        }

        opens++;

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN [ {1} ]",
                new Object[] { getHashText(), Integer.valueOf(opens) });
        }
    }

    /*
     * Close this class source. If this is the last open,
     * the underlying jar file will be closed.
     * 
     * @throws ClassSourceException Thrown if the JAR file could not be opened.
     */
    @Override
    public void close() throws ClassSource_Exception {
        String methodName = "close";
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER [ {1} ]",
                new Object[] { getHashText(), Integer.valueOf(opens) });
        }

        if ( opens != 0 ) {
            opens--;
            
            if ( opens == 0 ) {
                JarFile useJarFile = jarFile;
                jarFile = null;

                try {
                    useJarFile.close(); // throws IOException
                } catch ( IOException e ) {
                    String eMsg = "[ " + getHashText() + " ] Failed to close [ " + getAbsJarPath() + " ]";
                    throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, e);
                }
            }
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN [ {1} ]",
                new Object[] { getHashText(), Integer.valueOf(opens) });
        }
    }

    //

    protected final String jarPath;
    protected final String absJarPath;
    protected final File rawJarFile;

    @Override
    public String getJarPath() {
        return jarPath;
    }

    @Trivial
    public String getAbsJarPath() {
        return absJarPath;
    }

    public File getRawJarFile() {
        return rawJarFile;
    }

    //

    protected JarFile jarFile;

    public JarFile getJarFile() {
        return jarFile;
    }

    //

    protected boolean hasPrefix(String resourceName) {
        String useEntryPrefix = getEntryPrefix();
        if ( useEntryPrefix == null ) {
            return true;
        }

        // Prefix: "prefixTop/prefixBottom/"
        // The prefix is in unix format and always has a trailing slash.
        //
        // NO: "prefixTop" 
        // NO: "prefixTop/prefixBottom"
        // NO: "prefixTop/prefixBottom/"
        // YES: "prefixTop/prefixBottom/t"

        int prefixLen = useEntryPrefix.length();
        int entryLen = resourceName.length();
        if ( entryLen < (prefixLen + 1) ) { // Must have at least one more character.
            return false;
        } else if ( !resourceName.regionMatches(0,  useEntryPrefix,  0,  entryLen) ) {
            return false;
        }

        return true;
    }

    protected String removePrefix(String resourceName) {
        String useEntryPrefix = getEntryPrefix();
        if ( useEntryPrefix == null ) {
            return resourceName;
        } else {
            return resourceName.substring(useEntryPrefix.length());
        }
    }

    //

    @Override
    public InputStream openResourceStream(String className, String resourceName) throws ClassSource_Exception {
        String methodName = "openResourceStream";

        JarFile useJarFile = getJarFile();
        if ( useJarFile == null ) {
            String eMsg = "[ " + getHashText() + " ] is closed; processing [ " + resourceName + " ]";
            throw getFactory().newClassSourceException(eMsg);
        }

        JarEntry jarEntry = useJarFile.getJarEntry(resourceName);
        if ( jarEntry == null ) {
            return null;
        }

        InputStream result;

        try {
            result = useJarFile.getInputStream(jarEntry); // throws IOException

        } catch ( IOException e ) {
            String eMsg =
                "[ " + getHashText() + " ]" +
                " Failed to open [ " + resourceName + " ]" + " for class [ " + className + " ]" +
                " in [ " + getAbsJarPath() + " ]";

            // defect 84235:we are generating multiple Warning/Error messages for each error due to each level reporting them.
            // Disable the following warning and defer message generation to a higher level,
            // preferably the ultimate consumer of the exception.
            //
            // logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_CLASSSOURCE_OPEN5_EXCEPTION",
            //     new Object[] { getHashText(), resourceName, className, getJarAbsPath() });

            throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, e);
        }

        return result;
    }

    @Override
    public void closeResourceStream(String className, String resourceName, InputStream inputStream) {
        String methodName = "closeResourceStream";
        try {
            inputStream.close(); // throws IOException
        } catch ( IOException e ) {
            // "[ {0} ] Failed to close [ {1} ] for class [ {2} ] in [ {3} ]: {4}"
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_CLASSSOURCE_CLOSE6_EXCEPTION",
                new Object[] { getHashText(), resourceName, className, getAbsJarPath(), e });
        }
    }

    //

    @Override
    public void log(Logger useLogger) {
        String methodName = "log";
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class Source [ {0} ]", getHashText());
    }

    //

    @Override
    public int processFromScratch(ClassSource_Streamer streamer)
        throws ClassSource_Exception {

        String methodName = "processFromScratch";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,  "[ {0} ] ENTER", getHashText());
        }

        JarFile useJarFile = getJarFile();

        int numClasses = 0;
        int numEntries = 0;
        
        Enumeration<JarEntry> jarEntries = useJarFile.entries();
        while ( jarEntries.hasMoreElements() ) {
            JarEntry nextEntry = jarEntries.nextElement();
            String nextEntryName = nextEntry.getName();

            numEntries++;

            if ( !hasPrefix(nextEntryName) ) {
                continue;
            }

            if ( isDirectoryResource(nextEntryName) ) {
                continue;
            } else if ( !isClassResource(nextEntryName) ) {
                continue;
            }

            numClasses++;

            String nextClassName = getClassNameFromResourceName( removePrefix(nextEntryName) );
            if ( ClassSourceImpl.isJava9PackageName(nextClassName) ) {  // PI89708
                logger.logp(Level.FINER, CLASS_NAME, methodName,  "Java9 class name [ {0} ]", nextClassName);
                continue;
            }

            String i_nextClassName = internClassName(nextClassName);

            try {
                scan(streamer, i_nextClassName, nextEntryName); // throws ClassSource_Exception

            } catch ( ClassSource_Exception e ) {
                logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_SCAN_EXCEPTION [ {0} ]", e);
            }
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, 
                "[ {0} ] RETURN Entries [ {1} ] Classes [ {2} ]",
                 new Object[] { getHashText(), Integer.valueOf(numEntries), Integer.valueOf(numClasses) });
        }
        return numClasses;
    }

    @Override
    public void processSpecific(ClassSource_Streamer streamer, Set<String> i_classNames)
        throws ClassSource_Exception {

        String methodName = "processSpecific";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER [ {1} ]",
                new Object[] { getHashText(), Integer.valueOf(i_classNames.size()) });
        }

        long scanStart = System.nanoTime();

        JarFile useJarFile = getJarFile();

        Enumeration<JarEntry> jarEntries = useJarFile.entries();
        while ( jarEntries.hasMoreElements() ) {
            JarEntry nextEntry = jarEntries.nextElement();
            String nextEntryName = nextEntry.getName();

            if ( !hasPrefix(nextEntryName) ) {
                continue;
            } else if ( isDirectoryResource(nextEntryName) ) {
                continue;
            } else if ( !isClassResource(nextEntryName) ) {
                continue;
            } 

            String nextClassName = getClassNameFromResourceName( removePrefix(nextEntryName) );
            if ( ClassSourceImpl.isJava9PackageName(nextClassName) ) {  // PI89708
                logger.logp(Level.FINER, CLASS_NAME, methodName, "Java9 class name [ {0} ]", nextClassName);
                continue;
            }

            String i_className = internClassName(nextClassName, Util_InternMap.DO_NOT_FORCE);
            if ( i_className == null ) {
                continue;
            } else if ( !i_classNames.contains(i_className) ) {
                continue;
            }

            try {
                scan(streamer, i_className, nextEntryName); // throws ClassSource_Exception
            } catch ( ClassSource_Exception e ) {
                logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_SCAN_EXCEPTION [ {0} ]", e);
            }
        }

        long scanTime = System.nanoTime() - scanStart;

        setProcessTime(scanTime);
        setProcessCount( i_classNames.size() );

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,  "[ {0} ] RETURN", getHashText());
        }
    }

    protected void scan(ClassSource_Streamer streamer, String i_className, String resourceName)
        throws ClassSource_Exception {

        InputStream inputStream = openClassResourceStream(i_className, resourceName); // throws ClassSource_Exception

        try {
            streamer.process(i_className, inputStream); // throws ClassSource_Exception

        } finally {
            closeResourceStream(i_className, resourceName, inputStream); // throws ClassSource_Exception
        }
    }

    //

    @Override
    protected boolean basicHasJandexIndex() {
        return ( getJarFile().getJarEntry( getJandexIndexPath() ) != null );
    }

    @Override
    protected Index basicGetJandexIndex() {
        String methodName = "basicGetJandexIndex";

        String useJandexIndexPath = getJandexIndexPath();

        InputStream jandexStream;
        try {
            jandexStream = openResourceStream(null, useJandexIndexPath, JANDEX_BUFFER_SIZE);
            // throws ClassSource_Exception

        } catch ( ClassSource_Exception e ) {
            // ANNO_CLASSSOURCE_ENTRY_JANDEX_OPEN_EXCEPTION=CWWKC0087W:
            // [ {0} ] Open of Jandex index resource [{1}] from [{2}] failed.  Error message: {3}

            logger.logp(Level.SEVERE, CLASS_NAME, methodName,
                "ANNO_CLASSSOURCE_ENTRY_JANDEX_OPEN_EXCEPTION",
                new Object[] { getHashText(),
                               useJandexIndexPath,
                               getAbsolutePath( getRawJarFile().getAbsolutePath() ),
                               e });

            return null;
        }

        if ( jandexStream == null ) {
            return null;
        }

        try {
            Index jandexIndex = Jandex_Utils.basicReadIndex(jandexStream); // throws IOException
            if ( logger.isLoggable(Level.FINER) ) {
                String message = MessageFormat.format(
                    "[ {0} ] Read JANDEX index [ {1} ] from [ {2} ] Classes  [ {3} ]", 
                    new Object[] { getHashText(), getAbsolutePath(useJandexIndexPath), getCanonicalName(), Integer.toString(jandexIndex.getKnownClasses().size()) });
                logger.logp(Level.FINER, CLASS_NAME, methodName, message);
            }
            return jandexIndex;

        } catch ( IOException e ) {
            // ANNO_CLASSSOURCE_ENTRY_JANDEX_READ_EXCEPTION=CWWKC0088W:
            // [ {0} ] Read of Jandex index resource [{1}] from [{2}] failed.
            // Error message: {3}

            logger.logp(Level.SEVERE, CLASS_NAME, methodName,
                "ANNO_CLASSSOURCE_ENTRY_JANDEX_READ_EXCEPTION",
                new Object[] { getHashText(),
                               useJandexIndexPath,
                               getAbsolutePath( getRawJarFile().getAbsolutePath() ),
                               e });

            return null;
        }
    }

    @Override
    protected SparseIndex basicGetSparseJandexIndex() {
        String methodName = "basicGetSparseJandexIndex";

        String useJandexIndexPath = getJandexIndexPath();

        InputStream jandexStream;
        try {
            jandexStream = openResourceStream(null, useJandexIndexPath, JANDEX_BUFFER_SIZE);
            // throws ClassSource_Exception
        } catch ( ClassSource_Exception e ) {
            // ANNO_CLASSSOURCE_ENTRY_JANDEX_OPEN_EXCEPTION=CWWKC0087W:
            // [ {0} ] Open of Jandex index resource [{1}] from [{2}] failed.  Error message: {3}

            logger.logp(Level.SEVERE, CLASS_NAME, methodName,
                "ANNO_CLASSSOURCE_ENTRY_JANDEX_OPEN_EXCEPTION",
                new Object[] { getHashText(),
                               useJandexIndexPath,
                               getAbsolutePath( getRawJarFile().getAbsolutePath() ),
                               e });

            return null;
        }

        if ( jandexStream == null ) {
            return null;
        }

        try {
            SparseIndex jandexIndex = Jandex_Utils.basicReadSparseIndex(jandexStream); // throws IOException

            if ( logger.isLoggable(Level.FINER) ) {
                String message = MessageFormat.format(
                    "[ {0} ] Read sparse JANDEX index [ {1} ] from [ {2} ] Classes  [ {3} ]", 
                    new Object[] { getHashText(), getAbsolutePath(useJandexIndexPath), getCanonicalName(), Integer.toString(jandexIndex.getKnownClasses().size()) });
                logger.logp(Level.FINER, CLASS_NAME, methodName, message);
            }
            return jandexIndex;

        } catch ( IOException e ) {
            // ANNO_CLASSSOURCE_ENTRY_JANDEX_READ_EXCEPTION=CWWKC0088W:
            // [ {0} ] Read of Jandex index resource [{1}] from [{2}] failed.
            // Error message: {3}

            logger.logp(Level.SEVERE, CLASS_NAME, methodName,
                "ANNO_CLASSSOURCE_ENTRY_JANDEX_READ_EXCEPTION",
                new Object[] { getHashText(),
                               useJandexIndexPath,
                               getAbsolutePath( getRawJarFile().getAbsolutePath() ),
                               e });

            return null;
        }
    }

    // Obsolete ...

    @Override
    @Trivial
    public void log(TraceComponent tc) {
        Tr.debug(tc, MessageFormat.format("Class Source [ {0} ]", getHashText()));
        logCounts(tc);
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
