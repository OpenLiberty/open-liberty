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
package com.ibm.ws.kernel.boot.internal;

import java.security.SecureRandom;

import com.ibm.websphere.ras.annotation.Trivial;

public class PasswordGenerator {

    public static final int PASSWORD_LENGTH = 23;
    private static final char[] CHAR_SET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final SecureRandom rand = new SecureRandom();

    @Trivial
    public static char[] generateRandom() {
        char[] pass = new char[PASSWORD_LENGTH];
        for (int i = 0; i < PASSWORD_LENGTH; i++)
            pass[i] = CHAR_SET[rand.nextInt(CHAR_SET.length)];
        return pass;
    }

}
