/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.internal;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.targets.cache.internal.TargetCacheImpl_DataCon;
import com.ibm.ws.annocache.targets.cache.internal.TargetCacheImpl_DataMod;
import com.ibm.ws.annocache.util.internal.UtilImpl_IdentityStringSet;
import com.ibm.ws.annocache.util.internal.UtilImpl_InternMap;
import com.ibm.ws.annocache.util.internal.UtilImpl_PoolExecutor;
import com.ibm.ws.annocache.util.internal.UtilImpl_Utils;
import com.ibm.wsspi.annocache.classsource.ClassSource;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_Options;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.util.Util_InternMap;

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
 *   Internal Source -
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
 *   { ( Internal Resolved Class Names, Internal Unresolved Class Names ) } -
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
 *     Overall Internal Unresolved Class Names -
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
 *   { Internal Class Table } - Overall Internall Class Table
 *
 *   ( Internal Annotations Table } - Overall Annotations Table
 *
 *   Overall Internal Class Table + External Class Table -
 *     Overall Class Table
 *
 * Caching is performed largely according to this data flow:
 *
 * Data from each internal class source is recorded with the class
 * table and the annotation targets table stored to a single file,
 * and with stamp information stored to a single file.
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
 * 
 * TODO: Merge more of the result type data.
 * 
 * TODO: The steps to validate and read the internal results could be
 *       revised.
 */
public class TargetsScannerOverallImpl extends TargetsScannerBaseImpl {
    @SuppressWarnings("hiding")
    public static final String CLASS_NAME = TargetsScannerOverallImpl.class.getSimpleName();

    /**
     * Generate a comma delimited text for a set of values.  Enclose
     * the text with braces ("{" and "}").  Place spaces after the first
     * brace and after each comma.
     *
     * @param values Value for which to generate comma delimited text.
     *
     * @return Comma delimited text for the values.
     */
    @Trivial
    private static String printString(Set<String> values) {
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
            int valueNo = 0;
            int valueLim = 10;

            for ( String value : values ) {
                if ( !first ) {
                    builder.append(", ");
                } else {
                    first = false;
                }

                if ( valueNo == valueLim ) {
                    value = "...";
                }
                builder.append(value);

                if ( valueNo == valueLim ) {
                    break;
                } else {
                    valueNo++;
                }
            }

            builder.append(" }");
            return builder.toString();
        }
    }

    /**
     * Generate result text for value which was previously obtained.
     *
     * @param resultType A description of the result.
     * @param resultReason The reason for the result value.
     * @param result The result value.
     *
     * @return Result text for a value which was previously obtained.
     */
    @Trivial
    private String priorResult(String resultType, String resultReason, boolean result) {
        return MessageFormat.format(
                "[ {0} ] ENTER / RETURN Valid (prior result) [ {1} ] [ {2} ]: {3}",
                getHashText(),
                resultType,
                Boolean.valueOf(result), resultReason);
    }

    /**
     * Generate result text for value which was newly obtained.
     *
     * @param resultType A description of the result.
     * @param resultReason The reason for the result value.
     * @param result The result value.
     *
     * @return Result text for a value which was newly obtained.
     */
    @Trivial
    private String newResult(String resultType, String isValidReason, boolean isValid) {
        return MessageFormat.format(
                "[ {0} ] RETURN Valid (new result) [ {1} ] [ {2} ]: {3}",
                getHashText(),
                resultType,
                Boolean.valueOf(isValid), isValidReason);
    }

    //

    /**
     * Create a scanner for specified targets table.  Results are generated for
     * the targets from a specified root class source, and are managed in the cache
     * using a module data widget.
     *
     * @param targets The targets table which is to be generated.
     * @param rootClassSource The class source which is to be scanned.
     * @param modData Widget for accessing cache data.
     */
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

    @Trivial
    public TargetsTableImpl getTargetsTable(String classSourceName) {
        synchronized ( getTargetsControl() ) {
            return super.getTargetsTable(classSourceName);
        }
    }

    /**
     * Create an isolated targets table for a leaf class source.  "Isolated"
     * means has its own intern mappings.
     * 
     * One copy of leaf class source tables is stored in the container cache
     * widgets.  Because that copy is shared between scanners, it cannot use
     * the intern maps of the overall result data. 
     *
     * @param classSource The class source for which to create the targets table.
     *
     * @return A new targets table for the class source.
     */
    public TargetsTableImpl createIsolatedTargetsTable(String classSourceName, String classSourceStamp) {
        TargetsTableImpl targetsTable =
            new TargetsTableImpl( getFactory(), classSourceName, getUseJandexFormat() );
        targetsTable.setStamp(classSourceStamp);
        return targetsTable;
    }

    /**
     * Create a result table.  Isolate the table if scanning is multi-threaded.
     *
     * @param scanPolicy The policy of the results.  This is used to name the table.
     * @param conData Widget used to access the result cache data.
     * 
     * @return A new result table.
     */
    protected TargetsTableImpl createResultTargetsTable(
        ScanPolicy scanPolicy,
        TargetCacheImpl_DataCon conData) {

        if ( isolateResultTargets(conData) ) {
            TargetsTableImpl resultTable =
                new TargetsTableImpl( getFactory(),
                    scanPolicy.name(),
                    TargetsTableImpl.DO_NOT_USE_JANDEX_FORMAT );
            // A stamp is not available for result tables.
            resultTable.setStamp(ClassSource.UNRECORDED_STAMP);
            return resultTable;

        } else {
            return createResultTargetsTable(scanPolicy);
        }
    }

    /**
     * Tell if targets data for a result container is to be isolated.
     * 
     * Result container data is isolated only when scanning is multi-threaded.
     *
     * @param conData Container data for which a table is to be created.
     *
     * @return True or false telling if the table is to be isolated.
     */
    protected boolean isolateResultTargets(TargetCacheImpl_DataCon conData) {
        String methodName = "isolateResultTargets";

        boolean isolate;
        String isolateCase;
        if ( !isScanSingleThreaded() ) {
            isolate = true;
            isolateCase = "Isolated: Multi-threaded";
        } else {
            isolate = false;
            isolateCase = "Integrated: Single-threaded";
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Result targets {1}",
                new Object[] { getHashText(), isolateCase });
        }
        return isolate;
    }

    /**
     * Internal a target table.  That is, recreate it using the overall result
     * intern mappings.
     *
     * @param targetsTable The targets table which is to be recreated.
     * 
     * @return A copy of the targets table which uses the overall result intern
     *     mappings.
     */
    protected TargetsTableImpl internTargetsTable(TargetsTableImpl targetsTable) {
        synchronized ( getInternMapControl() ) {
            targetsTable = new TargetsTableImpl( targetsTable,
                                                 getClassNameInternMap(),
                                                 getFieldNameInternMap(),
                                                 getMethodSignatureInternMap() );
        }

        if ( logger.isLoggable(Level.FINER) ) {
            verifyTargets(targetsTable);
        }

        return targetsTable;
    }

    /**
     * Intern a result table.  That is, recreate it using the overall result
     * intern mappings.
     * 
     * Answer the parameter table if it already uses the overall result intern
     * mappings.
     *
     * @param targetsTable A result table which is to be recreated.
     * @param conData Cache widget for the result table.
     *
     * @return A copy of the targets table which uses the overall result intern
     *     mappings.
     */
    protected TargetsTableImpl internResultTargetsTable(
        TargetsTableImpl targetsTable,
        TargetCacheImpl_DataCon conData) {

        if ( isolateResultTargets(conData) ) {
            synchronized ( getInternMapControl() ) {
                targetsTable = new TargetsTableImpl(
                    targetsTable,
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
    
    /**
     * Verify that the string values of a targets table are stored in the
     * appropriate intern mappings.
     *
     * @param targetsTable The table which is to be verified.
     */
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

    @Trivial
    public int getWriteLimit() {
        return getModData().getCacheOptions().getWriteLimit();
    }

    /**
     * Subclass API: Tell if scanning should create a Jandex index.
     *
     * @return True or false telling if scanning should create a Jandex
     *     index.  This implementation delegates to the module data.
     */
    @Override
    public boolean getUseJandexFormat() {
        return getModData().getCacheOptions().getUseJandexFormat();
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

    protected final Map<String, String> changedTargetsReasons; // Class source name - reason
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
        synchronized( getTargetsControl() ) {
            getChangedTargetsTableReasons().put(classSourceName, reason);

            if ( isChanged ) {
                getChangedTargetsTable().add(classSourceName);
            }
        }
    }

    public String getChangedTargetsTableReason(String classSourceName) {
        synchronized( getTargetsControl() ) {        
            return getChangedTargetsTableReasons().get(classSourceName);
        }
    }

    public boolean isChangedTargetsTable(String classSourceName) {
        synchronized( getTargetsControl() ) {
            return getChangedTargetsTable().contains(classSourceName);
        }
    }

    protected void putTargetsTable(String classSourceName, TargetsTableImpl targetsTable,
                                  String reason, boolean isChanged) {
        synchronized ( getTargetsControl() ) {
            putTargetsTable(classSourceName, targetsTable);
            setChangedTargetsTable(classSourceName, reason, isChanged);
        }
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

    @Trivial
    public Set<String> getResolvedClassNames() {
        return i_resolvedClassNames;
    }

    @Trivial
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
                    changedContainerTableReason, !changedContainerTable);
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

        if ( modData.getIsLightweight() ) {
            isChanged = false;
            isChangedReason = "Lightweight";

            useContainerTable = createContainerTable(rootClassSource);

        } else if ( !modData.shouldRead("Containers table") || !modData.hasContainersTable() ) {
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
            String resultMsg = newResult("container table", isChangedReason, !isChanged);
            logger.logp(Level.FINER, CLASS_NAME, methodName, resultMsg);
        }
        return !isChanged;
    }

    /** Local constant: Parameter to 'sameAs' telling that the data use different intern maps. */
    private static final boolean HAVE_DIFFERENT_INTERN_MAPS = false;

    // TargetsScannerOverallImpl.validInternalContainers_Concurrent()
    // TargetsScannerOverallImpl.validInternalContainers()

    /**
     * Validate an internal container.
     *
     * Validation of the container may cause the container data to be loaded,
     * depending on the availability of cached data and the cache validation policy.
     *
     * @param classSource The class source which is to be validated.
     * @param scanPolicy The scan policy of the class source.
     *
     * @return True or false telling if the container data is unchanged.
     */
    protected boolean validInternalContainer(ClassSource classSource, ScanPolicy scanPolicy) {
        String methodName = "validInternalContainer";

        String useHash = ( logger.isLoggable(Level.FINER) ? getHashText() : null );

        boolean classSourceIsNamed = ( classSource.getName() != null );
        String classSourceName = classSource.getCanonicalName();

        TargetCacheImpl_DataCon conData = modData.getSourceConForcing(classSourceIsNamed, classSourceName);

        synchronized ( conData ) {
            TargetsTableImpl priorTargetsTable = getTargetsTable(classSourceName);
            if ( priorTargetsTable != null ) {
                boolean priorIsChanged = isChangedTargetsTable(classSourceName);
                if ( useHash != null ) {
                    String priorIsChangedReason = getChangedTargetsTableReason(classSourceName);
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                        priorResult("Internal class source " + classSourceName, priorIsChangedReason, priorIsChanged));
                }
                return !priorIsChanged;
            }

            String currentStamp = classSource.getStamp();

            if ( useHash != null ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] ENTER [ {1} ] [ {2} ]",
                    new Object[] { useHash, classSourceName, currentStamp });
            }

            String isValidStampReason = conData.isValid(this, classSourceName, currentStamp);
            if ( isValidStampReason == null ) {
                if ( useHash != null ) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                        newResult("Internal class source " + classSourceName, "Valid stamp", true));
                }

                TargetsTableImpl conTable = conData.getTargetsTable();
                if ( conTable != null ) {
                    conTable = internTargetsTable(conTable);
                }
                putTargetsTable(classSourceName, conTable, "Valid stamp", false);

                return true;
            }

            if ( useHash != null ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] [ {1} ]: Valid stamp [ false ]: {2}",
                    new Object[] { useHash, classSourceName, isValidStampReason });
            }

            TargetsTableImpl newTargets = createIsolatedTargetsTable(classSourceName, currentStamp);

            scanInternal(classSource,
                TargetsVisitorClassImpl.DONT_RECORD_RESOLVED,
                TargetsVisitorClassImpl.DONT_RECORD_UNRESOLVED,
                newTargets);

            String isValidReason;
            if ( !conData.hasCoreDataFile() ) {
                isValidReason = "New data";
            } else {
                TargetsTableImpl priorTargets = createIsolatedTargetsTable(classSourceName, currentStamp);
                if ( !conData.readCoreData(priorTargets) ) {
                    isValidReason = "Read failure";
                } else if ( !newTargets.getClassTable().sameAs( priorTargets.getClassTable(), HAVE_DIFFERENT_INTERN_MAPS ) ) {
                    isValidReason = "Change to classes";
                } else if ( !newTargets.getAnnotationTable().sameAs( priorTargets.getAnnotationTable(), HAVE_DIFFERENT_INTERN_MAPS ) ) {
                    isValidReason = "Change to annotations";
                } else {
                    isValidReason = null;
                }
            }

            boolean isValid;

            if ( isValidReason == null ) {
                isValidReason = "Only the stamp changed";
                isValid = true;
            } else {
                isValid = false;
            }

            if ( useHash != null ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] [ {1} ]: Is valid [ {2} ]: {3}",
                    new Object[] { useHash, classSourceName, isValid, isValidReason });
            }

            conData.writeStamp(modData, newTargets);
            if ( !isValid ) {
                conData.writeData(modData, newTargets);
            }
            conData.setTargetsTable(newTargets);

            putTargetsTable(
                classSourceName,
                internTargetsTable(newTargets),
                isValidReason, !isValid);

            if ( useHash != null ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    newResult("Internal class source " + classSourceName, isValidReason, isValid));
            }
            return isValid;
        }
    }

    // TargetsScannerOverallImpl.validInternalUnresolved()
    // TargetsScannerOverallImpl.validInternalResults()
    // TargetsScannerOverallImpl.validInternalClasses()

    // Control parameter: Force loading of data when validating internal containers.
    protected static final boolean DO_LOAD = true;

    /**
     * Validate the internal containers.  Choose a threading policy based on the
     * number of internal containers and the policy setting.
     *
     * If any of the container data is changed, all of the container data will be
     * loaded.  Otherwise, particular container data is loaded depending on the
     * availability of cached data and the cache validation policy.
     *
     * @return True or false telling if none of the container data changed.
     */
    protected boolean validInternalContainers_Select() {
        String methodName = "validInternalContainers_Select";

        String useHashText = ( logger.isLoggable(Level.FINER) ? getHashText() : null );

        if ( changedAnyTargetsReason != null ) {
            if ( useHashText != null ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] ENTER / RETURN [ {1} ] {2}",
                    new Object[] { useHashText, Boolean.valueOf(!changedAnyTargets), changedAnyTargetsReason });
            }
            return !changedAnyTargets;
        }

        if ( useHashText != null ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", useHashText);
        }

        if ( isScanSingleThreaded() || isScanSingleSource() ) {
            if ( useHashText != null ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Single-threaded scanning", useHashText);
            }
            validInternalContainers();

        } else {
            if ( useHashText != null ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Multi-threaded scanning", useHashText);
            }
            validInternalContainers_Concurrent();
        }

        if ( useHashText != null ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] ENTER / RETURN [ {1} ] {2}",
                new Object[] { useHashText, Boolean.valueOf(!changedAnyTargets), changedAnyTargetsReason });
        }
        return !changedAnyTargets;
    }

    // TargetsScannerOverallImpl.validInternalContainers_Select()

    /**
     * Validate the internal containers.  Process the container data in single
     * threaded mode.
     *
     * If any of the container data is changed, all of the container data will be
     * loaded.  Otherwise, particular container data is loaded depending on the
     * availability of cached data and the cache validation policy.
     *
     * @return True or false telling if none of the container data changed.
     */
    @Trivial
    protected void validInternalContainers() {
        String methodName = "validInternalContainers";

        String useHashText = ( logger.isLoggable(Level.FINER) ? getHashText() : null );
        if ( useHashText != null ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", useHashText);
        }

        boolean changedTable = !validContainerTable();

        int changedContainers = 0;
        int internalContainers = 0;

        for ( ClassSource childClassSource : rootClassSource.getClassSources() ) {
            ScanPolicy childScanPolicy = rootClassSource.getScanPolicy(childClassSource);
            if ( childScanPolicy == ScanPolicy.EXTERNAL ) {
                continue;
            }
            internalContainers++;
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
                changedReason += "; changed tables [ " + changedContainers + " ]";
            } else {
                changedReason = "changed tables [ " + changedContainers + " ]";
            }
        }

        changedAnyTargetsReason = changedReason;
        changedAnyTargets = changed;

        //

        if ( !changedAnyTargets ) {
            if ( useHashText != null ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] RETURN [ {1} ] unchanged internal containers",
                    new Object[] { useHashText, Integer.valueOf(internalContainers) });
            }
            return;
        }

        if ( useHashText != null ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Changed [ {1} ] out of [ {2} ] internal containers: [ {3} ]",
                new Object[] {
                    useHashText,
                    Integer.valueOf(changedContainers),
                    Integer.valueOf(internalContainers),
                    changedAnyTargetsReason });
        }

        forceInternalContainers();

        if ( useHashText != null ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] RETURN Completed [ {1} ] of [ {2} ] internal containers",
                new Object[] { useHashText,
                               Integer.valueOf(changedContainers),
                               Integer.valueOf(internalContainers) });
        }
    }

    protected void forceInternalContainers_Select() {
        int numMissingTables = 0;

        for ( ClassSource childClassSource : rootClassSource.getClassSources() ) {
            ScanPolicy childScanPolicy = rootClassSource.getScanPolicy(childClassSource);
            if ( childScanPolicy == ScanPolicy.EXTERNAL ) {
                continue;
            }

            String classSourceName = childClassSource.getCanonicalName();
            if ( getTargetsTable(classSourceName) == null ) {
                numMissingTables++;
            }
        }

        if ( numMissingTables == 0 ) {
            return;
        } else if ( isScanSingleThreaded() || (numMissingTables == 1) ) {
            forceInternalContainers();
        } else {
            forceInternalContainers_Concurrent();
        }
    }

    /**
     * Ensure that all internal container data is read.
     *
     * Read the internal containers which were not read by prior
     * processing.  Do not re-read containers which were previous
     * 
     * Read the data in single threaded mode.
     */
    @Trivial
    protected void forceInternalContainers() {
        String methodName = "forceInternalContainers";

        String useHashText = ( logger.isLoggable(Level.FINER) ? getHashText() : null );
        if ( useHashText != null ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", useHashText);
        }

        for ( ClassSource childClassSource : rootClassSource.getClassSources() ) {
            ScanPolicy childScanPolicy = rootClassSource.getScanPolicy(childClassSource);
            if ( childScanPolicy == ScanPolicy.EXTERNAL ) {
                continue;
            }

            boolean classSourceIsNamed = ( childClassSource.getName() != null );
            String classSourceName = childClassSource.getCanonicalName();
            String classSourceStamp = childClassSource.getStamp();

            TargetsTableImpl priorTargets = getTargetsTable(classSourceName);
            if ( priorTargets != null ) {
                continue;
            }

            TargetsTableImpl conTargets;
            boolean didRead;
            String dataReason;

            TargetCacheImpl_DataCon conData = modData.getSourceConForcing(classSourceIsNamed, classSourceName);

            synchronized ( conData ) {
                if ( conData.getHasStampFile() && conData.hasCoreDataFile() ) {
                    conTargets = createIsolatedTargetsTable(classSourceName, classSourceStamp);                    
                    if ( ! (conData.readStamp( conTargets.getStampTable() ) && 
                            conData.readCoreData(conTargets)) ) {
                        conTargets = null;
                        didRead = false;
                    } else {
                        didRead = true;
                    }
                } else {
                    conTargets = null;
                    didRead = false;
                }

                if ( didRead ) {
                    dataReason = "Cache read";

                } else {
                    conTargets = createIsolatedTargetsTable(classSourceName, classSourceStamp);

                    scanInternal(childClassSource,
                        TargetsVisitorClassImpl.DONT_RECORD_RESOLVED,
                        TargetsVisitorClassImpl.DONT_RECORD_UNRESOLVED,
                        conTargets);

                    conData.writeStamp(modData, conTargets);
                    conData.writeData(modData, conTargets);

                    dataReason = "New scan";
                }

                conData.setTargetsTable(conTargets);
            }

            putTargetsTable(
                classSourceName,
                internTargetsTable(conTargets),
                dataReason, !didRead);
        }

        if ( useHashText != null ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] RETURN", useHashText);
        }
    }

    // Set the number of scan threads to the minimum of three values:
    //     the requested count of scan threads
    //     the maximum allowed count of scan threads
    //     the number of child class sources

    // The number of threads should be always at least two:
    //     If scan threads option is set to 0 or 1, the non-concurrent
    //     scan is performed: This code is never reached.
    //     If the number of containers is 1, the non-concurrent scan
    //     is performed: This code is never reached.

    /**
     * Create an executor sized for the available class sources.
     * The executor expects to be completed by calls to
     * {@link UtilImpl_PoolExecutor#completeExecution}, once for
     * each class source.
     *
     * @return An executor sized for the available class sources.
     */
    protected UtilImpl_PoolExecutor createSourceExecutor() {
        List<? extends ClassSource> childSources = rootClassSource.getClassSources();
        int numChildSources = childSources.size();

        int scanThreads = getScanThreads();
        if ( scanThreads <= ClassSource_Options.SCAN_THREADS_UNBOUNDED) {
            scanThreads = ClassSource_Options.SCAN_THREADS_MAX;
        }
        if ( scanThreads > numChildSources ) {
            scanThreads = numChildSources; 
        }
        if ( scanThreads > ClassSource_Options.SCAN_THREADS_MAX ) {
            scanThreads = ClassSource_Options.SCAN_THREADS_MAX;
        }

        int corePoolSize = scanThreads;
        int maxPoolSize = scanThreads;

        return UtilImpl_PoolExecutor.createBlockingExecutor(corePoolSize, maxPoolSize, numChildSources);
    }

    /**
     * Ensure that all internal container data is read.
     *
     * Read the internal containers which were not read by prior
     * processing.  Do not re-read containers which were previous
     * 
     * Read the data using multiple threads.
     */
    protected void forceInternalContainers_Concurrent() {
        String methodName = "forceInternalContainers_Concurrent";

        String useHashText = ( logger.isLoggable(Level.FINER) ? getHashText() : null );
        if ( useHashText != null ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", useHashText);
        }

        List<? extends ClassSource> childClassSources = rootClassSource.getClassSources();
        int numChildSources = childClassSources.size();

        final UtilImpl_PoolExecutor readPool = createSourceExecutor();

        for ( int sourceNo = 0; sourceNo < numChildSources; sourceNo++ ) {
            final ClassSource childClassSource = childClassSources.get(sourceNo);
            final ScanPolicy childScanPolicy = rootClassSource.getScanPolicy(childClassSource);

            if ( childScanPolicy == ScanPolicy.EXTERNAL ) {
                readPool.completeExecution();
                continue;
            }

            final boolean classSourceIsNamed = ( childClassSource.getName() != null );
            final String classSourceName = childClassSource.getCanonicalName();
            final String classSourceStamp = childClassSource.getStamp();

            TargetsTableImpl priorTargets = getTargetsTable(classSourceName);
            if ( priorTargets != null ) {
                readPool.completeExecution();
                continue;
            }

            Runnable validator = new Runnable() {
                @Override
                public void run() {
                    try {
                        TargetsTableImpl conTargets;
                        boolean didRead;
                        String dataReason;

                        TargetCacheImpl_DataCon conData = modData.getSourceConForcing(classSourceIsNamed, classSourceName);

                        synchronized ( conData ) {
                            if ( conData.getHasStampFile() && conData.hasCoreDataFile() ) {
                                conTargets = createIsolatedTargetsTable(classSourceName, classSourceStamp);                    
                                if ( ! (conData.readStamp( conTargets.getStampTable() ) && 
                                        conData.readCoreData(conTargets)) ) {
                                    conTargets = null;
                                    didRead = false;
                                } else {
                                    didRead = true;
                                }
                            } else {
                                conTargets = null;
                                didRead = false;
                            }

                            if ( didRead ) {
                                dataReason = "Cache read";

                            } else {
                                conTargets = createIsolatedTargetsTable(classSourceName, classSourceStamp);

                                scanInternal(childClassSource,
                                    TargetsVisitorClassImpl.DONT_RECORD_RESOLVED,
                                    TargetsVisitorClassImpl.DONT_RECORD_UNRESOLVED,
                                    conTargets);

                                conData.writeStamp(modData, conTargets);
                                conData.writeData(modData, conTargets);

                                dataReason = "New scan";
                            }

                            conData.setTargetsTable(conTargets);
                        }

                        putTargetsTable(
                            classSourceName,
                            internTargetsTable(conTargets),
                            dataReason, !didRead);
         
                    } finally {
                        readPool.completeExecution();
                    }
                }
            };

            readPool.execute(validator);
        }

        try {
            readPool.waitForCompletion(); // throws InterruptedException
        } catch ( InterruptedException e ) {
            // CWWKC00??W 
            logger.logp(Level.WARNING, CLASS_NAME, methodName,
               "[ {0} ] ANNO_TARGETS_CACHE_EXCEPTION [ {1} ]",
               new Object[] { getHashText(), e });
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cache error", e);
        }

        if ( useHashText != null ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] RETURN", useHashText);
        }
    }

    // TargetsScannerOverallImpl.validInternalContainers_Select()

    /**
     * Validate the internal containers.  Process the container data using
     * multiple threads.
     *
     * If any of the container data is changed, all of the container data will be
     * loaded.  Otherwise, particular container data is loaded depending on the
     * availability of cached data and the cache validation policy.
     *
     * @return True or false telling if none of the container data changed.
     */
    protected void validInternalContainers_Concurrent() {
        String methodName = "validInternalContainers_Concurrent";

        String useHashText = ( logger.isLoggable(Level.FINER) ? getHashText() : null );
        if ( useHashText != null ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", useHashText);
        }

        boolean changedTable = !validContainerTable();

        List<? extends ClassSource> childClassSources = rootClassSource.getClassSources();
        int numChildSources = childClassSources.size();

        final boolean validContainers[] = new boolean[numChildSources];

        final UtilImpl_PoolExecutor validatorPool = createSourceExecutor();

        int internalContainers = 0;

        for ( int sourceNo = 0; sourceNo < numChildSources; sourceNo++ ) {
            final ClassSource childClassSource = childClassSources.get(sourceNo);
            final ScanPolicy childScanPolicy = rootClassSource.getScanPolicy(childClassSource);

            if ( childScanPolicy == ScanPolicy.EXTERNAL ) {
                validContainers[sourceNo] = true;
                validatorPool.completeExecution();
                continue;
            }

            internalContainers++;

            final int useSourceNo = sourceNo;

            Runnable validator = new Runnable() {
                @Override
                public void run() {
                    try {
                        validContainers[useSourceNo] = validInternalContainer(childClassSource, childScanPolicy);
                    } finally {
                        validatorPool.completeExecution();
                    }
                }
            };

            validatorPool.execute(validator);
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

        //

        if ( !changedAnyTargets ) {
            if ( useHashText != null ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] RETURN [ {1} ] unchanged internal containers",
                    new Object[] { useHashText, Integer.valueOf(internalContainers) });
            }
            return;
        }

        if ( useHashText != null ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Changed [ {1} ] out of [ {2} ] internal containers",
                new Object[] {
                    useHashText,
                    Integer.valueOf(changedContainers),
                    Integer.valueOf(internalContainers) });
        }

        final UtilImpl_PoolExecutor completionPool = createSourceExecutor();

        for ( final ClassSource childClassSource : rootClassSource.getClassSources() ) {
            ScanPolicy childScanPolicy = rootClassSource.getScanPolicy(childClassSource);
            if ( childScanPolicy == ScanPolicy.EXTERNAL ) {
                completionPool.completeExecution();
                continue;
            }

            final boolean classSourceIsNamed = ( childClassSource.getName() != null );
            final String classSourceName = childClassSource.getCanonicalName();

            TargetsTableImpl alreadyRefreshedTargets = getTargetsTable(classSourceName);
            if ( alreadyRefreshedTargets != null ) {
                completionPool.completeExecution();
                continue;
            }

            Runnable validator = new Runnable() {
                @Override
                public void run() {
                    try {
                        String classSourceStamp = childClassSource.getStamp();

                        TargetsTableImpl newTargets = createIsolatedTargetsTable(classSourceName, classSourceStamp);

                        scanInternal(childClassSource,
                            TargetsVisitorClassImpl.DONT_RECORD_RESOLVED,
                            TargetsVisitorClassImpl.DONT_RECORD_UNRESOLVED,
                            newTargets);

                        TargetCacheImpl_DataCon conData = modData.getSourceConForcing(classSourceIsNamed, classSourceName);

                        synchronized ( conData ) {
                            conData.writeStamp(modData, newTargets);
                            conData.writeData(modData, newTargets);

                            conData.setTargetsTable(newTargets);
                        }

                        putTargetsTable(
                            classSourceName,
                            internTargetsTable(newTargets),
                            "New data", false);

                    } finally {
                        completionPool.completeExecution();
                    }
                }
            };

            completionPool.execute(validator);
        }

        try {
            completionPool.waitForCompletion(); // throws InterruptedException
        } catch ( InterruptedException e ) {
            // CWWKC00??W 
            logger.logp(Level.WARNING, CLASS_NAME, methodName,
               "[ {0} ] ANNO_TARGETS_CACHE_EXCEPTION [ {1} ]",
               new Object[] { getHashText(), e });
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cache error", e);
        }

        if ( useHashText != null ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] RETURN Completed [ {1} ] of [ {2} ] internal containers",
                new Object[] { useHashText,
                               Integer.valueOf(changedContainers),
                               Integer.valueOf(internalContainers) });
        }
    }

    //

    /**
     * Validate the internal unresolved class list.
     * 
     * Use cached data when available, and when dependency
     * data is unchanged.  Otherwise, generate and cache the
     * data.
     */
    protected void validInternalUnresolved() {
        String methodName = "validInternalUnresolved";

        if ( i_resolvedClassNames != null ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            priorResult("Unresolved classes", changedClassNamesReason, !changedClassNames));
            }
            return;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", getHashText());
        }

        boolean doRead;
        boolean didValidate;
        String validReason;

        if ( modData.isAlwaysValid() ) {
            doRead = true;
            didValidate = false;
        } else {
            doRead = validInternalContainers_Select();
            didValidate = true;
        }

        boolean didRead;

        if ( !doRead ) {
            didRead = false;
            validReason = "Changed container data";

        } else {
            if ( !modData.shouldRead("Unresolved class data") ) {
                didRead = false;
                validReason = "Cache miss (read disabled)";
            } else if ( !modData.hasResolvedRefs() ) {
                didRead = false;
                validReason = "Cache miss (resolved)";
            } else if ( !modData.hasUnresolvedRefs() ) {
                didRead = false;
                validReason = "Cache miss (unresolved)";

            } else {
                UtilImpl_IdentityStringSet i_cachedResolved = null;
                UtilImpl_IdentityStringSet i_cachedUnresolved = null;

                i_cachedResolved = createIdentityStringSet();
                if ( !modData.readResolvedRefs(getClassNameInternMap(), i_cachedResolved) ) {
                    didRead = false;
                    validReason = "Cache miss (failed to read resolved)";
                } else {
                    i_cachedUnresolved = createIdentityStringSet();
                    if ( !modData.readUnresolvedRefs(getClassNameInternMap(), i_cachedUnresolved) ) {
                        didRead = false;
                        validReason = "Cache miss (failed to read unresolved)";
                    } else {
                        didRead = true;
                        validReason = "Cache hit";
                    }
                }
                
                if ( didRead ) {
                    i_resolvedClassNames = i_cachedResolved;
                    i_unresolvedClassNames = i_cachedUnresolved;

                    changedClassNames = false;
                    changedClassNamesReason = validReason;
                }
            }
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Read [ {1} ] ({2})",
                new Object[] { getHashText(), Boolean.valueOf(didRead), validReason });
        }

        if ( !didRead ) {
            if ( !didValidate ) {
                @SuppressWarnings("unused")
                boolean isValid = validInternalContainers_Select();
            }
            forceInternalContainers_Select();

            UtilImpl_IdentityStringSet i_newResolved = createIdentityStringSet();
            UtilImpl_IdentityStringSet i_newUnresolved = createIdentityStringSet();

            for ( ClassSource classSource : rootClassSource.getClassSources() ) {
                if ( rootClassSource.getScanPolicy(classSource) == ScanPolicy.EXTERNAL ) {
                    continue;
                }

                getTargetsTable(classSource).updateClassNames(i_newResolved, i_newUnresolved);

                if ( logger.isLoggable(Level.FINER) ) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] Class source [ {1} ] Resolved [ {2} ]",
                        new Object[] { getHashText(), classSource.getCanonicalName(), printString(i_newResolved) });
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] Class source [ {1} ] Unresolved [ {2} ]",
                        new Object[] { getHashText(), classSource.getCanonicalName(), printString(i_newUnresolved) });
                }
            }

            i_resolvedClassNames = i_newResolved;
            i_unresolvedClassNames = i_newUnresolved;

            changedClassNames = true;
            changedClassNamesReason = validReason;

            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Recomposed", getHashText());
            }

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

            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Written", getHashText());
            }
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    newResult("Unresolved classes", validReason, didRead));
            verifyUnresolved();
        }
    }

    @Trivial
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

    protected void validInternalResults() {
        String methodName = "validInternalResults";
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER", getHashText());
        }

        boolean doRead;
        boolean didValidate;
        String validReason;

        if ( modData.isAlwaysValid() ) {
            doRead = true;
            didValidate = false;
        } else {
            doRead = validInternalContainers_Select();
            didValidate = true;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Do read [ {1} ] Validate [ {2} ]",
                new Object[] { getHashText(), Boolean.valueOf(doRead), Boolean.valueOf(didValidate) });
        }

        TargetsTableImpl[] cachedTables = createResultTables();
        boolean didRead;

        if ( !doRead ) {
            didRead = false;
            validReason = "Changed container data";

        } else {
            didRead = readInternalResults_Select(cachedTables);
            if ( didRead ) {
                validReason = "Cache hit";
            } else {
                validReason = "Cache miss or read failure";
            }

            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Did read [ {1} ]",
                    new Object[] { getHashText(), Boolean.valueOf(doRead) });
            }
        }

        if ( !didRead ) {
            if ( !didValidate ) {
                @SuppressWarnings("unused")
                boolean isValid = validInternalContainers_Select();
            }
            forceInternalContainers_Select();

            TargetsTableImpl[] newTables = createResultTables();

            mergeInternalResults(newTables, !FORCE_SEED_RESULTS);

//            int useWriteLimit = getWriteLimit();
//            if ( logger.isLoggable(Level.FINER) ) {
//                logger.logp(Level.FINER, CLASS_NAME, methodName,
//                            "Write limit [ {0} ]", Integer.valueOf(useWriteLimit));
//            }

//            // Use of the write limit causes problems for the result tables:
//            // Often, one or more of the tables is too small to write.  But,
//            // we don't want to skip such writes when the class source data
//            // is big enough.  What would be preferred would be to key the
//            // writes of the results on the sizes of the class source tables.
//            //
//            // Here, we key the writes of the results on whether any one of
//            // the result tables is big enough to write.
//
//            boolean anyIsBigEnough = false;
//
//            for ( ScanPolicy scanPolicy : ScanPolicy.values() ) {
//                if ( scanPolicy == ScanPolicy.EXTERNAL ) {
//                    continue;
//                }
//
//                if ( modData.shouldWrite("Result container") ) {
//                    TargetsTableImpl newTable = newTables[ scanPolicy.ordinal() ];
//                    int numClasses = newTable.getClassNames().size();
//
//          if ( logger.isLoggable(Level.FINER) ) {
//                        logger.logp(Level.FINER, CLASS_NAME, methodName,
//                                    "Result [ {0} ]: Count of classes [ {1} ]",
//                                    new Object[] { scanPolicy, Integer.valueOf(numClasses) });
//                    }
//
//                    if ( numClasses >= useWriteLimit ) {
//                        anyIsBigEnough = true;
//                    }
//                }
//            }
//
//            if ( anyIsBigEnough ) {
//                logger.logp(Level.FINER, CLASS_NAME, methodName, "Write threshhold crossed: Writing all results");

                for ( ScanPolicy scanPolicy : ScanPolicy.values() ) {
                    if ( getPolicyCount(scanPolicy) == 0 ) {
                    	if ( logger.isLoggable(Level.FINER) ) {                    	
                    		logger.logp(Level.FINER, CLASS_NAME, methodName, "Skip write of result [ " + scanPolicy + " ]: No sources use that policy.");
                    	}
                        continue;
                    }
                    if ( (scanPolicy != ScanPolicy.EXTERNAL) && modData.shouldWrite("Result container") ) {
                        modData.writeResultCon( scanPolicy, newTables[ scanPolicy.ordinal() ] );
                    }
                }

//            } else {
//                if ( logger.isLoggable(Level.FINER) ) {
//                    logger.logp(Level.FINER, CLASS_NAME, methodName, "Write threshhold was not crossed; skiping result writes");
//                }
//            }

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
            logger.logp(Level.FINER, CLASS_NAME, methodName, newResult("Internal results", validReason, didRead));
        }
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

            if ( getPolicyCount(scanPolicy) == 0 ) {
                tables[ scanPolicy.ordinal() ] = createResultTargetsTable(scanPolicy);
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

            if ( getPolicyCount(scanPolicy) == 0 ) {
                tables[ scanPolicy.ordinal() ] = createResultTargetsTable(scanPolicy);
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

            if ( getPolicyCount(scanPolicy) == 0 ) {
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
    // In single threaded mode, the targets data is processed using the main
    // intern maps.  In multi-threaded mode, each targets data has its own intern
    // maps, and a final step is added to recreate the targets data using the
    // main intern maps.  Intern maps are placed by 'getTargetsTable'.  The
    // recreation of the targets data, when necessary, is done by
    // 'internTargetsTable'.

    protected TargetsTableImpl readResults(ScanPolicy scanPolicy, TargetsTableImpl[] tables) {
        if ( !modData.shouldRead("Result container") ) {
            return null;
        }

        TargetCacheImpl_DataCon resultCon =  modData.getResultCon(scanPolicy);

        TargetsTableImpl cachedResultData;

        synchronized( resultCon ) {
            if ( !resultCon.hasCoreDataFile() ) {
                return null;
            }

            cachedResultData = createResultTargetsTable(scanPolicy, resultCon);
            if ( !resultCon.readCoreData(cachedResultData) ) {
                return null;
            }
        }

        cachedResultData = internResultTargetsTable(cachedResultData, resultCon);

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

        boolean doRead;
        boolean didValidate;

        if ( modData.isAlwaysValid() ) {
            doRead = true;
            didValidate = false;
        } else {
            doRead = validInternalContainers_Select();
            didValidate = true;
        }

        TargetsTableClassesMultiImpl cachedClassTable;
        String reason;
        boolean isChanged;

        if ( !doRead ) {
            cachedClassTable = null;
            isChanged = true;
            reason = "Changed data";

        } else {
            if ( !modData.shouldRead("Class refs") || !modData.hasClasses() ) {
                cachedClassTable = null;
                isChanged = true;
                reason = "Absent class refs";
            } else {
                cachedClassTable = createClassTable();
                if ( !modData.readClasses(cachedClassTable) ) {
                    cachedClassTable = null;
                    isChanged = true;
                    reason = "Class refs read failure";
                } else {
                    isChanged = false;
                    reason = "Cached"; 
                }
            }
        }

        if ( cachedClassTable == null ) {
            if ( !didValidate ) {
                @SuppressWarnings("unused")
                boolean isValid = validInternalContainers_Select();
            }
            forceInternalContainers_Select();

            cachedClassTable = createClassTable();

            // Merge all of the internal containers to the overall class table.
            mergeClasses(cachedClassTable);

            if ( modData.shouldWrite("Class refs") ) {
                modData.writeClasses(cachedClassTable);
            }
        }

        classTable = cachedClassTable;

        setClassTable(classTable, reason, isChanged);

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, newResult("Internal results", reason, !isChanged));
        }
        return !isChanged;
    }

    //

    /**
     * Ensure that valid internal results are available.
     * 
     * This has three steps: Validating unresolved classes, validating
     * internal results, and validating internal class and annotations information.
     */
    protected void validInternal() {
        validInternalUnresolved();
        validInternalResults();
        validInternalClasses();

        displayCoverage();

        if ( logger.isLoggable(Level.FINER) ) {
            logInternalResults(logger);
        }
    }

    /**
     * Ensure that valid external results are available.
     * 
     * This requires that valid internal unresolved classes be available.
     * The external results are for unresolved classes and any classes
     * referenced by unresolved classes.
     */
    protected void validExternal() {
        String methodName = "validExternal";

        ClassSource externalClassSource = getExternalClassSource();
        if ( externalClassSource == null ) {
            throw new IllegalStateException("An external class source is required");
        }

        String classSourceName = externalClassSource.getCanonicalName();

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
            return;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "ENTER: External source [ {0} ]",
                        classSourceName);
        }

        isChanged = true;
        isChangedReason = "Always invalid";

        validInternalUnresolved();

        externalData = scanExternal(externalClassSource, i_resolvedClassNames, i_unresolvedClassNames);
        // externalData = internTargetsTable(externalData);

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

        // Neither the external results nor the fully populated class table is written.

        if ( logger.isLoggable(Level.FINER) ) {
            logClassInfo(logger);
            logExternalResults(logger);
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        newResult("External source " + classSourceName, isChangedReason, !isChanged));
        }
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
}
