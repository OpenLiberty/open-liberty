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

package com.ibm.ws390.sm.smfview;

/**
 * The main class to use to parse SMF records with this browser
 *
 */
public class SMF {

    private static ISmfFile m_file = null;
    private static SMFFilter filter = null;

    /**
     * The main method
     * 
     * @param args The standard input array of strings. May contain two strings consisting
     *                 of INFILE(input file name) and PLUGIN(class,parms).
     */
    public static void main(String[] args) {
        boolean foundINFILE = false;
        boolean foundPLUGIN = false;

        // No parms?  Asking for help?  
        if ((args.length == 0) ||
            ((args.length == 1) &&
             ((args[0].equalsIgnoreCase("-help")) ||
              (args[0].equalsIgnoreCase("-?"))))) {
            System.out.println("Specify INFILE(dataset.name) to process an SMF dump dataset");
            System.out.println("Specify PLUGIN(class,parms) to run an alternate plugin");
            System.out.println("The default is PLUGIN(DEFAULT,STDOUT");
            System.out.println("The plugin PERFSUM is also built into the browser");
            System.out.println("Additional documentation, license information, source code,");
            System.out.println("and javadoc may be found inside this .jar file");
            return;
        }
        for (int i = 0; i < args.length; ++i) {
            String parm = args[i];
            int openpren = parm.indexOf("(");
            int closepren = parm.indexOf(")");

            if ((openpren == -1) | (closepren == -1)) {
                System.out.println("Must specify keywords with values in parenthesis");
                return;
            }
            String keyword = parm.substring(0, openpren);
            String value = parm.substring(openpren + 1, closepren);
            if ((keyword == null) | (keyword.length() == 0)) {
                System.out.println("Must specify keywords with values in parenthesis");
                return;
            }
            if ((value == null) | value.length() == 0) {
                System.out.println("Keyword values must be greater than zero length");
                return;
            }

            if ((keyword.equalsIgnoreCase("INFILE")) & (foundINFILE == false)) {
                foundINFILE = true;

                try {
                    m_file = new JzOSSmfFile();
//	 m_file = new SmfFile();  Removed use of JRIO
                    m_file.open(value);
                } catch (Exception e) {
                    System.out.println(" Exception during open " + value);
                    System.out.println(" Exception data:\n" + e.toString());
                    return;
                }
            }

            if ((keyword.equalsIgnoreCase("PLUGIN")) & (foundPLUGIN == false)) {
                foundPLUGIN = true;
                try {
                    int commaloc = value.indexOf(",");
                    if (commaloc == -1) {
                        System.out.println("PLUGIN keyword requires class and parm string separated by comma");
                        return;
                    }
                    String classname = value.substring(0, commaloc);
                    String parmstring = value.substring(commaloc + 1);
                    if ((classname.length() == 0) | (parmstring.length() == 0)) {
                        System.out.println("classname and parm string must be non-zero length");
                        return;
                    }

                    if (classname.equals("DEFAULT"))
                        classname = "com.ibm.ws390.sm.smfview.DefaultFilter";
                    if (classname.equals("PERFSUM"))
                        classname = "com.ibm.ws390.sm.smfview.PerformanceSummary";
                    Class filterclass = Class.forName(classname);
                    filter = (SMFFilter) filterclass.newInstance();
                    boolean result = filter.initialize(parmstring);
                    if (result == false) {
                        System.out.println("plugin initialization failed..terminating");
                        return;
                    }
                } catch (Exception e) {
                    System.out.println("Exception loading class " + value);
                    System.out.println(e.toString());
                    return;
                }
            }
        } // end loop

        if (foundPLUGIN == false) {
            filter = new DefaultFilter();
            filter.initialize("STDOUT");
        }

        if (foundINFILE == false) {
            System.out.println("Must specify an input file via INFILE");
            return;
        }

        Interpreter.interpret(m_file, filter);

    }
}
