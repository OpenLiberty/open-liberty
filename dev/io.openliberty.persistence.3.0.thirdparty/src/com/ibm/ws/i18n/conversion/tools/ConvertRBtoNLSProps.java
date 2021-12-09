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

package com.ibm.ws.i18n.conversion.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public class ConvertRBtoNLSProps {

    private static final String NEW_LINE = "\n";

    /**
     * Converts a ResourceBundle to a *.nlsprops file.  
     * The first argument should be a path to a JAR file that contains the ResourceBundle(s) to 
     * convert. The second argument should be a path to the output nlsprops file.
     * Additional arguments can be regular expressions to indicate which ResouceBundle(s) to
     * convert.
     * <br/>
     * Here is an example of how to use this method:
     * <br/>
     * java com.ibm.ws.i18n.conversion.tools.ConvertRBtoNLSProps /path/to/openSource.jar /path/to/myProps.nlsprops org.my.open.src.i18n.*
     * 
     * This will pull any ResourceBundles classes from the org.my.open.src.i18n package (and any sub packages) in the openSource.jar
     * and will convert them to nlsprops format in the myProps.nlsprops file.
     * 
     * @param args - args[0] must be a valid path to a JAR file
     *               args[1] must be a valid path to an output directory (i.e. /path/to/myNLSFiles)
     *               args[2] (and beyond) are optional, and can specify which packages inside the jar to search
     *    
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            usage();
            System.exit(1);
        }

        String jarPath = args[0];
        String outputDirPath = args[1];
        
        String[] filters;
        if (args.length == 2) {
            filters = new String[]{".*"};
        } else {
            filters = new String[args.length-2];
            System.arraycopy(args, 2, filters, 0, filters.length);
        }
        
        System.exit(convert(jarPath, outputDirPath, filters));
    }

    public static void usage() {
        System.err.println("usage: java com.ibm.ws.i18n.conversion.tools.ConvertRBtoNLSProps <path/to/someJarFile> <path/to/output/nlsprops/file> [<optional-regex-package-filters>...]");
    }

    public static int convert(String jarPath, String outputDirPath, String... regexFilters) {
        int rc;
        try {
            
            File jarFile = new File(jarPath);
            URLClassLoader classLoader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, ConvertRBtoNLSProps.class.getClassLoader());
            rc = convert(classLoader, jarFile, outputDirPath, regexFilters);
        } catch (IOException e) {
            e.printStackTrace();
            rc = 1;
        }
        return rc;
    }
    
    static int convert(ClassLoader classLoader, File jarFile, String outputDirPath, String... regexFilters) {
        int rc = 0;

        try {
            for (String filter : regexFilters) {
                List<Class<? extends ResourceBundle>> rbClasses = getResourceBundleClasses(new JarFile(jarFile), filter, classLoader);
                for (Class<? extends ResourceBundle> rbClass : rbClasses) {
                    PrintWriter out = null;
                    try {
                        String packagePath = convertPackageToDirName(rbClass.getPackage());
                        out = createOutputWriter(outputDirPath, packagePath, rbClass.getSimpleName()+".nlsprops");
                        // we want to preserve ordering, so we'll sort the message keys using  sorted set
                        SortedSet<String> msgKeys = new TreeSet<String>();
                        ResourceBundle rb = rbClass.newInstance();
                        for (String msgKey : rb.keySet()) {
                            msgKeys.add(msgKey);
                        }
                        for (String msgKey : msgKeys) {
                            String msgText = rb.getString(msgKey);
                            println(out, msgKey + "=" + msgText);
                            println(out, "");
                        }
                    } finally {
                        if (out != null) out.close();
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            rc = 1;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            rc = 1;
        } catch (InstantiationException e) {
            e.printStackTrace();
            rc = 1;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            rc = 1;
        }
        return rc;
    }
    
    @SuppressWarnings("unchecked")
    private static List<Class<? extends ResourceBundle>> getResourceBundleClasses(JarFile jarFile, String filter, ClassLoader classLoader)
            throws ClassNotFoundException {
        
        List<Class<? extends ResourceBundle>> filteredClasses = new ArrayList<Class<? extends ResourceBundle>>();
        Enumeration<JarEntry> en = jarFile.entries();
        while (en.hasMoreElements()) {
            JarEntry entry = en.nextElement();
            String entryName = entry.getName();
            if ( Pattern.matches(filter, entryName) ) {
                String className = convertToClassName(entryName);
                if (className != null) {
                    Class<?> clazz = classLoader.loadClass(className);
                    if (ResourceBundle.class.isAssignableFrom(clazz)) {
                        filteredClasses.add((Class<? extends ResourceBundle>) clazz);
                    }
                }
                
            }
        }
        return filteredClasses;
    }
    
    private static String convertToClassName(String entryName) {
        String className = null;
        // valid classes must end with the ".class" extension
        
        if (entryName.endsWith(".class")) {
            int index = entryName.lastIndexOf(".class");
            className = entryName.substring(0, index);
            className = className.replaceAll("/", ".");
        }
        return className;
    }
    
    private static String convertPackageToDirName(Package pkg) {
        String dirName = pkg.getName();
        dirName = dirName.replace('.', '/');
        return dirName;
    }

    private static PrintWriter createOutputWriter(String outputDirPath, String packagePath, String fileName) throws IOException {
        File packageDirFile = new File(outputDirPath, packagePath);
        if (!packageDirFile.exists()) {
            packageDirFile.mkdirs();
        }
        File f = new File(packageDirFile, fileName);
        if (f.exists()) {
            File renameTo = new File(outputDirPath, fileName);
            int i = 1;
            while (renameTo.exists()) {
                renameTo = new File(outputDirPath, fileName + "." + i++);
            }
            f.renameTo(renameTo);
        }
        PrintWriter pw = new PrintWriter(f, "UTF-8");
        printHeader(pw, new File(packagePath, fileName));
        return pw;
    }
    
    private static void printHeader(PrintWriter out, File file) {
        println(out, "###############################################################################");
        println(out, "# Copyright (c) 2014 IBM Corporation and others.");
        println(out, "# All rights reserved. This program and the accompanying materials");
        println(out, "# are made available under the terms of the Eclipse Public License v1.0");
        println(out, "# which accompanies this distribution, and is available at");
        println(out, "# http://www.eclipse.org/legal/epl-v10.html");
        println(out, "#");
        println(out, "# Contributors:");
        println(out, "#     IBM Corporation - initial API and implementation");
        println(out, "###############################################################################");
        println(out, "# # {0} description of each insert field");
        println(out, "# MSG_DESCRIPTIVE_NAME_CWSJX0000=CWSJX0000I: This is a message with inserts {0}");
        println(out, "# MSG_DESCRIPTIVE_NAME_CWSJX0000.explanation=Explanation text for the message");
        println(out, "# MSG_DESCRIPTIVE_NAME_CWSJX0000.useraction=User action text for the message");
        println(out, "#");
        println(out, "#CMVCPATHNAME " + getPath(file)); //com.ibm.ws.kernel.feature/resources/com/ibm/ws/kernel/feature/internal/resources/ProvisionerMessages.nlsprops");
        println(out, "#COMPONENTPREFIX None");
        println(out, "#COMPONENTNAMEFOR None - generated from open source project");
        println(out, "#ISMESSAGEFILE TRUE");
        println(out, "#NLS_ENCODING=UNICODE");
        println(out, "#");
          println(out, "# NLS_MESSAGEFORMAT_ALL");
        println(out, "#");
        println(out, "#   Strings in this file which contain replacement variables are processed by the MessageFormat"); 
        println(out, "#   class (single quote must be coded as 2 consecutive single quotes ''). Strings in this file ");
        println(out, "#   which do NOT contain replacement variables are NOT processed by the MessageFormat class ");
        println(out, "#   (single quote must be coded as one single quote '). ");
        println(out, "#");
        println(out, "# -------------------------------------------------------------------------------------------------");
    }
    
    private static void println(PrintWriter out, String s) {
        out.print(s);
        out.print(NEW_LINE);
    }
    
    private static String getPath(File f) {
    	String path = f.getPath();
    	path = path.replace('\\', '/'); // avoid backslashes vs slashes on windows
    	return path;
    }
    
}
