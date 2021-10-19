/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.mdb.jms.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.ejbcontainer.mdb.jms.fat.tests.MDB20Test;
import com.ibm.ws.ejbcontainer.mdb.jms.fat.tests.MDB21Test;

@RunWith(Suite.class)
@SuiteClasses({
                MDB20Test.class,
                MDB21Test.class
})
public class FATSuite {
}
