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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.boot.internal.commands.ServerDumpUtil;

public class MissingDoPrivDetectionSecurityManager extends SecurityManager {

    public final static String lineSep = System.getProperty("line.separator");

    private static final Set<String> exceptions = new HashSet<String>();
    private static boolean UNIQUE_ONLY = false;

    private final static TraceComponent tc = Tr.register(MissingDoPrivDetectionSecurityManager.class);

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

        if (UNIQUE_ONLY && !isUniqueACE(e))
            return;

        try {
            // Bad thing to do, but hey, quick and dirty.
            Tr.warning(tc, "warning.java.security.permdenied.quickmsg", getCodeBaseLoc(perm, e));

            // additional information

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String stackStr = sw.toString();

            String[] codebaseloc = null;
            codebaseloc = getCodeBaseLocForPerm(perm);

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

        } catch (Exception e2) {

            StringWriter sw2 = new java.io.StringWriter();
            e2.printStackTrace(new java.io.PrintWriter(sw2));

            String stackStr2 = sw2.toString();
            Tr.error(tc,
                     "error.java.security.exception.codebase",
                     new Object[] { e2.toString() + "(" + e2.getMessage() + ")" + lineSep + lineSep + stackStr2 });

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
     * @param e
     * 
     */
    public String getCodeBaseLoc(final Permission perm, final SecurityException e) {

        return AccessController.doPrivileged(new java.security.PrivilegedAction<String>() {

            @Override
            public String run() {

                Class<?>[] classes = getClassContext();

                StringBuffer sb = new StringBuffer(classes.length * 100);
                sb.append(lineSep);

                boolean runtimeCodeOnStack = false;
                int appCodeIndex = -1;

                for (int i = 0; i < classes.length; i++) {

                    Class<?> clazz = classes[i];

                    ClassLoader cl = clazz.getClassLoader();

                    boolean jvmClassLoader = isJvmClassLoader(cl);
                    boolean rtClassLoader = isRuntimeClassLoader(clazz, cl);
                    boolean appClassLoader = isAppClassLoader(cl);

                    String loader = null;
                    if (cl != null) {
                        loader = cl.getClass().getName();
                    }

                    if (clazz.getName().startsWith("com.ibm._jsp.")) {
                        System.out.println(clazz.getClassLoader().getClass().getName());
                    }

                    runtimeCodeOnStack |= rtClassLoader;

                    if (appClassLoader) {
                        appCodeIndex = i;
                    }
                }

                StringBuilder stack = new StringBuilder();
                StackTraceElement[] stackTrace = e.getStackTrace();
                int endIndex = stackTrace.length;
                if (appCodeIndex > -1 && runtimeCodeOnStack) {
                    endIndex = appCodeIndex;
                    stack.append("Detected a possible missing doPriv statement. There was Liberty code between the check and the application code.\r\n");
                } else if (appCodeIndex == -1 && runtimeCodeOnStack) {
                    stack.append("No app code on stack, but a permission check failed.\r\n");
                } else if (appCodeIndex > -1 && !!!runtimeCodeOnStack) {
                    stack.append("The application needs to have permissions added");
                } else {
                    stack.append("No app code, or Liberty code on stack, so pretty much shouldn't happen.\r\n");
                }

                stack.append("Permission: \r\n");
                stack.append(perm.toString());
                stack.append("\r\nStack: \r\n");
                stack.append(e.toString());
                for (int i = 0; i < endIndex; i++) {
                    stack.append(stackTrace[i].toString());
                    stack.append("\r\n");
                }

                return stack.toString();

            }

        });
    }

    private boolean isAppClassLoader(ClassLoader cl) {
        if (cl == null)
            return false;

        String clName = cl.getClass().getName();
        return clName.startsWith("com.ibm.ws.classloading") || clName.equals("com.ibm.ws.jsp.webcontainerext.JSPExtensionClassLoader");
    }

    private boolean isRuntimeClassLoader(Class<?> c, ClassLoader cl) {
        if (cl == null)
            return false;

        if (c.getName().startsWith("com.ibm.ws.kernel.launch.internal.MissingDoPrivDetectionSecurityManager"))
            return false;

        return (cl.getClass().getName().equals(this.getClass().getClassLoader().getClass().getName()));
    }

    private boolean isJvmClassLoader(ClassLoader cl) {
        return cl == null || cl == ClassLoader.getSystemClassLoader();
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
    public String[] getCodeBaseLocForPerm(Permission perm) {

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

                ProtectionDomain pd2 = null;
                for (int i = 0; i < classes.length; i++) {

                    Class<?> clazz = classes[i];
                    ProtectionDomain pd = clazz.getProtectionDomain();

                    // check for occurrence of checkPermission from stack
                    if (classes[i].getName().indexOf("com.ibm.ws.kernel.launch.internal.MissingDoPrivDetectionSecurityManager") != -1) {

                        // found SecurityManager, start to go through
                        // the stack starting next class
                        for (int j = i + 1; j < classes.length; j++) {

                            pd2 = classes[j].getProtectionDomain();

                            if (isOffendingClass(classes, j, pd2, inPerm)) {
                                retMsg[0] = lineSep + lineSep +
                                            "     " + classes[j].getName() + "  in  " + "{" + getCodeSource(pd2) + "}" +
                                            lineSep +
                                            lineSep;

                                StringBuffer sb2 = new StringBuffer(classes.length * 100);
                                sb2.append(lineSep);
                                sb2.append(classes[j].getName()).append(" : ").append(getCodeSource(pd2) + lineSep);
                                sb2.append("  ").append(permissionToString(pd2.getCodeSource(), classes[j].getClassLoader(), pd2.getPermissions()))
                                                .append(lineSep);

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

                Tr.info(tc, "java.security.permdenied.class.info", retMsg[0]);
                Tr.info(tc, "java.security.permdenied.codebaseloc.info", sb.toString());

                retMsg[1] = getCodeSource(pd2).concat(lineSep);
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
        //System.out.println("isOffendingClass: " + classes[j].getName() + " inPerm " + inPerm.getName() + " protectionDomain: " + pd2.toString());
        return (!classes[j].getName().startsWith("java")) && // as long as not
                                                             // starting with
                                                             // java
               (classes[j].getName().indexOf("com.ibm.ws.kernel.launch.internal.MissingDoPrivDetectionSecurityManager") == -1) && // not
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

    private static boolean isUniqueACE(final SecurityException e) {
        for (StackTraceElement ste : e.getStackTrace()) {
            String line = ste.toString();
            if (line.startsWith("com.ibm.ws") && !line.startsWith("com.ibm.ws.kernel.launch.internal.MissingDoPrivDetectionSecurityManager")) {
                return exceptions.add(line);
            }
        }
        // If we didn't find any WS code in the stack, assume it's unique
        return true;
    }

    public static void setUniqueOnly(boolean uniqueOnly) {
        UNIQUE_ONLY = uniqueOnly;
    }
}
