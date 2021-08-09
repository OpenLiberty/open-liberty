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
package com.ibm.ws.ejbcontainer.osgi.internal.ejb;

import javax.ejb.DependsOn;
import javax.ejb.Singleton;

@Singleton
@DependsOn({ TestDependsOn.DEPENDS_ON_1, TestDependsOn.DEPENDS_ON_2 })
public class TestDependsOn {
    public static final String DEPENDS_ON_1 = "TestSingleton";
    public static final String DEPENDS_ON_2 = "TestSingletonNamed";
}
