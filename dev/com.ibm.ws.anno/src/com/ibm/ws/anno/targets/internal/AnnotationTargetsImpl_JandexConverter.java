/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.targets.internal;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets.AnnotationCategory;
import com.ibm.wsspi.anno.util.Util_InternMap;

/**
 * Utility for populating annotation targets from a JANDEX index file.
 * 
 * The intent is to process the contents of a JANDEX index instead of
 * scanning the target class source.
 * 
 * See {@link #AnnotationTargetsImpl_JandexConverter(AnnotationTargetsImpl_Targets)}
 * and {@link #convertIndex(String, Index, ScanPolicy)}.
 */
public class AnnotationTargetsImpl_JandexConverter {
    public static final TraceComponent tc = Tr.register(AnnotationTargetsImpl_JandexConverter.class);
    
    /** Name of package classes.*/
    public static final String PACKAGE_INFO_CLASS_NAME = "package-info";

    /**
     * <p>Tell if a name is a package name.</p>
     * 
     * @param name The name which is to be tested. 
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
    
    public AnnotationTargetsImpl_JandexConverter(AnnotationTargetsImpl_Targets annotationTargets) {
        this.annotationTargets = annotationTargets;
        this.hashText = AnnotationServiceImpl_Logging.getBaseHash(this);
    }

    //

    protected final AnnotationTargetsImpl_Targets annotationTargets;

    public AnnotationTargetsImpl_Targets getAnnotationTargets() {
        return annotationTargets;
    }

    protected String internClassName(String className) {
        return getAnnotationTargets().internClassName(className,  Util_InternMap.DO_FORCE);
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

    protected boolean i_recordScannedClassName(
        ScanPolicy scanPolicy,
        String i_classSourceName, String i_className) {

        return ( annotationTargets.i_placeClass(i_classSourceName, i_className) &&
                 annotationTargets.i_addScannedClassName(i_className, scanPolicy) );
    }

    protected void i_recordSuperclassName(String i_className, String i_superclassName) {
        annotationTargets.i_setSuperclassName(i_className, i_superclassName);
    }

    protected void i_recordInterfaceNames(String i_className, String[] i_interfaceNames) {
        annotationTargets.i_setInterfaceNames(i_className, i_interfaceNames);
    }

    protected void i_recordReferencedClassName(String i_className) {
        annotationTargets.i_addReferencedClassName(i_className);
    }

    protected void i_removeReferencedClassName(String i_className) {
        annotationTargets.i_removeReferencedClassName(i_className);
    }

    protected void jandex_recordAnnotation(
        ScanPolicy scanPolicy,
        AnnotationCategory annotationCategory,
        String i_className,
        DotName annotationClassDotName) {

        annotationTargets.i_recordAnnotation(
            scanPolicy, annotationCategory,
            i_className,
            internClassName(annotationClassDotName) );
    }

    //

    public void convertIndex(
        String i_classSourceName,
        Index index,
        ScanPolicy scanPolicy) {

        Collection<ClassInfo> knownClasses = index.getKnownClasses();

        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] Classes [ {1} ]", 
                     new Object[] { hashText, Integer.toString(knownClasses.size()) } ));
        }

        for ( ClassInfo jandexClassInfo : knownClasses ) {
            convertClassInfo(i_classSourceName, jandexClassInfo, scanPolicy);
        }
    }

    public void convertClassInfo(
        String i_classSourceName,
        ClassInfo jandexClassInfo,
        ScanPolicy scanPolicy) {

        String methodName = "convertClassInfo";

        DotName classDotName = jandexClassInfo.name();
        String className = classDotName.toString();

        if ( isPackageName(className) ) {
            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] Package load [ {1} ]", 
                         new Object[] { hashText, className } ));
            }

            return;
        }
        
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] Class load [ {1} ]", 
                     new Object[] { hashText, className } ));
        }

        String i_className = internClassName(className);

        if ( !i_recordScannedClassName(scanPolicy, i_classSourceName, i_className) ) {
            // TODO:  this one is a WARNING : pull from props
            System.out.println(methodName + " WARNING: Duplicate class [ " + i_className + " ]");
            return;
        }

        i_removeReferencedClassName(i_className);

        DotName superClassDotName = jandexClassInfo.superName();

        if ( superClassDotName != null ) {
            String superClassName = superClassDotName.toString();

            String i_superclassName = internClassName(superClassName);

            i_recordSuperclassName(i_className, i_superclassName);
            i_recordReferencedClassName(i_superclassName);
        }

        List<DotName> interfaceDotNames = jandexClassInfo.interfaceNames();
        if ( (interfaceDotNames != null) && (interfaceDotNames.size() > 0) ) {
            String[] i_interfaceNames = internClassNames(interfaceDotNames);

            i_recordInterfaceNames(i_className, i_interfaceNames);

            for ( String i_interfaceName : i_interfaceNames ) {
                i_recordReferencedClassName(i_interfaceName);
            }
        }

        if ( scanPolicy == ScanPolicy.EXTERNAL ) {
            return;
        }

        for ( AnnotationInstance jandexClassAnnotation : jandexClassInfo.classAnnotations() ) {
            jandex_recordAnnotation(
                scanPolicy, AnnotationCategory.CLASS,
                i_className, jandexClassAnnotation.name());
        }

        for ( FieldInfo jandexFieldInfo : jandexClassInfo.fields() ) {
            for ( AnnotationInstance jandexFieldAnnotation : jandexFieldInfo.annotations() ) {
                jandex_recordAnnotation(
                    scanPolicy, AnnotationCategory.FIELD,
                    i_className, jandexFieldAnnotation.name());
            }
        }

        for ( MethodInfo jandexMethodInfo : jandexClassInfo.methods() ) {
            for ( AnnotationInstance jandexMethodAnnotation : jandexMethodInfo.annotations() ) {
                jandex_recordAnnotation(
                    scanPolicy, AnnotationCategory.METHOD,
                    i_className, jandexMethodAnnotation.name());
            }
        }
    }
}
