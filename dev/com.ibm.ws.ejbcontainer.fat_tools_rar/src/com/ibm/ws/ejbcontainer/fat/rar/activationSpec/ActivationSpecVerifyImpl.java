/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.activationSpec;

import java.util.logging.Logger;

public class ActivationSpecVerifyImpl {
    private final static String CLASSNAME = ActivationSpecVerifyImpl.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    static String userName1;
    static String password1;
    static String userName2;
    static String password2;
    static String userName3;
    static String password3;

    static public boolean testVariation1() {
        svLogger.info("testVariation1 - verify activation spec alias used when MDB does not contain alias or userid/pw");
        svLogger.info("userName1: " + userName1);
        svLogger.info("password1: " + password1);
        svLogger.info("Comparing to aliasUserId1 and aliasPw1");

        if ((userName1 == null) || (password1 == null)) {
            svLogger.info("Test variation 1 failed");
            return false;
        } else {
            if ((userName1.equals("aliasUserId1")) && (password1.equals("aliasPw1"))) {
                svLogger.info("Test variation 1 passed");
                return true;
            } else {
                svLogger.info("Test variation 1 failed");
                return false;
            }
        }
    }

    static public boolean testVariation2() {
        svLogger.info("testVariation2 - verify MDB userid/pw overrides activation spec alias");
        svLogger.info("userName2: " + userName2);
        svLogger.info("password2: " + password2);
        svLogger.info("Comparing to mdbUser2 and mdbPassword2");

        if ((userName2 == null) || (password2 == null)) {
            svLogger.info("Test variation 2 failed");
            return false;
        } else {
            if ((userName2.equals("mdbUser2")) && (password2.equals("mdbPassword2"))) {
                svLogger.info("Test variation 2 passed");
                return true;
            } else {
                svLogger.info("Test variation 2 failed");
                return false;
            }
        }
    }

    static public boolean testVariation3() {
        svLogger.info("testVariation3 - verify mdb alias overrides activation spec alias and MDB userid/pw");
        svLogger.info("userName3: " + userName3);
        svLogger.info("password3: " + password3);
        svLogger.info("Comparing to aliasUserId3 and aliasPw3");

        if ((userName3 == null) || (password3 == null)) {
            svLogger.info("Test variation 3 failed");
            return false;
        } else {
            if ((userName3.equals("aliasUserId3")) && (password3.equals("aliasPw3"))) {
                svLogger.info("Test variation 3 passed");
                return true;
            } else {
                svLogger.info("Test variation 3 failed");
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