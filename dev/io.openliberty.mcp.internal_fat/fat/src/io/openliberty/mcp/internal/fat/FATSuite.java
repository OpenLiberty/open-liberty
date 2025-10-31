/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat;

import static componenttest.rules.repeater.EERepeatActions.EE10;
import static componenttest.rules.repeater.EERepeatActions.EE11;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import io.openliberty.mcp.internal.fat.lifecycle.tests.BeanLifecycleTest;
import io.openliberty.mcp.internal.fat.lifecycle.tests.LifecycleTest;
import io.openliberty.mcp.internal.fat.protocol.HttpTest;
import io.openliberty.mcp.internal.fat.protocol.ProtocolVersionTest;
import io.openliberty.mcp.internal.fat.tool.CancellationTest;
import io.openliberty.mcp.internal.fat.tool.DeploymentProblemTest;
import io.openliberty.mcp.internal.fat.tool.McpUrlPathTest;
import io.openliberty.mcp.internal.fat.tool.NoParamNameTest;
import io.openliberty.mcp.internal.fat.tool.ToolErrorHandlingTest;
import io.openliberty.mcp.internal.fat.tool.ToolTest;

/**
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
                BeanLifecycleTest.class,
                DeploymentProblemTest.class,
                CancellationTest.class,
                HttpTest.class,
                LifecycleTest.class,
                McpUrlPathTest.class,
                NoParamNameTest.class,
                ProtocolVersionTest.class,
                ToolErrorHandlingTest.class,
                ToolTest.class

})
public class FATSuite {

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(null, /* skipTransformation */ true, EE10, EE11);
}
