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

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.targets.cache.internal.TargetCacheImpl_DataCon;
import com.ibm.ws.anno.targets.cache.internal.TargetCacheImpl_DataMod;
import com.ibm.ws.anno.util.internal.UtilImpl_IdentityStringSet;
import com.ibm.ws.anno.util.internal.UtilImpl_InternMap;
import com.ibm.ws.anno.util.internal.UtilImpl_PoolExecutor;
import com.ibm.ws.anno.util.internal.UtilImpl_Utils;

import com.ibm.wsspi.anno.util.Util_InternMap;
import com.ibm.wsspi.anno.classsource.ClassSource;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Options;
import com.ibm.wsspi.anno.targets.cache.TargetCache_Options;

/**
 * Data dependencies:
 *
 * Inputs:
 *
 *   A list of internal class sources, plus an optional external
 *   class source.
 *
 *   { Internal Source } + External Source
 *
 * Internal class source dependencies:
 *
 *   Each internal class source generates a class table and an
 *   annotation targets table.  As a side effect of generating
 *   the class and targets tables, a collections of resolved
 *   and unresolved class names are generated.
 *
 *   Processing of each internal class source is exhaustive:
 *   All classes of each internal class source are processed.
 *
 *   Each internal class source operates independent of all
 *   other internal class sources.
 *
 *   Internal Source ->
 *   Internal Class Table +
 *     Internal Annotation Targets Table +
 *     Internal Resolved Class Names +
 *     Internal Unresolved Class Names
 *
 * Resolved class name dependencies:
 *
 *   The resolved and unresolved class names from all of the
 *   internal class sources is merged into single collections.
 *   Precedence is given according to the ordering of the
 *   internal class sources, including masking effects when
 *   duplicate class information is present.
 *
 *   { ( Internal Resolved Class Names, Internal Unresolved Class Names ) } ->
 *     ( Overall Internal Resolved Class Names, overall Internal Unresolved Class Names )
 *
 * External class source dependencies:
 *
 *   The single external class source, when present, generates
 *   a single external class table.  No external annotation
 *   targets table is generated.
 *
 *   Initially only unresolved class names obtained from the
 *   internal class sources are processed.  This often generates
 *   new unresolved class names, which are processed iteratively
 *   until no new unresolved class names are generated.
 *
 *   Some class names may fail to resolve.
 *
 *   External Source +
 *     Overall Internal Resolved Class Names +
 *     Overall Internal Unresolved Class Names ->
 *   External Class Data +
 *     External Resolved Class Names +
 *     External Unresolved Class Names
 *
 * Final class data dependencies:
 *
 *   An overall internal class table is generated from the
 *   internal class tables.
 * 
 *   An overall class table is generated from the overall
 *   internal class table and from the external class table.
 *
 *   An overall annotations table is generated from the
 *   internal annotations tables.  (There is no external
 *   annotations table: annotations are not recorded from the
 *   external class source.)
 *
 *   Results are partitioned according to policy assignments:
 *   SEED, PARTIAL, EXCLUDED, and EXTERNAL.
 *
 *   { Internal Class Table } -> Overall Internall Class Table
 *
 *   ( Internal Annotations Table } -> Overall Annotations Table
 *
 *   Overall Internal Class Table + External Class Table ->
 *     Overall Class Table
 *
 * Caching is performed largely according to this data flow:
 *
 * Data from each internal class source is recorded with the class
 * table stored to a single file, with the annotation targets table
 * table stored to a single file, and with stamp information stored
 * to a aingle file.
 *
 * The list of internal class sources, including the policy assignment
 * of each internal class source, is recorded to a single file.
 *
 * The overall internal class table is stored.  The overall internal
 * resolved and unresolved class names are caches.
 *
 * Data from the external class source is not cached: The external
 * class source includes environment specific components which are not
 * guaranteed to be stable.
 */

public class TargetsScannerOverallImpl extends TargetsScannerBaseImpl {
    public static final String CLASS_NAME = TargetsScannerOverallImpl.class.getSimpleName();

    @Trivial
    private String printString(Set<String> values) {
        if ( values.isEmpty() ) {
            return "{ }";

        } else if ( values.size() == 1 ) {
            for ( String value : values ) {
                return "{ " + value + " }";
            }
            return null; // Unreachable

        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("{ ");
            boolean first = true;
            for ( String value : values ) {
                if ( !first ) {
                    builder.append(", ");
                } else {
                    first = false;
                }
                builder.append(value);
            }
            builder.append(" }");
            return builder.toString();
        }
    }

    @Trivial
    public String priorResult(String resultType, String reason, boolean isChanged) {
        return MessageFormat.format(
                "[ {0} ] ENTER / RETURN Valid (prior result) [ {1} ] [ {2} ]: {3}",
                getHashText(),
                resultType,
                Boolean.valueOf(!isChanged),
                reason);
    }

    @Trivial
    public String newResult(String resultType, String reason, boolean isChanged) {
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

        //

        this.containerTable = null;

        this.changedContainerTableReason = null;
        this.changedContainerTable = false;

        // The targets map is in the superclass.

        this.changedTargets = new HashSet<String>();
        this.changedTargetsReasons = new HashMap<String, String>();

        this.changedAnyTargetsReason = null;
        this.changedAnyTargets = false;

        // The class table is in the superclass.

        this.changedClassTableReason = null;
        this.changedClassTable = false;

        //

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

    @Trivial
    protected Object getInternMapControl() {
        return internMapControl;
    }

    // Used to safely get/put targets data and targets results to the several targets
    // collections.
    protected static class TargetsControl extends Object {
        // EMPTY
    }
    protected final Object targetsControl;

    @Trivial
    protected Object getTargetsControl() {
        return targetsControl;
    }

    /**
     * Override: Create a targets table for a class source.  If multiple scan
     * threads are being used, create the targets table with its own intern
     * maps.
     *
     * The table must be interned relative to the overall intern maps before
     * it is merged into the overall reaults.
     *
     * Having independent intern maps adds storage overhead.  The alternative
     * is expensive fine grained locking on intern maps.
     *
     * See {@link #internTargetsTable}.
     *
     * @param classSource The class source for which to create the targets
     *     table.
     *
     * @return A new targets table for the class source.
     */
    @Override
    protected TargetsTableImpl createTargetsTable(ClassSource classSource) {
        if ( isScanSingleThreaded() || isScanSingleSource() ) {
            // Create the targets table using the intern map of the scanner,
            // which is the intern map of the annotation targets.
            return super.createTargetsTable(classSource);

        } else {
            // Create the targets table with its own intern map.
            TargetsTableImpl targetsTable = new TargetsTableImpl( getFactory(), classSource.getName() );
            targetsTable.setStamp( classSource.getStamp() );
            return targetsTable;
        }
    }

    /**
     * Conditionally recreate a targets table using the overall intern maps.
     *
     * Do nothing if mutiple scan threads are not in use.
     *
     * @return The targets table interned using the overall intern maps.
     */
    protected TargetsTableImpl internTargetsTable(TargetsTableImpl targetsTable) {
        if ( !isScanSingleThreaded() && !isScanSingleSource() ) {
            synchronized ( getInternMapControl() ) {
                targetsTable = new TargetsTableImpl( targetsTable,
                                                     getClassNameInternMap(),
                                                     getFieldNameInternMap(),
                                                     getMethodSignatureInternMap() );
            }
        }

        if ( logger.isLoggable(Level.FINER) ) {
            verifyTargets(targetsTable);
        }

        return targetsTable;
    }

    private void verifyTargets(TargetsTableImpl targetsTable) {
        String methodName = "verifyTargets";
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", getHashText());
        }

        Set<String> i_packageNames = targetsTable.i_getPackageNames();
        verify("Package names", i_packageNames);
        Set<String> i_classNames = targetsTable.i_getClassNames();
        verify("Class names", i_classNames);

        Set<String> i_packageAnnotations = targetsTable.i_getPackageAnnotations().getHeldSet();
        verify("Package annotations", i_packageAnnotations);
        Set<String> i_classAnnotations = targetsTable.i_getClassAnnotations().getHeldSet();
        verify("Class annotations", i_classAnnotations);
        Set<String> i_fieldAnnotations = targetsTable.i_getFieldAnnotations().getHeldSet();
        verify("Field annotations", i_fieldAnnotations);
        Set<String> i_methodAnnotations = targetsTable.i_getMethodAnnotations().getHeldSet();
        verify("Method annotations", i_methodAnnotations);

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN", getHashText());
        }
    }

    // External class source processing ...

    /**
     * Answer the external class source.  Answer null if no external class
     * source is present.
     *
     * The external class source need not be present initially, but is
     * required for inhertance based APIs.
     *
     * At most one external class source may be present.
     *
     * See {@link ScanPolicy#EXTERNAL}.
     *
     * @return The external class source.  Null if there is no external
     *     class source.
     */
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

    @Trivial
    public TargetCacheImpl_DataMod getModData() {
        return modData;
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

    // The table of child class sources of the root aggregate class source.

    protected TargetsTableContainersImpl containerTable;

    @Trivial
    public TargetsTableContainersImpl getContainerTable() {
        return containerTable;
    }

    // Change information for the child class sources.

    protected String changedContainerTableReason;
    protected boolean changedContainerTable;

    @Trivial
    public String getChangedContainerReason() {
        return changedContainerTableReason;
    }

    @Trivial
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

    @Trivial
    protected Map<String, String> getChangedTargetsTableReasons() {
        return changedTargetsReasons;
    }

    @Trivial
    protected Set<String> getChangedTargetsTable() {
        return changedTargets;
    }

    protected void setChangedTargetsTable(String classSourceName, String reason, boolean isChanged) {
        getChangedTargetsTableReasons().put(classSourceName, reason);

        if ( isChanged ) {
            getChangedTargetsTable().add(classSourceName);
        }
    }

    public String getChangedTargetsTableReason(String classSourceName) {
        return getChangedTargetsTableReasons().get(classSourceName);
    }

    public boolean isChangedTargetsTable(String classSourceName) {
        return getChangedTargetsTable().contains(classSourceName);
    }

    protected void putTargetsTable(String classSourceName, TargetsTableImpl targetsTable,
                                  String reason, boolean isChanged) {

        putTargetsTable(classSourceName, targetsTable);
        setChangedTargetsTable(classSourceName, reason, isChanged);
    }

    // Summary for the targets tables.

    protected String changedAnyTargetsReason;
    protected boolean changedAnyTargets;

    public String getChangedAnyTargetsReason() {
        return changedAnyTargetsReason;
    }

    public boolean isChangedAnyTargets() {
        return changedAnyTargets;
    }

    // Change information for the overall class table.

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
                String resultMsg = priorResult("container table",
                    changedContainerTableReason, changedContainerTable);
                logger.logp(Level.FINER, CLASS_NAME, methodName, resultMsg);
            }
            return !changedContainerTable;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", getHashText());
        }

        boolean isChanged;
        String isChangedReason;

        TargetsTableContainersImpl useContainerTable;

        if ( !modData.shouldRead("Containers table") || !modData.hasContainersTable() ) {
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
            if ( modData.shouldWrite("Containers table") ) {
                modData.writeContainersTable(useContainerTable);
            }
        }

        setContainerTable(useContainerTable, isChangedReason, isChanged);

        if ( logger.isLoggable(Level.FINER) ) {
            String resultMsg = newResult("container table", isChangedReason, isChanged);
            logger.logp(Level.FINER, CLASS_NAME, methodName, resultMsg);
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
    // master intern maps.  Intern maps are placed by 'getTargetsTable'.  The
    // recreation of the targets data, when necessary, is done by
    // 'internTargetsTable'.

    protected boolean validInternalContainer(ClassSource classSource, ScanPolicy scanPolicy) {
        String methodName = "validInternalContainer";

        String classSourceName = classSource.getName();

        synchronized ( getTargetsControl() ) {
            TargetsTableImpl priorTargetsTable = getTargetsTable(classSourceName);
            if ( priorTargetsTable != null ) {
                String priorIsChangedReason = getChangedTargetsTableReason(classSourceName);
                boolean priorIsChanged = isChangedTargetsTable(classSourceName);

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

        TargetCacheImpl_DataCon conData = modData.getConForcing(classSourceName);
        TargetsTableImpl useTargetsTable = createTargetsTable(classSource);

        boolean attemptRead;
        boolean completedRead;

        if ( attemptRead = (conData.shouldRead("Container data") && conData.hasFiles()) ) {
            completedRead = conData.read(useTargetsTable);
        } else {
            completedRead = false;
        }

        if ( !completedRead ) { 
            useTargetsTable = scanInternal(classSource,
                TargetsVisitorClassImpl.DONT_RECORD_RESOLVED,
                TargetsVisitorClassImpl.DONT_RECORD_UNRESOLVED);
            useTargetsTable = internTargetsTable(useTargetsTable);

            isChangedAll = true;
            isChangedJustStamp = false;

            if ( attemptRead ) {
                isChangedReason = "Cache miss (read failure)";
            } else {
                isChangedReason = "Cache miss";
            }

        } else {
            if ( modData.isAlwaysValid() ) {
                useTargetsTable = internTargetsTable(useTargetsTable);

                isChangedAll = false;
                isChangedJustStamp = false;
                isChangedReason = "Cache hit (forced valid)";

            } else if ( validInternalStamp(classSource, conData) ) {
                useTargetsTable = internTargetsTable(useTargetsTable);

                isChangedAll = false;
                isChangedJustStamp = false;
                isChangedReason = "Cache hit (valid stamp)";

            } else {
                TargetsTableImpl newTargetsTable = scanInternal(classSource,
                    TargetsVisitorClassImpl.DONT_RECORD_RESOLVED,
                    TargetsVisitorClassImpl.DONT_RECORD_UNRESOLVED);

                boolean isCongruent = (isScanSingleThreaded() || isScanSingleSource());

                if ( !useTargetsTable.sameAs(newTargetsTable, isCongruent) ) {
                    useTargetsTable = internTargetsTable(newTargetsTable);

                    isChangedAll = true;
                    isChangedJustStamp = false;
                    isChangedReason = "Cache hit (invalid stamp; invalid data)";

                } else {
                    useTargetsTable = internTargetsTable(useTargetsTable);

                    isChangedAll = false;
                    isChangedJustStamp = true;
                    isChangedReason = "Cache hit (invalid stamp; valid data)";
                }
            }
        }

        if ( isChangedAll ) {
            if ( conData.shouldWrite("Container data") ) {
                conData.write(modData, useTargetsTable);
            }
        } else if ( isChangedJustStamp ) {
            if ( conData.shouldWrite("Time stamp") ) {
                conData.writeStamp(modData, useTargetsTable);
            }
        }

        synchronized( getTargetsControl() ) {
            putTargetsTable(classSourceName, useTargetsTable, isChangedReason, isChangedAll);
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        newResult("Internal class source " + classSourceName, isChangedReason, isChangedAll));
        }
        return !isChangedAll;
    }

    protected boolean validInternalStamp(ClassSource classSource, TargetCacheImpl_DataCon conData) {
        String methodName = "validInternalStamp";

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

        } else if ( !conData.shouldRead("Time Stamp") ) {
            isChanged = true;
            isChangedReason = "Cache miss (disabled)";

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
                isChanged = !cachedStamp.equals(currentStamp);
                if ( isChanged ) {
                    isChangedReason = "Cache hit (invalid stamp; current " + currentStamp + " prior " + cachedStamp + ")";
                } else {
                    isChangedReason = "Cache hit (valid stamp " + currentStamp + ")";
                }
            }
        }

        // Write is done with the container.

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] RETURN [ {1} ]: {2} ]",
                        new Object[] { getHashText(), Boolean.valueOf(!isChanged), isChangedReason });
        }
        return !isChanged;
    }

    protected boolean validInternalContainers_Select() {
        String methodName = "validInternalContainers_Select";

        if ( changedAnyTargetsReason != null ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] ENTER / RETURN [ {1} ] {2}",
                    new Object[] { getHashText(), Boolean.valueOf(!changedAnyTargets), changedAnyTargetsReason });
            }
            return !changedAnyTargets;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", getHashText());
        }

        if ( isScanSingleThreaded() || isScanSingleSource() ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Single-threaded scanning", getHashText());
            }
            validInternalContainers();
        } else {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Multi-threaded scanning", getHashText());
            }
            validInternalContainers_Concurrent();
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] ENTER / RETURN [ {1} ] {2}",
                new Object[] { getHashText(), Boolean.valueOf(!changedAnyTargets), changedAnyTargetsReason });
        }
        return !changedAnyTargets;
    }

    protected void validInternalContainers() {
        // String methodName = "validInternalContainers";

        boolean changedTable = !validContainerTable();

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

        boolean changed = false;
        String changedReason = null;

        if ( changedTable ) {
            changed = true;
            changedReason = "changed containers list";
        }
        if ( changedContainers > 0 ) {
            changed = true;
            if ( changedTable ) {
                changedReason += "; changed tables [ " + Integer.valueOf(changedContainers) + " ]";
            } else {
                changedReason += "changed tables [ " + Integer.valueOf(changedContainers) + " ]";
            }
        }

        changedAnyTargetsReason = changedReason;
        changedAnyTargets = changed;
    }

    protected void validInternalContainers_Concurrent() {
        String methodName = "validInternalContainers_Concurrent";

        boolean changedTable = !validContainerTable();

        // Don't return until all internal containers were checked!
        // That forces data to be present for all internal containers.

        List<? extends ClassSource> childClassSources = rootClassSource.getClassSources();
        int numChildClassSources = childClassSources.size();

        final boolean validContainers[] = new boolean[numChildClassSources];

        // Set the number of scan threads to the minimum of three values:
        //     the requested count of scan threads
        //     the maximum allowed count of scan threads
        //     the number of child class sources
        // The number of threads should be always at least two:
        //     If scan threads option is set to 0 or 1, the non-concurrent
        //     scan is performed: This code is never reached.
        //     If the number of containers is 1, the non-concurrent scan
        //     is performed: This code is never reached.

        int scanThreads = getScanThreads();
        if ( scanThreads <= ClassSource_Options.SCAN_THREADS_UNBOUNDED) {
            scanThreads = ClassSource_Options.SCAN_THREADS_MAX;
        }
        if ( scanThreads > numChildClassSources ) {
            scanThreads = numChildClassSources; 
        }
        if ( scanThreads > ClassSource_Options.SCAN_THREADS_MAX ) {
            scanThreads = ClassSource_Options.SCAN_THREADS_MAX;
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

        int changedContainers = 0;
        for ( boolean validContainer : validContainers ) {
            if ( !validContainer ) {
                changedContainers++;
            }
        }

        boolean changed = false;
        String changedReason = null;

        if ( changedTable ) {
            changed = true;
            changedReason = "changed containers list";
        }

        if ( changedContainers > 0 ) {
            changed = true;
            if ( changedTable ) {
                changedReason += "; changed tables [ " + Integer.valueOf(changedContainers) + " ]";
            } else {
                changedReason += "Changed tables [ " + Integer.valueOf(changedContainers) + " ]";
            }
        }

        changedAnyTargetsReason = changedReason;
        changedAnyTargets = changed;
    }

    //

    protected boolean validInternalUnresolved() {
        String methodName = "validInternalUnresolved";

        if ( this.i_resolvedClassNames != null ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            priorResult("Unresolved classes", this.changedClassNamesReason, this.changedClassNames));
            }
            return !this.changedClassNames;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", getHashText());
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Single threaded [ {1} ]",
                new Object[] { getHashText(), Boolean.valueOf(isScanSingleThreaded()) });
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Single sourced [ {1} ]",
                new Object[] { getHashText(), Boolean.valueOf(isScanSingleSource()) });
        }

        String validReason;

        boolean mustCompute;

        UtilImpl_IdentityStringSet i_cachedResolved = null;
        UtilImpl_IdentityStringSet i_cachedUnresolved = null;

        if ( !modData.shouldRead("Unresolved class data") ) {
            mustCompute = true;
            validReason = "Cache miss (read disabled)";

        } else if ( !modData.hasResolvedRefs() ) {
            mustCompute = true;
            validReason = "Cache miss (resolved)";

        } else if ( !modData.hasUnresolvedRefs() ) {
            mustCompute = true;
            validReason = "Cache miss (unresolved)";

        } else {
            i_cachedResolved = createIdentityStringSet();
            if ( !modData.readResolvedRefs(getClassNameInternMap(), i_cachedResolved) ) {
                i_cachedResolved = null;

                mustCompute = true;
                validReason = "Cache miss (failed to read resolved)";

            } else {
                i_cachedUnresolved = createIdentityStringSet();
                if ( !modData.readUnresolvedRefs(getClassNameInternMap(), i_cachedUnresolved) ) {
                    i_cachedResolved = null;
                    i_cachedUnresolved = null;

                    mustCompute = true;
                    validReason = "Cache miss (failed to read unresolved)";

                } else {
                    mustCompute = false;
                    validReason = "Cache hit";
                }
            }
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] (Pre-validation) Must compute [ {1} ] ({2})",
                new Object[] { getHashText(), Boolean.valueOf(mustCompute), validReason });
        }

        if ( !mustCompute ) {
            if ( modData.isAlwaysValid() ) {
                validReason += " (forced valid)";
            } else if ( !validInternalContainers_Select() ) {
                mustCompute = true;
                validReason += " (invalid containers)";
            } else {
                validReason += " (valid containers)";
            }
        } else {
            validInternalContainers_Select();
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] (Post-validation) Must compute [ {1} ] ({2})",
                new Object[] { getHashText(), Boolean.valueOf(mustCompute), validReason });
        }

        UtilImpl_IdentityStringSet i_newResolved;
        UtilImpl_IdentityStringSet i_newUnresolved;

        boolean useNew;

        if ( mustCompute ) {
            i_newResolved = createIdentityStringSet();
            i_newUnresolved = createIdentityStringSet();

            for ( ClassSource classSource : rootClassSource.getClassSources() ) {
                if ( rootClassSource.getScanPolicy(classSource) == ScanPolicy.EXTERNAL ) {
                    continue;
                }

                getTargetsTable(classSource).updateClassNames(i_newResolved, i_newUnresolved);

                if ( logger.isLoggable(Level.FINER) ) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] Class source [ {1} ] Resolved [ {2} ]",
                        new Object[] { getHashText(), classSource.getName(), printString(i_newResolved) });
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] Class source [ {1} ] Unresolved [ {2} ]",
                        new Object[] { getHashText(), classSource.getName(), printString(i_newUnresolved) });
                }
            }

            if ( (i_cachedResolved == null) || (i_cachedUnresolved == null) ) {
                useNew = true;
            } else if ( !i_newResolved.i_equals(i_cachedResolved) ) {
                validReason += " (invalid resolved)";
                useNew = true;
            } else if ( !i_newUnresolved.i_equals(i_cachedUnresolved) ) {
                validReason += " (invalid unresolved)";
                useNew = true;
            } else {
                validReason += " (valid)";
                useNew = false;
            }

            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] (Post-compute) Use new [ {1} ] ({2})",
                    new Object[] { getHashText(), Boolean.valueOf(useNew), validReason });
            }

        } else {
            i_newResolved = null;
            i_newUnresolved = null;

            useNew = false;
        }

        this.changedClassNames = useNew;
        this.changedClassNamesReason = validReason;

        if ( !useNew ) {
            this.i_resolvedClassNames = i_cachedResolved;
            this.i_unresolvedClassNames = i_cachedUnresolved;

        } else {
            this.i_resolvedClassNames = i_newResolved;
            this.i_unresolvedClassNames = i_newUnresolved;

            // These writes must tolerate subsequent updates to
            // i_resolvedClassNames and i_unresolvedClassNames,
            // which will be made by a subsequent call to
            // validExternal.
            //
            // That is, when asynchronous writes are enabled,
            // 'writeResolvedRefs' and 'writeUnresolvedRefs' must
            // copy the class names collection before proceeding
            // with the write.

            if ( modData.shouldWrite("Resolved refs") ) {
                modData.writeResolvedRefs(i_resolvedClassNames);
            }
            if ( modData.shouldWrite("Unresolved refs") ) {
                modData.writeUnresolvedRefs(i_unresolvedClassNames);
            }
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    newResult("Unresolved classes", validReason, useNew));

            verifyUnresolved();
        }

        return !mustCompute;
    }

    private void verify(String description, Set<String> i_values) {
        String methodName = "verify";

        logger.logp(Level.FINER, CLASS_NAME, methodName,
           "{0} count [ {1} ]",
           new Object[] { description, Integer.toString(i_values.size()) });

        for ( String i_value : i_values ) {
            if ( internClassName(i_value, Util_InternMap.DO_NOT_FORCE) == null ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] is not interned", i_value);
            } else {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] is interned", i_value);
            }
        }
    }

    private void verifyUnresolved() {
        String methodName = "verifyUnresolved";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", getHashText());
        }

        verify("Resolved class names", i_resolvedClassNames);
        verify("Unresolved class names", i_unresolvedClassNames);

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN", getHashText());
        }
    }

    protected boolean validInternalResults() {
        String methodName = "validInternalResults";
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", getHashText());
        }

        boolean isValid;
        String isValidReason;

        TargetsTableImpl[] cachedTables = createResultTables();

        boolean didRead = readInternalResults_Select(cachedTables);

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Read [ {1} ]",
                new Object[] { getHashText(), Boolean.valueOf(didRead) });
        }

        if ( didRead ) {
            if ( modData.isAlwaysValid() ) {
                isValid = true;
                isValidReason = "Cache hit (forced valid)";
            } else if ( !validInternalContainers_Select() ) {
                isValid = false;
                isValidReason = "Cache hit (invalid)";
            } else {
                isValid = true;
                isValidReason = "Cache hit (valid)";
            }
        } else {
            validInternalContainers_Select();
            isValid = false;
            isValidReason = "Cache miss";
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Valid [ {1} ] ({2})",
                new Object[] { getHashText(), Boolean.valueOf(isValid), isValidReason });
        }

        if ( !isValid ) {
            TargetsTableImpl[] newTables = createResultTables();

            mergeInternalResults(newTables);

            for ( ScanPolicy scanPolicy : ScanPolicy.values() ) {
                if ( scanPolicy == ScanPolicy.EXTERNAL ) {
                    continue;
                }
                if ( modData.shouldWrite("Result container") ) {
                    modData.writeResultCon( scanPolicy, newTables[ scanPolicy.ordinal() ] );
                }
            }

            cachedTables = newTables;
        }

        putResultTables(cachedTables);

        if ( logger.isLoggable(Level.FINER) ) {
            for ( ScanPolicy scanPolicy : ScanPolicy.values() ) {
                if ( scanPolicy == ScanPolicy.EXTERNAL ) {
                    continue;
                }

                TargetsTableImpl resultTable = cachedTables[ scanPolicy.ordinal() ];
                if ( resultTable == null ) {
                    continue;
                }

                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Result [ {1} ]",
                    new Object[] { getHashText(), scanPolicy });
                verifyTargets(resultTable);
             }
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, newResult("Internal results", isValidReason, isValid));
        }
        return !isValid;
    }

    protected boolean readInternalResults_Select(TargetsTableImpl[] tables) {
        if ( isScanSingleThreaded() || isScanSingleSource() ) {
            return readInternalResults(tables);
        } else {
            return readInternalResults_Concurrent(tables);
        }
    }

    protected boolean readInternalResults(TargetsTableImpl[] tables) {
        boolean isAnyMissing = false;

        for ( ScanPolicy scanPolicy : ScanPolicy.values() ) {
            if ( scanPolicy == ScanPolicy.EXTERNAL ) {
                continue;
            }

            if ( readResults(scanPolicy, tables) == null ) {
                isAnyMissing = true;
            }
        }

        return !isAnyMissing;
    }

    protected boolean readInternalResults_Concurrent(final TargetsTableImpl[] tables) {
        String methodName = "readInternalResults";

        // Each processing array have a cell for the EXTERNAL scan policy,
        // which is unused.  Having the extra unused cell is simpler than
        // adjusting for the unused policy.

        final boolean[] missingResults = new boolean[ ScanPolicy.values().length ];

        // Do not pool the internal result readers: there will only be at most
        // three of these.

        Thread[] readThreads = new Thread[ ScanPolicy.values().length ];

        for ( ScanPolicy scanPolicy : ScanPolicy.values() ) {
            if ( scanPolicy == ScanPolicy.EXTERNAL ) {
                continue;
            }

            final ScanPolicy useScanPolicy = scanPolicy;

            Runnable readRunner = new Runnable() {
                @Override
                public void run() {
                    if ( readResults(useScanPolicy, tables) == null ) {
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
                readThreads[scanPolicy.ordinal()].join(); // throws InterruptedException

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
            if ( scanPolicy == ScanPolicy.EXTERNAL ) {
                continue;
            }
            if ( missingResults[scanPolicy.ordinal()] ) {
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
    // master intern maps.  Intern maps are placed by 'getTargetsTable'.  The
    // recreation of the targets data, when necessary, is done by
    // 'internTargetsTable'.

    protected TargetsTableImpl readResults(ScanPolicy scanPolicy, TargetsTableImpl[] tables) {
        if ( !modData.shouldRead("Result container") ) {
            return null;
        }

        TargetCacheImpl_DataCon resultCon =  modData.getResultCon(scanPolicy);
        if ( !resultCon.exists() || !resultCon.hasFiles() ) {
            return null;
        }

        TargetsTableImpl cachedResultData = createTargetsTable(scanPolicy);
        if ( !resultCon.read(cachedResultData) ) {
            return null;
        }

        cachedResultData = internTargetsTable(cachedResultData);

        if ( tables != null ) {
            tables[ scanPolicy.ordinal() ] = cachedResultData;
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

        if ( !modData.shouldRead("Class refs") || !modData.hasClassRefs() ) {
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

            // Merge all of the internal containers to the overall class table.
            mergeClasses(newClassTable);

            if ( modData.shouldWrite("Class refs") ) {
                modData.writeClassRefs(newClassTable);
            }

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
        boolean validInternalUnresolved = validInternalUnresolved();
        boolean validInternalResults = validInternalResults();
        boolean validInternalClasses = validInternalClasses();

        if ( logger.isLoggable(Level.FINER) ) {
            logInternalResults(logger);
        }

        return ( validInternalUnresolved && validInternalResults && validInternalClasses );
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

        TargetsTableImpl externalData = getTargetsTable(classSourceName);
        if ( externalData != null ) {
            isChangedReason = getChangedTargetsTableReason(classSourceName);
            isChanged = isChangedTargetsTable(classSourceName);

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
        boolean validInternalUnresolved = validInternalUnresolved();

        externalData = scanExternal(externalClassSource, i_resolvedClassNames, i_unresolvedClassNames);
        externalData = internTargetsTable(externalData);

        // See the comment on 'mergeClasses': This must be synchronized
        // with the write of the class table which occurs in 
        // 'TargetCacheImpl_ModData.writeClassRefs'.

        // Merge the external data into the overall class table.
        // The internal containers are expected to already have been
        // merged to the overall class table.

        TargetsTableClassesMultiImpl useClassTable = getClassTable();
        synchronized( useClassTable ) {
            mergeClasses(useClassTable, externalData);
        }

        putTargetsTable(classSourceName, externalData, isChangedReason, isChanged);
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

    @Trivial
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

    @Trivial
    public void logInternalResults(Logger useLogger) {
        String methodName = "logInternalResults";

        if ( logger.isLoggable(Level.FINER) ) {
            return;
        }

        TargetsTableImpl[] useResultTables = getResultTables();

        for ( ScanPolicy policy : ScanPolicy.values() ) {
            if ( policy == ScanPolicy.EXTERNAL ) {
                continue;
            }

            TargetsTableImpl resultTable = useResultTables[ policy.ordinal() ];

            int totalAnnotations = resultTable.countAnnotations();

            useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "Result [ {0} ] [ {1} ]",
                        new Object[] { policy.name(), Integer.valueOf(totalAnnotations) });
        }
    }

    @Trivial
    public void logExternalResults(Logger useLogger) {
        String methodName = "logExternalResults";

        if ( useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        TargetsTableImpl[] useResultTables = getResultTables();

        TargetsTableImpl resultTable = useResultTables[ ScanPolicy.EXTERNAL.ordinal() ];

        int totalAnnotations = resultTable.countAnnotations();

        useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                    "Result [ {0} ] [ {1} ]",
                    new Object[] { ScanPolicy.EXTERNAL.name(), Integer.valueOf(totalAnnotations) });
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
