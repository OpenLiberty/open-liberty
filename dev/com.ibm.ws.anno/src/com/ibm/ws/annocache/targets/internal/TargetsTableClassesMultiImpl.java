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

import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.targets.TargetsTableClassesMulti;
import com.ibm.ws.annocache.targets.cache.TargetCache_ParseError;
import com.ibm.ws.annocache.targets.cache.TargetCache_Reader;
import com.ibm.ws.annocache.util.internal.UtilImpl_Factory;
import com.ibm.ws.annocache.util.internal.UtilImpl_InternMap;
import com.ibm.wsspi.annocache.util.Util_InternMap;

/**
 * Extension of the class table: Partitions package and class names by
 * class source name: Each package name and each class name is in a single
 * class source.  (Bi-directional maps are not used since the mappings are
 * one-to-many, not many-to-many.)
 *
 * The extended table is used a hold the all of the class information
 * for of an aggregate class source.
 *
 * The extended table uses the intern map of the annotation targets table.
 */
public class TargetsTableClassesMultiImpl extends TargetsTableClassesImpl
    implements TargetsTableClassesMulti {

    @SuppressWarnings("hiding")
	public static final String CLASS_NAME = TargetsTableClassesMultiImpl.class.getSimpleName();

    //

    @Trivial
    protected TargetsTableClassesMultiImpl(
            UtilImpl_Factory utilFactory,
            UtilImpl_InternMap classNameInternMap) {

        super(utilFactory, classNameInternMap, "root");

        String methodName = "<init>";

        // many-to-one: package name to class source name
        this.i_packageNameClassSourceMap = new IdentityHashMap<String, String>();
        this.i_classSourcePackageNamesMap = new HashMap<String, Set<String>>();

        // many-to-one: class name to class source name
        this.i_classNameClassSourceMap = new IdentityHashMap<String, String>();
        this.i_classSourceClassNamesMap = new HashMap<String, Set<String>>();

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", this.hashText);
        }
    }

    //

    protected final Map<String, String> i_packageNameClassSourceMap;
    protected final Map<String, Set<String>> i_classSourcePackageNamesMap;

    protected final Map<String, String> i_classNameClassSourceMap;
    protected final Map<String, Set<String>> i_classSourceClassNamesMap;

    @Trivial
    public Map<String, String> i_getPackageNameClassSourceMap() {
        return i_packageNameClassSourceMap;
    }

    @Trivial
    public Map<String, Set<String>> i_getClassSourcePackageNamesMap() {
        return i_classSourcePackageNamesMap;
    }

    @Trivial
    public Map<String, String> i_getClassNameClassSourceMap() {
        return i_classNameClassSourceMap;
    }

    @Trivial
    public Map<String, Set<String>> i_getClassSourceClassNamesMap() {
        return i_classSourceClassNamesMap;
    }

    @Override
    public String getClassSourceNameForPackageName(String packageName) {
        String i_packageName = internClassName(packageName, Util_InternMap.DO_NOT_FORCE);
        if ( i_packageName == null ) {
            return null;
        } else {
            return i_getClassSourceNameForPackageName(i_packageName);
        }
    }

    @Override
    public String i_getClassSourceNameForPackageName(String i_packageName) {
        return i_packageNameClassSourceMap.get(i_packageName);
    }

    @Override
    public Set<String> getClassSourceNames() {
        return i_classSourceClassNamesMap.keySet();
    }

    @Override
    @Trivial
    public Set<String> getPackageNames(String childClassSourceName) {
        return uninternClassNames( i_getPackageNames(childClassSourceName) );
    }

    @Override
    @Trivial
    public Set<String> i_getPackageNames(String childClassSourceName) {
        Set<String> result = i_classSourcePackageNamesMap.get(childClassSourceName);
//        if ( result == null ) {
//            System.out.println("TargetsClassTable package names [ " + classSourceName + " ] [ 0 (null) ]");
//        } else {
//            System.out.println("TargetsClassTable package names [ " + classSourceName + " ] [ " + result.size() + " ]");
//        }
        return result;
    }

    @Override
    public boolean containsPackageName(String childClassSourceName, String packageName) {
        Set<String> i_classSourcePackageNames = i_classSourcePackageNamesMap.get(childClassSourceName);
        if ( i_classSourcePackageNames == null ) {
            return false;
        } else {
            String i_packageName = internClassName(packageName, Util_InternMap.DO_NOT_FORCE);
            if ( i_packageName == null ) {
                return false;
            } else {
                return i_classSourcePackageNames.contains(i_packageName);
            }
        }
    }

    @Override
    public boolean i_containsPackageName(String childClassSourceName, String i_packageName) {
        Set<String> i_classSourcePackageNames = i_classSourcePackageNamesMap.get(childClassSourceName);
        if ( i_classSourcePackageNames == null ) {
            return false;
        } else {
            return i_classSourcePackageNames.contains(i_packageName);
        }
    }

    @Override
    public String getClassSourceNameForClassName(String className) {
        String i_className = internClassName(className, Util_InternMap.DO_NOT_FORCE);
        if ( i_className == null ) {
            return null;
        } else {
            return i_getClassSourceNameForClassName(i_className);
        }
    }

    @Override
    public String i_getClassSourceNameForClassName(String i_className) {
        return i_classNameClassSourceMap.get(i_className);
    }

    @Override
    @Trivial
    public Set<String> getClassNames(String childClassSourceName) {
        return uninternClassNames( i_getClassNames(childClassSourceName) );
    }

    @Override
    @Trivial
    public Set<String> i_getClassNames(String childClassSourceName) {
        Set<String> result = i_classSourceClassNamesMap.get(childClassSourceName);
//        if ( result == null ) {
//            System.out.println("TargetsClassTable class names [ " + classSourceName + " ] [ 0 (null) ]");
//        } else {
//            System.out.println("TargetsClassTable class names [ " + classSourceName + " ] [ " + result.size() + " ]");
//        }
        return result;
    }

    @Override
    public boolean containsClassName(String childClassSourceName, String className) {
        Set<String> i_classSourceClassNames = i_classSourceClassNamesMap.get(childClassSourceName);
        if ( i_classSourceClassNames == null ) {
            return false;
        } else {
            String i_className = internClassName(className, Util_InternMap.DO_NOT_FORCE);
            if ( i_className == null ) {
                return false;
            } else {
                return i_classSourceClassNames.contains(i_className);
            }
        }
    }

    @Override
    public boolean i_containsClassName(String childClassSourceName, String i_className) {
        Set<String> i_classSourceClassNames = i_classSourceClassNamesMap.get(childClassSourceName);
        if ( i_classSourceClassNames == null ) {
            return false;
        } else {
            return i_classSourceClassNames.contains(i_className);
        }
    }

    //

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

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class Relationships: BEGIN [ {0} ]", getHashText());

        logClassNames(useLogger);
        logSuperclassNames(useLogger);
        logInterfaceNames(useLogger);

        logClassSources(useLogger);

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class Relationships: END [ {0} ]", getHashText());
    }

    @Trivial
    public void logClassSources(Logger useLogger) {
        String methodName = "logClassSources";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class Sources: BEGIN");

        for (String childClassSourceName : getClassSourceNames() ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ {0} ]", childClassSourceName);
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, 
                        "    [ {0} ] packages",
                        Integer.toString(getPackageNames(childClassSourceName).size()));
            useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "    [ {0} ] classes",
                        Integer.toString(getClassNames(childClassSourceName).size()));
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Classes Sources: END");
    }

    //

    @Override
    public void record(String childClassSourceName, String i_packageName) {
        super.record(i_packageName);

        i_packageNameClassSourceMap.put(i_packageName, childClassSourceName);

        Set<String> i_classSourcePackageNames = i_classSourcePackageNamesMap.get(childClassSourceName);
        if ( i_classSourcePackageNames == null ) {
            i_classSourcePackageNames = createIdentityStringSet();
            i_classSourcePackageNamesMap.put(childClassSourceName, i_classSourcePackageNames);
        }

        i_classSourcePackageNames.add(i_packageName);
    }

//    private Throwable classSourceRecord;
//
//    private synchronized void markClassSourceRecord(String i_className, String classSourceName) {
//        classSourceRecord = new Throwable(
//                "ClassTableMulti:" +
//                " LastRecord [ " + Thread.currentThread().getName() + " ]" +
//                " [ " + i_className + " ] [ " + classSourceName + " ]");
//    }
//
//    public synchronized void clearClassRecord() {
//        classSourceRecord = null;
//    }
//    
//    public synchronized void verifyClassRecord() {
//        if ( classSourceRecord != null ) {
//            System.out.println("ClassTableMulti: Class table [ " + getHashText() + " ]");
//            System.out.println("ClassTableMutli: Class names [ " + i_classNames.hashCode() + " ]");
//            classSourceRecord.printStackTrace(System.out);
//            classSourceRecord = null;
//
//            (new Throwable("ClassTableMulti: Eager [ " + Thread.currentThread().getName() + " ]")).printStackTrace(System.out);
//        }
//    }
    
    @Override
    public void record(String childClassSourceName,
                       String i_className, String i_superclassName, List<String> i_useInterfaceNames,
                       int modifiers) {

        super.record(i_className, i_superclassName, i_useInterfaceNames, modifiers);

//        markClassSourceRecord(i_className, classSourceName);
        
        i_classNameClassSourceMap.put(i_className, childClassSourceName);

        Set<String> i_classSourceClassNames = i_classSourceClassNamesMap.get(childClassSourceName);
        if ( i_classSourceClassNames == null ) {
            i_classSourceClassNames = createIdentityStringSet();
            i_classSourceClassNamesMap.put(childClassSourceName, i_classSourceClassNames);
        }

        i_classSourceClassNames.add(i_className);
    }

    /**
     * <p>Add data from a table into this table.</p>
     *
     * <p>Only add data for new packages and classes.</p>
     *
     * <p>Record which packages and classes were added.  Other
     * data addition steps needs the additions information to
     * restrict other additions.</p>
     *
     * @param otherClassTable The table which is to be added to this table.
     *
     * @param i_newlyAddedPackageNames The names of the packages which were added.
     * @param i_addedPackageNames The names of the packages which were added.
     *
     * @param i_newlyAddedClassNames The names of the classes which were added.
     * @param i_addedClassNames The names of the classes which were added.
     */
    @Override
    @Trivial
    protected void restrictedAdd(TargetsTableClassesImpl otherClassTable,
                                 Set<String> i_newlyAddedPackageNames, Set<String> i_addedPackageNames,
                                 Set<String> i_newlyAddedClassNames, Set<String> i_addedClassNames) {

        i_addPackageNames( otherClassTable.getClassSourceName(),
                           otherClassTable.i_getPackageNames(),
                           i_newlyAddedPackageNames, i_addedPackageNames );

        i_addClassNames( otherClassTable.getClassSourceName(),
                         otherClassTable.i_getClassNames(),
                         i_newlyAddedClassNames, i_addedClassNames );

        i_addSuperclassNames( otherClassTable.i_getSuperclassNames(), i_newlyAddedClassNames );
        i_addInterfaceNames( otherClassTable.i_getInterfaceNames(), i_newlyAddedClassNames );
        i_addModifiers( otherClassTable.i_getModifiers(), i_newlyAddedClassNames );
    }

    @Trivial
    protected void i_addPackageNames(String otherClassSourceName,
                                     Set<String> i_otherPackageNames,
                                     Set<String> i_newlyAddedPackageNames,
                                     Set<String> i_addedPackageNames) {

        Set<String> i_classSourcePackageNames = i_classSourcePackageNamesMap.get(otherClassSourceName);

        for ( String i_packageName : i_otherPackageNames ) {
            if ( i_addedPackageNames.contains(i_packageName) ) {
                continue;
            }

            // Step 1: Add the class name; same as in the superclass.

            i_packageNames.put(i_packageName, i_packageName);

            i_newlyAddedPackageNames.add(i_packageName);
            i_addedPackageNames.add(i_packageName);

            // Step 2: Map the class name to the class source; new to the subclass.

            i_packageNameClassSourceMap.put(i_packageName, otherClassSourceName);

            if ( i_classSourcePackageNames == null ) {
                i_classSourcePackageNames = createIdentityStringSet();
                i_classSourcePackageNamesMap.put(otherClassSourceName, i_classSourcePackageNames);
            }

            i_classSourcePackageNames.add(i_packageName);
        }
    }

    @Trivial
    protected void i_addClassNames(String otherClassSourceName,
                                   Set<String> i_otherClassNames,
                                   Set<String> i_newlyAddedClassNames,
                                   Set<String> i_addedClassNames) {

//        markClassRecord("multiple");
//        markClassSourceRecord("multiple", otherClassSourceName);
        
        Set<String> i_classSourceClassNames = i_classSourceClassNamesMap.get(otherClassSourceName);

        for ( String i_className : i_otherClassNames ) {
            if ( i_addedClassNames.contains(i_className) ) {
                continue;
            }

            // Step 1: Add the class name; same as in the superclass.

            i_classNames.put(i_className, i_className);

            i_newlyAddedClassNames.add(i_className);
            i_addedClassNames.add(i_className);

            // Step 2: Map the class name to the class source; new to the subclass.

            i_classNameClassSourceMap.put(i_className, otherClassSourceName);

            if ( i_classSourceClassNames == null ) {
                i_classSourceClassNames = createIdentityStringSet();
                i_classSourceClassNamesMap.put(otherClassSourceName, i_classSourceClassNames);
            }

            i_classSourceClassNames.add(i_className);
        }
    }

    //

    @Override
    public List<TargetCache_ParseError> readUsing(TargetCache_Reader reader) throws IOException {
        return reader.readMulti(this); // throws IOException
    }

    //

    @Override
    @Trivial
    public void updateClassNames(Set<String> i_resolvedClassNames, Set<String> i_newlyResolvedClassNames,
                                 Set<String> i_unresolvedClassNames, Set<String> i_newlyUnresolvedClassNames) {

        for ( String i_packageName : i_getPackageNames() ) {
            if ( i_resolvedClassNames.add(i_packageName) ) {
                i_newlyResolvedClassNames.add(i_packageName);

                if ( i_unresolvedClassNames.remove(i_packageName) ) {
                    i_newlyUnresolvedClassNames.add(i_packageName);
                }
            }
        }

        for ( String i_className : i_getClassNames() ) {
            if ( i_resolvedClassNames.add(i_className) ) {
                i_newlyResolvedClassNames.add(i_className);

                if ( i_unresolvedClassNames.remove(i_className) ) {
                    i_newlyUnresolvedClassNames.add(i_className);
                }
            }
        }

        for ( Map.Entry<String, String[]> i_interfaceEntry : i_getInterfaceNamesMap().entrySet() ) {
            String i_className = i_interfaceEntry.getKey();
            if ( !i_newlyResolvedClassNames.contains(i_className) ) {
                // Don't process it!  It was processed previously,
                // possibly with different interfaces.
                continue;
            }

            String[] use_i_interfaceNames = i_interfaceEntry.getValue();
            for ( String i_interfaceName : use_i_interfaceNames ) {
                if ( !i_resolvedClassNames.contains(i_interfaceName) ) {
                    if ( i_unresolvedClassNames.add(i_interfaceName) ) {
                        i_newlyUnresolvedClassNames.add(i_interfaceName);
                    }
                }
            }
        }
    }

    public static final boolean IS_CONGRUENT = true; // Uses the same intern maps.

    public boolean sameAs(TargetsTableClassesMultiImpl otherTable) {
        if ( otherTable == null ) {
            return false;
        } else if ( otherTable == this ) {
            return true;
        }

        if ( !sameAs(otherTable, IS_CONGRUENT) ) {
            return false;
        }

        Map<String, String> i_thisPackageMap = i_packageNameClassSourceMap;
        Map<String, String> i_otherPackageMap = otherTable.i_packageNameClassSourceMap;

        for ( Map.Entry<String, String> i_thisEntry : i_thisPackageMap.entrySet() ) {
            String i_thisPackageName = i_thisEntry.getKey();

            String otherClassSourceName = i_otherPackageMap.get(i_thisPackageName);
            if ( otherClassSourceName == null ) {
                return false;
            }

            String thisClassSourceName = i_thisEntry.getValue();
            if ( !thisClassSourceName.equals(otherClassSourceName) ) {
                return false;
            }
        }

        Map<String, String> i_thisClassMap = i_packageNameClassSourceMap;
        Map<String, String> i_otherClassMap = otherTable.i_packageNameClassSourceMap;

        for ( Map.Entry<String, String> i_thisEntry : i_thisClassMap.entrySet() ) {
            String i_thisClassName = i_thisEntry.getKey();

            String otherClassSourceName = i_otherClassMap.get(i_thisClassName);
            if ( otherClassSourceName == null ) {
                return false;
            }

            String thisClassSourceName = i_thisEntry.getValue();
            if ( !thisClassSourceName.equals(otherClassSourceName) ) {
                return false;
            }
        }

        return true;
    }
}
