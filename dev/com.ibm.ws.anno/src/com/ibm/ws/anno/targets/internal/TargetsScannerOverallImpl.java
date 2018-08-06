/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.targets.internal;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.anno.targets.cache.TargetCache_Options;
import com.ibm.ws.anno.targets.cache.internal.TargetCacheImpl_DataCon;
import com.ibm.ws.anno.targets.cache.internal.TargetCacheImpl_DataMod;
import com.ibm.ws.anno.util.internal.UtilImpl_IdentityStringSet;
import com.ibm.ws.anno.util.internal.UtilImpl_InternMap;
import com.ibm.ws.anno.util.internal.UtilImpl_PoolExecutor;
import com.ibm.ws.anno.util.internal.UtilImpl_Utils;
import com.ibm.wsspi.anno.classsource.ClassSource;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;

/**
 * Data dependencies:
 *
 * { Internal Source } + External Source
 *
 * Internal Source -> Internal Data
 *
 * { Internal Data } -> Targets Data
 *
 * { Internal Data } -> Class References Data
 * { Internal Data } -> Internal Class Data
 *
 * External Source + Class References Data -> External Class Data
 *
 * Internal Class Data + External Class Data -> Overall Class Data
 *
 * Internal sources are provided by the initial root class source.
 * An external source may be present, but is expected to be absent
 * in the initial root class source.
 *
 * Processing of the initial sources generates intermediate data, which
 * consists of annotation tables and class tables.
 *
 * Processing of the initial sources also generates incomplete class
 * references data, which is a set of resolved class names and a set of
 * unresolved class names.  The incomplete class references data is necessary
 * to complete the class data.
 *
 * Processing of the external source and the incomplete class references
 * data generates the external class data.
 *
 * A merge of the internal class data and the external class data generates
 * the overall class data.
 *
 * Data caching is performed according to this data flow:
 *
 * The list of class sources is cached as a class source table.  Changes
 * to this table force particular targets data to be recomputed.  The external
 * source is not listed in the class source table.
 *
 * Data for each internal source is cached.
 *
 * The class references data is cached.
 *
 * The internal class data is cached.
 *
 * The external class data is cached.
 *
 * The overall class data is not cached: This data is always regenerated
 * by merging the internal class data and the external class data.
 */

public class TargetsScannerOverallImpl extends TargetsScannerBaseImpl {
    public static final String CLASS_NAME = TargetsScannerOverallImpl.class.getSimpleName();
    
    protected String priorResult(String resultType, String reason, boolean isChanged) {
        return MessageFormat.format(
                "[ {0} ] ENTER / RETURN Valid (prior result) [ {1} ] [ {2} ]: {3}",
                getHashText(),
                resultType,
                Boolean.valueOf(!isChanged),
                reason);
    }

    protected String newResult(String resultType, String reason, boolean isChanged) {
        return MessageFormat.format(
                "[ {0} ] RETURN Valid (new result) [ {1} ] [ {2} ]: {3}",
                getHashText(),
                resultType,
                Boolean.valueOf(!isChanged),
                reason);
    }

    //

    protected TargetsScannerOverallImpl(
            AnnotationTargetsImpl_Targets targets,
            ClassSource_Aggregate rootClassSource,
            TargetCacheImpl_DataMod modData) {

        super(targets, rootClassSource);

        String methodName = "<init>";

        this.internMapControl = new InternMapControl();
        this.targetsControl = new TargetsControl();

        this.modData = modData;

        this.containerTable = null;

        this.changedContainerTableReason = null;
        this.changedContainerTable = false;

        // The targets map is in the superclass.

        this.changedTargets = new HashSet<String>();
        this.changedTargetsReasons = new HashMap<String, String>();

        // The class table is in the superclass.

        this.changedClassTableReason = null;
        this.changedClassTable = false;

        this.i_resolvedClassNames = null;
        this.i_unresolvedClassNames = null;

        this.changedClassNamesReason = null;
        this.changedClassNames = false;

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", getHashText());
        }
    }

    // Used to lock down the intern maps while re-interning internal container data.
    protected static class InternMapControl extends Object {
        // EMPTY
    }
    protected final Object internMapControl;

    protected Object getInternMapControl() {
        return internMapControl;
    }

    // Used to safely get/put targets data and targets results to the several targets
    // collections.
    protected static class TargetsControl extends Object {
        // EMPTY
    }
    protected final Object targetsControl;

    protected Object getTargetsControl() {
        return targetsControl;
    }

    // When extra treads are enabled, targets data are created and populated
    // using their own intern maps.
    //
    // That adds a step to the targets data, which is to re-intern the data
    // using the master intern maps.
    //
    // The added intern step requires a lock-down of the master intern maps.
    // The alternative is to add fine grained locking to the intern maps.
    // That alternative is avoided for efficiency.

    @Override
    protected TargetsTableImpl createTargetsData(String childClassSourceName) {
        if ( isScanSingleThreaded() ) {
            return super.createTargetsData(childClassSourceName);
        } else {
            return new TargetsTableImpl( getFactory(), childClassSourceName );
        }
    }

    protected TargetsTableImpl internTargetsData(TargetsTableImpl targetsData) {
        if ( isScanSingleThreaded() ) {
            return targetsData;
        }

        synchronized ( getInternMapControl() ) {
            return new TargetsTableImpl( targetsData,
                                         getClassNameInternMap(),
                                         getFieldNameInternMap(),
                                         getMethodSignatureInternMap() );
        }
    }

    // External class source processing ...
    //
    // The external class source is expected to be added after
    // processing the internal class sources.
    //
    // There is at most one external class source.

    public ClassSource getExternalClassSource() {
        ClassSource_Aggregate useRootClassSource = getRootClassSource();

        for ( ClassSource classSource : useRootClassSource.getClassSources() ) {
            if ( useRootClassSource.getScanPolicy(classSource) == ScanPolicy.EXTERNAL ) {
                return classSource;
            }
        }

        return null;
    }

    // Cache access ...

    protected TargetCacheImpl_DataMod modData;

    public TargetCacheImpl_DataMod getModData() {
        return modData;
    }

    // The table of child class sources of the root aggregate class source.

    protected TargetsTableContainersImpl containerTable;

    public TargetsTableContainersImpl getContainerTable() {
        return containerTable;
    }

    // Change information for the list of child class sources.

    protected String changedContainerTableReason;
    protected boolean changedContainerTable;

    public String getChangedContainerReason() {
        return changedContainerTableReason;
    }

    public boolean getChangedContainerTable() {
        return changedContainerTable;
    }

    protected void setContainerTable(TargetsTableContainersImpl containerTable,
                                     String reason, boolean isChanged) {
        this.containerTable = containerTable;

        this.changedContainerTableReason = reason;
        this.changedContainerTable = isChanged;
    }

    // Change information for the child class sources.

    protected final Map<String, String> changedTargetsReasons; // Class source name -> reason
    protected final Set<String> changedTargets; // Class source name

    protected Map<String, String> getChangedTargetsDataReasons() {
        return changedTargetsReasons;
    }

    protected Set<String> getChangedTargetsData() {
        return changedTargets;
    }

    protected void setChangedTargetsData(String classSourceName, String reason, boolean isChanged) {
        getChangedTargetsDataReasons().put(classSourceName, reason);

        if ( isChanged ) {
            getChangedTargetsData().add(classSourceName);
        }
    }

    public String getChangedTargetsDataReason(String classSourceName) {
        return getChangedTargetsDataReasons().get(classSourceName);
    }

    public boolean isChangedTargetsData(String classSourceName) {
        return getChangedTargetsData().contains(classSourceName);
    }

    protected void putTargetsData(String classSourceName, TargetsTableImpl targetsData,
                                  String reason, boolean isChanged) {

        putTargetsData(classSourceName, targetsData);
        setChangedTargetsData(classSourceName, reason, isChanged);
    }

    // Change information for the aggregate class table.

    protected String changedClassTableReason;
    protected boolean changedClassTable;

    public String getChangedClassTableReason() {
        return changedClassTableReason;
    }

    public boolean isChangedClassTable() {
        return changedClassTable;
    }

    public void setClassTable(TargetsTableClassesMultiImpl classTable,
                              String reason, boolean isChanged) {
        setClassTable(classTable);

        this.changedClassTableReason = reason;
        this.changedClassTable = isChanged;
    }

    // Partial class name results:
    //
    // Processing of the class sources is partitioned into processing of
    // the initial class sources and processing of the final class source.
    // Different processing applies to the initial and final class sources.
    //
    // The initial class sources comprise the module class path elements.
    // The final class source is the module external references class loader.
    //
    // Normal scan processing, which records both annotations and class
    // information, and which records data for all elements, is performed on
    // the initial class sources.
    //
    // Limited scan processing, which records just class information, and which
    // is restricted to referenced classes, is performed on the final class
    // source.
    //
    // Resolved and unresolved class names, generated from the processing of
    // the initial class sources, are inputs to the processing of the final
    // class source.  These class names must be stored to cache to handle the
    // case where none of the initial class sources has changes but the final
    // class source must be rescanned.  This is the most frequent caching case,
    // where the initial class sources have no changes, but the final class source
    // shows changes.  The final class source will usually show changes because
    // it cannot be time stamped.

    protected Set<String> i_resolvedClassNames;
    protected Set<String> i_unresolvedClassNames;

    protected String changedClassNamesReason;
    protected boolean changedClassNames;

    public Set<String> getResolvedClassNames() {
        return i_resolvedClassNames;
    }

    public Set<String> getUnresolvedClassNames() {
        return i_unresolvedClassNames;
    }

    public String getChangedClassNamesReason() {
        return changedClassNamesReason;
    }

    public boolean isChangedClassNames() {
        return changedClassNames;
    }

    //

    /**
     * Tell if the list of child class sources has changed.
     *
     * Answer the previously stored result, if one is available.
     * 
     * If the containers table has changed, write the updated table. 
     *
     * A change to the class source list means that the aggregate
     * information must be recomputed.
     * 
     * @return True if the child class source list is unchanged.  False
     *     if the list is changed.
     */
    protected boolean validContainerTable() {
        String methodName = "validContainerTable";

        if ( containerTable != null ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                        priorResult("container table", changedContainerTableReason, changedContainerTable));
            }
            return !changedContainerTable;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", getHashText());
        }

        boolean isChanged;
        String isChangedReason;

        TargetsTableContainersImpl useContainerTable;

        if ( !modData.hasContainersTable() ) {
            isChanged = true;
            isChangedReason = "Cache miss";

            useContainerTable = createContainerTable(rootClassSource);

        } else {
            useContainerTable = createContainerTable();
            if ( !modData.readContainerTable(useContainerTable) ) {
                isChanged = true;
                isChangedReason = "Cache miss (read failure)";

                useContainerTable = createContainerTable(rootClassSource);

            } else {
                if ( modData.isAlwaysValid() ) {
                    isChanged = false;
                    isChangedReason = "Cache hit (forced valid)";

                } else {
                    TargetsTableContainersImpl newContainerTable = createContainerTable(rootClassSource);
                    if ( newContainerTable.sameAs(useContainerTable) ) {
                        isChanged = false;
                        isChangedReason = "Cache hit (valid)";
                    } else {
                        useContainerTable = newContainerTable;
                        isChanged = true;
                        isChangedReason = "Cache hit (invalid)";
                    }
                }
            }
        }

        if ( isChanged ) {
            modData.writeContainersTable(useContainerTable);
        }

        setContainerTable(useContainerTable, isChangedReason, isChanged);

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, newResult("container table", isChangedReason, isChanged));
        }
        return !isChanged;
    }

    // This method supports both concurrent and single threaded processed.
    //
    // Synchronization is placed around the targets data, at the beginning, when
    // a check is made for prior results, and at the end, when storing new results.
    //
    // Synchronization, while not necessary for single threaded processing, is
    // added for both modes to keep the code consistent.
    //
    // In single threaded mode, the targets data is processed using the master
    // intern maps.  In multi-threaded mode, each targets data has its own intern
    // maps, and a final step is added to recreate the targets data using the
    // master intern maps.  Intern maps are placed by 'getTargetsData'.  The
    // recreation of the targets data, when necessary, is don by by
    // 'internTargetsData'.

    protected boolean validInternalContainer(ClassSource classSource, ScanPolicy scanPolicy) {
        String methodName = "validInternalContainer";

        String classSourceName = classSource.getName();

        synchronized ( getTargetsControl() ) {
            TargetsTableImpl priorTargetsData = getTargetsData(classSourceName);
            if ( priorTargetsData != null ) {
                String priorIsChangedReason = getChangedTargetsDataReason(classSourceName);
                boolean priorIsChanged = isChangedTargetsData(classSourceName);

                if ( logger.isLoggable(Level.FINER) ) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                        priorResult("Internal class source " + classSourceName, priorIsChangedReason, priorIsChanged));
                }
                return !priorIsChanged;
            }
        }

        boolean isChangedAll;
        boolean isChangedJustStamp;
        String isChangedReason;

        TargetsTableImpl useTargetsData;

        TargetCacheImpl_DataCon conData = modData.getActiveCon(classSourceName);
        if ( conData == null ) {
            conData = modData.putActiveCon(classSourceName);

            useTargetsData = scanInternal(classSource,
                TargetsVisitorClassImpl.DONT_RECORD_UNRESOLVED,
                TargetsVisitorClassImpl.DONT_RECORD_RESOLVED);
            useTargetsData = internTargetsData(useTargetsData);

            isChangedAll = true;
            isChangedJustStamp = false;
            isChangedReason = "Cache miss";

        } else {
            useTargetsData = createTargetsData(classSourceName);
            if ( !conData.read(useTargetsData) ) {
                useTargetsData = scanInternal(classSource,
                    TargetsVisitorClassImpl.DONT_RECORD_UNRESOLVED,
                    TargetsVisitorClassImpl.DONT_RECORD_RESOLVED);
                useTargetsData = internTargetsData(useTargetsData);

                isChangedAll = true;
                isChangedJustStamp = false;
                isChangedReason = "Cache miss (read failure)";

            } else {
                if ( modData.isAlwaysValid() ) {
                    useTargetsData = internTargetsData(useTargetsData);

                    isChangedAll = false;
                    isChangedJustStamp = false;
                    isChangedReason = "Cache hit (forced valid)";

                } else if ( validStamp(classSource, conData) ) {
                    useTargetsData = internTargetsData(useTargetsData);

                    isChangedAll = false;
                    isChangedJustStamp = false;
                    isChangedReason = "Cache hit (valid stamp)";

                } else {
                    TargetsTableImpl newTargetsData = scanInternal(classSource,
                        TargetsVisitorClassImpl.DONT_RECORD_UNRESOLVED,
                        TargetsVisitorClassImpl.DONT_RECORD_RESOLVED);

                    if ( !useTargetsData.sameAs(newTargetsData) ) {
                        useTargetsData = internTargetsData(newTargetsData);

                        isChangedAll = true;
                        isChangedJustStamp = false;
                        isChangedReason = "Cache hit (invalid stamp; invalid data)";

                    } else {
                        useTargetsData = internTargetsData(useTargetsData);

                        isChangedAll = false;
                        isChangedJustStamp = true;
                        isChangedReason = "Cache hit (invalid stamp; valid data)";
                    }
                }
            }
        }

        if ( isChangedAll ) {
            conData.write(useTargetsData);
        } else if ( isChangedJustStamp ) {
            conData.writeStamp(useTargetsData);
        }

        synchronized( getTargetsControl() ) {
            putTargetsData(classSourceName, useTargetsData, isChangedReason, isChangedAll);
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        newResult("Internal class source " + classSourceName, isChangedReason, isChangedAll));
        }
        return !isChangedAll;
    }

    protected boolean validStamp(ClassSource classSource, TargetCacheImpl_DataCon conData) {
        String methodName = "validStamp";

        String classSourceName = classSource.getName();

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] ENTER [ {1} ]",
                        new Object[] { getHashText(), classSourceName });
        }

        String isChangedReason;
        boolean isChanged;

        String currentStamp = classSource.getStamp();

        if ( currentStamp.equals(ClassSource.UNRECORDED_STAMP) ||
             currentStamp.equals(ClassSource.UNAVAILABLE_STAMP) ) {
            isChanged = true;
            isChangedReason = "Non-comparable stamp [ " + currentStamp + " ]";
            
        } else if ( conData == null ) {
            isChanged = true;
            isChangedReason = "Cache miss";

        } else if ( !conData.hasTimeStampFile() ) {
            isChanged = true;
            isChangedReason = "Cache miss (stamp)";

        } else {
            TargetsTableTimeStampImpl cachedStampTable = conData.readStampTable();

            if ( cachedStampTable == null ) {
                isChanged = true;
                isChangedReason = "Cache miss (read failure)";

            } else {
                String cachedStamp = cachedStampTable.getStamp();
                isChanged = cachedStamp.equals(currentStamp);
                if ( isChanged ) {
                    isChangedReason = "Cache hit (valid stamp " + currentStamp + ")";
                } else {
                    isChangedReason = "Cache hit (invalid stamp; current " + currentStamp + " prior " + cachedStamp + ")";
                }
            }
        }

        // Write is done with the container.

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] RETURN [ {1} ]: {2} ]",
                        new Object[] { getHashText(), Boolean.valueOf(isChanged), isChangedReason });
        }
        return isChanged;
    }

    protected boolean validInternalContainers_Select() {
        if ( isScanSingleThreaded() ) {
            return validInternalContainers();
        } else {
            return validInternalContainers_Concurrent();
        }
    }
    
    protected boolean validInternalContainers() {
        String methodName = "validInternalContainers";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", getHashText());
        }

        boolean changedTable = validContainerTable();

        // Don't return until all internal containers were checked!
        // That forces data to be present for all internal containers.

        int changedContainers = 0;

        for ( ClassSource childClassSource : rootClassSource.getClassSources() ) {
            ScanPolicy childScanPolicy = rootClassSource.getScanPolicy(childClassSource);
            if ( childScanPolicy == ScanPolicy.EXTERNAL ) {
                continue;
            }

            if ( !validInternalContainer(childClassSource, childScanPolicy) ) {
                changedContainers++;
            }
        }

        boolean changed = ( changedTable || (changedContainers != 0) );

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Changed Containers [ {1} ]",
                    new Object[] { getHashText(), Integer.valueOf(changedContainers) });
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] RETURN [ {1} ]",
                    new Object[] { getHashText(), Boolean.valueOf(!changed) });
        }
        return !changed;
    }

    protected boolean validInternalContainers_Concurrent() {
        String methodName = "validInternalContainers_Concurrent";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", getHashText());
        }

        boolean changedTable = validContainerTable();

        // Don't return until all internal containers were checked!
        // That forces data to be present for all internal containers.

        List<? extends ClassSource> childClassSources = rootClassSource.getClassSources();
        int numChildClassSources = childClassSources.size();

        final boolean validContainers[] = new boolean[numChildClassSources];

        // Set the number of scan threads to the minimum of three values:
        //     the requested count of scan threads
        //     the maximum allowed count of scan threads
        //     the number of child class sources

        int scanThreads = getScanOptions().getScanThreads();
        if ( scanThreads == TargetCache_Options.WRITE_THREADS_UNBOUNDED) {
            scanThreads = TargetCache_Options.WRITE_THREADS_MAX;
        }
        if ( scanThreads > numChildClassSources ) {
            scanThreads = numChildClassSources; 
        }
        if ( scanThreads > TargetCache_Options.WRITE_THREADS_MAX ) {
            scanThreads = TargetCache_Options.WRITE_THREADS_MAX;
        }

        int corePoolSize = scanThreads;
        int maxPoolSize = scanThreads;

        final UtilImpl_PoolExecutor validatorPool = UtilImpl_PoolExecutor.createBlockingExecutor(
            corePoolSize, maxPoolSize,
            numChildClassSources);

        for ( int sourceNo = 0; sourceNo < numChildClassSources; sourceNo++ ) {
            final ClassSource childClassSource = childClassSources.get(sourceNo);
            final ScanPolicy childScanPolicy = rootClassSource.getScanPolicy(childClassSource);

            if ( childScanPolicy == ScanPolicy.EXTERNAL ) {
                validContainers[sourceNo] = true;
                validatorPool.completeExecution();

            } else {
                final int useSourceNo = sourceNo;

                Runnable validator = new Runnable() {
                    @Override
                    public void run() {
                        validContainers[useSourceNo] = validInternalContainer(childClassSource, childScanPolicy);
                        validatorPool.completeExecution();
                    }
                };

                validatorPool.execute(validator);
            }
        }

        try {
            validatorPool.waitForCompletion(); // throws InterruptedException
        } catch ( InterruptedException e ) {
               // CWWKC00??W 
               logger.logp(Level.WARNING, CLASS_NAME, methodName,
                       "[ {0} ] ANNO_TARGETS_CACHE_EXCEPTION [ {1} ]",
                       new Object[] { getHashText(), e });
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cache error", e);
        }

        boolean changedAnyContainer = false;
        for ( boolean validContainer : validContainers ) {
            if ( !validContainer ) {
                changedAnyContainer = true;
                break;
            }
        }

        boolean changed = ( changedTable || changedAnyContainer );

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Changed Container [ {1} ]",
                    new Object[] { getHashText(), Boolean.valueOf(changedAnyContainer) });

            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] RETURN [ {1} ]",
                        new Object[] { getHashText(), Boolean.valueOf(!changed) });
        }
        return !changed;
    }

    //

    protected boolean validUnresolvedClasses() {
        String methodName = "validUnresolvedClasses";

        boolean isChanged;
        String isChangedReason;

        if ( i_resolvedClassNames != null ) {
            isChangedReason = getChangedClassNamesReason();
            isChanged = isChangedClassNames();

            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            priorResult("Unresolved classes", isChangedReason, isChanged));
            }
            return !isChanged;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] ENTER: Unresolved classes",
                        getHashText());
        }

        UtilImpl_IdentityStringSet i_cachedResolvedClassNames = null;
        UtilImpl_IdentityStringSet i_cachedUnresolvedClassNames = null;

        if ( !modData.hasResolvedRefs() ) {
            isChanged = true;
            isChangedReason = "Cache miss (resolved class names)";

        } else if ( !modData.hasUnresolvedRefs() ) {
            isChanged = true;
            isChangedReason = "Cache miss (unresolved class names)";

        } else {
            i_cachedResolvedClassNames = createIdentityStringSet();
            if ( !modData.readResolvedRefs(getClassNameInternMap(), i_cachedResolvedClassNames) ) {
                i_cachedResolvedClassNames = null;

                isChanged = true;
                isChangedReason = "Cache miss (read failure on resolved class names)";

            } else {
                i_cachedUnresolvedClassNames = createIdentityStringSet();
                if ( !modData.readUnresolvedRefs(getClassNameInternMap(), i_cachedUnresolvedClassNames) ) {
                    i_cachedResolvedClassNames = null;
                    i_cachedUnresolvedClassNames = null;

                    isChanged = true;
                    isChangedReason = "Cache miss (read failure on unresolved class names)";

                } else {
                    isChanged = false;
                    isChangedReason = "Comparison needed -- prior data is available";
                }
            }
        }

        boolean didValidate;
        
        if ( !isChanged ) {
            if ( modData.isAlwaysValid() ) {
                didValidate = false;
                isChangedReason = "Cache hit (forced valid)";

            } else if ( !validInternalContainers_Select() ) {
                didValidate = true;
                isChanged = true;
                isChangedReason = "Comparison still needed -- containers show changes";

            } else {
                didValidate = false;
                isChangedReason = "Cache hit (valid)";
            }

        } else {
            didValidate = false;
        }

        if ( isChanged ) {
            if ( !didValidate ) {
                validInternalContainers_Select();
            }

            UtilImpl_IdentityStringSet i_newResolvedClassNames = createIdentityStringSet();
            UtilImpl_IdentityStringSet i_newUnresolvedClassNames = createIdentityStringSet();

            for ( ClassSource classSource : rootClassSource.getClassSources() ) {
                if ( rootClassSource.getScanPolicy(classSource) == ScanPolicy.EXTERNAL ) {
                    continue;
                }

                TargetsTableImpl targetsData = getTargetsData(classSource);

                targetsData.updateClassNames(i_newResolvedClassNames, i_newUnresolvedClassNames);
            }

            if ( !i_newResolvedClassNames.i_equals(i_cachedResolvedClassNames) ) {
                isChangedReason = "Cache hit (invalid resolved class names)";

                i_cachedResolvedClassNames = i_newResolvedClassNames;
                i_cachedUnresolvedClassNames = i_newUnresolvedClassNames;

            } else if ( !i_newUnresolvedClassNames.i_equals(i_cachedUnresolvedClassNames) ) {
                isChangedReason = "Cache hit (invalid unresolved class names)";

                i_cachedResolvedClassNames = i_newResolvedClassNames;
                i_cachedUnresolvedClassNames = i_newUnresolvedClassNames;

            } else {
                isChangedReason = "Cache hit (valid)";
                isChanged = false;
            }

            if ( isChanged && modData.isValidating() ) {
                logger.logp(Level.WARNING, CLASS_NAME, methodName,
                        "[ {0} ] Validation Error: Class resolution data [ {1} ]",
                        new Object[] { getHashText(), isChangedReason });
            }
        }

        i_resolvedClassNames = i_cachedResolvedClassNames;
        i_unresolvedClassNames = i_cachedUnresolvedClassNames;

        changedClassNamesReason = isChangedReason;
        changedClassNames = isChanged;

        if ( isChanged ) {
            modData.writeResolvedRefs(i_resolvedClassNames);
            modData.writeUnresolvedRefs(i_unresolvedClassNames);
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    newResult("Unresolved classes", isChangedReason, isChanged));
        }
        return !isChanged;
    }

    protected boolean validInternalResults() {
        String methodName = "validInternalResults";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", getHashText());
        }

        boolean isChanged;
        String isChangedReason;

        TargetsTableImpl[] cachedBuckets = createResultBuckets();

        boolean readInternalResults;

        if ( isScanSingleThreaded() ) {
            readInternalResults = readInternalResults(cachedBuckets);
        } else {
            readInternalResults = readInternalResults_Concurrent(cachedBuckets);
        }

        boolean didValidate;

        if ( !readInternalResults ) {
            isChangedReason = "Cache miss";
            didValidate = false;
            isChanged = true;

        } else {
            if ( modData.isAlwaysValid() ) {
                didValidate = false;                
                isChanged = false;
                isChangedReason = "Cache hit (forced valid)";

            } else {
                if ( !validInternalContainers_Select() ) {
                    didValidate = true;
                    isChanged = true;
                    isChangedReason = "Cache hit (invalid)";

                } else {
                    didValidate = true;
                    isChanged = false;
                    isChangedReason = "Cache hit (valid)";
                }
            }
        }

        if ( isChanged ) {
            if ( !didValidate ) {
                validInternalContainers_Select();
            }

            TargetsTableImpl[] newBuckets = createResultBuckets();

            mergeAnnotations(newBuckets);

            for ( ScanPolicy scanPolicy : ScanPolicy.values() ) {
                if ( scanPolicy == ScanPolicy.EXTERNAL ) {
                    continue;
                }

                modData.writeResultData( scanPolicy, newBuckets[ scanPolicy.ordinal() ] );
            }

            cachedBuckets = newBuckets;
        }

        putResultBuckets(cachedBuckets);

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, newResult("Internal results", isChangedReason, isChanged));
        }
        return !isChanged;
    }

    protected boolean readInternalResults(TargetsTableImpl[] buckets) {
        boolean isAnyMissing = false;

        for ( ScanPolicy scanPolicy : ScanPolicy.values() ) {
            if ( scanPolicy == ScanPolicy.EXTERNAL ) {
                continue;
            }

            if ( readResults(scanPolicy, buckets) == null ) {
                isAnyMissing = true;
            }
        }

        return !isAnyMissing;
    }

    // Do not pool the internal result readers: there will only be at most three of these.

    protected boolean readInternalResults_Concurrent(final TargetsTableImpl[] buckets) {
        String methodName = "readInternalResults";

        final boolean[] missingResults = new boolean[ ScanPolicy.values().length ];

        Thread[] readThreads = new Thread[ ScanPolicy.values().length ];

        for ( ScanPolicy scanPolicy : ScanPolicy.values() ) {
            if ( scanPolicy == ScanPolicy.EXTERNAL ) {
                continue;
            }

            final ScanPolicy useScanPolicy = scanPolicy;

            Runnable readRunner = new Runnable() {
                @Override                
                public void run() {
                    if ( readResults(useScanPolicy, buckets) == null ) {
                        missingResults[ useScanPolicy.ordinal() ] = true;
                    }
                }
            };

            Thread readThread = UtilImpl_Utils.createThread(readRunner, "annotation reader : " + scanPolicy.name());

            readThread.start();

            readThreads[ scanPolicy.ordinal() ] = readThread;
        }

        for ( ScanPolicy scanPolicy : ScanPolicy.values() ) {
            if ( scanPolicy == ScanPolicy.EXTERNAL ) {
                continue;
            }

            try {
                readThreads[ scanPolicy.ordinal() ].join(); // throws InterruptedException

            } catch ( InterruptedException e ) {
                   // CWWKC00??W 
                   logger.logp(Level.WARNING, CLASS_NAME, methodName,
                           "[ {0} ] ANNO_TARGETS_CACHE_EXCEPTION [ {1} ]",
                           new Object[] { getHashText(), e });
                logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cache error", e);

                missingResults[scanPolicy.ordinal()] = true;
            }
        }

        for ( ScanPolicy scanPolicy : ScanPolicy.values() ) {
            if ( (scanPolicy != ScanPolicy.EXTERNAL) && missingResults[scanPolicy.ordinal()] ) {
                return false;
            }
        }
        return true;
    }

    // Similar to 'validInternalContainer', this method supports both
    // concurrent and single threaded processed.
    //
    // The result data is thread local when read: No synchronization is necessary
    // between reads.
    //
    // In single threaded mode, the targets data is processed using the master
    // intern maps.  In multi-threaded mode, each targets data has its own intern
    // maps, and a final step is added to recreate the targets data using the
    // master intern maps.  Intern maps are placed by 'getTargetsData'.  The
    // recreation of the targets data, when necessary, is done by
    // 'internTargetsData'.

    protected TargetsTableImpl readResults(ScanPolicy scanPolicy, TargetsTableImpl[] buckets) {
        if ( !modData.hasResultConData(scanPolicy) ) {
            return null;
        }

        TargetsTableImpl cachedResultData = createTargetsData( scanPolicy.name() );

        if ( modData.readResultData(scanPolicy, cachedResultData) ) {
            cachedResultData = internTargetsData(cachedResultData);

            if ( buckets != null ) {
                buckets[ scanPolicy.ordinal() ] = cachedResultData;
            }

        } else {
            cachedResultData = null;
        }

        return cachedResultData;
    }

    protected boolean validInternalClasses() {
        String methodName = "validInternalClasses";
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", getHashText());
        }

        String reason;
        boolean isChanged;

        TargetsTableClassesMultiImpl cachedClassTable;

        if ( !modData.hasClassRefs() ) {
            cachedClassTable = null;
        } else {
            cachedClassTable = createClassTable();
            if ( !modData.readClassRefs(cachedClassTable) ) {
                cachedClassTable = null;
            }
        }

        if ( cachedClassTable == null ) {
            reason = "Absent class refs";
            isChanged = true;

        } else {
            if ( modData.isAlwaysValid() ) {
                reason = "Cached; Forced valid";
                isChanged = false;

            } else {
                reason = "Cached";
                isChanged = false;

                if ( !validInternalContainers_Select() ) {
                    reason += "; changed container";
                    isChanged = true;
                }

                if ( !isChanged ) {
                    reason += "; unchanged containers";
                }
            }
        }

        if ( isChanged ) {
            @SuppressWarnings("unused")
            boolean validInternalContainers = validInternalContainers_Select();

            TargetsTableClassesMultiImpl newClassTable = createClassTable();

            mergeClasses(newClassTable);

            modData.writeClassRefs(newClassTable);

            cachedClassTable = newClassTable;
        }

        classTable = cachedClassTable;

        setClassTable(classTable, reason, isChanged);

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, newResult("Internal results", reason, isChanged));
        }
        return !isChanged;
    }

    protected boolean validInternal() {
        boolean validUnresolvedClasses = validUnresolvedClasses();
        boolean validInternalResults = validInternalResults();
        boolean validInternalClasses = validInternalClasses();

        if ( logger.isLoggable(Level.FINER) ) {
            logInternalResults(logger);
        }

        return ( validUnresolvedClasses && validInternalResults && validInternalClasses );
    }

    //

    protected boolean validExternal() {
        String methodName = "validExternal";

        ClassSource externalClassSource = getExternalClassSource();
        if ( externalClassSource == null ) {
            throw new IllegalStateException("An external class source is required");
        }

        String classSourceName = externalClassSource.getName();

        String isChangedReason;
        boolean isChanged;

        TargetsTableImpl externalData = getTargetsData(classSourceName);
        if ( externalData != null ) {
            isChangedReason = getChangedTargetsDataReason(classSourceName);
            isChanged = isChangedTargetsData(classSourceName);

            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            priorResult("External source " + classSourceName, isChangedReason, isChanged));
            }
            return !isChanged;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "ENTER: External source [ {0} ]",
                        classSourceName);
        }

        isChanged = true;
        isChangedReason = "Always invalid";

        @SuppressWarnings("unused")
        boolean validUnresolvedClasses = validUnresolvedClasses();

        externalData = scanExternal(externalClassSource, i_resolvedClassNames, i_unresolvedClassNames);

        // See the comment on 'mergeClasses': This must be synchronized
        // with the write of the class table which occurs in 
        // 'TargetCacheImpl_ModData.writeClassRefs'.

        TargetsTableClassesMultiImpl useClassTable = getClassTable();
        synchronized( useClassTable ) {
            mergeClasses( useClassTable, externalData );
        }

        putTargetsData(classSourceName, externalData, isChangedReason, isChanged);
        putExternalResults(externalData); // Note the table reuse.

        // No need to put the class table: It is already set.

        // Neither the external results not the fully populated class table is written.

        if ( logger.isLoggable(Level.FINER) ) {
            logClassInfo(logger);
            logExternalResults(logger);
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    newResult("External source " + classSourceName, isChangedReason, isChanged));
        }
        return !isChanged;
    }

    //

    public void logClassInfo(Logger useLogger) {
        String methodName = "logClassInfo";

        if ( useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        UtilImpl_InternMap useClassNameInternMap = getClassNameInternMap();
        TargetsTableClassesMultiImpl useClassTable = getClassTable();

        useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                    "Interned [ {0} ] class names [ {1} ] bytes",
                    new Object[] { Integer.valueOf(useClassNameInternMap.getSize()),
                                   Integer.valueOf(useClassNameInternMap.getTotalLength()) });
        useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                    "Packages [ {0} ]",
                    Integer.valueOf(useClassTable.i_getPackageNames().size()));
        useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                    "Classes [ {0} ]",
                    Integer.valueOf(useClassTable.i_getClassNames().size()));

        for ( String classSourceName : useClassTable.getClassSourceNames() ) {
            Set<String> i_packageNames = useClassTable.getPackageNames(classSourceName);
            useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "Packages [ {0} ] [ {1} ]",
                        new Object[] { classSourceName, Integer.valueOf(i_packageNames.size()) });

            Set<String> i_classNames = useClassTable.getClassNames(classSourceName);
            useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "Classes [ {0} ] [ {1} ]",
                        new Object[] { classSourceName, Integer.valueOf(i_classNames.size()) });
        }
    }

    public void logInternalResults(Logger useLogger) {
        String methodName = "logInternalResults";

        if ( logger.isLoggable(Level.FINER) ) {
            return;
        }

        TargetsTableImpl[] useBuckets = getResultBuckets();

        for ( ScanPolicy policy : ScanPolicy.values() ) {
            if ( policy == ScanPolicy.EXTERNAL ) {
                continue;
            }

            TargetsTableImpl bucket = useBuckets[ policy.ordinal() ];

            Set<String> packageAnnotationNames = bucket.getPackageAnnotations();
            Set<String> classAnnotationNames = bucket.getClassAnnotations();
            Set<String> fieldAnnotationNames = bucket.getFieldAnnotations();
            Set<String> methodAnnotationNames = bucket.getMethodAnnotations();

            int totalAnnotations = packageAnnotationNames.size() +
                            classAnnotationNames.size() +
                            fieldAnnotationNames.size() +
                            methodAnnotationNames.size();

            useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "Result [ {0} ] [ {1} ]",
                        new Object[] { policy.name(), Integer.valueOf(totalAnnotations) });
        }
    }

    public void logExternalResults(Logger useLogger) {
        String methodName = "logExternalResults";

        if ( useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        TargetsTableImpl[] useBuckets = getResultBuckets();

        TargetsTableImpl bucket = useBuckets[ ScanPolicy.EXTERNAL.ordinal() ];

        Set<String> packageAnnotationNames = bucket.getPackageAnnotations();
        Set<String> classAnnotationNames = bucket.getClassAnnotations();
        Set<String> fieldAnnotationNames = bucket.getFieldAnnotations();
        Set<String> methodAnnotationNames = bucket.getMethodAnnotations();

        int totalAnnotations = packageAnnotationNames.size() +
                        classAnnotationNames.size() +
                        fieldAnnotationNames.size() +
                        methodAnnotationNames.size();

        useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                    "Result [ {0} ] [ {1} ]",
                    new Object[] { ScanPolicy.EXTERNAL.name(), Integer.valueOf(totalAnnotations) });
    }

    public long getCacheReadTime() {
        return getModData().getReadTime();
    }

    public long getCacheWriteTime() {
        return getModData().getWriteTime();
    }

    public long getContainerReadTime() {
        return getModData().getContainerReadTime();
    }

    public long getContainerWriteTime() {
        return getModData().getContainerWriteTime();
    }

//
//            if ( !packageAnnotationNames.isEmpty() ) {
//                logger.logp(Level.FINER, CLASS_NAME, methodName, "Package annotations [ {0} ]", Integer.valueOf( packageAnnotationNames.size()));
//                for ( String annotationName : packageAnnotationNames ) {
//                    logger.logp(Level.FINER, CLASS_NAME, methodName, "  [ {0} ]", annotationName);
//                }
//            }
//
//            if ( !classAnnotationNames.isEmpty() ) {
//                logger.logp(Level.FINER, CLASS_NAME, methodName, "Class annotations [ {0} ]", Integer.valueOf(classAnnotationNames.size()));
//                for ( String annotationName : classAnnotationNames ) {
//                    logger.logp(Level.FINER, CLASS_NAME, methodName, "  [ {0} ]", annotationName);
//                }
//            }
//
//            if ( !fieldAnnotationNames.isEmpty() ) {
//                logger.logp(Level.FINER, CLASS_NAME, methodName, "Field annotations [ {0} ]", Integer.valueOf(fieldAnnotationNames.size()));
//                for ( String annotationName : fieldAnnotationNames ) {
//                    logger.logp(Level.FINER, CLASS_NAME, methodName, "  [ {0} ]", annotationName);
//                }
//            }
//
//            if ( !methodAnnotationNames.isEmpty() ) {
//                logger.logp(Level.FINER, CLASS_NAME, methodName, "Method annotations [ {0} ]", Integer.valueOf(methodAnnotationNames.size()));
//                for ( String annotationName : methodAnnotationNames ) {
//                    logger.logp(Level.FINER, CLASS_NAME, methodName, "  [ {0} ]", annotationName);
//                }
//            }
}
