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

package com.ibm.ws.security.pwdhash.test;

import javax.enterprise.context.Dependent;
import javax.security.enterprise.identitystore.PasswordHash;

import com.ibm.ws.security.javaeesec.fat_helper.Constants;

/**
 * Test PasswordHash sample for DatabaseIdentityStore. For testing purposes only.
 */
@Dependent
public class TestHash implements PasswordHash {

    public TestHash() {
        System.out.println("Init TestHash");
    }

    @Override
    public boolean verify(char[] incomingPwd, String existingPwd) {
        System.out.println("TestHash is for testing purposes only.");
        String pwd = String.valueOf(incomingPwd) + Constants.DB_CUSTOM_HASH;
        if (pwd.equals(existingPwd)) {
            return true;
        }
        return false;
    }

    @Override
    public String generate(char[] pwdToHash) {
        return String.valueOf(pwdToHash) + Constants.DB_CUSTOM_HASH;
    }

}
