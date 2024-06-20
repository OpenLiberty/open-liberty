/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.credentials.saf.internal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Note: this Rule isn't actually used anywhere but it's a handy-dandy Rule
 * for running a test multiple times, to flush out intermittents and what not,
 * so it's worth keeping around.
 *
 * Copied from here: http://www.codeaffine.com/2013/04/10/running-junit-tests-repeatedly-without-loops/
 *
 * Usage:
 *
 * @Rule
 *       public RepeatRule repeatRule = new RepeatRule();
 *
 *       ...
 *
 * @Test
 *       @Repeat(times=10) // Run the test 10 times.
 *       public void someTest() {
 *       ...
 *       }
 *
 */
public class RepeatRule implements TestRule {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ java.lang.annotation.ElementType.METHOD })
    public @interface Repeat {
        public abstract int times();
    }

    private static class RepeatStatement extends Statement {

        private final int times;
        private final Statement statement;

        private RepeatStatement(int times, Statement statement) {
            this.times = times;
            this.statement = statement;
        }

        @Override
        public void evaluate() throws Throwable {
            for (int i = 0; i < times; i++) {
                statement.evaluate();
            }
        }
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        Statement result = statement;
        Repeat repeat = description.getAnnotation(Repeat.class);
        if (repeat != null) {
            int times = repeat.times();
            result = new RepeatStatement(times, statement);
        }
        return result;
    }
}
