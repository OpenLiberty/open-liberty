/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.annocache.classsource;

public interface ClassSource_ClassLoader
    extends ClassSource, com.ibm.wsspi.anno.classsource.ClassSource_ClassLoader {

    String CLASSLOADER_CLASSSOURCE_NAME = "classloader";

    ClassLoader getClassLoader();
}