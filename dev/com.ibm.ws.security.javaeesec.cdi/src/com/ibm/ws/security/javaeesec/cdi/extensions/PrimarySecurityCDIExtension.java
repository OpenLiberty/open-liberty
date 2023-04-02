/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.cdi.extensions;

import java.lang.annotation.Annotation;
import java.util.Properties;
import java.util.Set;

public interface PrimarySecurityCDIExtension {

    void registerMechanismClass(Class<?> mechanismClass);

    void deregisterMechanismClass(Class<?> mechanismClass);

    void addAuthMech(String applicationName, Class<?> annotatedClass, Class<?> implClass, Set<Annotation> annotations, Properties props);

    boolean existAuthMech(String applicationName, Class<?> implClass);

}
