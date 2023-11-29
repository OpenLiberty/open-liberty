/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.cdi.internal.core;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTestAction;

/**
 * Repeats a test in a given set of configurations
 * <p>
 * Example:
 *
 * <pre>
 * {@code @ClassRule}
 * public RepeatRule{@code <Config>} r = new RepeatRule{@code <>}(Config.A, Config.B);
 *
 * {@code @Test}
 * public void test() {
 *    Config c = r.getRepeat(); // Will return Config.A on the first run and Config.B on the second run
 *    // ...
 * }
 * </pre>
 *
 * @param <T> the type of the object that describes the configuration
 */
public class RepeatRule<T> implements TestRule {

    private RepeatTestAction[] repeats;

    private RepeatTestAction currentRepeat;

    @SafeVarargs
    public RepeatRule(RepeatTestAction... repeats) {
        this.repeats = repeats;
    }

    public RepeatTestAction getRepeat() {
        return currentRepeat;
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                for (RepeatTestAction repeat : repeats) {
                    currentRepeat = repeat;
                    RepeatTestFilter.activateRepeatAction(repeat);
                    try {
                        statement.evaluate();
                    } finally {
                        RepeatTestFilter.deactivateRepeatAction();
                        currentRepeat = null;
                    }
                }
            }
        };
    }
}
