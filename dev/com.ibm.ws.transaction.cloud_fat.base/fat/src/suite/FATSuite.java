/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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
import tests.DupXidTest;
import tests.LongTranTest;
import tests.Simple2PCCloudTest;

@RunWith(Suite.class)
@SuiteClasses({
                DupXidTest.class,
                Simple2PCCloudTest.class,
                LongTranTest.class,
})
public class FATSuite extends TxTestContainerSuite {

    static String[] serverNames = new String[] { "com.ibm.ws.transaction_DUPXID001",
                                                 "com.ibm.ws.transaction_DUPXID002",
                                                 "com.ibm.ws.transaction_CLOUD001",
                                                 "com.ibm.ws.transaction_CLOUD002",
                                                 "longLeaseLengthServer1",
                                                 "peerPrecedenceServer1",
                                                 "com.ibm.ws.transaction_longtran",
    };

    static {
        beforeSuite(DatabaseContainerType.Derby);
    }

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModificationInFullMode()
                    .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers(serverNames))
                    .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11).forServers(serverNames))
                    .andWith(FeatureReplacementAction.EE10_FEATURES().forServers(serverNames));
}
