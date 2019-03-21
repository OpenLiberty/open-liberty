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

package com.ibm.ws.jpa.injection.dfi;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                JPA10Injection_DFI_NoInheritance_EJB.class,
                JPA10Injection_DFI_NoInheritance_Web.class,
                JPA10Injection_DFI_NoInheritance_WebLib.class,
                JPA10Injection_DFI_YesInheritance_EJB.class,
                JPA10Injection_DFI_YesInheritance_DDOvrd_EJB.class,
                JPA10Injection_DFI_YesInheritance_Web.class,
                JPA10Injection_DFI_YesInheritance_WebLib.class
})
public class JPA10Injection_DFI {

}
