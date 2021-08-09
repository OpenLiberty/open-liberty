/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security;

/**
 * Drives most test cases with two inputs: hard-coded and constructed.
 * This verifies all possible permutations of input are covered.
 */
public class AccessIdUtilMockUrlRealmTest extends AccessIdUtilTest {

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.AccessIdUtilTest#setRealm()
     */
    @Override
    void setRealm() {
        defaultRealm = "https://test-realm.com/";
    }

}