/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import tests.ContainerAuthTest;

@RunWith(Suite.class)
@SuiteClasses({
                ContainerAuthTest.class,
})
public class FATSuite extends TxTestContainerSuite {

	static {
		beforeSuite(DatabaseContainerType.Oracle);
	}

  @ClassRule
  public static RepeatTests r = RepeatTests.withoutModification()
                  .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly())
                  .andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly())
                  .andWith(FeatureReplacementAction.EE10_FEATURES().fullFATOnly());
}
