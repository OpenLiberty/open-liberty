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
package com.ibm.ws.artifact.fat_bvt.test.utils;

/**
 * Feature definition.  Used when preparing liberty servers.
 */
public class FATBundleDef {
    public final String sourceJarPath;

    public FATBundleDef(String sourceJarPath) {
        this.sourceJarPath = sourceJarPath;
    }
}
