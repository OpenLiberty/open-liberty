/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(FATSuiteNotNeeded.class)
public class FATSuiteNotNeeded extends Suite {

    public FATSuiteNotNeeded(Class<?> setupClass) throws Exception {
        // Override suite method to include test classes dynamically
        super(setupClass, FATSuiteBuilder.suite());
    }
}
