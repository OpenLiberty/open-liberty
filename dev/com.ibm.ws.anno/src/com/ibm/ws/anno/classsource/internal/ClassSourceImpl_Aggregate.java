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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.anno.classsource.ClassSource;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts;
import com.ibm.wsspi.anno.classsource.ClassSource_Streamer;
import com.ibm.wsspi.anno.util.Util_InternMap;

/**
 * <p>Standard aggregate class source implementation.</p>
 */
public class ClassSourceImpl_Aggregate extends ClassSourceImpl implements ClassSource_Aggregate {
    @SuppressWarnings("hiding")
    public static final String CLASS_NAME = ClassSourceImpl_Aggregate.class.getName();
    private static final TraceComponent tc = Tr.register(ClassSourceImpl_Aggregate.class);

    // Top O' the world

    public ClassSourceImpl_Aggregate(
        ClassSourceImpl_Factory factory,
        Util_InternMap internMap,
        String name) {

        super(factory, internMap, name, null);

        this.seedClassSources = new HashSet<ClassSource>();
        this.partialClassSources = new HashSet<ClassSource>();
        this.excludedClassSources = new HashSet<ClassSource>();
        this.externalClassSources = new HashSet<ClassSource>();

        this.classSources = new ArrayList<ClassSource>();
        this.classSourceNames = new HashMap<String, String>();

        this.openCount = 0;
        this.successfulOpens = new ArrayList<ClassSource>();
        this.failedOpens = new HashSet<ClassSource>();

        this.totalLookups = 0L;
        this.repeatLookups = 0L;

        this.i_lookupCounts = new IdentityHashMap<String, Integer>();
        this.i_globalResults = new IdentityHashMap<String, Boolean>();
        this.i_failedLookups = new IdentityHashMap<ClassSource, Set<String>>();
        this.i_firstSuccesses = new IdentityHashMap<String, ClassSource>();

        // logging in the super constructor
    }

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

    protected int openCount;
    protected List<ClassSource> successfulOpens;
    protected Set<ClassSource> failedOpens;

    @Trivial
    public int getOpenCount() {
        return openCount;
    }

    @Trivial
    public boolean getIsOpen() {
        return ( openCount > 0 );
    }

    protected List<ClassSource> retrieveSuccessfulOpens() {
        List<ClassSource> oldSuccessfulOpens = successfulOpens;
        successfulOpens = new ArrayList<ClassSource>();
        return oldSuccessfulOpens;
    }

    @Trivial
    protected List<ClassSource> getSuccessfulOpens() {
        return successfulOpens;
    }

    protected void addSuccessfulOpen(ClassSource classSource) {
        successfulOpens.add(classSource);
    }

    @Trivial
    protected Set<ClassSource> getFailedOpens() {
        return failedOpens;
    }

    protected void addFailedOpen(ClassSource classSource) {
        failedOpens.add(classSource);
    }

    @Override
    @Trivial
    public void open() throws ClassSource_Exception {
        String methodName = "open";
        if ( tc.isEntryEnabled() ) {
            String msg = MessageFormat.format(
                "[ {0} ] Open count [ {1} ]",
                new Object[] { getHashText(), Integer.valueOf(openCount) });
            Tr.entry(tc, methodName, msg);
        }

        openCount++;

        if ( openCount == 1 ) { // First one; need to open children.
            // 'retrieveSuccessfulOpens' clears the collection of successful opens!
            //
            // The class sources must be conveyed to the re-constituted successful opens,
            // or to the failed opens.
            //
            // Once a class source reaches the failed opens collection, it will remain
            // there forever.

            for ( ClassSource nextClassSource : retrieveSuccessfulOpens() ) {
                try {
                    nextClassSource.open(); // throws ClassSource_Exception
                    addSuccessfulOpen(nextClassSource);

                } catch ( ClassSource_Exception e ) {
                    addFailedOpen(nextClassSource);

                    // autoFFDC will display the stack trace
                    // [ {0} ]: The open of child class source [{1}] caused an exception. The message is: {2}
                    Tr.warning(tc, "ANNO_CLASSSOURCE_CHILD_OPEN_EXCEPTION",
                        getHashText(), nextClassSource.getHashText(), e.getMessage());
                }
            }
        }

        if ( tc.isEntryEnabled() ) {
            String msg = MessageFormat.format(
                "[ {0} ] Open count [ {1} ]",
                new Object[] { getHashText(), Integer.valueOf(openCount) });
            Tr.exit(tc, methodName, msg);
        }
    }

    @Override
    @Trivial
    public void close() throws ClassSource_Exception {
        String methodName = "close";
        if ( tc.isEntryEnabled() ) {
            Tr.entry(tc, methodName, getHashText());
        }

        if ( openCount == 0 ) {
            if ( tc.isEntryEnabled() ) {
                String msg = MessageFormat.format(
                    "[ {0} ] ENTER/RETURN [ {1} ]",
                    new Object[] { getHashText(), Integer.valueOf(openCount) });
                Tr.exit(tc, methodName, msg);
            }
            return;
        }

        openCount--;

        if ( openCount == 0 ) { // Last one which is active; need to close the children.
            for ( ClassSource nextClassSource : getSuccessfulOpens() ) {
                String nextClassSourceName = nextClassSource.getCanonicalName();

                try {
                    nextClassSource.close(); // throws ClassSource_Exception

                } catch ( ClassSource_Exception e )  {
                    // autoFFDC will display the stack trace
                    // [ {0} ]: The close of child class source [{1}] failed with an exception. The message is {3}
                    Tr.warning(tc, "ANNO_CLASSSOURCE_CHILD_CLOSE_EXCEPTION",
                        getHashText(), nextClassSource.getHashText(), e.getMessage());
                }
            }
        }

        if ( tc.isEntryEnabled() ) {
            String msg = MessageFormat.format(
                "[{0}] Open Count [{1}]",
                getHashText(), Integer.valueOf(openCount));
            Tr.exit(tc, methodName, msg);
        }
    }

    //

    @Override
    public void addClassSource(ClassSource classSource) {
        addClassSource(classSource, ScanPolicy.SEED);
    }

    @Override
    @Trivial
    public void addClassSource(ClassSource classSource, ScanPolicy scanPolicy) {
        if ( tc.isDebugEnabled() ) {
            String msg = MessageFormat.format(
                "[ {0} ] Adding [ {1} ] [ {2} ]",
                new Object[] { getHashText(), classSource.getHashText(), scanPolicy });
            Tr.debug(tc, msg);
        }

        basicAddClassSource(classSource, scanPolicy);

        // Initially, all class sources are valid to be opened.
        addSuccessfulOpen(classSource);
    }

    @Override
    @Trivial
    public Set<ClassSource> getClassSources(ScanPolicy scanPolicy) {
        if (scanPolicy == ScanPolicy.SEED ) {
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
    @Trivial
    public Set<ClassSource> getSeedClassSources() {
        return seedClassSources;
    }

    @Override
    @Trivial
    public Set<ClassSource> getPartialClassSources() {
        return partialClassSources;
    }

    @Override
    @Trivial
    public Set<ClassSource> getExcludedClassSources() {
        return excludedClassSources;
    }

    @Override
    @Trivial
    public Set<ClassSource> getExternalClassSources() {
        return externalClassSources;
    }

    //

    protected final List<ClassSource> classSources;
    protected final Map<String, String> classSourceNames;

    protected final Set<ClassSource> seedClassSources;
    protected final Set<ClassSource> partialClassSources;
    protected final Set<ClassSource> excludedClassSources;
    protected final Set<ClassSource> externalClassSources;

    @Override
    @Trivial
    public List<ClassSource> getClassSources() {
        return classSources;
    }

    protected void basicAddClassSource(ClassSource classSource, ScanPolicy scanPolicy) {
        classSources.add(classSource);
        classSourceNames.put(classSource.getName(), classSource.getCanonicalName());

        if ( scanPolicy == ScanPolicy.SEED ) {
            seedClassSources.add(classSource);
        } else if ( scanPolicy == ScanPolicy.PARTIAL ) {
            partialClassSources.add(classSource);
        } else if ( scanPolicy == ScanPolicy.EXCLUDED ) {
            excludedClassSources.add(classSource);
        } else {
            externalClassSources.add(classSource);
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
    public String getCanonicalName(String classSourceName) {
        return classSourceNames.get(classSourceName);
    }

    @Override
    public Map<String, String> getCanonicalNames() {
        return classSourceNames;
    }

    //

    @Override
    @Trivial
    public void scanClasses(ClassSource_Streamer streamer) {
        if ( tc.isDebugEnabled() ) {
            String msg = MessageFormat.format(
                "ENTER [ {0} ] [ {1} ]",
                new Object[] { getHashText(), streamer });
            Tr.debug(tc, msg);
        }

        Set<String> i_seedClassNames = new HashSet<String>();

        int initialSize = 0;
        int finalSize = 0;

        // Only scan the children which were successfully opened.
        // Children which could not be opened are removed from view.

        for ( ClassSource childSource : getSuccessfulOpens() ) {
            String childName = childSource.getCanonicalName();

            ScanPolicy scanPolicy = getScanPolicy(childSource);
            if ( scanPolicy == ScanPolicy.EXTERNAL ) {
                continue; // Skip: External sources are only used to process referenced classes.
            }

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

            // TODO: Should the seed class scan update the lookup tables?
            //       Currently, the lookup tables are only populated on demand
            //       from info store usage.  That keeps the lookup tables small,
            //       with the added cost of repeating lookups across the class
            //       sources.  The alternative would be to populate the lookup
            //       tables during this initial scan step, which would add a
            //       storage overhead as a tradeoff for faster lookups.

            childSource.scanClasses(streamer, i_seedClassNames, scanPolicy);
            // throws ClassSource_Exception

            ClassSource_ScanCounts childScanCounts = childSource.getScanResults();
            addResults(childScanCounts);

            int nextSize = i_seedClassNames.size();

            if ( tc.isDebugEnabled()) {
                String msg = MessageFormat.format(
                    "[ {0} ] [ {1} ] [ {2} ] Added [ {3} ]",
                    new Object[] { getHashText(), childName,
                                  childSource.getHashText(),
                                  Integer.valueOf(nextSize - finalSize) });
                Tr.debug(tc, msg);
            }

            finalSize = nextSize;
        }

        if ( tc.isDebugEnabled() ) {
            Object[] logParms = new Object[] { getHashText(), null, null };

            logParms[1] = Integer.valueOf(finalSize - initialSize);
            Tr.debug(tc, MessageFormat.format("[ {0} ] RETURN [ {1} ] Added classes", logParms));

            for (ClassSource_ScanCounts.ResultField resultField : ClassSource_ScanCounts.ResultField.values()) {
                int nextResult = getResult(resultField);
                String nextResultTag = resultField.getTag();

                logParms[1] = Integer.valueOf(nextResult);
                logParms[2] = nextResultTag;

                Tr.debug(tc, MessageFormat.format("[ {0} ]  [ {1} ] {0}", logParms));
            }
        }
    }

    // Leaf class source API.
    //
    // Would need to be implemented if aggregate class sources could be put
    // within aggregate class sources.  That is not currently supported.

    @Override
    public void scanClasses(
        ClassSource_Streamer streamer,
        Set<String> i_seedClassNamesSet,
        ScanPolicy scanPolicy) {

        throw new UnsupportedOperationException();
    }

    @Override
    protected void processFromScratch(
        ClassSource_Streamer streamer,
        Set<String> i_seedClassNames,
        ScanPolicy scanPolicy) {

        throw new UnsupportedOperationException();
    }

    //

    protected long totalLookups;
    protected long repeatLookups;

    protected Map<String, Integer> i_lookupCounts;

    @Override
    @Trivial
    public long getTotalLookups() {
        return totalLookups;
    }

    protected void recordLookup() {
        totalLookups++;
    }

    @Override
    @Trivial
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
    @Trivial
    public Boolean getGlobalResult(String className) {
        return i_getGlobalResult( internClassName(className) );
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

        return ( (specificFailedLookups != null) &&
                 specificFailedLookups.contains(i_className) );
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
    @Trivial
    public ClassSource getFirstSuccess(String className) {
        return i_getFirstSuccess( internClassName(className) );
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
    @FFDCIgnore({ ClassSource_Exception.class })
    public InputStream openResourceStream(String className, String resourceName) throws ClassSource_Exception {
        Object[] logParams = ( (tc.isDebugEnabled()
            ? new Object[] { getHashText(), className, resourceName, null }
            : null) );

        String i_className = internClassName(className);

        i_recordLookup(i_className);

        Boolean globalSuccess = i_getGlobalResult(i_className);
        if ( globalSuccess != null ) {
            if ( !globalSuccess.booleanValue() ) {
                if ( logParams != null ) {
                    if (tc.isDebugEnabled()) {
                        String msg = MessageFormat.format(
                            "[ {0} ] Resource [ {1} ] Class [ {2} ]: ENTRY / RETURN [ null ] - prior failure",
                            logParams);
                        Tr.debug(tc, msg);
                    }
                }
                return null;

            } else {
                ClassSource firstSuccess = i_getFirstSuccess(i_className);

                if ( logParams != null ) {
                    logParams[3] = firstSuccess;
                    if ( tc.isDebugEnabled() ) {
                        String msg = MessageFormat.format(
                            "[ {0} ] Resource [ {1} ] Class [ {2} ] Found in [ {3} ]: RETURN [ non-null ] - prior lookup",
                            logParams);
                        Tr.debug(tc, msg);
                    }
                }

                // TODO: There is a narrow case of a resource being located in a class
                //       source which was initially opened, but which could not be
                //       opened by a later open request.
                //
                //       That can result in a resource open request linking into the
                //       a child which is not open.

                return firstSuccess.openResourceStream(className, resourceName);
                // throws ClassSource_Exception
            }
        }

        if ( logParams != null ) {
            if ( tc.isDebugEnabled() ) {
                String msg = MessageFormat.format(
                    "[ {0} ] Resource [ {1} ] Class [ {2} ] ENTRY - no prior lookup",
                    logParams);
                Tr.debug(tc, msg);
            }
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
                    String msg = MessageFormat.format(
                        "[ {0} ] Resource [ {1} ] Class [ {2} ] Skipping [ {3} ] - prior failure",
                        logParams);
                    Tr.debug(tc, msg);
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
                    inputStream = nextClassSource.openClassStream(className);
                    // throws ClassSource_Exception

                    if ( inputStream == null ) {
                        if ( logParams != null ) {
                            if ( tc.isDebugEnabled() ) {
                                String msg = MessageFormat.format(
                                    "[ {0} ] Resource [ {1} ] Class [ {2} ] Not found in [ {3} ]",
                                    logParams);
                                Tr.debug(tc, msg);
                            }
                        }
                        i_markFailed(nextClassSource, i_className);

                    } else {
                        i_setGlobalResult(i_className, true);
                        i_setFirstSuccess(i_className, nextClassSource);

                        break;
                    }

                } catch ( ClassSource_Exception e ) {
                    // do NOT process with FFDC

                    i_setGlobalResult(i_className, true);
                    i_setFirstSuccess(i_className, nextClassSource);

                    throw e;
                }
            }
        }

        if ( inputStream == null ) {
            i_setGlobalResult(i_className, false);

            if ( logParams != null ) {
                if ( tc.isDebugEnabled() ) {
                    String msg = MessageFormat.format(
                        "[ {0} ] Resource [ {1} ] Class [ {2} ]: ENTRY / RETURN [ null ] - first failure",
                        logParams);
                    Tr.debug(tc, msg);
                }
            }
            return null;

        } else {
            if ( logParams != null ) {
                if ( tc.isDebugEnabled() ) {
                    String msg = MessageFormat.format(
                        "[ {0} ] Resource [ {1} ] Class [ {2} ] Found in [ {3} ]: RETURN [ non-null ] - first lookup",
                        logParams);
                    Tr.debug(tc, msg);
                }
            }
            return inputStream;
        }
    }

    @Override
    public void closeResourceStream(String className, String resourceName, InputStream inputStream) {
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
    public int getResourceExclusionCount() {
        // Use all class sources for the exclusion count.
        //
        // Values will be zero for a class source which was never
        // successfully opened.
        //
        // TODO: There will be possible inaccuracies when there
        //       are inconsistent child open results.

        int exclusionCount = 0;

        for ( ClassSource classSource : getClassSources() ) {
            exclusionCount += classSource.getResourceExclusionCount();
        }

        return exclusionCount;
    }

    @Override
    public int getClassExclusionCount() {
        // Use all class sources for the exclusion count.
        //
        // Values will be zero for a class source which was never
        // successfully opened.
        //
        // TODO: There will be possible inaccuracies when there
        //       are inconsistent child open results.

        int exclusionCount = 0;

        for ( ClassSource classSource : getClassSources() ) {
            exclusionCount += classSource.getClassExclusionCount();
        }

        return exclusionCount;
    }

    @Override
    public int getClassInclusionCount() {
        // Use all class sources for the exclusion count.
        //
        // Values will be zero for a class source which was never
        // successfully opened.
        //
        // TODO: There will be possible inaccuracies when there
        //       are inconsistent child open results.

        int inclusionCount = 0;

        for ( ClassSource classSource : getClassSources() ) {
            inclusionCount += classSource.getClassInclusionCount();
        }

        return inclusionCount;
    }

    //

    @Override
    @Trivial
    public void log(TraceComponent logger) {
        if ( !logger.isDebugEnabled() ) {
            return;
        }

        Tr.debug(logger, MessageFormat.format("BEGIN STATE [ {0} ]", getHashText()));

        Tr.debug(logger, "Class sources: BEGIN");

        for (ClassSource nextClassSource : getClassSources()) {
            Tr.debug(logger, MessageFormat.format("  [ {0} ]", nextClassSource));
        }

        Tr.debug(logger, "Class sources: END");

        logCounts(logger);

        Tr.debug(logger, "Overall results: BEGIN");

        log_lookupCounts(logger);
        log_globalResults(logger);
        log_failedLookups(logger);
        log_firstSuccesses(logger);

        Tr.debug(logger, "Overall results: END");

        Tr.debug(logger, MessageFormat.format("END STATE [ {0} ]", getHashText()));
    }

    @Trivial
    protected void log_lookupCounts(TraceComponent logger) {
        if ( !logger.isDebugEnabled() ) {
            return;
        }

        Tr.debug(logger, "Lookup Counts: BEGIN");

        Tr.debug(logger, MessageFormat.format("Total unique [ {0} ]",
                                              Integer.valueOf(getLookupCounts().size())));

        Object[] params = new Object[] { null, null };

        for ( Map.Entry<String, Integer> nextEntry : getLookupCounts().entrySet() ) {
            params[0] = nextEntry.getKey();
            params[1] = nextEntry.getValue();

            Tr.debug(logger, MessageFormat.format("  [ {0} ] [ {1} ]", params));
        }

        Tr.debug(logger, "Lookup Counts: END");
    }

    @Trivial
    protected void log_globalResults(TraceComponent logger) {
        if ( !logger.isDebugEnabled() ) {
            return;
        }

        Tr.debug(logger, "Global Results: BEGIN");

        Tr.debug(logger, MessageFormat.format("Total unique [ {0} ]",
                                              Integer.valueOf(getGlobalResults().size())));

        Object[] params = new Object[] { null, null };

        for ( Map.Entry<String, Boolean> nextEntry : getGlobalResults().entrySet() ) {
            params[0] = nextEntry.getKey();
            params[1] = nextEntry.getValue();

            Tr.debug(logger, MessageFormat.format("  [ {0} ] [ {1} ]", params));
        }

        Tr.debug(logger, "Global Results: END");
    }

    @Trivial
    protected void log_firstSuccesses(TraceComponent logger) {
        if ( !logger.isDebugEnabled() ) {
            return;
        }

        Tr.debug(logger, "First Successes: BEGIN");

        Object[] params = new Object[] { null, null };

        for ( Map.Entry<String, ClassSource> nextEntry : getFirstSuccesses().entrySet() ) {
            params[0] = nextEntry.getKey();
            params[1] = nextEntry.getValue().getHashText();

            Tr.debug(logger, MessageFormat.format("  [ {0} ] [ {1} ]", params));
        }

        Tr.debug(logger, "First Successes: END");
    }

    @Trivial
    protected void log_failedLookups(TraceComponent logger) {
        if ( !logger.isDebugEnabled() ) {
            return;
        }

        Tr.debug(logger, "Failed Lookups: BEGIN");

        for ( Map.Entry<ClassSource, Set<String>> nextFailedLookups : getFailedLookups().entrySet() ) {
            ClassSource nextClassSource = nextFailedLookups.getKey();
            Set<String> nextFailedClasses = nextFailedLookups.getValue();

            Tr.debug(logger, MessageFormat.format("  [ {0} ]", nextClassSource.getHashText()));
            for ( String nextFailedClass : nextFailedClasses ) {
                Tr.debug(logger, MessageFormat.format("    [ {0} ]", nextFailedClass));
            }
        }

        Tr.debug(logger, "Failed Lookups: END");
    }
}
