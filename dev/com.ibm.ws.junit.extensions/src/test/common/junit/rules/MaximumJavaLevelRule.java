/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.common.junit.rules;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * @deprecated This test rule is for a (hopefully) temporary workaround to Java 11 support
 */
@Deprecated
public class MaximumJavaLevelRule implements TestRule {

    public final int maxJavaLevel;

    public MaximumJavaLevelRule(int maxJavaLevel) {
        this.maxJavaLevel = maxJavaLevel;
    }

    @Override
    public Statement apply(Statement arg0, Description arg1) {
        if (maxJavaLevel < JavaInfo.JAVA_VERSION)
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {}
            };
        else
            return arg0;
    }

}
