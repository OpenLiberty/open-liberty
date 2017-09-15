/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.webapp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Collection;

import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.MethodInfo;

/**
 *
 */
public class WebAppSCIHelper {
    public static boolean isClassTarget(Class<?> candidateClass) {
        Target targetAnnotation = candidateClass.getAnnotation(java.lang.annotation.Target.class);      
        if ( targetAnnotation == null ) {
            return true; // TODO: Can this happen?
        }
        
        ElementType[] elementTypes = targetAnnotation.value();
        for ( ElementType elementType : elementTypes ) {
            if ( elementType == ElementType.TYPE ) {
                return true;
            }
        }

        return false;
    }
    
    public static boolean isMethodTarget(Class<?> targetClass) {
        Target targetAnnotation = targetClass.getAnnotation(java.lang.annotation.Target.class);
        if ( targetAnnotation == null ) {
            return true; // TODO: Can this happen?
        }
        
        ElementType[] elementTypes = targetAnnotation.value();
        for ( ElementType elementType : elementTypes ) {
            if ( elementType == ElementType.METHOD ) {
                return true;
            }
        }

        return false;
    }
    
    public static boolean anyMethodHasAnnotation(ClassInfo subclassInfo, String annotationName) {
        Collection<? extends MethodInfo> methods = subclassInfo.getMethods();
        for ( MethodInfo nextMethod : methods ) {
            if ( nextMethod.isAnnotationPresent(annotationName) ) {
                return true;
            }
        }

        return false;
    }
}
