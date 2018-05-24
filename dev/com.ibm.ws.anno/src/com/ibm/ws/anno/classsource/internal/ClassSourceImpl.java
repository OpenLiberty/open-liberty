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

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Set;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.wsspi.anno.classsource.ClassSource;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_Options;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts;
import com.ibm.wsspi.anno.classsource.ClassSource_Streamer;
import com.ibm.wsspi.anno.util.Util_InternMap;

public abstract class ClassSourceImpl implements ClassSource {
    private static final TraceComponent tc = Tr.register(ClassSourceImpl.class);
    public static final String CLASS_NAME = ClassSourceImpl.class.getName();

    @Trivial
    protected static long getTime() {
        return System.currentTimeMillis();
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
    protected ClassSourceImpl(ClassSourceImpl_Factory factory,
                              Util_InternMap internMap,
                              String name,
                              ClassSource_Options options,
                              String hashTextSuffix) {
        super();

        this.factory = factory;

        this.internMap = internMap;

        this.name = name;
        this.canonicalName = factory.getCanonicalName(this.name);

        this.parentSource = null;

        this.options = options;

        String useHashText = AnnotationServiceImpl_Logging.getBaseHash(this);
        useHashText += "(" + this.canonicalName;
        if (hashTextSuffix != null) {
            useHashText += ", " + hashTextSuffix;
        }
        useHashText += ")";

        this.hashText = useHashText;

        this.scanCounts = new ClassSourceImpl_ScanCounts();

        if ( tc.isDebugEnabled() ) {
            String msg = MessageFormat.format(
                "[ {0} ] InternMap [ {1} ]",
                this.hashText, internMap.getHashText());
            Tr.debug(tc, msg);
        }
    }

    //

    protected final ClassSourceImpl_Factory factory;

    @Override
    @Trivial
    public ClassSourceImpl_Factory getFactory() {
        return factory;
    }

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

    protected ClassSource parentSource;

    @Override
    @Trivial
    public ClassSource getParentSource() {
        return parentSource;
    }

    @Override
    public void setParentSource(ClassSource parentSource) {
        this.parentSource = parentSource;
    }

    //

    protected final ClassSource_Options options;

    @Override
    @Trivial
    public ClassSource_Options getOptions() {
    	return options;
    }

    @Trivial
    public boolean getUseJandex() {
    	return getOptions().getUseJandex();
    }

    //

    @Trivial
    public boolean getConvertResourceNames() {
        return false;
    }

    @Override
    @Trivial
    public String inconvertResourceName(String externalResourceName) {
        return externalResourceName;
    }

    @Override
    @Trivial
    public String outconvertResourceName(String resourceName) {
        return resourceName;
    }

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

    //

    @Override
    public abstract void open() throws ClassSource_Exception;

    @Override
    public abstract void close() throws ClassSource_Exception;

    //

    @Override
    @Trivial
    public boolean isDirectoryResource(String resourceName) {
        return resourceName.endsWith(ClassSource.RESOURCE_SEPARATOR_STRING);
    }

    @Override
    @Trivial
    public boolean isClassResource(String resourceName) {
        return resourceName.endsWith(CLASS_EXTENSION);
    }

    @Override
    @Trivial
    public String getClassNameFromResourceName(String resourceName) {
        int endingOffset = resourceName.length() - ClassSource.CLASS_EXTENSION.length();
        String className = resourceName.substring(0, endingOffset);
        className = className.replace(RESOURCE_SEPARATOR_CHAR, ClassSource.CLASS_SEPARATOR_CHAR);

        return className;
    }

    @Override
    @Trivial
    public String getResourceNameFromClassName(String className) {
        return
            className.replace(ClassSource.CLASS_SEPARATOR_CHAR, RESOURCE_SEPARATOR_CHAR) +
            ClassSource.CLASS_EXTENSION;
    }

    @Override
    @Trivial
    public String resourceAppend(String head, String tail) {
        if ( head.isEmpty() ) {
            return tail;
        } else {
            return (head + ClassSource.RESOURCE_SEPARATOR_CHAR + tail);
        }
    }

    //

    protected final ClassSourceImpl_ScanCounts scanCounts;

    @Override
    @Trivial
    public ClassSourceImpl_ScanCounts getScanResults() {
        return scanCounts;
    }

    protected void markResult(ClassSource_ScanCounts.ResultField resultField) {
        scanCounts.increment(resultField);
    }

    protected void addResults(ClassSource_ScanCounts partialScanCounts) {
        scanCounts.addResults(partialScanCounts);
    }

    @Trivial
    @Override
    public int getResult(ClassSource_ScanCounts.ResultField resultField) {
        return scanCounts.getResult(resultField);
    }

    //

    @Override
    @Trivial
    public boolean scanSpecificSeedClass(String className, ClassSource_Streamer streamer) throws ClassSource_Exception {
        return scanClass(streamer, className, ScanPolicy.SEED); // throws ClassSource_Exception
    }

    @Override
    @Trivial
    public boolean scanReferencedClass(String referencedClassName, ClassSource_Streamer streamer) throws ClassSource_Exception {
        return scanClass(streamer, referencedClassName, ScanPolicy.EXTERNAL); // throws ClassSource_Exception
    }

    // Entry from:
    //   ClassSourceImpl.scanSpecifiedSeedClasses(Set<String>, ClassSource_Streamer)
    //   ClassSourceImpl.scanReferencedClasses(Set<String>, ClassSource_Streamer)

    /**
     * <p>Attempt to process a specified class. Answer whether processing was handed
     * successfully by the streamer via {@link ClassSource_Streamer#process(String, InputStream, boolean, boolean, boolean)}.</p>
     *
     * <p>A failure (false) result occurs the class is blocked by {@link ClassSource_Streamer#doProcess(String, boolean, boolean, boolean)},
     * or if no resource is available for the class. A failure to open the resource results
     * in an exception. Certain processing failures also result in an exception.</p>
     *
     * <p>An exception thrown by the streamer indicates that the class was not successfully
     * handled by the streamer.</p>
     *
     * <p>A failure to close the stream for the class is handled locally.</p>
     *
     * @param streamer The streamer which is to be used to process the named class.
     * @param className The class which is to be processed.
     * @param scanPolicy The scan policy of the class source of the named class.
     *
     * @return True if processing reaches the streamer. Otherwise, false.
     *
     * @throws ClassSource_Exception Thrown in case of a processing failure.
     */
    @Trivial
    protected boolean scanClass(ClassSource_Streamer streamer, String className, ScanPolicy scanPolicy)
        throws ClassSource_Exception {

        String methodName = "scanClass";
        Object[] logParms;
        if ( tc.isEntryEnabled() ) {
            logParms = new Object[] { getHashText(), className };
            Tr.entry(tc, methodName, MessageFormat.format("[ {0} ] Name [ {1} ]", logParms));
        } else {
            logParms = null;
        }

        if ( !streamer.doProcess(className, scanPolicy) ) {
            if ( logParms != null ) {
                Tr.exit(tc, methodName,
                        MessageFormat.format("[ {0} ] Return [ {1} ] [ false ]: Filtered by streamer", logParms));
            }
            return false;
        }

        String resourceName = getResourceNameFromClassName(className);

        InputStream inputStream = openResourceStream(className, resourceName); // throws ClassSource_Exception
        if ( inputStream == null ) {
            if ( logParms != null ) {
                Tr.exit(tc, methodName, MessageFormat.format("[ {0} ] Return [ {1} ] [ false ]: No resource is available", logParms));
            }
            return false;
        }

        try {
            streamer.process( getCanonicalName(), className, inputStream, scanPolicy );
        } finally {
            closeResourceStream(className, resourceName, inputStream);
        }

        if ( logParms != null ) {
            Tr.exit(tc, methodName, MessageFormat.format("[ {0} ] Return [ {1} ] [ true ]", logParms));
        }
        return true;
    }

    //

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

        if ( tc.isDebugEnabled() ) {
            String msg = MessageFormat.format(
                "[ {0} ] Resource [ {1} ]: [ {2} ]",
                getHashText(), i_resourceName, Boolean.valueOf(didAdd));
            Tr.debug(tc, methodName, msg);
        }

        return didAdd;
    }

    //

    // Mostly common, but overloaded by mapped jars.
    @Override
    @Trivial
    public InputStream openClassStream(String className) throws ClassSource_Exception {
        String resourceName = getResourceNameFromClassName(className);
        return openResourceStream(className, resourceName); // throws ClassSource_Exception
    }

    // Mostly common, but overloaded by mapped jars.
    @Override
    @Trivial
    public void closeClassStream(String className, InputStream inputStream) {
        String resourceName = getResourceNameFromClassName(className);
        closeResourceStream(className, resourceName, inputStream);
    }

    @Override
    public abstract InputStream openResourceStream(String className, String resourceName)
        throws ClassSource_Exception;

    @Override
    public abstract void closeResourceStream(String className, String resourceName, InputStream inputStream);

    //

    protected int resourceExclusionCount;
    protected int classExclusionCount;
    protected int classInclusionCount;

    protected void incrementResourceExclusionCount() {
        resourceExclusionCount++;
    }

    @Override
    @Trivial
    public int getResourceExclusionCount() {
        return resourceExclusionCount;
    }

    protected void incrementClassExclusionCount() {
        classExclusionCount++;
    }

    @Override
    @Trivial
    public int getClassExclusionCount() {
        return classExclusionCount;
    }

    protected void incrementClassInclusionCount() {
        classInclusionCount++;
    }

    @Override
    @Trivial
    public int getClassInclusionCount() {
        return classInclusionCount;
    }

    //

    @Override
    @Trivial
    public void logState() {
        TraceComponent stateLogger = AnnotationServiceImpl_Logging.stateLogger;

        if (stateLogger.isDebugEnabled()) {
            log(stateLogger);
        }
    }

    @Override
    public abstract void log(TraceComponent logger);

    @Trivial
    protected void logCounts(TraceComponent logger) {
        if (logger.isDebugEnabled()) {
            Tr.debug(logger, MessageFormat.format("  Included classes: [ {0} ]",
                                                  Integer.valueOf(getClassInclusionCount())));

            Tr.debug(logger, MessageFormat.format("  Excluded classes: [ {0} ]",
                                                  Integer.valueOf(getClassExclusionCount())));
        }
    }

    //

    @Override
    public void scanClasses(ClassSource_Streamer streamer, Set<String> i_seedClassNames, ScanPolicy scanPolicy) {
    	String methodName = "scanClasses";

        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] ENTER", getHashText()));
        }

        int initialClasses = i_seedClassNames.size();

        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] Processing [ {1} ] Initial classes [ {2} ]",
                     new Object[] { getHashText(), getCanonicalName(), Integer.valueOf(initialClasses) } ));
        }

        boolean fromJandex;

        if ( processFromCache(streamer, i_seedClassNames, scanPolicy) ) {
        	fromJandex = true;
        } else {
            processFromScratch(streamer, i_seedClassNames, scanPolicy);
            fromJandex = false;
        }

        int finalClasses = i_seedClassNames.size();

        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] Processing [ {1} ] {2}; Final classes [ {3} ]",
                    new Object[] { getHashText(),
                    		       getCanonicalName(),
                    		       (fromJandex ? "New Scan" : "Jandex"),
                    		       Integer.valueOf(finalClasses) } ));

            Object[] logParms = new Object[] { getHashText(), null, null };

            logParms[1] = Integer.valueOf(finalClasses - initialClasses);
            Tr.debug(tc, MessageFormat.format("[ {0} ] RETURN [ {1} ] Added classes", logParms));

            for ( ClassSource_ScanCounts.ResultField resultField : ClassSource_ScanCounts.ResultField.values() ) {
                int nextResult = getResult(resultField);
                String nextResultTag = resultField.getTag();

                logParms[1] = Integer.valueOf(nextResult);
                logParms[2] = nextResultTag;

                Tr.debug(tc, MessageFormat.format("[ {0} ]  [ {1} ] {2}", logParms));
            }
        }

        if ( fromJandex && JandexLogger.doLog() ) {
        	String useHashText = getHashText();

        	String msg = MessageFormat.format(
        		"[ {0} ] Processing [ {1} ] {2}; Final classes [ {3} ]",
        		useHashText, getCanonicalName(),
        		(fromJandex ? "New Scan" : "Jandex"),
        		Integer.valueOf(finalClasses) );
        	JandexLogger.log(CLASS_NAME, methodName, msg);

        	JandexLogger.log(CLASS_NAME, methodName,
        		MessageFormat.format("[ {0} ] Added classes [ {1} ]",
        			useHashText, Integer.valueOf(finalClasses - initialClasses)));

        	for ( ClassSource_ScanCounts.ResultField resultField : ClassSource_ScanCounts.ResultField.values() ) {
        		int nextResult = getResult(resultField);
        		String nextResultTag = resultField.getTag();

            	JandexLogger.log(CLASS_NAME, methodName,
                    MessageFormat.format("[ {0} ]  [ {1} ] {2}",
                    	useHashText, Integer.valueOf(nextResult), nextResultTag));
            }
        }
    }

    /**
     * <p>Main scan implementation step: Process the classes of this class source.  No cache
     * data is available.</p>
     *
     * @param streamer The streamer used to process the classes.
     * @param i_seedClassNames The seed class names.  Updated with new seed class names from the scan.
     * @param scanPolicy The policy to apply to the scan data.
     */
    protected abstract void processFromScratch(
        ClassSource_Streamer streamer,
        Set<String> i_seedClassNames,
        ScanPolicy scanPolicy);

    //

    /**
     * <p>Answer the path to JANDEX index files.</p>
     *
     * <p>The default implementation answers <code>"META-INF/jandex.ndx"</code>.</p>
     *
     * @return The relative path to JANDEX index files.
     */
    @Trivial
    public String getJandexIndexPath() {
        return "/META-INF/jandex.idx";
    }

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

        boolean doLog = tc.isDebugEnabled();
        boolean doJandexLog = JandexLogger.doLog();

        boolean useJandex = getUseJandex();

        if ( !useJandex ) {
        	// Figuring out if there is a Jandex index is mildly expensive,
        	// and is to be avoided when logging is disabled.

        	if ( doLog || doJandexLog ) {
        		boolean haveJandex = basicHasJandexIndex();

        		String msg;
        		if ( haveJandex ) {
        			msg = MessageFormat.format(
        				"[ {0} ] Jandex disabled; Jandex index [ {1} ] found",
        				getHashText(), getJandexIndexPath());
        		} else {
        			msg = MessageFormat.format(
        				"[ {0} ] Jandex disabled; Jandex index [ {1} ] not found",
        				getHashText(), getJandexIndexPath());

        		}
        		if ( doLog ) {
        		    Tr.debug(tc, msg);
        		}
        		if ( doJandexLog ) {
        		    JandexLogger.log(CLASS_NAME,  methodName, msg);
        		}
        	}

        	return null;

        } else {
        	Index jandexIndex = basicGetJandexIndex();

        	if ( doLog || doJandexLog ) {
        		String msg;
        		if ( jandexIndex != null ) {
        			msg = MessageFormat.format(
        				"[ {0} ] Jandex enabled; Jandex index [ {1} ] found",
        				getHashText(), getJandexIndexPath());
        		} else {
        			msg = MessageFormat.format(
        				"[ {0} ] Jandex enabled; Jandex index [ {1} ] not found",
        				getHashText(), getJandexIndexPath());
        		}
        		if ( doLog ) {
        	        Tr.debug(tc, msg);
        		}
        		if ( doJandexLog ) {
        			JandexLogger.log(CLASS_NAME,  methodName, msg);
        		}
        	}

        	return jandexIndex;
        }
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
    protected boolean basicHasJandexIndex() {
        return false;
    }
    
    protected boolean processedUsingJandex = false;
    
    public boolean isProcessedUsingJandex() {
        return processedUsingJandex;
    }

    /**
     * <p>Attempt to process this class source using cache data.</p>
     *
     * @param streamer The streamer used to process the class source.
     * @param i_seedClassNames The class names of the class source.
     * @param scanPolicy The policy of this class source.
     *
     * @return True or false telling if the class was successfully
     *     processed using cache data.
     */
    protected boolean processFromCache(
        ClassSource_Streamer streamer,
        Set<String> i_seedClassNames,
        ScanPolicy scanPolicy) {

        if ( streamer == null ) {
            return false;
        }

        Index jandexIndex = getJandexIndex();
        if ( jandexIndex == null ) {
        	return false;
        }
        
        processedUsingJandex = true;

        String useClassSourceName = getCanonicalName();

        for ( org.jboss.jandex.ClassInfo nextJandexClassInfo : jandexIndex.getKnownClasses() ) {
            DotName nextClassDotName = nextJandexClassInfo.name();
            String nextClassName = nextClassDotName.toString();

            markResult(ClassSource_ScanCounts.ResultField.ENTRY);
            markResult(ClassSource_ScanCounts.ResultField.NON_CONTAINER);
            markResult(ClassSource_ScanCounts.ResultField.CLASS);

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

            String i_nextClassName = internClassName(nextClassName);
            boolean didAdd = i_maybeAdd(i_nextClassName, i_seedClassNames);

            if ( !didAdd ) {
                incrementClassExclusionCount();
                markResult(ClassSource_ScanCounts.ResultField.DUPLICATE_CLASS);

            } else {
                incrementClassInclusionCount();

                boolean didProcess;

                if ( !streamer.doProcess(i_nextClassName, scanPolicy) ) {
                    didProcess = false;

                } else {
                    try {
                        didProcess = streamer.process(useClassSourceName, nextJandexClassInfo, scanPolicy);
                    } catch ( ClassSource_Exception e ) {
                        didProcess = false;

                        // CWWKC0065W: An exception occurred while processing class [ {0} ].  The identifier for the class source is [ {1} ]. The exception was {2}.
                        Tr.warning(tc, "JANDEX_SCAN_EXCEPTION", i_nextClassName, getHashText(), e);
                    }
                }

                if ( didProcess ) {
                    markResult(ClassSource_ScanCounts.ResultField.PROCESSED_CLASS);
                } else {
                    markResult(ClassSource_ScanCounts.ResultField.UNPROCESSED_CLASS);
                }
            }
        }

        return true;
    } 
    

    private static final int META_INF_LENGTH = "META-INF".length();   
    /**
     *
     * The intent of this method is to add toleration of Java V9 classes in multi-release
     * JAR files.  Java V9 adds classes under the META-INF directory. This is a characteristic of
     * multi-release JARs.  Java 9 also adds a file module-info.java which we want to allow, but 
     * we don't want to scan these until support for Java 9 is added.  For now we want
     * to just ignore all of these classes.
     *
     * @param name - simple class name,  (without .class suffix)
     *               fully qualified class name, (example com.ibm.someClass OR com/ibm/someClass)
     *               or simple directory name
     *
     * @return     - true if the name startsWith "META-INF" or ends with "module-info"
     *             The startsWith & endsWith tests do not definitively confirm Java-9 specificity,
     *             but for all intents and purposes, it does what we want.  For example if we get a 
     *             class name or directory name that starts with META-INF or ends with module-info, 
     *             but it's not really a Java 9 case, well it's still not a valid class name or it's not
     *             a valid directory name that could be part of a java package name.  Returning
     *             true has the end result of the class being ignored.
     */
    protected boolean isJava9SpecificClass(String name) {

        if ( ( name.length() >= META_INF_LENGTH ) && name.regionMatches(true, 0, "META-INF", 0, META_INF_LENGTH) ) {
            return true; // Starts with "META-INF" ignoring case
        }        

        if ( name.endsWith("module-info") ) {
            return true;
        }        

        return false;
    }
    
}
