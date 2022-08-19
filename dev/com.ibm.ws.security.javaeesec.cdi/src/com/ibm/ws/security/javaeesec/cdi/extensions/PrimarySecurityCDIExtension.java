/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.cdi.extensions;

import java.util.Properties;

public interface PrimarySecurityCDIExtension {

    void registerMechanismClass(Class<?> mechanismClass);

    void deregisterMechanismClass(Class<?> mechanismClass);

    void addAuthMech(String applicationName, Class<?> annotatedClass, Class<?> implClass, Properties props);

    boolean existAuthMech(String applicationName, Class<?> implClass);

}
