/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.test.data.sub;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// @formatter:off
/**
 * Self referential annotation definition. Modeled after the 81538,L6Q,000
 * problem class 'GwtCompatible':
 *
 * public annotation com.google.common.annotations.GwtCompatible
 * extends java.lang.Object
 * implements java.lang.annotation.Annotation
 * Version [ 50 ] ( 0x32 ) ( J2SE 6.0 )
 *
 * @java.lang.annotation.Retention
 *                                 [ value ] [ CLASS ] (enum)
 * @java.lang.annotation.Target
 *                              [ value ] [ 2 elements ] (array)
 *                              [ 0 ] [ TYPE ] (enum)
 *                              [ 1 ] [ METHOD ] (enum)
 * @java.lang.annotation.Documented
 * @com.google.common.annotations.GwtCompatible
 *                                              Is Visible: [ false ]
 *                                              [M] serializable : [ ()Z ] ( boolean )
 *                                              Default: [ false ] (primitive)
 *                                              [M] emulated : [ ()Z ] ( boolean )
 *                                              Default: [ false ] (primitive)
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE, ElementType.METHOD })
@SelfAnno(false)
public @interface SelfAnno {
    boolean value();
}
