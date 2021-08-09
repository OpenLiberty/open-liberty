/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 ** The recommended file location is:
 ** "${server.config.dir}/resources/security/roleMapping.props"
 ** The file format is:
 ** <application name>::<role name>::<type : USER, GROUP or SPECIAL>::<accessid of user, group, or special subject name (either EVERYONE or ALLAUTHENTICATEDUSERS)):
 ** for example:
 ** SecureEJBSample::servletRole::USER::user:BasicRealm/user1
 ** SecureEJBSample::servletRole::USER::user:BasicRealm/user2
 ** SecureEJBSample::ejbRole::USER::user:BasicRealm/user1
 **/

package com.ibm.ws.security.authorization.jacc.role;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class FileRoleMapping {
    private static final TraceComponent tc = Tr.register(FileRoleMapping.class);
    private static String roleMappingFile = null;
    private static FileRoleMapping instance = null;

    public enum Type {
        SPECIAL,
        USER,
        GROUP
    }

    public static final String EVERYONE = "EVERYONE";
    public static final String ALL_AUTHENTICATED_USERS = "ALLAUTHENTICATEDUSERS";

    private FileRoleMapping() {}

    public static void initialize(String roleMappingFile) {
        FileRoleMapping.roleMappingFile = roleMappingFile;
        FileRoleMapping.instance = new FileRoleMapping();
    }

    public static FileRoleMapping getInstance() {
        return instance;
    }

    public List<String> getRolesForSpecialSubject(String applicationName, String accessId) {
        return getRoles(applicationName, accessId, Type.SPECIAL);
    }

    public List<String> getRolesForUser(String applicationName, String accessId) {
        return getRoles(applicationName, accessId, Type.USER);
    }

    public List<String> getRolesForGroup(String applicationName, String accessId) {
        return getRoles(applicationName, accessId, Type.GROUP);
    }

    private List<String> getRoles(String applicationName, String accessId, Type type) {
        List<String> output = null;
        BufferedReader in = null;

        try {
            in = fileOpen(roleMappingFile);
            String s;
            while ((s = in.readLine()) != null) {
                if (!(s.startsWith("#") || s.trim().length() <= 0)) {
                    String[] list = s.split("::");
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "# of items : " + list.length + " " + s);
                    }
                    if (list.length == 4) {
                        if (list[0].equals(applicationName) && list[2].equals(getTypeString(type)) && list[3].equals(accessId)) {
                            if (output == null) {
                                output = new ArrayList<String>();
                            }
                            output.add(list[1]);
                        }
                    } else {
                        Tr.error(tc, "Syntax error in the role mapping file : " + s);
                    }
                }
            }
        } catch (Exception e) {
            Tr.error(tc, "Exception is caught : " + e);
        } finally {
            fileClose(in);
        }
        return output;
    }

    private String getTypeString(Type type) {
        if (type == Type.USER) {
            return "USER";
        } else if (type == Type.GROUP) {
            return "GROUP";
        } else {
            return "SPECIAL";
        }
    }

    // private methods
    private BufferedReader fileOpen(String fileName) throws FileNotFoundException, UnsupportedEncodingException, PrivilegedActionException {
        final String f = fileName;
        return (BufferedReader) AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<Object>() {
            @Override
            public Object run() throws UnsupportedEncodingException, FileNotFoundException {
                return new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            }
        });
    }

    private void fileClose(BufferedReader in) {
        try {
            if (in != null)
                in.close();
        } catch (Exception e) {
            Tr.error(tc, "Error closing file : " + e);
        }
    }

}
