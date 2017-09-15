/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.custom.junit.runner;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import componenttest.annotation.MinimumJavaLevel;

/**
 * This test is an alternative to {@link SimpleFindServerAndStartItTest} for when there is no server to be found.
 * Deprecated: Use {@link MinimumJavaLevel} instead
 */
@Deprecated
public class NoOpTestForOldJava7 {

    @Test
    public void noOp() {
        assertTrue(true);
    }

}
