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

package com.ibm.ws.jpa;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jpa.spec10.query.aggregatefunctions.TestQueryAggregateFunctions_Web;
import com.ibm.ws.jpa.spec10.query.apars.TestAggregateFunctionAPARs_Web;
import com.ibm.ws.jpa.spec10.query.apars.TestDB2OnZSpecificAPARs_Web;
import com.ibm.ws.jpa.spec10.query.jpql.TestQueryJPQL_Web;

@RunWith(Suite.class)
@SuiteClasses({
                TestQueryAggregateFunctions_Web.class,
                TestAggregateFunctionAPARs_Web.class,
//                TestForceBindParametersAPARs_Web.class,
                TestQueryJPQL_Web.class,
                TestDB2OnZSpecificAPARs_Web.class,
                componenttest.custom.junit.runner.AlwaysPassesTest.class
})
public class FATSuite {
    public final static String[] JAXB_PERMS = { "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind.v2.runtime.reflect\";",
                                                "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind\";",
                                                "permission java.lang.RuntimePermission \"accessDeclaredMembers\";" };
//    @ClassRule
//    public static RepeatTests r = RepeatTests.withoutModification()
//                    .andWith(FeatureReplacementAction.EE7_FEATURES())
//                    .andWith(new RepeatWithJPA20());

}
