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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.targets.TargetsTableClasses;
import com.ibm.ws.annocache.targets.cache.TargetCache_ParseError;
import com.ibm.ws.annocache.targets.cache.TargetCache_Readable;
import com.ibm.ws.annocache.targets.cache.TargetCache_Reader;
import com.ibm.ws.annocache.util.internal.UtilImpl_Factory;
import com.ibm.ws.annocache.util.internal.UtilImpl_InternMap;
import com.ibm.ws.annocache.util.internal.UtilImpl_NonInternSet;
import com.ibm.wsspi.annocache.util.Util_InternMap;

/**
 * Store of class information.  Has three current uses:
 *
 *   * Stores class information for a single non-aggregate class source.
 *
 *   * Stores class information for a single scan policy (SEED, PARTIAL,
 *     EXCLUDED, or EXTERNAL).
 *
 *   * Through subclass "TargetsTableClassesMultiImpl", stores overall
 *     class information for a single aggregate class source.
 *
 * The name of a classes table is either the name of a non-aggregate
 * class source, the text value of a scan policy, or "root", depending
 * on the particular use.
 *
 * Class information consists of:
 *
 *   * package names
 *   * class names
 *   * super class names
 *   * interface names
 *   * class modifiers
 */
public class TargetsTableClassesImpl
    implements TargetsTableClasses, TargetCache_Readable {

    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;
    protected static final Logger stateLogger = AnnotationCacheServiceImpl_Logging.ANNO_STATE_LOGGER;

    public static final String CLASS_NAME = TargetsTableClassesImpl.class.getSimpleName(); 

    protected final String hashText;

    @Override
    @Trivial
    public String getHashText() {
        return hashText;
    }

    //

    protected TargetsTableClassesImpl(TargetsTableClassesImpl otherTable,
                                      UtilImpl_InternMap classNameInternMap,
                                      String classSourceName) {

        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.utilFactory = otherTable.getUtilFactory();
        this.classNameInternMap = classNameInternMap;

        this.classSourceName = classSourceName;

        this.i_packageNames = internClassNamesMap( otherTable.i_packageNames );

        this.i_classNames = internClassNamesMap( otherTable.i_classNames );
        this.i_superclassNames = internClassNamesMap( otherTable.i_superclassNames );
        this.i_interfaceNames = internInterfaceNamesMap( otherTable.i_interfaceNames );
        this.i_modifiers = internModifiersMap(otherTable.i_modifiers);

        //

        this.didSetDescendents = false;
        this.i_descendants = new IdentityHashMap<String, Set<String>>();

        this.didSetImplementers = false;
        this.i_allImplementers = new IdentityHashMap<String, Set<String>>();

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", this.hashText);
        }
    }

    @Trivial
    private Map<String, String[]> internInterfaceNamesMap(Map<String, String[]> i_otherInterfaceNamesMap) {
        Map<String, String[]> i_thisInterfaceNamesMap = new IdentityHashMap<String, String[]>();

        for ( Map.Entry<String, String[]> i_otherEntry : i_otherInterfaceNamesMap.entrySet() ) {
            String i_otherClassName = i_otherEntry.getKey();
            String[] i_otherInterfaceNames = i_otherEntry.getValue();

            String i_thisClassName = internClassName(i_otherClassName, Util_InternMap.DO_FORCE);
            String[] i_thisInterfaceNames = internInterfaceNames(i_otherInterfaceNames);

            i_thisInterfaceNamesMap.put(i_thisClassName, i_thisInterfaceNames);
        }

        return i_thisInterfaceNamesMap;
    }

    @Trivial
    protected String[] internInterfaceNames(String[] i_otherInterfaceNames) {
        String[] i_thisInterfaceNames = new String[ i_otherInterfaceNames.length ];
        for ( int interfaceNameNo = 0; interfaceNameNo < i_otherInterfaceNames.length; interfaceNameNo++ ) {
            i_thisInterfaceNames[interfaceNameNo] = internClassName(i_otherInterfaceNames[interfaceNameNo], Util_InternMap.DO_FORCE);
        }

        return i_thisInterfaceNames;
    }

    @Trivial
    private Map<String, String> internClassNamesMap(Map<String, String> i_otherClassNamesMap) {
        Map<String, String> i_thisClassNamesMap = new IdentityHashMap<String, String>();

        for ( Map.Entry<String, String> i_otherEntry : i_otherClassNamesMap.entrySet() ) {
            String i_otherClassName = i_otherEntry.getKey();
            String i_otherMappedClassName = i_otherEntry.getValue();

            String i_thisClassName = internClassName(i_otherClassName, Util_InternMap.DO_FORCE);
            String i_thisMappedClassName = internClassName(i_otherMappedClassName, Util_InternMap.DO_FORCE);

            i_thisClassNamesMap.put(i_thisClassName, i_thisMappedClassName);

        }

        return i_thisClassNamesMap;
    }

    @Trivial
    private Map<String, Integer> internModifiersMap(Map<String, Integer> i_otherModifiersMap) {
        Map<String, Integer> i_thisModifiersMap = new IdentityHashMap<String, Integer>();

        for ( Map.Entry<String, Integer> i_otherEntry : i_otherModifiersMap.entrySet() ) {
            String i_otherClassName = i_otherEntry.getKey();
            Integer otherModifier = i_otherEntry.getValue();

            String i_thisClassName = internClassName(i_otherClassName, Util_InternMap.DO_FORCE);

            i_thisModifiersMap.put(i_thisClassName, otherModifier);

        }

        return i_thisModifiersMap;
    }
    
    protected TargetsTableClassesImpl(UtilImpl_Factory utilFactory,
                                      UtilImpl_InternMap classNameInternMap,
                                      String classSourceName) {

        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.utilFactory = utilFactory;
        this.classNameInternMap = classNameInternMap;

        this.classSourceName = classSourceName;

        this.i_packageNames = new IdentityHashMap<String, String>();

        this.i_classNames = new IdentityHashMap<String, String>();
        this.i_superclassNames = new IdentityHashMap<String, String>();
        this.i_interfaceNames = new IdentityHashMap<String, String[]>();
        this.i_modifiers = new IdentityHashMap<String, Integer>();
        
        this.didSetDescendents = false;
        this.i_descendants = new IdentityHashMap<String, Set<String>>();

        this.didSetImplementers = false;
        this.i_allImplementers = new IdentityHashMap<String, Set<String>>();

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", this.hashText);
        }
    }

    //

    protected final UtilImpl_Factory utilFactory;

    @Trivial
    public UtilImpl_Factory getUtilFactory() {
        return utilFactory;
    }

    protected final UtilImpl_InternMap classNameInternMap;

    @Trivial
    public UtilImpl_InternMap getClassNameInternMap() {
        return classNameInternMap;
    }

    @Override
    @Trivial
    public String internClassName(String className) {
        return classNameInternMap.intern(className);
    }

    @Trivial
    public String internClassName(String className, boolean doForce) {
        return classNameInternMap.intern(className, doForce);
    }

    @Trivial
    public String lookupClassName(String className) {
        return classNameInternMap.intern(className, Util_InternMap.DO_NOT_FORCE);
    }

    protected Set<String> createIdentityStringSet() {
        return utilFactory.createIdentityStringSet();
    }

    /**
     * Create a set which tests containment using equality.
     * Intern based sets use identity, and are not suitable
     * for external uses, which do not provide interned string
     * values.
     *
     * @param use_i_classNames An identity based set.
     *
     * @return An equality based set containing the values of the
     *     initial set.
     */
    @Trivial
    public Set<String> uninternClassNames(Set<String> use_i_classNames) {
        if ( use_i_classNames == null ) {
            // System.out.println("TargetsClassTable Unintern [ 0 (null) ]");
            return Collections.emptySet();

        } else if ( use_i_classNames.isEmpty() ) {
            // System.out.println("TargetsClassTable Unintern [ 0 (empty) ]");
            return Collections.emptySet();

        } else {
            // System.out.println("TargetsClassTable Unintern [ " + i_classNames.size() + " ]");
            return new UtilImpl_NonInternSet( getClassNameInternMap(), use_i_classNames );
        }
    }

    //

    protected final String classSourceName;

    @Override
    @Trivial
    public String getClassSourceName() {
        return classSourceName;
    }

    //

    protected final Map<String, String> i_packageNames;

    @Trivial
    public Map<String, String> i_getPackageNamesMap() {
        return i_packageNames;
    }

    @Override
    @Trivial
    public Set<String> getPackageNames() {
        return uninternClassNames( i_packageNames.keySet() );
    }

    @Override
    @Trivial
    public Set<String> i_getPackageNames() {
        return i_packageNames.keySet();
    }

    @Override
    public boolean containsPackageName(String packageName) {
        String i_packageName = internClassName(packageName, Util_InternMap.DO_NOT_FORCE);
        if ( i_packageName == null ) {
            return false;
        } else {
            return i_containsPackageName(i_packageName);
        }
    }

    @Override
    public boolean i_containsPackageName(String i_packageName) {
        return i_packageNames.containsKey(i_packageName);
    }

    //

    protected final Map<String, String> i_classNames;

    @Trivial
    public Map<String, String> i_getClassNamesMap() {
        return i_classNames;
    }

    @Override
    @Trivial
    public Set<String> getClassNames() {
        return uninternClassNames( i_classNames.keySet() );
    }

    @Override
    @Trivial
    public Set<String> i_getClassNames() {
        return i_classNames.keySet();
    }

    @Override
    public boolean containsClassName(String className) {
        String i_className = internClassName(className, Util_InternMap.DO_NOT_FORCE);
        if ( i_className == null ) {
            return false;
        } else {
            return i_containsClassName(i_className);
        }
    }

    @Override
    public boolean i_containsClassName(String i_className) {
        return i_classNames.containsKey(i_className);
    }

    //

    protected final Map<String, String> i_superclassNames;

    @Trivial
    public Map<String, String> i_getSuperclassNamesMap() {
        return i_superclassNames;
    }

    @Override
    public String getSuperclassName(String subclassName) {
        String i_subclassName = internClassName(subclassName, Util_InternMap.DO_NOT_FORCE);
        if (i_subclassName == null) {
            return null;
        } else {
            return i_getSuperclassName(i_subclassName);
        }
    }

    @Override
    public String i_getSuperclassName(String i_subclassName) {
        return i_superclassNames.get(i_subclassName);
    }

    public Map<String, String> i_getSuperclassNames() {
        return i_superclassNames;
    }

    //

    protected Map<String, String[]> i_interfaceNames;

    @Trivial
    public Map<String, String[]> i_getInterfaceNamesMap() {
        return i_interfaceNames;
    }

    @Override
    public String[] getInterfaceNames(String classOrInterfaceName) {
        String i_classOrInterfaceName = internClassName(classOrInterfaceName, Util_InternMap.DO_NOT_FORCE);
        if (i_classOrInterfaceName == null) {
            return null;
        } else {
            return i_getInterfaceNames(i_classOrInterfaceName);
        }
    }

    @Override
    public String[] i_getInterfaceNames(String i_classOrInterfaceName) {
        return i_interfaceNames.get(i_classOrInterfaceName);
    }

    @Trivial
    public Map<String, String[]> i_getInterfaceNames() {
        return i_interfaceNames;
    }

    //
    
    protected Map<String, Integer> i_modifiers;
    
    @Trivial
    public Map<String, Integer> i_getModifiers() {
        return i_modifiers;
    }    
    
    @Override
    public Integer getModifiers(String classOrInterfaceName) {
        String i_classOrInterfaceName = internClassName(classOrInterfaceName, Util_InternMap.DO_NOT_FORCE);
        if (i_classOrInterfaceName == null) {
            return null;
        } else {
            return i_getModifiers(i_classOrInterfaceName);
        }
    }

    @Override
    public int getModifiersValue(String classOrInterfaceName) {
        Integer modifiers = getModifiers(classOrInterfaceName);
        return ( (modifiers == null) ? 0 : modifiers.intValue() );
    }

    @Override
    public Integer i_getModifiers(String i_classOrInterfaceName) {
        return i_modifiers.get(i_classOrInterfaceName);
    }

    @Override
    public int i_getModifiersValue(String i_classOrInterfaceName) {
        Integer modifiers = i_getModifiers(i_classOrInterfaceName);
        return ( (modifiers == null) ? 0 : modifiers.intValue() );
    }

    //

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
        logModifiers(useLogger);
        
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class Relationships: END [ {0} ]", getHashText());
    }

    @Trivial
    public void logClassNames(Logger useLogger) {
        String methodName = "logClassNames";
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Classes: BEGIN");

        for (String i_className : i_getClassNames()) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ {0} ]", i_className);
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Classes: END");
    }

    @Trivial
    public void logSuperclassNames(Logger useLogger) {
        String methodName = "logSuperclsasNames";
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Superclasses: BEGIN");

        Object[] logParms = new Object[] { null, null };

        Map<String, String> i_mapToIterate = (i_superclassNames == null) ? Collections.<String, String> emptyMap() : i_superclassNames;

        for (Map.Entry<String, String> i_superclassNameEntry : i_mapToIterate.entrySet()) {
            String i_subclassName = i_superclassNameEntry.getKey();
            String i_superclassName = i_superclassNameEntry.getValue();

            logParms[0] = i_subclassName;
            logParms[1] = i_superclassName;

            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Subclass [ {0} ] Superclass [ {1} ]", logParms);
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Superclasses: END");
    }

    @Trivial
    public void logInterfaceNames(Logger useLogger) {
        String methodName = "logInterfaceNames";
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Interfaces: BEGIN");

        Object[] logParms = new Object[] { null, null };

        Map<String, String[]> i_mapToIterate =
            ((i_interfaceNames == null) ? Collections.<String, String[]> emptyMap() : i_interfaceNames);

        for (Map.Entry<String, String[]> i_interfaceNamesEntry : i_mapToIterate.entrySet()) {
            String i_childName = i_interfaceNamesEntry.getKey();
            String[] use_i_interfaceNames = i_interfaceNamesEntry.getValue();

            logParms[0] = i_childName;
            logParms[1] = use_i_interfaceNames;

            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Child [ {0} ] Interfaces [ {1} ]", logParms);
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Interfaces: END");
    }

    @Trivial
    public void logModifiers(Logger useLogger) {
        String methodName = "logModifiers";
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Modifiers: BEGIN");

        Object[] logParms = new Object[] { null, null };

        Map<String, Integer> i_mapToIterate =
            ((i_modifiers == null) ? Collections.<String, Integer> emptyMap() : i_modifiers);

        for (Map.Entry<String, Integer> i_modifiersEntry : i_mapToIterate.entrySet()) {
            String i_childName = i_modifiersEntry.getKey();
            Integer modifiers = i_modifiersEntry.getValue();

            logParms[0] = i_childName;
            logParms[1] = modifiers;

            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Child [ {0} ] Modifiers [ {1} ]", logParms);
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Modifiers: END");
    }    

    //

    @Override
    public void record(String i_packageName) {
        i_packageNames.put(i_packageName, i_packageName);
    }

//    private Throwable classRecord;
//
//    public synchronized void markClassRecord(String i_className) {
//        classRecord = new Throwable("ClassTable: LastRecord [ " + Thread.currentThread().getName() + " ]" +
//                " [ " + i_className + " ]");
//    }
//
//    public synchronized void clearClassRecord() {
//        classRecord = null;
//    }
//
//    public synchronized void verifyClassRecord() {
//        if ( classRecord != null ) {
//            System.out.println("ClassTable: Class table [ " + getHashText() + " ]");
//            System.out.println("ClassTable: Class names [ " + i_classNames.hashCode() + " ]");
//            classRecord.printStackTrace(System.out);
//            classRecord = null;
//
//            (new Throwable("ClassTable: Eager [ " + Thread.currentThread().getName() + " ]")).printStackTrace(System.out);
//        }
//    }

    @Override
    public void record(String i_className,
                       String i_superclassName, List<String> i_useInterfaceNames,
                       int modifiers) {

//        markClassRecord(i_className);

        i_classNames.put(i_className, i_className);

        if ( i_superclassName != null) {
            i_superclassNames.put(i_className, i_superclassName);
        }

        if ( (i_useInterfaceNames != null) && !i_useInterfaceNames.isEmpty() ) {
            i_interfaceNames.put(
                i_className,
                i_useInterfaceNames.toArray( new String[ i_useInterfaceNames.size() ]));
        }
        
        if ( modifiers != 0 ) {
            i_modifiers.put(i_className,  Integer.valueOf(modifiers));
        }
    }

    protected boolean record(TargetsVisitorClassImpl.ClassData classData) {
        String methodName = "record";

        boolean didAdd;

        String useClassSourceName = classData.classSourceName;

        boolean isClass = classData.isClass;

        if ( !isClass ) {
            String i_packageName = classData.i_className;

            didAdd = ( i_packageNames.put(i_packageName, i_packageName) == null );

            if (logger.isLoggable(Level.FINER)) {
                String useHashText = getHashText();

                String caseText = ( didAdd ? "added" : "ignored (duplicated)" );

                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] Class [ {1} ] {2} Source [ {3} ]",
                            new Object[] { useHashText, i_packageName, caseText, useClassSourceName });
            }

        } else {
            String i_className = classData.i_className;
            String i_superclassName = classData.i_superclassName;
            String[] i_useInterfaceNames = classData.i_interfaceNames;
            int modifiers = classData.modifiers;
            
            didAdd = ( i_classNames.put(i_className, i_className) == null );

//            boolean isInterface = ( (modifiers & Opcodes.ACC_INTERFACE) != 0 );
//            System.out.println(
//                "Class [ " + i_className + " ]" +
//                " Modifiers [ " + modifiers + (isInterface ? " (I)" : " (C)") + " ]" +
//                " Super [ " + i_superclassName + " ]");

            if ( i_superclassName != null) {
                i_superclassNames.put(i_className, i_superclassName);
            }

            if ( i_useInterfaceNames != null ) {
                i_interfaceNames.put(i_className, i_useInterfaceNames);
            }

            if ( modifiers != 0 ) {
                i_modifiers.put(i_className, Integer.valueOf(modifiers));
            }
            // logger.logp(Level.INFO, CLASS_NAME, methodName,
            //     "Class [ {0} ] Modifier [ {1} ]",
            //     new Object[] { i_className, Integer.valueOf(modifiers) });
            
            if (logger.isLoggable(Level.FINER)) {
                String useHashText = getHashText();

                String caseText = ( didAdd ? "added" : "ignored (duplicated)" );

                logger.logp(Level.FINER, CLASS_NAME, methodName, 
                            "[ {0} ] Class [ {1} ] {2} Source [ {3} ]",
                            new Object[] { useHashText, i_className, caseText, useClassSourceName });
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] [ {1} ] Superclass [ {2} ] {3}",
                            new Object[] { useHashText, i_className, i_superclassName, caseText });

                if ( i_useInterfaceNames != null ) {
                    for ( String i_interfaceName : i_useInterfaceNames ) {
                        logger.logp(Level.FINER, CLASS_NAME, methodName,
                                    "[ {0} ] [ {1} ] Interface [ {2} ] {3} Source [ {4} ]",
                                    new Object[] { useHashText, i_className, i_interfaceName, caseText, useClassSourceName });
                    }
                }
            }
        }

        return didAdd;
    }

    // implementer to implemented

    protected boolean didSetImplementers;

    protected final IdentityHashMap<String, Set<String>> i_allImplementers;

    @Override
    public Set<String> i_getAllImplementorsOf(String i_interfaceName) {
        forceAllImplementers();

        // Map direction is: implementer to implemented
        Set<String> result = i_allImplementers.get(i_interfaceName);
        if (result == null) {
            result = Collections.emptySet();
        }

        return result;
    }

    protected void forceAllImplementers() {
        if ( didSetImplementers ) {
            return;
        }

        didSetImplementers = true;

        // Do NOT use the direct interface map to seed table generation;
        // the direct interface map only locates classes which are immediate
        // implementers.
        //
        // All classes which are immediate implementers ... or which are subclasses ...
        // must be scanned!  A subclass may be an indirect implementer, through
        // one of it's superclasses.

        // A class implements the interfaces that it declares,
        // and implements and superinterfaces of the interfaces that it declares,
        // and implements the interfaces implemented by its superclasses.

        for (String i_implementerName : i_classNames.keySet()) {
            // System.out.println("Recording implements of [ " + i_implementerName + " ]");

            String i_nextImplementsSource = i_implementerName;

            while (i_nextImplementsSource != null) {
                String[] i_immediateImplements = i_interfaceNames.get(i_nextImplementsSource);
                if (i_immediateImplements != null) {
                    for (String i_immediateImplement : i_immediateImplements) {
                        Set<String> implementers = i_allImplementers.get(i_immediateImplement);
                        if (implementers == null) {
                            implementers = createIdentityStringSet();
                            i_allImplementers.put(i_immediateImplement, implementers);
                        }
                        implementers.add(i_implementerName);
                    }
                }

                i_nextImplementsSource = i_superclassNames.get(i_nextImplementsSource);
            }
        }
    }

    // superclass to subclass

    protected boolean didSetDescendents;

    protected final IdentityHashMap<String, Set<String>> i_descendants;

    @Override
    public Set<String> getSubclassNames(String superclassName) {
        String i_superclassName = internClassName(superclassName, Util_InternMap.DO_NOT_FORCE);
        if ( i_superclassName == null ) {
            return Collections.emptySet();
        } else {
            return uninternClassNames( i_getSubclassNames(i_superclassName) );
        }
    }

    @Override
    public Set<String> i_getSubclassNames(String i_superclassName) {
        forceDescendantsMap();

        // Map direction is: superclass to subclass
        Set<String> result = i_descendants.get(i_superclassName);
        if (result == null) {
            result = Collections.emptySet();
        }
        return result;
    }

    protected void forceDescendantsMap() {
        if ( didSetDescendents ) {
            return;
        }

        didSetDescendents = true;

        // For each class recorded as a subclass ...
        for (String i_subclassName : i_superclassNames.keySet()) {

            // Start with the immediate super class of that subclass, and walk
            // upwards to the ascendant super classes.
            //
            // For each super class, either as an immediate super class or as an
            // ancestral super class, record the subclass as a descendant of the
            // superclass.

            String i_superclassName = i_subclassName;
            while ((i_superclassName = i_superclassNames.get(i_superclassName)) != null) {
                Set<String> subclasses = i_descendants.get(i_superclassName);
                if (subclasses == null) {
                    subclasses = createIdentityStringSet();
                    i_descendants.put(i_superclassName, subclasses);
                }
                subclasses.add(i_subclassName);
            }

            // Note: The irreflexive closure is generated: A class is not recorded
            //       as the subclass of itself.
        }
    }

    //

    @Override
    public boolean i_isInstanceOf(String i_candidateClassName, String i_targetName, boolean targetIsInterface) {
        if ( i_candidateClassName == i_targetName ) {
            return true; // Same
        }

        if ( targetIsInterface ) {
            while ( i_candidateClassName != null) {
                String[] use_i_interfaceNames = i_getInterfaceNames(i_candidateClassName);
                if ( use_i_interfaceNames != null) {
                    for ( String use_i_interfaceName : use_i_interfaceNames ) {
                        if ( use_i_interfaceName == i_targetName ) {
                            return true;
                        }
                    }
                }

                i_candidateClassName = i_getSuperclassName(i_candidateClassName);
            }

        } else {
            while ( (i_candidateClassName = i_getSuperclassName(i_candidateClassName)) != null ) {
                if ( i_candidateClassName == i_targetName ) {
                    return true;
                }
            }
        }

        return false;
    }

    //

    @Override
    public List<TargetCache_ParseError> readUsing(TargetCache_Reader reader) throws IOException {
        return reader.read(this); // throws IOException
    }

    //

    @Trivial
    public void updateClassNames(
        Set<String> i_allResolvedClassNames, Set<String> i_newlyResolvedClassNames,
        Set<String> i_allUnresolvedClassNames, Set<String> i_newlyUnresolvedClassNames) {

        String methodName = "updateClassNames";

        Object[] logParms;
        if ( logger.isLoggable(Level.FINER) ) {
            logParms = new Object[] { this.hashText, null };
        } else {
            logParms = null;
        }

        if ( logParms != null ) {
            logParms[1] = Integer.toString( i_allResolvedClassNames.size() );
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] All resolved [ {1} ]", logParms);
            logParms[1] = Integer.toString( i_allUnresolvedClassNames.size() );
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] All unresolved [ {1} ]", logParms);
        }

        // The presence of a package or a class in this table resolves that package or class.
        //
        // Mark each such package or class as resolved, remove each such package or class as being unresolved,
        //
        // When appropriate, mark the package or class as being newly resolved.
        //
        // Newly resolved packages and classes must have the additional associated data transferred
        // during a later step.

        for ( String i_packageName : i_getPackageNames() ) {
            if ( !i_allResolvedClassNames.add(i_packageName) ) {
                if ( logParms != null ) {
                    logParms[1] = i_packageName;
                    logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Already resolved package [ {1} ]", logParms);
                }
                continue;
            }

            i_allUnresolvedClassNames.remove(i_packageName);
            i_newlyResolvedClassNames.add(i_packageName);
            // The package name cannot be newly unresolved.

            if ( logParms != null ) {
                logParms[1] = i_packageName;
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Resolved package [ {1} ]", logParms);
            }
        }

        for ( String i_className : i_getClassNames() ) {
            if ( !i_allResolvedClassNames.add(i_className) ) {
                if ( logParms != null ) {
                    logParms[1] = i_className;
                    logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Already resolved class [ {1} ]", logParms);
                }
                continue;
            }

            i_allUnresolvedClassNames.remove(i_className);
            i_newlyResolvedClassNames.add(i_className);
            // The class name cannot be newly unresolved.

            if ( logParms != null ) {
                logParms[1] = i_className;
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Resolved class [ {1} ]", logParms);
            }
        }

        // There are, in effect, three loops across the classes of this table:
        // * A loop to record resolved classes;
        // * A loop to record superclasses as unresolved;
        // * A loop to record interfaces as unresolved.
        //
        // The loop to resolve classes is done first as a distinct loop because
        // all resolutions must be recorded before testing for unresolved superclasses
        // and interfaces.

        // Check each super class of each newly resolved class.  If necessary,
        // mark the super class as a new unresolved class.

        for ( Map.Entry<String, String> i_subclassEntry : i_superclassNames.entrySet() ) {
            // Do *NOT* process the super class if the sub class was previously resolved.
            // That means the sub class exists in more than one table.  The super class available
            // during first resolution of the sub class has precedence.

            String i_subclassName = i_subclassEntry.getKey();
            if ( !i_newlyResolvedClassNames.contains(i_subclassName) ) {
                if ( logParms != null ) {
                    logParms[1] = i_subclassName;
                    logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Resolved subclass [ {1} ]", logParms);
                }
                continue;
            }

            String i_superclassName = i_subclassEntry.getValue();
            if ( i_allResolvedClassNames.contains(i_superclassName) ) {
                if ( logParms != null ) {
                    logParms[1] = i_superclassName;
                    logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Resolved superclass [ {1} ]", logParms);
                }
                continue;
            }

            i_allUnresolvedClassNames.add(i_superclassName);
            i_newlyUnresolvedClassNames.add(i_superclassName);
            
            if ( logParms != null ) {
                logParms[1] = i_superclassName;
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Unresolved superclass [ {1} ]", logParms);
            }
        }

        for ( Map.Entry<String, String[]> i_interfaceEntry : i_getInterfaceNamesMap().entrySet() ) {
            // Do *NOT* process the interfaces if the implementer was previously resolved.
            // That means the implementer exists in more than one table.  The interfaces available
            // during first resolution of the implementer have precedence.

            String i_implementerName = i_interfaceEntry.getKey();
            if ( !i_newlyResolvedClassNames.contains(i_implementerName) ) {
                if ( logParms != null ) {
                    logParms[1] = i_implementerName;
                    logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Previously resolved implementer [ {1} ]", logParms);
                }
                continue;
            }

            String[] use_i_interfaceNames = i_interfaceEntry.getValue();
            for ( String i_interfaceName : use_i_interfaceNames ) {
                if ( i_allResolvedClassNames.contains(i_interfaceName) ) {
                    if ( logParms != null ) {
                        logParms[1] = i_interfaceName;
                        logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Previously resolved interface [ {1} ]", logParms);
                    }
                    continue;
                }

                i_allUnresolvedClassNames.add(i_interfaceName);
                i_newlyUnresolvedClassNames.add(i_interfaceName);

                if ( logParms != null ) {
                    logParms[1] = i_interfaceName;
                    logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Unresolved interface [ {1} ]", logParms);
                }
            }
        }

        if ( logParms != null ) {
            logParms[1] = Integer.toString( i_allResolvedClassNames.size() );
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] All resolved [ {1} ]", logParms);

            logParms[1] = Integer.toString( i_newlyResolvedClassNames.size() );
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] New resolved [ {1} ]", logParms);

            logParms[1] = Integer.toString( i_allUnresolvedClassNames.size() );
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] All unresolved [ {1} ]", logParms);

            logParms[1] = Integer.toString( i_newlyUnresolvedClassNames.size() );
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] New unresolved [ {1} ]", logParms);
        }
    }

    @Trivial
    public boolean sameAs(TargetsTableClassesImpl otherTable, boolean isCongruent) {
        String methodName = "sameAs";

        boolean sameAs;
        String sameAsReason;

        if ( otherTable == null ) {
            sameAs = false;
            sameAsReason = "Null other table";
        } else if ( otherTable == this ) {
            sameAs = true;
            sameAsReason = "Same table";
        } else {
            sameAsReason = basicSameAs(otherTable, isCongruent);
            if ( sameAs = (sameAsReason == null) ) {
                sameAsReason = "Same";
            }
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] [ {1} ] ({2})",
                        new Object[] { hashText, Boolean.valueOf(sameAs), sameAsReason });
        }
        return sameAs;
    }

    protected String basicSameAs(TargetsTableClassesImpl otherTable, boolean isCongruent) {
        boolean doLog = logger.isLoggable(Level.FINER);

        Map<String, String> i_useClassNames = i_classNames;
        Map<String, String> i_otherClassNames = otherTable.i_classNames;
        if ( i_useClassNames.size() != i_otherClassNames.size() ) {
            if ( !doLog ) {
                return "different class count";
            } else {
                return "Classes [ " + i_useClassNames.size() + " ] other [ " + i_otherClassNames.size() + " ]";
            }
        }

        Map<String, String> i_useSuperclassNames = i_superclassNames;
        Map<String, String> i_otherSuperclassNames = otherTable.i_superclassNames;
        if ( i_useSuperclassNames.size() != i_otherSuperclassNames.size() ) {
            if ( !doLog ) {
                return "different super class count";
            } else {
                return "Super classes [ " + i_useSuperclassNames.size() + " ] other [ " + i_otherSuperclassNames.size() + " ]";
            }
        }

        Map<String, String[]> i_useInterfaceNames = i_interfaceNames;
        Map<String, String[]> i_otherInterfaceNames = otherTable.i_interfaceNames;
        if ( i_useInterfaceNames.size() != i_otherInterfaceNames.size() ) {
            if ( !doLog ) {
                return "different interface count";
            } else {
                return "Interfaces [ " + i_useInterfaceNames.size() + " ] other [ " + i_otherInterfaceNames.size() + " ]";
            }
        }
        
        Map<String, Integer> i_useModifiers = i_modifiers ;
        Map<String, Integer> i_otherModifiers = otherTable.i_modifiers;
        if ( i_useModifiers.size() != i_otherModifiers.size() ) {
            if ( !doLog ) {
                return "different modifiers count";
            } else {
                return "Modifiers [ " + i_useModifiers.size() + " ] other [ " + i_otherModifiers.size() + " ]";
            }
        }        

        for ( String i_className : i_useClassNames.keySet() ) {
            String i_otherClassName = ( isCongruent ? i_className : otherTable.lookupClassName(i_className) );
            if ( i_otherClassName == null ) {
                if ( !doLog ) {
                    return "different class names";
                } else {
                    return "Other does not store [ " + i_className + " ] from classes table";
                }
            }

            if ( !i_otherClassNames.containsKey(i_otherClassName) ) {
                if ( !doLog ) {
                    return "different class names";
                } else {
                    return "Other does not store class [ " + i_className + " ]";
                }
            }
        }

        for ( Map.Entry<String, String> i_superclassEntry : i_useSuperclassNames.entrySet() ) {
            String i_className = i_superclassEntry.getKey();

            String i_otherClassName = ( isCongruent ? i_className : otherTable.lookupClassName(i_className) );
            if ( i_otherClassName == null ) {
                if ( !doLog ) {
                    return "different superclass names";
                } else {
                    return "Other does not store [ " + i_className + " ] from superclasses table";
                }
            }

            String i_otherSuperclassName = i_otherSuperclassNames.get(i_otherClassName);
            if ( i_otherSuperclassName == null ) {
                if ( !doLog ) {
                    return "different superclass names";
                } else {
                    return "Other does not store superclass of [ " + i_className + " ]";
                }
            }

            String i_superclassName = i_superclassEntry.getValue();
            if ( (isCongruent && (i_superclassName != i_otherSuperclassName)) ||
                 (!isCongruent && !i_superclassName.equals(i_otherSuperclassName)) ) {

                if ( !doLog ) {
                    return "different superclass names";
                } else {
                    return "Changed superclass of [ " + i_className + " ] from [ " + i_superclassName + " ] to [ " + i_otherSuperclassName + " ]";
                }
            }
        }

        for ( Map.Entry<String, String[]> i_interfaceNamesEntry : i_useInterfaceNames.entrySet() ) {
            String i_className = i_interfaceNamesEntry.getKey();

            String i_otherClassName = ( isCongruent ? i_className : otherTable.lookupClassName(i_className) );
            if ( i_otherClassName == null ) {
                if ( !doLog ) {
                    return "different interface names";
                } else {
                    return "Other does not store of [ " + i_className + " ] from interfaces table";
                }
            }

            String[] i_otherNames = i_otherInterfaceNames.get(i_otherClassName);
            if ( i_otherNames == null ) {
                if ( !doLog ) {
                    return "different interface names";
                } else {
                    return "Other does not store interfaces of [ " + i_className + " ]";
                }
            }

            String[] i_names = i_interfaceNamesEntry.getValue();
            if ( i_names.length != i_otherNames.length ) {
                if ( !doLog ) {
                    return "different interface names";
                } else {
                    return "Changed interface count of [ " + i_className + " ] from [ " + i_names.length + " ] to [ " + i_otherNames.length + " ]";
                }
            }

            for ( int offset = 0; offset < i_names.length; offset++ ) {
                String i_interface = i_names[offset];
                String i_otherInterface = i_otherNames[offset];

                if ( (isCongruent && (i_interface != i_otherInterface)) ||
                     (!isCongruent && !i_interface.equals(i_otherInterface)) ) {
                    if ( !doLog ) {
                        return "different interface names";
                    } else {
                        return "Changed interface of [ " + i_className + " ] [ " + offset + " ] from [ " + i_interface + " ] to [ " + i_otherInterface + " ]";
                    }
                }
            }
        }

        for ( Map.Entry<String, Integer> i_modifiersEntry : i_useModifiers.entrySet() ) {
            String i_className = i_modifiersEntry.getKey();

            String i_otherClassName = ( isCongruent ? i_className : otherTable.lookupClassName(i_className) );
            if ( i_otherClassName == null ) {
                if ( !doLog ) {
                    return "different modifiers";
                } else {
                    return "Other does not store class [ " + i_className + " ] from modifiers table";
                }
            }

            Integer otherModifier = i_otherModifiers.get(i_otherClassName);
            if ( otherModifier == null ) {
                if ( !doLog ) {
                    return "different modifiers";
                } else {
                    return "Other does not store modifiers of [ " + i_className + " ]";
                }
            }

            Integer modifier = i_modifiersEntry.getValue();
            if ( modifier.intValue() != otherModifier.intValue() ) {
                if ( !doLog ) {
                    return "different modifier";
                } else {
                    return "Changed modifier of [ " + i_className + " ] from [ " + modifier + " ] to [ " + otherModifier + " ]";
                }
            }
        }

        return null;
    }

    //

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

    /**
     * <p>Merge data into this table.</p>
     *
     * <p>Add packages and classes, super class names, interfaces names, and modifiers.</p>
     *
     * <p>Only add data for packages and classes which are not already present.</p>
     *
     * <p>Do not add data for packages or classes which were previously added.</p>
     *
     * <p>The "newly" and "all" added package and classes parameters track packages
     * and classes which were added.</p>
     *
     * @param otherClassTable A classes table to merge into this classes table.
     *
     * @param i_newlyAddedPackageNames The names of packages which were added from the
     *     classes table.
     * @param i_allAddedPackageNames The names of packages and classes which were added
     *     from the classes table and from other classes tables.
     * @param i_newlyAddedClassNames The names of the classes which were added from the
     *     classes table.
     * @param i_allAddedClassNames The names of the classes which were added from the
     *     classes table and from other classes tables.
     */
    @Trivial
    protected void restrictedAdd(TargetsTableClassesImpl otherClassTable,
                                 Set<String> i_newlyAddedPackageNames, Set<String> i_allAddedPackageNames,
                                 Set<String> i_newlyAddedClassNames, Set<String> i_allAddedClassNames) {

        String methodName = "restrictedAdd";

        // Put in any new packages; 'newly' starts empty, and ends with the added packages.
        i_addPackageNames(
            otherClassTable.i_getPackageNames(),
            i_newlyAddedPackageNames, i_allAddedPackageNames );

        // Put in any new classes; 'newly' starts empty, and ends with the added classes.
        i_addClassNames(
            otherClassTable.i_getClassNames(),
            i_newlyAddedClassNames, i_allAddedClassNames );

        // Put in superclass data, but only for the added classes.
        i_addSuperclassNames( otherClassTable.i_getSuperclassNames(), i_newlyAddedClassNames );

        // Put in interface data, but only for the added classes.
        i_addInterfaceNames( otherClassTable.i_getInterfaceNames(), i_newlyAddedClassNames );

        // Put in modifiers, but only for the added classes.
        i_addModifiers( otherClassTable.i_getModifiers(), i_newlyAddedClassNames );

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ]", hashText);
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] New Packages [ {1} ]",
                        new Object[] { hashText, printString(i_newlyAddedPackageNames) } );
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] New Classes [ {1} ]",
                        new Object[] { hashText, printString(i_newlyAddedClassNames) } );
        }
    }

    @Trivial
    protected void i_addPackageNames(Set<String> i_otherPackageNames,
                                     Set<String> i_newlyAddedPackageNames,
                                     Set<String> i_allAddedPackageNames) {

        for ( String i_packageName : i_otherPackageNames ) {
            if ( !i_allAddedPackageNames.contains(i_packageName) ) {
                i_packageNames.put(i_packageName, i_packageName);

                // Record to *both* tracking collections.
                i_newlyAddedPackageNames.add(i_packageName);
                i_allAddedPackageNames.add(i_packageName);
            }
        }
    }

    @Trivial
    protected void i_addClassNames(Set<String> i_otherClassNames,
                                   Set<String> i_newlyAddedClassNames,
                                   Set<String> i_allAddedClassNames) {

        for ( String i_className : i_otherClassNames ) {
            if ( !i_allAddedClassNames.contains(i_className) ) {
                i_classNames.put(i_className, i_className);

                // Record to *both* tracking collections.
                i_newlyAddedClassNames.add(i_className);
                i_allAddedClassNames.add(i_className);
            }
        }
    }

    @Trivial
    protected void i_addSuperclassNames(Map<String, String> i_otherSuperclassNames,
                                        Set<String> i_newlyAddedClassNames) {

        // Iterate across the entries of the other class data should be more efficient than
        // iterating across the new added class names and doing gets: Most if not all of the
        // superclass data is expected to be added.

        for ( Map.Entry<String, String> i_otherSuperclassNameEntry : i_otherSuperclassNames.entrySet() ) {
            String i_otherClassName = i_otherSuperclassNameEntry.getKey();
            String i_otherSuperclassName = i_otherSuperclassNameEntry.getValue();

            if ( i_newlyAddedClassNames.contains(i_otherClassName) ) {
                i_superclassNames.put(i_otherClassName, i_otherSuperclassName);
            }
        }
    }

    @Trivial
    protected void i_addInterfaceNames(Map<String, String[]> i_otherInterfaceNames,
                                       Set<String> i_newlyAddedClassNames) {

        // Iterate across the entries of the other class data should be more efficient than
        // iterating across the new added class names and doing gets: Most if not all of the
        // interface data is expected to be added.

        for ( Map.Entry<String, String[]> i_otherInterfaceNamesEntry : i_otherInterfaceNames.entrySet() ) {
            String i_otherClassName = i_otherInterfaceNamesEntry.getKey();
            String[] i_useOtherInterfaceNames = i_otherInterfaceNamesEntry.getValue();

            if ( i_newlyAddedClassNames.contains(i_otherClassName) ) {
                i_interfaceNames.put(i_otherClassName, i_useOtherInterfaceNames);
            }
        }
    }

    @Trivial
    protected void i_addModifiers(Map<String, Integer> i_otherModifiers,
                                  Set<String> i_newlyAddedClassNames) {
        // String methodName = "i_addModifiers";
        
        // logger.logp(Level.INFO, CLASS_NAME, methodName,
        //     "Classes [ {0} ]",
        //     Integer.valueOf(i_newlyAddedClassNames.size()));
        
        // Iterate across the entries of the other class data should be more efficient than
        // iterating across the new added class names and doing gets: Most if not all of the
        // modifiers are expected to be added.

        for ( Map.Entry<String, Integer> i_otherModifiersEntry : i_otherModifiers.entrySet() ) {
            String i_otherClassName = i_otherModifiersEntry.getKey();
            Integer otherModifier = i_otherModifiersEntry.getValue();

            if ( i_newlyAddedClassNames.contains(i_otherClassName) ) {
                i_modifiers.put(i_otherClassName, otherModifier);
                
                // logger.logp(Level.INFO, CLASS_NAME, methodName,
                //     "Class [ {0} ] Modifier [ {1} ]",
                //     new Object[] { i_otherClassName, otherModifier });
            }
        }
    }

    //

    public boolean jandex_i_addPackage(String i_className) {
        return ( i_packageNames.put(i_className, i_className) == null );
    }

    //

    public boolean jandex_i_addClass(String i_className) {
        return ( i_classNames.put(i_className, i_className) == null );
    }

    public void jandex_i_setSuperclassName(String i_className, String i_superclassName) {
        i_superclassNames.put(i_className, i_superclassName);
    }

    public void jandex_i_setInterfaceNames(String i_className, String[] i_useInterfaceNames) {
        i_interfaceNames.put(i_className, i_useInterfaceNames);
    }

    public void jandex_i_setModifiers(String i_className, int modifiers) {
        if ( modifiers != 0 ) {
            i_modifiers.put(i_className, Integer.valueOf(modifiers));
        }
    }
}
