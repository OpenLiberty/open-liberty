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
package com.ibm.ws.annocache.targets.delta.internal;

import java.io.PrintWriter;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.targets.delta.TargetsDelta_Classes;
import com.ibm.ws.annocache.targets.internal.AnnotationTargetsImpl_Factory;
import com.ibm.ws.annocache.targets.internal.TargetsTableClassesImpl;
import com.ibm.ws.annocache.util.delta.internal.UtilImpl_DeltaUtils;
import com.ibm.ws.annocache.util.delta.internal.UtilImpl_IdentityMapDelta;
import com.ibm.ws.annocache.util.delta.internal.UtilImpl_IdentitySetDelta;
import com.ibm.ws.annocache.util.delta.internal.UtilImpl_PrintLogger;
import com.ibm.ws.annocache.util.internal.UtilImpl_Factory;
import com.ibm.ws.annocache.util.internal.UtilImpl_InternMap;
import com.ibm.wsspi.annocache.util.Util_PrintLogger;

public class TargetsDeltaImpl_Classes implements TargetsDelta_Classes {
    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;

    public static final String CLASS_NAME = TargetsDeltaImpl_Classes.class.getSimpleName();
    
    protected String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    public TargetsDeltaImpl_Classes(AnnotationTargetsImpl_Factory factory,
                                 TargetsTableClassesImpl finalTable, TargetsTableClassesImpl initialTable) {

        String methodName = "<init>";

        this.factory = factory;

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        //

        UtilImpl_Factory utilFactory = factory.getUtilFactory();

        UtilImpl_InternMap finalClassNameMap = finalTable.getClassNameInternMap();
        UtilImpl_InternMap initialClassNameMap = initialTable.getClassNameInternMap();

        this.packageDelta = utilFactory.subtractSet(
            finalTable.i_getPackageNamesMap(), finalClassNameMap,
            initialTable.i_getPackageNamesMap(), initialClassNameMap);

        this.classDelta = utilFactory.subtractSet(
            finalTable.i_getClassNamesMap(), finalClassNameMap,
            initialTable.i_getClassNamesMap(), initialClassNameMap);

        this.superclassDelta = utilFactory.subtractMap(
            finalTable.i_getSuperclassNamesMap(), finalClassNameMap,
            initialTable.i_getSuperclassNamesMap(), initialClassNameMap );

        //

        Map<String, String[]> i_useAddedInterfaceNames = new IdentityHashMap<String, String[]>();
        Map<String, String[]> i_useRemovedInterfaceNames = new IdentityHashMap<String, String[]>();
        Map<String, String[]> i_useStillInterfaceNames = null; // Not recording these.

        UtilImpl_DeltaUtils.subtractArrays(
            finalTable.i_getInterfaceNamesMap(), finalClassNameMap, finalClassNameMap,
            initialTable.i_getInterfaceNamesMap(), initialClassNameMap, initialClassNameMap,
            i_useAddedInterfaceNames, i_useRemovedInterfaceNames, i_useStillInterfaceNames);

        this.i_addedInterfaceNames = i_useAddedInterfaceNames;
        this.i_removedInterfaceNames = i_useRemovedInterfaceNames;

        //

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", this.hashText);

            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Added Packages [ {1} ]",
                    new Object[] { this.hashText, Integer.valueOf(packageDelta.getAdded().size()) });
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Removed Packages [ {1} ]",
                    new Object[] { this.hashText, Integer.valueOf(packageDelta.getRemoved().size()) });

            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Added Classes [ {1} ]",
                    new Object[] { this.hashText, Integer.valueOf(classDelta.getAdded().size()) });
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Removed Clases [ {1} ]",
                    new Object[] { this.hashText, Integer.valueOf(classDelta.getRemoved().size()) });

            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Added Superclasses [ {1} ]",
                    new Object[] { this.hashText, Integer.valueOf(superclassDelta.getAddedMap().size()) });
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Removed Superclasses [ {1} ]",
                    new Object[] { this.hashText, Integer.valueOf(superclassDelta.getRemovedMap().size()) });
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Changed Superclasses [ {1} ]",
                    new Object[] { this.hashText, Integer.valueOf(superclassDelta.getChangedMap().size()) });

            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Classes adding interfaces [ {1} ]",
                    new Object[] { this.hashText, Integer.valueOf(i_addedInterfaceNames.size()) });
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Classes removing interfaces [ {1} ]",
                    new Object[] { this.hashText, Integer.valueOf(i_removedInterfaceNames.size()) });
        }
    }

    //

    public AnnotationTargetsImpl_Factory factory;

    @Override
    public  AnnotationTargetsImpl_Factory getFactory() {
        return factory;
    }

    //

    protected final UtilImpl_IdentitySetDelta packageDelta;

    @Override
    public UtilImpl_IdentitySetDelta getPackageDelta() {
        return packageDelta;
    }

    protected final UtilImpl_IdentitySetDelta classDelta;

    @Override
    public UtilImpl_IdentitySetDelta getClassDelta() {
        return classDelta;
    }

    protected final UtilImpl_IdentityMapDelta superclassDelta;

    @Override
    public UtilImpl_IdentityMapDelta getSuperclassDelta() {
        return superclassDelta;
    }

    //

    protected final Map<String, String[]> i_addedInterfaceNames;
    protected final Map<String, String[]> i_removedInterfaceNames;

    @Override
    public Map<String, String[]> i_getAddedInterfaceNames() {
        return i_addedInterfaceNames;
    }

    @Override
    public Map<String, String[]> i_getRemovedInterfaceNames() {
        return i_removedInterfaceNames;
    }

    @Override
    public boolean isNullInterfaceChanges() {
        return ( i_addedInterfaceNames.isEmpty() &&
                 i_removedInterfaceNames.isEmpty() );
    }

    @Override
    public boolean isNullInterfaceChanges(boolean ignoreRemovedInterfaces) {
        return ( i_addedInterfaceNames.isEmpty() &&
                 (ignoreRemovedInterfaces || i_removedInterfaceNames.isEmpty()) );
    }

    protected void describeInterfaces(String prefix, List<String> nonNull) {
        if ( !i_addedInterfaceNames.isEmpty() ) {
            nonNull.add(prefix + " Added [ " + i_addedInterfaceNames.size() + " ]");
        }
        if ( !i_removedInterfaceNames.isEmpty() ) {
            nonNull.add(prefix + " Removed [ " + i_removedInterfaceNames.size() + " ]");
        }
    }

    //

    @Override
    public boolean isNull() {
        return (  getPackageDelta().isNull() &&
                  getClassDelta().isNull() &&
                  getSuperclassDelta().isNull() &&
                  isNullInterfaceChanges() );
    }

    @Override
    public boolean isNull(boolean ignoreRemovedPackages, boolean ignoreRemovedInterfaces) {
        return (  getPackageDelta().isNull(ignoreRemovedPackages) &&
                  getClassDelta().isNull() &&
                  getSuperclassDelta().isNull() &&
                  isNullInterfaceChanges(ignoreRemovedInterfaces) );

    }

    //

    @Override
    public void describe(String prefix, List<String> nonNull) {
        getPackageDelta().describe(prefix + ": Packages: ", nonNull);
        getClassDelta().describe(prefix + ": Classes: ", nonNull);
        getSuperclassDelta().describe(prefix + ": Superclass: ", nonNull);
        describeInterfaces(prefix + ": Interfaces: ", nonNull);
    }

    
    //

    @Override
    public void log(Logger useLogger) {
        if ( useLogger.isLoggable(Level.FINER) ) {
            log( new UtilImpl_PrintLogger(useLogger) );
        }
    }

    @Override
    public void log(PrintWriter writer) {
        log( new UtilImpl_PrintLogger(writer) );
    }

    @Override
    public void log(Util_PrintLogger useLogger) {
        String methodName = "log";
    
        useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                       "Class Relationships Delta: BEGIN: [ {0} ]", getHashText());

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Package Changes: BEGIN");
        getPackageDelta().log(useLogger);
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Package Changes: END");

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class Changes: BEGIN");
        getClassDelta().log(useLogger);
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class Changes: END");

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "SuperClass Changes: BEGIN");
        getSuperclassDelta().log(useLogger);
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "SuperClass Changes: END");

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Interface Changes: BEGIN");
        logInterfaces(useLogger);
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Interface Changes: END");

        useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                       "Class Relationships Delta: END: [ {0} ]",
                       getHashText());
    }

    public void logInterfaces(Util_PrintLogger useLogger) {
        String methodName = "logInterfaces";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Added Interfaces:");

        if ( i_addedInterfaceNames.isEmpty() ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NONE **");
        } else {
            int addedNo = 0;

            for ( Map.Entry<String, String[]> i_interfaceEntry : i_addedInterfaceNames.entrySet() ) {
                String i_className = i_interfaceEntry.getKey();
                String[] i_addedNames = i_interfaceEntry.getValue();
                if ( addedNo > 3 ) {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "  [ ... " + i_addedInterfaceNames.entrySet().size() + " ] ");
                    break;
                } else {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "  [ " + addedNo + " ] "  + i_className + " implements");
                    for ( String i_addedName : i_addedNames ) {
                        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "    " + i_addedName);
                    }
                }
                addedNo++;
            }
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Removed Interfaces:");

        if ( i_removedInterfaceNames.isEmpty() ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NONE **");
        } else {
            int removedNo = 0;
            for ( Map.Entry<String, String[]> i_interfaceEntry : i_removedInterfaceNames.entrySet() ) {
                String i_className = i_interfaceEntry.getKey();
                String[] i_removedNames = i_interfaceEntry.getValue();

                if ( removedNo > 3 ) {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "  [ ... " + i_removedInterfaceNames.entrySet().size() + " ] ");
                    break;
                } else {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "  [ " + removedNo + " ] "  + i_className + " implements");
                    for ( String i_removedName : i_removedNames ) {
                        useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "    " + i_removedName);
                    }
                }
                removedNo++;
            }
        }
    }

    protected String listText(String[] elements, StringBuilder builder) {
        boolean onFirst = true;
        for ( String element : elements ) {
            if ( !onFirst ) {
                builder.append(", ");
            } else {
                onFirst = false;
            }
            builder.append(element);
        }

        String result = builder.toString();
        builder.setLength(0);
        return result;
    }
}