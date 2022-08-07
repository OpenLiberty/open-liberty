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
package com.ibm.ws.jca.jdbc.mbean.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jca.mbean.fat.app.ConnectionPoolStatsTest;
import com.ibm.ws.jca.mbean.fat.app.JCA_JDBC_JSR77_MBeanTest;
import com.ibm.ws.jca.mbean.fat.app.JCA_JDBC_JSR77_MBean_ExtendedTest;
import com.ibm.ws.jca.mbean.fat.app.JCA_JDBC_JSR77_MBean_MultipleTest;

@RunWith(Suite.class)
@SuiteClasses({
                JCA_JDBC_JSR77_MBeanTest.class,
                JCA_JDBC_JSR77_MBean_ExtendedTest.class,
                JCA_JDBC_JSR77_MBean_MultipleTest.class,
                ConnectionPoolStatsTest.class

})
public class FATSuite {
}