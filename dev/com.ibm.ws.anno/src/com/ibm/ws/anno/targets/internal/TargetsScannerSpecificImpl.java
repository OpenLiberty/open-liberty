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

import java.util.Set;
import java.util.logging.Level;

import com.ibm.websphere.ras.annotation.Trivial;

import com.ibm.wsspi.anno.classsource.ClassSource;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Exception;

public class TargetsScannerSpecificImpl extends TargetsScannerBaseImpl {

    public static final String CLASS_NAME = TargetsScannerSpecificImpl.class.getSimpleName();

    //

    protected TargetsScannerSpecificImpl(
        AnnotationTargetsImpl_Targets targets,
        ClassSource_Aggregate rootClassSource) {

        super(targets, rootClassSource);
    }

    //

    /**
     * <p>Scan specified classes for annotations.  Record only annotations data.  Do not
     * record class reference information.</p>
     *
     * <p>Restrict results to the specific annotation classes.  An empty collection of
     * specified annotation class names selects no results.  A null collection of specified
     * annotation class names selects all results.</p>
     *
     * <p>Note: The restriction function is not currently implemented.</p>
     *
     * @param specificClassNames The class names which are to be scanned.
     * @param specificAnnotationClassNames
     *     Annotations which are to be noted.  Any other annotations are ignored.
     *     A null value here selects all annotations.
     * @throws AnnotationTargets_Exception
     *     Thrown in case of a failure during scanning.
     */
    @Trivial
    public void scan(Set<String> specificClassNames, Set<String> specificAnnotationClassNames)
        throws AnnotationTargets_Exception {

        String methodName = "scan";

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "ENTER [ {0} ] Classes [ {1} ]",
                        new Object[] { getHashText(),
                                       Integer.valueOf(specificClassNames.size()) });
        }

        ClassSource_Aggregate useRootClassSource = getRootClassSource();

        Set<String> i_specificClassNames =
            internClassNames(specificClassNames);
        Set<String> i_specificAnnotationClassNames =
            (specificAnnotationClassNames == null) ? null : internClassNames(specificAnnotationClassNames);

        Set<String> i_resolvedClassNames = createIdentityStringSet();

        for ( ClassSource classSource : useRootClassSource.getClassSources() ) {
            String classSourceName = classSource.getName();
            ScanPolicy scanPolicy = useRootClassSource.getScanPolicy(classSource);

            if ( i_specificClassNames.isEmpty() ) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                                "[ {0} ] [ {1} ] [ {2} ] [ {3} ] Skip: No unresolved classes",
                                new Object[] { getHashText(),
                                               classSourceName, classSource.getHashText(),
                                               scanPolicy });
                }
                continue;

            } else if ( scanPolicy == ScanPolicy.EXTERNAL ) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                                "[ {0} ] [ {1} ] [ {2} ] [ {3} ] Skip: External; Remaining [ {4} ]",
                                new Object[] { getHashText(),
                                               classSourceName, classSource.getHashText(),
                                               scanPolicy,
                                               Integer.valueOf(i_specificClassNames.size()) });
                }
                continue;

            } else {
                if (logger.isLoggable(Level.FINER)) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                                "[ {0} ] [ {1} ] [ {2} ] [ {3} ] Remaining [ {4} ]",
                                new Object[] { getHashText(),
                                               classSourceName, classSource.getHashText(),
                                               scanPolicy,
                                               Integer.valueOf(i_specificClassNames.size()) });
                }
            }

            TargetsTableImpl targetTable = createTargetsTable(classSource);

            targetTable.scanSpecific(classSource,
                                     i_specificClassNames, i_resolvedClassNames,
                                     i_specificAnnotationClassNames);
            // throws ClassSource_Exception

            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] [ {1} ]",
                            new Object[] { getHashText(), classSourceName });
            }

            putTargetsTable(classSourceName, targetTable);
        }

        mergeInternalResults( getResultTables() );

        TargetsTableClassesMultiImpl useClassTable = createClassTable();
        mergeClasses(useClassTable);
        setClassTable(useClassTable);

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "RETURN [ {0} ]", getHashText());
        }
    }
}
