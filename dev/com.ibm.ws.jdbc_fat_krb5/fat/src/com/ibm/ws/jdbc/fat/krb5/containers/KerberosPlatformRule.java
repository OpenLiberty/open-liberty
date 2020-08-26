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
package com.ibm.ws.jdbc.fat.krb5.containers;

import java.io.File;

import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KeyTab;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jdbc.fat.krb5.DB2KerberosTest;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.JavaInfo.Vendor;

/**
 * Rule that automatically skips tests if we are running on a platform that does NOT
 * support Kerberos
 */
public class KerberosPlatformRule implements TestRule {

    @Override
    public Statement apply(Statement stmt, Description desc) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                if (shouldRun(desc)) {
                    stmt.evaluate();
                }
            }
        };
    }

    public static boolean shouldRun(Description desc) {
        Class<?> c = desc == null ? KerberosPlatformRule.class : desc.getTestClass();
        String m = (desc == null || desc.getMethodName() == null) ? "shouldRun" : desc.getMethodName();

        // Kerberos is only supported on certain operating systems
        // Skip the tests if we are not on one of the supported OSes
        String os = System.getProperty("os.name", "UNKNOWN").toUpperCase();
        if (!os.contains("LINUX") && !os.contains("MAC OS")) {
            if (FATRunner.FAT_TEST_LOCALRUN) {
                throw new RuntimeException("Running on an unsupported os: " + os);
            } else {
                Log.info(c, m, "Skipping test because of unsupported os: " + os);
                return false;
            }
        }

        // Make sure the JDK we are on supports the keytab encryption type
        JavaInfo java = JavaInfo.forCurrentVM();
        if (java.majorVersion() == 8 && java.vendor() == Vendor.SUN_ORACLE) {
            File keytabFile = new File(System.getProperty("user.dir") + "/publish/servers/com.ibm.ws.jdbc.fat.krb5/security/krb5.keytab");
            KerberosPrincipal princ = new KerberosPrincipal(DB2KerberosTest.KRB5_USER + "@" + KerberosContainer.KRB5_REALM);
            KeyTab kt = KeyTab.getInstance(princ, keytabFile);
            Log.info(c, m, "Loaded keytab: " + kt);
            KerberosKey[] kks = kt.getKeys(princ);
            Log.info(c, m, "Loaded " + kks.length + " Kerberos keys");
            if (kks.length == 0) {
                Log.info(c, m, "Skipping test because this JVM does not support the keytab encoding we are using");
                return false;
            }
        }

        return true;
    }

}
