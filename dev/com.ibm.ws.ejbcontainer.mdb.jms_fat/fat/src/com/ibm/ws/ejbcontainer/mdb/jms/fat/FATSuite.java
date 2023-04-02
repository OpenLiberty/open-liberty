/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.mdb.jms.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.ejbcontainer.mdb.jms.fat.tests.MDB20Test;
import com.ibm.ws.ejbcontainer.mdb.jms.fat.tests.MDB21Test;
import com.ibm.ws.ejbcontainer.mdb.jms.fat.tests.MDB30AnnTest;
import com.ibm.ws.ejbcontainer.mdb.jms.fat.tests.MDB30MixTest;
import com.ibm.ws.ejbcontainer.mdb.jms.fat.tests.MDB30XMLTest;

@RunWith(Suite.class)
@SuiteClasses({
                MDB20Test.class,
                MDB21Test.class,
                MDB30AnnTest.class,
                MDB30MixTest.class,
                MDB30XMLTest.class
})
public class FATSuite {
}
