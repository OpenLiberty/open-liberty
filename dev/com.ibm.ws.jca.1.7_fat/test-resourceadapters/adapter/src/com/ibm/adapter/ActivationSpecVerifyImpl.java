/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter;

public class ActivationSpecVerifyImpl {
    static String userName1;
    static String password1;
    static String userName2;
    static String password2;
    static String userName3;
    static String password3;

    static public boolean testVariation1() {
        System.out.println("testVariation1 - verify activation spec alias used when MDB does not contain alias or userid/pw");
        System.out.println("userName1: " + userName1);
        System.out.println("password1: " + password1);
        System.out.println("Comparing to aliasUserId1 and aliasPw1");

        if ((userName1 == null) || (password1 == null)) {
            System.out.println("Test variation 1 failed");
            return false;
        } else {
            if ((userName1.equals("aliasUserId1")) && (password1.equals("aliasPw1"))) {
                System.out.println("Test variation 1 passed");
                return true;
            } else {
                System.out.println("Test variation 1 failed");
                return false;
            }
        }
    }

    static public boolean testVariation2() {
        System.out.println("testVariation2 - verify MDB userid/pw overrides activation spec alias");
        System.out.println("userName2: " + userName2);
        System.out.println("password2: " + password2);
        System.out.println("Comparing to mdbUser2 and mdbPassword2");

        if ((userName2 == null) || (password2 == null)) {
            System.out.println("Test variation 2 failed");
            return false;
        } else {
            if ((userName2.equals("mdbUser2")) && (password2.equals("mdbPassword2"))) {
                System.out.println("Test variation 2 passed");
                return true;
            } else {
                System.out.println("Test variation 2 failed");
                return false;
            }
        }
    }

    static public boolean testVariation3() {
        System.out.println("testVariation3 - verify mdb alias overrides activation spec alias and MDB userid/pw");
        System.out.println("userName3: " + userName3);
        System.out.println("password3: " + password3);
        System.out.println("Comparing to aliasUserId3 and aliasPw3");

        if ((userName3 == null) || (password3 == null)) {
            System.out.println("Test variation 3 failed");
            return false;
        } else {
            if ((userName3.equals("aliasUserId3")) && (password3.equals("aliasPw3"))) {
                System.out.println("Test variation 3 passed");
                return true;
            } else {
                System.out.println("Test variation 3 failed");
                return false;
            }
        }
    }

    static public void setVariation1(String userName, String password) {
        userName1 = userName;
        password1 = password;
    }

    static public void setVariation2(String userName, String password) {
        userName2 = userName;
        password2 = password;
    }

    static public void setVariation3(String userName, String password) {
        userName3 = userName;
        password3 = password;
    }
}
