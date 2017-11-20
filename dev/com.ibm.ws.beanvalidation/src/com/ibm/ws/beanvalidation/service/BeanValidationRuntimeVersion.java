/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.service;

import org.osgi.framework.Version;

public abstract class BeanValidationRuntimeVersion {
    public static final String VERSION = "version";

    public static final Version VERSION_1_0 = new Version(1, 0, 0);
    public static final Version VERSION_1_1 = new Version(1, 1, 0);
    public static final Version VERSION_2_0 = new Version(2, 0, 0);
}
