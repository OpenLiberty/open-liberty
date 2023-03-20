/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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
package suite;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.transaction.fat.util.TxTestContainerSuite;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.database.container.DatabaseContainerType;
import tests.FailoverTest1;

@RunWith(Suite.class)
@SuiteClasses({ FailoverTest1.class })
public class FATSuite extends TxTestContainerSuite {

    static {
        databaseContainerType = DatabaseContainerType.Oracle;
        
        beforeSuite();
    }

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers(FailoverTest1.serverNames))
                    .andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly().forServers(FailoverTest1.serverNames))
                    .andWith(FeatureReplacementAction.EE10_FEATURES().forServers(FailoverTest1.serverNames));
}
