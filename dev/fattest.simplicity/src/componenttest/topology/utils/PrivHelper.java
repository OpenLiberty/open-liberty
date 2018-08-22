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
package componenttest.topology.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;

public class PrivHelper {

    public static final String JAXB_PERMISSION = "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind.v2.runtime.reflect\";";

    public static String getProperty(final String prop) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(prop);
            }
        });
    }

    public static String getProperty(final String prop, final String defaultValue) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(prop, defaultValue);
            }
        });
    }

    public static boolean getBoolean(final String prop) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return Boolean.getBoolean(prop);
            }
        });
    }

    /**
     * Creates a new policy file that combines the JDK's default policy set and permissions passed into this method.
     *
     * @param server The LibertyServer to install the custom policy file for
     * @param permissionsToAdd Permission(s) to add to the JDK default policies, for example:<br>
     *            <code>permission java.lang.RuntimePermission "accessClassInPackage.com.sun.xml.internal.bind.v2.runtime.reflect";</code>
     */
    public static void generateCustomPolicy(LibertyServer server, String... permissionsToAdd) throws Exception {
        if (!server.isJava2SecurityEnabled() || permissionsToAdd == null || permissionsToAdd.length == 0)
            return;

        String policyPath = JavaInfo.forServer(server).javaHome() + "/lib/security/java.policy";
        String policyContents = FileUtils.readFile(policyPath);

        // Add in our custom policy for JAX-B permissions:
        StringBuilder sb = new StringBuilder("        // Permissions added by FAT bucket\n");
        for (String permToAdd : permissionsToAdd)
            sb.append("        ").append(permToAdd).append('\n');
        if (!policyContents.contains("grant {"))
            throw new Exception("Policy file did not contain special token 'grant {'.  The contents were: " + policyContents);
        policyContents = policyContents.replace("grant {", "grant {\n" + sb.toString() + '\n');
        File outputFile = new File(server.getServerRoot() + "/custom_j2sec.policy");
        Log.info(PrivHelper.class, "generateCustomPolicy", "Generating custom policy file at: " + outputFile + "\n" + policyContents);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(policyContents);
        }

        RemoteFile f = server.getServerBootstrapPropertiesFile();
        try (OutputStream w = f.openForWriting(true)) {
            String policySetting = "\njava.security.policy=" + outputFile.toURI().toURL();
            w.write(policySetting.getBytes());
            w.flush();
        }
    }

}
