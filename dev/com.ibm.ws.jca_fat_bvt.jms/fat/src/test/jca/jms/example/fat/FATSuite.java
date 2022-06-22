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
package test.jca.jms.example.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import test.jca.jms.example.tests.JCAStoreSampleAppTest;

@RunWith(Suite.class)
@SuiteClasses({ JCAStoreSampleAppTest.class })
public class FATSuite {
    public static final String SERVER = "com.ibm.ws.jca.bvt.jms";
    public static final String jcaapp = "jcastore";

}
