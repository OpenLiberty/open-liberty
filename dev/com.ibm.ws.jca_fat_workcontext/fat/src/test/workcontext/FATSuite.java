/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.workcontext;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({ AlwaysPassesTest.class,
                ResourceAdapterExampleTest.class
//WcRaExampleTest.class
// WorkContextJCATest
})
public class FATSuite {
    //this tests the EE10 connectors version 2.1 added generic support for MappedRecord, IndexedRecord and should not be repeated for previous versions
}
