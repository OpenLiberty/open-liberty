/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.ws.security.javaeesec.identitystore;

import javax.security.enterprise.identitystore.PasswordHash;

/**
 * Password hash implementation for testing.
 */
public class TestPasswordHash implements PasswordHash {

    @Override
    public String generate(char[] arg0) {
        return null;
    }

    @Override
    public boolean verify(char[] arg0, String arg1) {
        return false;
    }
}
