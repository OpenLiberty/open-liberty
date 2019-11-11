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
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import java.util.logging.Logger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts.ResultField;
import com.ibm.wsspi.anno.classsource.ClassSource_Streamer;
import com.ibm.wsspi.annocache.classsource.ClassSource;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_ClassLoader;
import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.classsource.ClassSource_Options;
import com.ibm.wsspi.annocache.util.Util_InternMap;

// Concurrency notes:
//
// The class source must be created and populated in a single thread.  No synchronization
// is provided for these steps.
//
// 'open' and 'close' are synchronized.
//
// 'openResource' and 'closeResource' are synchronized.
//
// InternMap is thread safe.

public class ClassSourceImpl_Aggregate implements ClassSource_Aggregate {
    // Logging ...

    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;
    protected static final Logger stateLogger = AnnotationCacheServiceImpl_Logging.ANNO_STATE_LOGGER;
    protected static final Logger jandexLogger = AnnotationCacheServiceImpl_Logging.ANNO_JANDEX_LOGGER;

    public static final String CLASS_NAME = ClassSourceImpl_Aggregate.class.getSimpleName();

    // Logging ...

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
    
    // Top O' the world

    public ClassSourceImpl_Aggregate(
        ClassSourceImpl_Factory factory,
        Util_InternMap internMap,
        String applicationName, String moduleName, String moduleCategoryName,
        ClassSource_Options options) {

        String methodName = "<init>";

        this.options = options;
        this.factory = factory;
        this.internMap = internMap;

        //

        this.applicationName = applicationName;
        this.moduleName = moduleName;
        this.moduleCategoryName = moduleCategoryName;

        //

        this.seedClassSources = new HashSet<ClassSource>();
        this.partialClassSources = new HashSet<ClassSource>();
        this.excludedClassSources = new HashSet<ClassSource>();
        this.externalClassSources = new HashSet<ClassSource>();

        this.classSources = new ArrayList<ClassSource>();
        this.classSourceNames = new HashMap<String, String>();
        this.internalSourceCount = 0;

        this.openCount = 0;
        this.successfulOpens = new ArrayList<ClassSource>();
        this.failedOpens = new ArrayList<ClassSource>();

        this.totalLookups = 0L;
        this.repeatLookups = 0L;

        this.i_lookupCounts = new IdentityHashMap<String, Integer>();
        this.i_globalResults = new IdentityHashMap<String, Boolean>();
        this.i_failedLookups = new IdentityHashMap<ClassSource, Set<String>>();
        this.i_firstSuccesses = new IdentityHashMap<String, ClassSource>();

        this.cacheReadTime = 0L;
        this.cacheWriteTime = 0L;

        this.hashText =
            getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) +
            "(" + this.applicationName + ":" + this.moduleName + ":" + this.moduleCategoryName + ")";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", this.hashText);
        }
    }

    // Embodiment ...

    private final ClassSource_Options options;

    @Override
    @Trivial
    public ClassSource_Options getOptions() {
        return options;
    }

    //

    private final ClassSourceImpl_Factory factory;

    @Override
    @Trivial
    public ClassSourceImpl_Factory getFactory() {
        return factory;
    }

    //

    private final Util_InternMap internMap;

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

    // Identify ...

    protected final String applicationName;
    protected final String moduleName;
    protected final String moduleCategoryName;

    /**
     * <p>Answer the name of the application of this class source.</p>
     *
     * @return The name of the application of this class source.
     */
    @Override
    @Trivial
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * <p>Answer the name of the module of this class source.</p>
     *
     * @return The name of the module of this class source.
     */
    @Override
    @Trivial
    public String getModuleName() {
        return moduleName;
    }

    /**
     * <p>Answer the module category name of this class source.</p>
     *
     * @return The module category name of this class source.
     */
    @Override
    @Trivial
    public String getModuleCategoryName() {
        return moduleCategoryName;
    }

    //

    // Open/close tracking ...

    // A couple of tricky issues here relating to reuse:
    //
    // A class source is reused, and may be opened/closed multiple times, as
    // each use will open and close the class source.
    //
    // Maintenance of the lookup tables becomes tricky when any child open
    // fails, and in particular when child opens are not consistent.  (That is,
    // when the result of a first open request does not match the result of
    // a later open request.)
    //
    // To simplify state management, once an open fails, that child class source
    // is set as inaccessible.  Only class sources which were successfully opened
    // are candidates for subsequent open requests.
    //
    // The initial successful opens list is initialized to the entire list.
    //
    // A new open request retrieves (obtains and clears) the successful opens list,
    // then updates that according to the new open results.
    //
    // To prevent problems of overlapping open/close requests, the open state is
    // managed using a count of current active open requests.  A new open request
    // causes no activity if the class source is already open.

    // When the open count is zero, 'successfulOpens' lists the class sources
    // which are valid to be opened.  That is, those class sources which have not
    // failed an open request.
    //
    // When the open count is greater than zero, 'successfulOpens' lists the class
    // sources which were successful opened by the base open request, and which must be
    // closed on the next base close request.  Base requests are those which perform
    // child opens or closes, and are those requests which transition from or to a
    // zero open count.

    private int openCount;
    private List<ClassSource> successfulOpens;
    private List<ClassSource> failedOpens;

    @Trivial
    public int getOpenCount() {
        return openCount;
    }

    @Trivial
    public boolean getIsOpen() {
        return (openCount > 0);
    }

    @Trivial
    protected List<ClassSource> retrieveSuccessfulOpens() {
        List<ClassSource> oldSuccessfulOpens = successfulOpens;
        successfulOpens = new ArrayList<ClassSource>();
        return oldSuccessfulOpens;
    }

    @Trivial
    @Override
    public List<ClassSource> getSuccessfulOpens() {
        return successfulOpens;
    }

    protected void addSuccessfulOpen(ClassSource classSource) {
        successfulOpens.add(classSource);
    }

    @Override
    @Trivial
    public List<ClassSource> getFailedOpens() {
        return failedOpens;
    }

    protected void addFailedOpen(ClassSource classSource) {
        failedOpens.add(classSource);
    }

    @Override
    @Trivial
    public synchronized void open() throws ClassSource_Exception {
        String methodName = "open";
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER Open count [ {1} ]",
                new Object[] { getHashText(), Integer.valueOf(openCount) });
        }

        openCount++;

        if ( openCount == 1 ) { // First one; need to open children.
            for ( ClassSource nextClassSource : retrieveSuccessfulOpens() ) {
                try {
                    nextClassSource.open(); // throws ClassSource_Exception
                    addSuccessfulOpen(nextClassSource);

                } catch ( ClassSource_Exception e ) {
                    addFailedOpen(nextClassSource);

                    logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_INFOSTORE_OPEN1_EXCEPTION",
                        new Object[] { getHashText(), nextClassSource.getHashText(), e });
                }
            }
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN Open count [ {1} ]",
                new Object[] { getHashText(), Integer.valueOf(openCount) });
        }
    }

    @Override
    @Trivial
    public synchronized void close() throws ClassSource_Exception {
        String methodName = "close";

        if ( openCount == 0 ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER/RETURN [ {1} ]",
                    new Object[] { getHashText(), Integer.valueOf(openCount) });
            }
            return;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER [ {1} ]",
                new Object[] { getHashText(), Integer.valueOf(openCount) });
        }

        openCount--;

        if ( openCount == 0 ) { // Last one which is active; need to close the children.
            for ( ClassSource nextClassSource : getSuccessfulOpens() ) {
                try {
                    nextClassSource.close(); // throws ClassSource_Exception
                } catch ( ClassSource_Exception e ) {
                    // String eMsg = "Class source [ " + getHashText() + " ]" +
                    //               " failed to close child class source [ " + nextClassSource.getHashText() + " ]";
                    logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_CLASSSOURCE_CLOSE1_EXCEPTION",
                        new Object[] { getHashText(),
                                       nextClassSource.getCanonicalName(),
                                       nextClassSource.getHashText(),
                                       e });
                }
            }
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[{0}] RETURN Open Count [{1}]",
                new Object[] { getHashText(), Integer.valueOf(openCount) });
        }
    }

    //

    @Override
    public void addClassSource(ClassSource classSource) {
        addClassSource(classSource, ScanPolicy.SEED);
    }

    @Override
    public void addClassLoaderClassSource(ClassSource_ClassLoader classSource) {
        addClassSource(classSource, ScanPolicy.EXTERNAL);
    }

    @Override
    public void addClassSource(ClassSource classSource, ScanPolicy scanPolicy) {
        String methodName = "addClassSource";
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Adding [ {1} ] [ {2} ]",
                new Object[] { getHashText(), classSource.getHashText(), scanPolicy });
        }

        basicAddClassSource(classSource, scanPolicy);

        // Initially, all class sources are valid to be opened.
        addSuccessfulOpen(classSource);
    }

    @Override
    public Set<ClassSource> getClassSources(ScanPolicy scanPolicy) {
        if ( scanPolicy == ScanPolicy.SEED ) {
            return seedClassSources;
        } else if ( scanPolicy == ScanPolicy.PARTIAL ) {
            return partialClassSources;
        } else if ( scanPolicy == ScanPolicy.EXCLUDED ) {
            return excludedClassSources;
        } else {
            return externalClassSources;
        }
    }

    @Override
    public Set<ClassSource> getSeedClassSources() {
        return seedClassSources;
    }

    @Override
    public Set<ClassSource> getPartialClassSources() {
        return partialClassSources;
    }

    @Override
    public Set<ClassSource> getExcludedClassSources() {
        return excludedClassSources;
    }

    @Override
    public Set<ClassSource> getExternalClassSources() {
        return externalClassSources;
    }

    //

    protected final List<ClassSource> classSources;
    protected final Map<String, String> classSourceNames;
    private int internalSourceCount;

    protected final Set<ClassSource> seedClassSources;
    protected final Set<ClassSource> partialClassSources;
    protected final Set<ClassSource> excludedClassSources;
    protected final Set<ClassSource> externalClassSources;

    @Override
    public List<ClassSource> getClassSources() {
        return classSources;
    }

    protected void basicAddClassSource(ClassSource classSource, ScanPolicy scanPolicy) {
        classSources.add(classSource);

        String classSourceName = classSource.getName();
        if ( classSourceName != null ) {
            classSourceNames.put( classSourceName, classSource.getCanonicalName() );
        }

        classSource.setParentSource(this);

        boolean isInternal;

        if ( scanPolicy == ScanPolicy.SEED ) {
            seedClassSources.add(classSource);
            isInternal = true;
        } else if ( scanPolicy == ScanPolicy.PARTIAL ) {
            partialClassSources.add(classSource);
            isInternal = true;            
        } else if ( scanPolicy == ScanPolicy.EXCLUDED ) {
            excludedClassSources.add(classSource);
            isInternal = true;            
        } else {
            externalClassSources.add(classSource);
            isInternal = false;
        }

        if ( isInternal ) {
        	internalSourceCount++;
        }
    }

    @Override
    public ScanPolicy getScanPolicy(ClassSource classSource) {
        if ( seedClassSources.contains(classSource) ) {
            return ScanPolicy.SEED;
        } else if ( partialClassSources.contains(classSource) ) {
            return ScanPolicy.PARTIAL;
        } else if ( excludedClassSources.contains(classSource) ) {
            return ScanPolicy.EXCLUDED;
        } else {
            return ScanPolicy.EXTERNAL;
        }
    }

    @Override
    public int getInternalSourceCount() {
    	return internalSourceCount;
    }

    @Override
    public Map<String, String> getCanonicalNames() {
        return classSourceNames;
    }

    @Override
    public String getCanonicalName(String classSourceName) {
        return classSourceNames.get(classSourceName);
    }

    //

    protected long totalLookups;
    protected long repeatLookups;

    protected Map<String, Integer> i_lookupCounts;

    @Override
    public long getTotalLookups() {
        return totalLookups;
    }

    protected void recordLookup() {
        totalLookups++;
    }

    @Override
    public long getRepeatLookups() {
        return repeatLookups;
    }

    protected void recordRepeatLookup() {
        repeatLookups++;
    }

    @Override
    @Trivial
    public Map<String, Integer> getLookupCounts() {
        return i_lookupCounts;
    }

    protected Integer i_recordLookup(String i_className) {
        Integer lookupCount = i_lookupCounts.get(i_className);

        if ( lookupCount == null ) {
            lookupCount = Integer.valueOf(1);
        } else {
            lookupCount = Integer.valueOf(lookupCount.intValue() + 1);
            recordRepeatLookup();
        }

        recordLookup();

        i_lookupCounts.put(i_className, lookupCount);

        return lookupCount;
    }

    // Keep track of prior lookup results:
    //
    // 1) globalSuccesses
    // 2) failedLookups
    // 3) firstSuccesses
    //
    // 1) globalResults
    // Track the global results for a particular target class name.
    // This table records whether a lookup has been performed, and what
    // was the overall result.
    //
    // The 'firstSuccesses' table is sufficient to tell which lookups
    // were successful, but is not sufficient to tell which lookups
    // failed.
    //
    // 2) failedLookups
    // Per class loader, track which values were not found.
    //
    // 3) firstSuccesses
    // Track the global result for a particular target class name.
    // When a global scan locates a target class, record which
    // class loader found that class.

    protected final Map<String, Boolean> i_globalResults;

    @Trivial
    public Map<String, Boolean> getGlobalResults() {
        return i_globalResults;
    }

    @Override
    public Boolean getGlobalResult(String className) {
        return i_getGlobalResult(internClassName(className));
    }

    protected Boolean i_getGlobalResult(String i_className) {
        return i_globalResults.get(i_className);
    }

    protected void i_setGlobalResult(String i_className, boolean value) {
        i_globalResults.put(i_className, Boolean.valueOf(value));
    }

    protected final Map<ClassSource, Set<String>> i_failedLookups;

    @Trivial
    public Map<ClassSource, Set<String>> getFailedLookups() {
        return i_failedLookups;
    }

    @Override
    @Trivial
    public Set<String> getFailedLookups(ClassSource classSource) {
        return i_failedLookups.get(classSource);
    }

    @Trivial
    protected Set<String> getFailedLookupsForcing(ClassSource classSource) {
        Set<String> specificFailedLookups = i_failedLookups.get(classSource);

        if ( specificFailedLookups == null ) {
            specificFailedLookups = new HashSet<String>();

            i_failedLookups.put(classSource, specificFailedLookups);
        }

        return specificFailedLookups;
    }

    protected boolean i_alreadyFailed(ClassSource classSource, String i_className) {
        Set<String> specificFailedLookups = getFailedLookups(classSource);

        return ( (specificFailedLookups != null) && specificFailedLookups.contains(i_className) );
    }

    protected void i_markFailed(ClassSource classSource, String i_className) {
        getFailedLookupsForcing(classSource).add(i_className);
    }

    public Map<String, ClassSource> i_firstSuccesses;

    @Override
    @Trivial
    public Map<String, ClassSource> getFirstSuccesses() {
        return i_firstSuccesses;
    }

    @Override
    public ClassSource getFirstSuccess(String className) {
        return i_getFirstSuccess(internClassName(className));
    }

    protected ClassSource i_getFirstSuccess(String i_className) {
        return i_firstSuccesses.get(i_className);
    }

    protected void i_setFirstSuccess(String i_className, ClassSource ClassSource) {
        i_firstSuccesses.put(i_className, ClassSource);
    }

    //

    protected void i_recordLookup(String i_className, ClassSource classSource) {
        i_setFirstSuccess(i_className, classSource);
        i_setGlobalResult(i_className, true);
    }

    //

    @Override
    @Trivial
    public BufferedInputStream openClassResourceStream(String className, String resourceName)
        throws ClassSource_Exception {
        return openResourceStream(className, resourceName, ClassSource.CLASS_BUFFER_SIZE);
    }

    @Override
    @Trivial
    public BufferedInputStream openResourceStream(String className, String resourceName, int bufferSize)
        throws ClassSource_Exception {
        InputStream inputStream = openResourceStream(className, resourceName); // throws ClassSource_Exception
        return new BufferedInputStream(inputStream, bufferSize);
    }

    @Override
    @Trivial
    public synchronized InputStream openResourceStream(String className, String resourceName) throws ClassSource_Exception {
        String methodName = "openResourceStream";

        Object[] logParams =
            ( logger.isLoggable(Level.FINER) ? new Object[] { getHashText(), className, resourceName, null } : null );

        String i_className = internClassName(className);

        i_recordLookup(i_className);

        Boolean globalSuccess = i_getGlobalResult(i_className);
        if ( globalSuccess != null ) {
            if ( !globalSuccess.booleanValue() ) {
                if ( logParams != null ) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] Resource [ {1} ] Class [ {2} ] ENTER / RETURN [ null ] - prior failure",
                        logParams);
                }
                return null;

            } else {
                ClassSource firstSuccess = i_getFirstSuccess(i_className);

                if (logParams != null) {
                    logParams[3] = firstSuccess;
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] Resource [ {1} ] Class [ {2} ] Found in [ {3} ] RETURN [ non-null ] - prior lookup",
                        logParams);
                }

                // TODO: There is a narrow case of a resource being located in a class
                //       source which was initially opened, but which could not be
                //       opened by a later open request.
                //
                //       That can result in a resource open request linking into the
                //       a child which is not open.

                return firstSuccess.openResourceStream(className, resourceName); // throws ClassSource_Exception
            }
        }

        if ( logParams != null ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Resource [ {1} ] Class [ {2} ] ENTER - no prior lookup",
                logParams);
        }

        InputStream inputStream = null;

        // Only attempt the open on children which were successfully opened.
        // Children which could not be opened are removed from view.

        for ( ClassSource nextClassSource : getSuccessfulOpens() ) {
            if ( logParams != null ) {
                logParams[3] = Integer.valueOf(nextClassSource.hashCode());
            }

            if ( i_alreadyFailed(nextClassSource, i_className) ) {
                if ( logParams != null ) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] Resource [ {1} ] Class [ {2} ] Skipping [ {3} ] - prior failure",
                        logParams);
                }

            } else {
                // The attempt to open the input stream on the resource
                // does not record the presence of the class in the target
                // class source.
                //
                // However, unless the open request returns null, the class
                // is present in the class source, and must be recorded as such,
                // even if an error occurred during the open.

                try {
                    inputStream = nextClassSource.openResourceStream(className, resourceName); // throws ClassSource_Exception

                    if ( inputStream == null ) {
                        if ( logParams != null ) {
                            logger.logp(Level.FINER, CLASS_NAME, methodName,
                                "[ {0} ] Resource [ {1} ] Class [ {2} ] Not found in [ {3} ]",
                                logParams);
                        }
                        i_markFailed(nextClassSource, i_className);

                    } else {
                        i_setGlobalResult(i_className, true);
                        i_setFirstSuccess(i_className, nextClassSource);

                        break;
                    }

                } catch ( ClassSource_Exception e ) {
                    i_setGlobalResult(i_className, true);
                    i_setFirstSuccess(i_className, nextClassSource);

                    throw e;
                }
            }
        }

        if ( inputStream == null ) {
            i_setGlobalResult(i_className, false);

            if ( logParams != null ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Resource [ {1} ] Class [ {2} ] ENTER / RETURN [ null ] - first failure",
                    logParams);
            }
            return null;

        } else {
            if ( logParams != null ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Resource [ {1} ] Class [ {2} ] Found in [ {3} ] RETURN [ non-null ] - first lookup",
                    logParams);
            }
            return inputStream;
        }
    }

    @Override
    @Trivial
    public synchronized void closeResourceStream(String className, String resourceName, InputStream inputStream) {
        String methodName = "closeResourceStream";
        try {
            inputStream.close(); // throws IOException
        } catch ( IOException e ) {
            // "[ {0} ] ] Failed to close resource [ {1} ] for class [ {2} ]: {3}",
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_CLASSSOURCE_CLOSE2_EXCEPTION",
                new Object[] { getHashText(), resourceName, className, e });
        }
    }

    // Logging ...
    
    @Override
    @Trivial
    public void logState() {
        if ( stateLogger.isLoggable(Level.FINER) ) {
            log(stateLogger);
        }
    }

    @Override
    @Trivial
    public void log(Logger useLogger) {
        String methodName = "log";

        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "BEGIN STATE [ {0} ]", getHashText());

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class sources: BEGIN");
        for (ClassSource nextClassSource : getClassSources()) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ {0} ]", nextClassSource);
        }
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class sources: END");

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Overall results: BEGIN");
        log_lookupCounts(useLogger);
        log_globalResults(useLogger);
        log_failedLookups(useLogger);
        log_firstSuccesses(useLogger);
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Overall results: END");

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "END STATE [ {0} ]", getHashText());
    }

    @Trivial
    protected void log_lookupCounts(Logger useLogger) {
        String methodName = "log_lookupCounts";

        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Lookup Counts: BEGIN");

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Total unique [ {0} ]",
                                              Integer.valueOf(getLookupCounts().size()));

        Object[] params = new Object[] { null, null };

        for ( Map.Entry<String, Integer> nextEntry : getLookupCounts().entrySet() ) {
            params[0] = nextEntry.getKey();
            params[1] = nextEntry.getValue();

            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ {0} ] [ {1} ]", params);
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Lookup Counts: END");
    }

    @Trivial
    protected void log_globalResults(Logger useLogger) {
        String methodName = "log_globalResults";
        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Global Results: BEGIN");

        useLogger.logp(Level.FINER, CLASS_NAME, methodName,
            "Total unique [ {0} ]",
            Integer.valueOf(getGlobalResults().size()));

        Object[] params = new Object[] { null, null };

        for ( Map.Entry<String, Boolean> nextEntry : getGlobalResults().entrySet() ) {
            params[0] = nextEntry.getKey();
            params[1] = nextEntry.getValue();

            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ {0} ] [ {1} ]", params);
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Global Results: END");
    }

    @Trivial
    protected void log_firstSuccesses(Logger useLogger) {
        String methodName = "log_firstSuccesses";

        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "First Successes: BEGIN");

        Object[] params = new Object[] { null, null };

        for ( Map.Entry<String, ClassSource> nextEntry : getFirstSuccesses().entrySet() ) {
            params[0] = nextEntry.getKey();
            params[1] = nextEntry.getValue().getHashText();

            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ {0} ] [ {1} ]", params);
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "First Successes: END");
    }

    @Trivial
    protected void log_failedLookups(Logger useLogger) {
        String methodName = "log_failedLookups";

        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Failed Lookups: BEGIN");

        for ( Map.Entry<ClassSource, Set<String>> nextFailedLookups : getFailedLookups().entrySet() ) {
            ClassSource nextClassSource = nextFailedLookups.getKey();
            Set<String> nextFailedClasses = nextFailedLookups.getValue();

            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ {0} ]", nextClassSource.getHashText());
            for ( String nextFailedClass : nextFailedClasses ) {
                useLogger.logp(Level.FINER, CLASS_NAME, methodName, "    [ {0} ]", nextFailedClass);
            }
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Failed Lookups: END");
    }

    //

    protected long cacheReadTime;

    @Override
    public long addCacheReadTime(long readTime, String description) {
        return ( cacheReadTime += readTime );
    }

    @Trivial
    @Override
    public long getCacheReadTime() {
        return cacheReadTime;
    }

    protected long cacheWriteTime;

    @Override
    public long addCacheWriteTime(long writeTime, String description) {
        return ( cacheWriteTime += writeTime );
    }

    @Trivial
    @Override
    public long getCacheWriteTime() {
        return cacheWriteTime;
    }

    public static class TimingDataImpl implements TimingData {
        private final int scanSources;
        private final long scanTime;
        private final int scanClasses;

        private final int readSources;
        private final long readTime;
        private final int readClasses;
        
        private final int jandexSources;
        private final long jandexTime;
        private final int jandexClasses;

        private final int externalSources;
        private final long externalTime;
        private final int externalClasses;

        private final long cacheReadTime;
        private final long cacheWriteTime;

        protected TimingDataImpl(
            int scanSources, long scanTime, int scanClasses,
            int readSources, long readTime, int readClasses,
            int jandexSources, long jandexTime, int jandexClasses,
            int externalSources, long externalTime, int externalClasses,
            long cacheReadTime, long cacheWriteTime) {

            this.scanSources = scanSources;
            this.scanTime = scanTime;
            this.scanClasses = scanClasses;

            this.readSources = readSources;
            this.readTime = readTime;
            this.readClasses = readClasses;
    
            this.jandexSources = jandexSources;
            this.jandexTime = jandexTime;
            this.jandexClasses = jandexClasses;

            this.externalSources = externalSources;
            this.externalTime = externalTime;
            this.externalClasses = externalClasses;

            this.cacheReadTime = cacheReadTime;
            this.cacheWriteTime = cacheWriteTime;
        }

        protected TimingDataImpl(TimingDataImpl other) {
            this(other.scanSources, other.scanTime, other.scanClasses,
                 other.readSources, other.readTime, other.readClasses,
                 other.jandexSources, other.jandexTime, other.jandexClasses,
                 other.externalSources, other.externalTime, other.externalClasses,
                 other.cacheReadTime, other.cacheWriteTime);
        }

        public TimingDataImpl clone() {
            return new TimingDataImpl(this);
        }

        @Override
        @Trivial
        public int getScanSources() {
            return scanSources;
        }

        @Override
        @Trivial
        public int getScanClasses() {
            return scanClasses;
        }

        @Override
        @Trivial
        public long getScanTime() {
            return scanTime;
        }

        @Override
        @Trivial
        public int getReadSources() {
            return readSources;
        }

        @Override
        @Trivial
        public int getReadClasses() {
            return readClasses;
        }

        @Override
        @Trivial
        public long getReadTime() {
            return readTime;
        }

        @Override
        @Trivial
        public int getJandexSources() {
            return jandexSources;
        }

        @Override
        @Trivial
        public long getJandexTime() {
            return jandexTime;
        }

        @Override
        @Trivial
        public int getJandexClasses() {
            return jandexClasses;
        }

        @Override
        @Trivial
        public int getExternalSources() {
            return externalSources;
        }

        @Override
        @Trivial
        public long getExternalTime() {
            return externalTime;
        }

        @Override
        @Trivial
        public int getExternalClasses() {
            return externalClasses;
        }

        @Override
        @Trivial
        public long getCacheReadTime() {
            return cacheReadTime;
        }

        @Override
        @Trivial
        public long getCacheWriteTime() {
            return cacheWriteTime;
        }
    }

    private TimingDataImpl timingData;

    @Trivial
    public void setTimingData() {
        int scanSources = 0;
        long scanTime = 0L;
        int scanClasses = 0;

        int readSources = 0;
        long readTime = 0L;
        int readClasses = 0;

        int jandexSources = 0;
        long jandexTime = 0L;
        int jandexClasses = 0;

        int externalSources = 0;
        long externalTime = 0L;
        int externalClasses = 0;

        for ( ClassSource classSource : getClassSources() ) {
            long processTime = classSource.getProcessTime();
            int processCount = classSource.getProcessCount();

            if ( getScanPolicy(classSource) == ScanPolicy.EXTERNAL ) {
                externalSources++;
                externalTime += processTime;
                externalClasses += processCount;
            } else if ( classSource.isProcessedUsingJandex() ) {
                jandexSources++;
                jandexTime += processTime;
                jandexClasses += processCount;
            } else if ( classSource.isReadFromCache() ) {
                readSources++;
                readTime += processTime;
                readClasses += processCount;
            } else {
                scanSources++;
                scanTime += processTime;
                scanClasses += processCount;
            }
        }

        timingData = new TimingDataImpl(
            scanSources, scanTime, scanClasses,
            readSources, readTime, readClasses,
            jandexSources, jandexTime, jandexClasses,
            externalSources, externalTime, externalClasses,
            getCacheReadTime(), getCacheWriteTime() );
    }

    @Override
    @Trivial
    public TimingDataImpl getTimingData() {
        return timingData;
    }

    // Obsolete

    @Override
    @Trivial
    public void log(TraceComponent tc) {
        if ( !tc.isDebugEnabled() ) {
            return;
        }

        Tr.debug(tc, MessageFormat.format("BEGIN STATE [ {0} ]", getHashText()));

        Tr.debug(tc, "Class sources: BEGIN");

        for (ClassSource nextClassSource : getClassSources()) {
            Tr.debug(tc, MessageFormat.format("  [ {0} ]", nextClassSource));
        }

        Tr.debug(tc, "Class sources: END");

        logCounts(tc);

        Tr.debug(tc, "Overall results: BEGIN");

        log_lookupCounts(tc);
        log_globalResults(tc);
        log_failedLookups(tc);
        log_firstSuccesses(tc);

        Tr.debug(tc, "Overall results: END");

        Tr.debug(tc, MessageFormat.format("END STATE [ {0} ]", getHashText()));
    }

    @Trivial
    protected void logCounts(TraceComponent tc) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("  Included classes: [ {0} ]",
                                                  Integer.valueOf(getClassInclusionCount())));

            Tr.debug(tc, MessageFormat.format("  Excluded classes: [ {0} ]",
                                                  Integer.valueOf(getClassExclusionCount())));
        }
    }

    @Trivial
    protected void log_lookupCounts(TraceComponent tc) {
        if ( !tc.isDebugEnabled() ) {
            return;
        }

        Tr.debug(tc, "Lookup Counts: BEGIN");

        Tr.debug(tc, MessageFormat.format("Total unique [ {0} ]",
                                              Integer.valueOf(getLookupCounts().size())));

        Object[] params = new Object[] { null, null };

        for ( Map.Entry<String, Integer> nextEntry : getLookupCounts().entrySet() ) {
            params[0] = nextEntry.getKey();
            params[1] = nextEntry.getValue();

            Tr.debug(tc, MessageFormat.format("  [ {0} ] [ {1} ]", params));
        }

        Tr.debug(tc, "Lookup Counts: END");
    }

    @Trivial
    protected void log_globalResults(TraceComponent tc) {
        if ( !tc.isDebugEnabled() ) {
            return;
        }

        Tr.debug(tc, "Global Results: BEGIN");

        Tr.debug(tc, MessageFormat.format("Total unique [ {0} ]",
                                              Integer.valueOf(getGlobalResults().size())));

        Object[] params = new Object[] { null, null };

        for ( Map.Entry<String, Boolean> nextEntry : getGlobalResults().entrySet() ) {
            params[0] = nextEntry.getKey();
            params[1] = nextEntry.getValue();

            Tr.debug(tc, MessageFormat.format("  [ {0} ] [ {1} ]", params));
        }

        Tr.debug(tc, "Global Results: END");
    }

    @Trivial
    protected void log_firstSuccesses(TraceComponent tc) {
        if ( !tc.isDebugEnabled() ) {
            return;
        }

        Tr.debug(tc, "First Successes: BEGIN");

        Object[] params = new Object[] { null, null };

        for ( Map.Entry<String, ClassSource> nextEntry : getFirstSuccesses().entrySet() ) {
            params[0] = nextEntry.getKey();
            params[1] = nextEntry.getValue().getHashText();

            Tr.debug(tc, MessageFormat.format("  [ {0} ] [ {1} ]", params));
        }

        Tr.debug(tc, "First Successes: END");
    }

    @Trivial
    protected void log_failedLookups(TraceComponent tc) {
        if ( !tc.isDebugEnabled() ) {
            return;
        }

        Tr.debug(tc, "Failed Lookups: BEGIN");

        for ( Map.Entry<ClassSource, Set<String>> nextFailedLookups : getFailedLookups().entrySet() ) {
            ClassSource nextClassSource = nextFailedLookups.getKey();
            Set<String> nextFailedClasses = nextFailedLookups.getValue();

            Tr.debug(tc, MessageFormat.format("  [ {0} ]", nextClassSource.getHashText()));
            for ( String nextFailedClass : nextFailedClasses ) {
                Tr.debug(tc, MessageFormat.format("    [ {0} ]", nextFailedClass));
            }
        }

        Tr.debug(tc, "Failed Lookups: END");
    }

    //

    @Override
    public void addClassSource(com.ibm.wsspi.anno.classsource.ClassSource classSource) {
        // EMPTY
    }

    @Override
    public void addClassSource(com.ibm.wsspi.anno.classsource.ClassSource classSource,
        com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy scanPolicy) {
        // EMPTY
    }

    @Override
    public Set<? extends com.ibm.wsspi.anno.classsource.ClassSource> getClassSources(
        com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy scanPolicy) {
        return null;
    }

    @Override
    public com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy getScanPolicy(
        com.ibm.wsspi.anno.classsource.ClassSource classSource) {
        return null;
    }

    @Override
    public void scanClasses(ClassSource_Streamer streamer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public com.ibm.wsspi.anno.classsource.ClassSource getParentSource() {
        return null;
    }

    @Override
    public void setParentSource(com.ibm.wsspi.anno.classsource.ClassSource classSource) {
        // EMPTY
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getCanonicalName() {
        return null;
    }

    @Override
    public void scanClasses(ClassSource_Streamer streamer, Set<String> i_seedClassNamesSet,
        com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy scanPolicy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean scanSpecificSeedClass(String specificClassName, ClassSource_Streamer streamer)
        throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean scanReferencedClass(String referencedClassNam, ClassSource_Streamer streamer)
        throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public String resourceAppend(String head, String tail) {
        return null;
    }

    @Override
    public boolean isDirectoryResource(String resourceName) {
        return false;
    }

    @Override
    public boolean isClassResource(String resourceName) {
        return false;
    }

    @Override
    public String getClassNameFromResourceName(String resourceName) {
        return null;
    }

    @Override
    public String getResourceNameFromClassName(String className) {
        return null;
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

    @Override
    public Set<String> getFailedLookups(com.ibm.wsspi.anno.classsource.ClassSource classSource) {
        return null;
    }

    @Override
    public boolean isProcessedUsingJandex() {
        return false;
    }
}
