/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wssecurity.fat.utils.common;

import org.junit.Test;

/**
 * Test case created to make sure that at least 1 test in the suite runs
 * 12/2020 This is a temporary workaround to avoid invalid bucket error from the common project in OL Personal Build when #build is tagged.
 * The PB automation script is fixing to accept the project naming pattern; once this fix is available, this test will not be needed.
 */
public class AlwaysRunAndPassTest {
    @Test
    public void alwaysPass() throws Exception {
        System.out.println("This testcase always runs and succeeds!");
    }
}
