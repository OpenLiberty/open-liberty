// IBM Confidential
//
// OCO Source Materials
//
// Copyright IBM Corp. 2013
//
// The source code for this program is not published or otherwise divested 
// of its trade secrets, irrespective of what has been deposited with the 
// U.S. Copyright Office.
//
// Change Log:
//  Date       pgmr    reason           Description
//  --------   ------  ------       ---------------------------------
//  04/06/04   cjn     LIDB2110-69  Create class
//
//  --------------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.activationSpec;

import java.util.logging.Logger;

public class ActivationSpecVerifyImpl
{
    private final static String CLASSNAME = ActivationSpecVerifyImpl.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    static String userName1;
    static String password1;
    static String userName2;
    static String password2;
    static String userName3;
    static String password3;

    static public boolean testVariation1()
    {
        svLogger.info("testVariation1 - verify activation spec alias used when MDB does not contain alias or userid/pw");
        svLogger.info("userName1: " + userName1);
        svLogger.info("password1: " + password1);
        svLogger.info("Comparing to aliasUserId1 and aliasPw1");

        if ((userName1 == null) || (password1 == null))
        {
            svLogger.info("Test variation 1 failed");
            return false;
        }
        else
        {
            if ((userName1.equals("aliasUserId1")) && (password1.equals("aliasPw1")))
            {
                svLogger.info("Test variation 1 passed");
                return true;
            }
            else
            {
                svLogger.info("Test variation 1 failed");
                return false;
            }
        }
    }

    static public boolean testVariation2()
    {
        svLogger.info("testVariation2 - verify MDB userid/pw overrides activation spec alias");
        svLogger.info("userName2: " + userName2);
        svLogger.info("password2: " + password2);
        svLogger.info("Comparing to mdbUser2 and mdbPassword2");

        if ((userName2 == null) || (password2 == null))
        {
            svLogger.info("Test variation 2 failed");
            return false;
        }
        else
        {
            if ((userName2.equals("mdbUser2")) && (password2.equals("mdbPassword2")))
            {
                svLogger.info("Test variation 2 passed");
                return true;
            }
            else
            {
                svLogger.info("Test variation 2 failed");
                return false;
            }
        }
    }

    static public boolean testVariation3()
    {
        svLogger.info("testVariation3 - verify mdb alias overrides activation spec alias and MDB userid/pw");
        svLogger.info("userName3: " + userName3);
        svLogger.info("password3: " + password3);
        svLogger.info("Comparing to aliasUserId3 and aliasPw3");

        if ((userName3 == null) || (password3 == null))
        {
            svLogger.info("Test variation 3 failed");
            return false;
        }
        else
        {
            if ((userName3.equals("aliasUserId3")) && (password3.equals("aliasPw3")))
            {
                svLogger.info("Test variation 3 passed");
                return true;
            }
            else
            {
                svLogger.info("Test variation 3 failed");
                return false;
            }
        }
    }

    static public void setVariation1(String userName, String password)
    {
        userName1 = userName;
        password1 = password;
    }

    static public void setVariation2(String userName, String password)
    {
        userName2 = userName;
        password2 = password;
    }

    static public void setVariation3(String userName, String password)
    {
        userName3 = userName;
        password3 = password;
    }
}