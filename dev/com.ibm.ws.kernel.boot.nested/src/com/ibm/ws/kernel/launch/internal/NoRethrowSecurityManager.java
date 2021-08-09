/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.kernel.launch.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Enumeration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.boot.internal.commands.ServerDumpUtil;

public class NoRethrowSecurityManager extends SecurityManager {

    public final static String lineSep = System.getProperty("line.separator");

    private final static TraceComponent tc = Tr.register(NoRethrowSecurityManager.class);

    @Override
    @FFDCIgnore({ SecurityException.class })
    public void checkPermission(final Permission perm) {
        try {

            super.checkPermission(perm);
        } catch (SecurityException ex) {

            handleSecurityException(perm, ex);
        }
    }

    @Override
    @FFDCIgnore({ SecurityException.class })
    public void checkPermission(Permission perm, Object context) {
        try {

            super.checkPermission(perm, context);
        } catch (SecurityException ex) {

            handleSecurityException(perm, ex);
        }
    }

    public void handleSecurityException(Permission perm, SecurityException e) throws SecurityException {

        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String stackStr = sw.toString();

        String[] codebaseloc = null;
        try {

            codebaseloc = getCodeBaseLoc(perm);
        } catch (Exception e2) {

            StringWriter sw2 = new java.io.StringWriter();
            e2.printStackTrace(new java.io.PrintWriter(sw2));

            String stackStr2 = sw2.toString();
            Tr.error(tc,
                     "error.java.security.exception.codebase",
                     new Object[] { e2.toString() + "(" + e2.getMessage() + ")" + lineSep + lineSep + stackStr2 });
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            String strDebug = "\nJava SecurityException:" +
                              "\n perm.getName<>: " + perm.getName() +
                              "\n e.getMessage<>: " + e.getMessage() +
                              "\nBegin SecurityException\n" + stackStr + "\n" +
                              "Code base location information:\n" + Arrays.toString(codebaseloc) +
                              "\n class ==> : " + codebaseloc[0] + //offending Class
                              "\nSuggested Action: Verify the attempted operation is permitted by" +
                              " examining all Java 2 security policy files and application code." +
                              " Additional permissions may be required, a doPrivileged API may be needed" +
                              " in some code on the call stack, or the Security Manager properly prevented" +
                              " access to a resource the caller does not have permission to access." +
                              "\nEnd SecurityException\n";
            Tr.debug(tc, strDebug);
        }

        // While it seems strange to have this message issued differently on
        // z/OS, we've taken an APAR to separate the stack trace from the
        // rest of the failure information because it often generates a
        // CTRACE buffer overflow.  To work around that, we issue two messages.
        if (!ServerDumpUtil.isZos()) {
            Tr.warning(tc, "warning.java.security.permdenied",
                       new Object[] {
                                     lineSep + lineSep,
                                     lineSep + lineSep + "      " + perm.getName() + " : " + e.getMessage() + lineSep + lineSep + lineSep,
                                     codebaseloc[0], //offendingClass,
                                     lineSep + lineSep,
                                     lineSep + lineSep + stackStr + lineSep + lineSep,
                                     lineSep + lineSep + codebaseloc[1] });

        }
        else {
            Tr.warning(tc, "warning.zOS.java.security.permdenied1", new Object[] {
                                                                                  lineSep + lineSep,
                                                                                  lineSep + lineSep + "      " + perm.getName() + " : " + e.getMessage()
                                                                                                  + lineSep + lineSep + lineSep,
                                                                                  codebaseloc[0], //offendingClass,
                                                                                  lineSep + lineSep + codebaseloc[1] });

            Tr.warning(tc, "warning.zOS.java.security.permdenied2", new Object[] {
                       "\nBegin SecurityException\n" + stackStr + "\nEnd SecurityException\n" });
        }
    }

    /**
     * The following method is used to print out the code base or code source
     * location. This would be the path/URL that a class is loaded from. This
     * information is useful when trying to debug AccessControlExceptions
     * because the AccessControlException stack trace does not include where the
     * class was loaded from. Where a class is loaded from is very important
     * because that is one of the essential items contributing to a policy in a
     * policy file.
     * 
     */
    public String[] getCodeBaseLoc(Permission perm) {

        final Permission inPerm = perm;

        return AccessController.doPrivileged(new java.security.PrivilegedAction<String[]>() {

            @Override
            public String[] run() {

                Class<?>[] classes = getClassContext();

                StringBuffer sb = new StringBuffer(classes.length * 100);
                sb.append(lineSep);

                // one for offending class and the other for code base
                // location
                String[] retMsg = new String[2];

                for (int i = 0; i < classes.length; i++) {

                    Class<?> clazz = classes[i];
                    ProtectionDomain pd = clazz.getProtectionDomain();

                    // check for occurrence of checkPermission from stack
                    if (classes[i].getName().indexOf("com.ibm.ws.kernel.launch.internal.NoRethrowSecurityManager") != -1) {

                        // found SecurityManager, start to go through
                        // the stack starting next class
                        for (int j = i + 1; j < classes.length; j++) {

                            if (classes[j].getName().indexOf("java.lang.ClassLoader") != -1) {

                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "Not printing AccessError since ClassLoader.getResource returns null per JavaDoc when privileges not there.");
                                String[] codebaseloc = new String[2];
                                codebaseloc[0] = "_nolog";
                                codebaseloc[1] = "";
                                return codebaseloc;
                            }

                            ProtectionDomain pd2 = classes[j].getProtectionDomain();
                            if (isOffendingClass(classes, j, pd2, inPerm)) {
                                retMsg[0] = lineSep + lineSep +
                                            "     " + classes[j].getName() + "  in  " + "{" + getCodeSource(pd2) + "}" +
                                            lineSep +
                                            lineSep;

                                break;
                            }

                        }

                    }

                    java.security.CodeSource cs = pd.getCodeSource();
                    String csStr = getCodeSource(pd);

                    // class name : location
                    sb.append(classes[i].getName()).append(" : ").append(csStr + lineSep);
                    sb.append("  ").append(permissionToString(cs, clazz.getClassLoader(), pd.getPermissions()))
                                    .append(lineSep);
                }

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Permission Error Code is " + retMsg[0]);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Code Base Location: is " + sb.toString());

                retMsg[1] = "";
                return retMsg;
            }
        });
    }

    /**
     * Find the code source based on the protection domain
     * 
     */
    public String getCodeSource(ProtectionDomain pd) {

        CodeSource cs = pd.getCodeSource();

        String csStr = null;

        if (cs == null) {

            csStr = "null code source";
        } else {

            URL url = cs.getLocation();

            if (url == null) {

                csStr = "null code URL";
            } else {

                csStr = url.toString();
            }

        }

        return csStr;
    }

    /**
     * Print out permissions granted to a CodeSource.
     * 
     */
    public String permissionToString(java.security.CodeSource cs, ClassLoader classloaderClass,
                                     PermissionCollection col) {

        StringBuffer buf = new StringBuffer("ClassLoader: ");
        if (classloaderClass == null) {

            buf.append("Primordial Classloader");
        } else {

            buf.append(classloaderClass.getClass().getName());
        }
        buf.append(lineSep);
        buf.append("  Permissions granted to CodeSource ").append(cs).append(lineSep);

        if (col != null) {

            Enumeration<Permission> e = col.elements();
            buf.append("  {").append(lineSep);
            while (e.hasMoreElements()) {

                Permission p = e.nextElement();
                buf.append("    ").append(p.toString()).append(";").append(lineSep);
            }
            buf.append("  }");
        } else {

            buf.append("  {").append(lineSep).append("  }");
        }

        return buf.toString();
    }

    /**
     * isOffendingClass determines the offending class from the classes defined
     * in the stack.
     * 
     */
    boolean isOffendingClass(Class<?>[] classes, int j, ProtectionDomain pd2, Permission inPerm) {
        // Return true if ...
        return (!classes[j].getName().startsWith("java")) && // as long as not
                                                             // starting with
                                                             // java
               (classes[j].getName().indexOf("com.ibm.ws.kernel.launch.internal.NoRethrowSecurityManager") == -1) && // not
               // our
               // SecurityManager
               (classes[j].getName().indexOf("ClassLoader") == -1) && // not a
                                                                      // class
                                                                      // loader
               // not the end of stack and next is not a class loader
               ((j == classes.length - 1) ? true : (classes[j + 1].getName().indexOf("ClassLoader") == -1)) &&
               // lacks the required permissions
               !pd2.implies(inPerm);
    }

}
