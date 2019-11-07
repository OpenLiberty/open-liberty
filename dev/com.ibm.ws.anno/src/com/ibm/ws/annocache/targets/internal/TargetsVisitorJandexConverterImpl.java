/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.internal;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Targets.AnnotationCategory;
import com.ibm.wsspi.annocache.util.Util_InternMap;

/**
 * Utility for populating annotation targets from a JANDEX index file.
 *
 * While not a visitor, the convert is placed with the visitor classes
 * because the converter is to populate the targets data, using the jandex
 * information, exactly the same as when the class visitor populates the
 * targets data from collected class data.
 */
public class TargetsVisitorJandexConverterImpl {
    // Logging ...

    protected static final Logger jandexLogger = AnnotationCacheServiceImpl_Logging.ANNO_JANDEX_LOGGER;

    public static final String CLASS_NAME = TargetsVisitorJandexConverterImpl.class.getSimpleName();

    //

    /** Name of package classes.*/
    public static final String PACKAGE_INFO_CLASS_NAME = "package-info";

    /**
     * <p>Tell if a name is a package name.  That is, if the name
     * ends with {@link #PACKAGE_INFO_CLASS_NAME}.</p>
     * 
     * @param name The name which is to be tested. 
     *
     * @return True or false telling if a name is a package name.
     */
    public static boolean isPackageName(String name) {
        return ( name.endsWith(PACKAGE_INFO_CLASS_NAME) );
    }

    /**
     * <p>Strip the package name from a qualified class name.</p>
     * 
     * @param className The qualified class name from which to remove
     *     the package name.
     * 
     * @return The simple class name.
     */
    public static String stripPackageNameFromClassName(String className) {
        return (className.substring(0, className.length() - (PACKAGE_INFO_CLASS_NAME.length() + 1)));
    }

    //
    
    protected final String hashText;

    @Trivial
    public String getHashText() {
        return hashText;
    }

    public TargetsVisitorJandexConverterImpl(TargetsTableImpl targetsTable) {
        this.targetsTable = targetsTable;
        this.classesTable = targetsTable.getClassTable();
        this.annotationsTable = targetsTable.getAnnotationTable();
        // this.detailsTable = targetsTable.getDetailsTable();

        this.hashText = CLASS_NAME + "@" + Integer.toHexString(hashCode());
    }

    //

    protected final TargetsTableImpl targetsTable;
    protected final TargetsTableClassesImpl classesTable;
    protected final TargetsTableAnnotationsImpl annotationsTable;

    @Trivial    
    public TargetsTableImpl getTargetsTable() {
        return targetsTable;
    }

    @Trivial
    public TargetsTableClassesImpl getClassesTable() {
        return classesTable;
    }

    @Trivial
    public TargetsTableAnnotationsImpl getAnnotationsTable() {
        return annotationsTable;
    }    

    protected String internClassName(String className) {
        return getTargetsTable().internClassName(className,  Util_InternMap.DO_FORCE);
    }

    protected String internClassName(DotName name) {
        return internClassName( name.toString() ); 
    }

    protected String[] internClassNames(List<DotName> dotNames) {
        int numNames = dotNames.size();
        String[] i_names = new String[numNames];

        for ( int nameNo = 0; nameNo < numNames; nameNo++ ) {
            i_names[nameNo] = internClassName( dotNames.get(nameNo) );
        }

        return i_names;
    }

    //

    public boolean convertClassInfo(String classSourceName, Object classInfoObj) {
        String methodName = "convertClassInfo";

        ClassInfo classInfo = (ClassInfo) classInfoObj;

        DotName classDotName = classInfo.name();
        String className = classDotName.toString();

        Object[] logParms =
            ( jandexLogger.isLoggable(Level.FINER)
                ? new Object[] { getHashText(), className, null, null }
                : null );

        int modifiers = classInfo.flags();

        if ( isPackageName(className) ) {
            String packageName = stripPackageNameFromClassName(className);

            if ( logParms != null ) {
                logParms[1] = packageName;
                logParms[2] = Integer.valueOf(modifiers);
                jandexLogger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Package [ {1} ] Modifiers [ {2} ]", logParms); 
            }

            String i_packageName = internClassName(packageName);

            if ( !classesTable.jandex_i_addPackage(i_packageName) ) {
                // ANNO_TARGETS_DUPLICATE_PACKAGE=CWWKC0054W:
                // Class scanning internal error: The visitor [ {0} ] attempted a second scan of package [ {1} ].

                jandexLogger.logp(Level.WARNING, CLASS_NAME, methodName, 
                    "ANNO_TARGETS_DUPLICATE_PACKAGE",
                    new Object[] { getHashText(), i_packageName });

                return false;
            }

            classesTable.jandex_i_setModifiers(i_packageName, modifiers);

            for ( Map.Entry<DotName, List<AnnotationInstance>> annoEntry : classInfo.annotations().entrySet() ) {
                String annotationClassName = annoEntry.getKey().toString();
                String i_annotationClassName = internClassName(annotationClassName);

                for ( AnnotationInstance annoInstance : annoEntry.getValue() ) {
                    AnnotationTarget annoTarget = annoInstance.target();
                    AnnotationTarget.Kind annoKind = annoTarget.kind();

                    if ( annoKind == AnnotationTarget.Kind.CLASS ) {
                        annotationsTable.jandex_i_recordAnnotation(
                            i_packageName, AnnotationCategory.PACKAGE, i_annotationClassName);

                        if ( logParms != null ) {
                            logParms[2] = i_annotationClassName;
                            jandexLogger.logp(Level.FINER, CLASS_NAME, methodName,
                                "[ {0} ] Package [ {1} ] Annotation [ {2} ]", logParms);
                        }

                    } else {
                        if ( logParms != null ) {
                            logParms[2] = i_annotationClassName;
                            jandexLogger.logp(Level.FINER, CLASS_NAME, methodName,
                                "[ {0} ] Package [ {1} ] Annotation [ {2} ] Unhandled type [ " + annoKind + " ]", logParms);
                        }
                    }
                }
            }

            return true;
        }

        DotName superClassDotName = classInfo.superName();
        List<DotName> interfaceDotNames = classInfo.interfaceNames();

        if ( logParms != null ) {
            logParms[2] = Integer.valueOf(modifiers);
            jandexLogger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Class [ {1} ] Modifiers [ {2} ]", logParms);
            logParms[2] = superClassDotName;
            jandexLogger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Class [ {1} ] Superclass [ {1} ]", logParms);
            logParms[2] = interfaceDotNames;
            jandexLogger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Class [ {1} ] Interfaces [ {1} ]", logParms);
        }

        String i_className = internClassName(className);

        if ( !classesTable.jandex_i_addClass(i_className) ) {
            // TODO: Find or create a message for this.
            jandexLogger.logp(Level.WARNING, CLASS_NAME, methodName, "[ {0} ] Duplicate class [ {1} ]", logParms);
            return false;
        }

        classesTable.jandex_i_setModifiers(i_className, modifiers);

        if ( superClassDotName != null ) {
            String superClassName = superClassDotName.toString();
            String i_superclassName = internClassName(superClassName);
            classesTable.jandex_i_setSuperclassName(i_className, i_superclassName);            
        }

        if ( (interfaceDotNames != null) && (interfaceDotNames.size() > 0) ) {
            String[] i_interfaceNames = internClassNames(interfaceDotNames);
            classesTable.jandex_i_setInterfaceNames(i_className, i_interfaceNames);
        }

        // Jandex places all annotations of a class in a single table which is held
        // by the class information.

        for ( Map.Entry<DotName, List<AnnotationInstance>> annoEntry : classInfo.annotations().entrySet() ) {
            String annotationClassName = annoEntry.getKey().toString();
            String i_annotationClassName = internClassName(annotationClassName);

            for ( AnnotationInstance annoInstance : annoEntry.getValue() ) {
                AnnotationTarget annoTarget = annoInstance.target();
                AnnotationTarget.Kind annoKind = annoTarget.kind();

                AnnotationCategory annoCategory;
                if ( annoKind == AnnotationTarget.Kind.CLASS ) {
                    annoCategory = AnnotationCategory.CLASS;
                } else if ( annoKind == AnnotationTarget.Kind.METHOD ) {
                    annoCategory = AnnotationCategory.METHOD;
                } else if ( annoKind == AnnotationTarget.Kind.FIELD ) {
                    annoCategory = AnnotationCategory.FIELD;
                } else {
                     annoCategory = null;
                }

                if ( annoCategory != null ) {
                    annotationsTable.jandex_i_recordAnnotation(i_className, annoCategory, i_annotationClassName);

                    if ( logParms != null ) {
                        logParms[1] = i_annotationClassName;
                        logParms[2] = annoCategory; 
                        jandexLogger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Annotation [ {1} ] [ {2} ]", logParms);
                    }
                }
            }
        }

        return true;
    }
}
