/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.security.jacc_fat;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Performs testing of EJB pure annotations with Java inheritance as specified in EJB 3.0 Specification section
 * 17.3.2.1 (Specification of Method Permissions with Metadata Annotations).
 *
 * This test uses SecurityEJBA07Base as the EJB superclass with SecurityEJBA07Bean as the derived class.
 * The superclass has class level annotations of @RolesAllowed ("Manager") and @RunAs ("Employee"). The test covers
 * a variety of annotation combinations with methods in the superclass and then
 * 1) method overrides in derived class should override superclass annotation,
 * 2) method not implemented in derived class should inherit superclass annotation,
 * 3) no annotation on superclass method should result in class level annotation if derived class does not override and
 * 4) if derived class with no annotation overrides superclass, then the result is no permissions enforced.
 * 5) superclass level annotations only apply to methods defined in that class and if a method is overridden, the class level
 * annotations from the superclass don't apply.
 *
 * This test invokes SecurityEJBA07Bean methods with a variety of method signatures to insure that
 * annotations are processed correctly with methods of the same name and different signature.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class PureAnnA07InheritanceTest extends PureAnnA07Base {

    protected static Class<?> logClass = PureAnnA07InheritanceTest.class;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(logClass, "setUp", "Starting the server....");
        commonSetup(logClass, Constants.SERVER_EJB,
                    Constants.APPLICATION_SECURITY_EJB, Constants.SERVLET_SECURITY_EJB, Constants.CONTEXT_ROOT_SECURITY_EJB);

    }

    @Override
    protected TestName getName() {
        return name;
    }

}