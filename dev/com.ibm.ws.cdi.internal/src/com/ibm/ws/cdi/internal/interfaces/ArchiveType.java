/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.internal.interfaces;

/**
 * The type of an archive
 */
public enum ArchiveType {
    MANIFEST_CLASSPATH,
    WEB_INF_LIB,
    EAR_LIB,
    WEB_MODULE,
    EJB_MODULE,
    CLIENT_MODULE,
    RAR_MODULE,
    JAR_MODULE,
    SHARED_LIB,
    ON_DEMAND_LIB, //hold random classes that needs to be in a bean archive
    RUNTIME_EXTENSION;
}
