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
package com.ibm.ws.persistence.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.ibm.websphere.persistence.mbean.DDLGenerationMBean;
import com.ibm.ws.kernel.service.util.UtilityTemplate;

/**
 * The basic outline of the flow is as follows:
 * <ol>
 * <li>If no arguments are specified, print usage.</li>
 * <li>If one argument is specified, and it is help, print general verbose help.</li>
 * <li>All other cases, invoke task.</li>
 * </ol>
 */
public class DDLGenerationUtility extends UtilityTemplate {

    static private final String SCRIPT_NAME = "ddlGen";
    static private final ResourceBundle messages = ResourceBundle.getBundle("com.ibm.ws.persistence.utility.resources.UtilityMessages");

    private final PrintStream stdout;
    private final PrintStream stderr;

    /**
     * Return code for invalid option or argument, as defined in this WIKI on January 13th, 2015. This must remain 20.
     * http://was.pok.ibm.com/xwiki/bin/view/Liberty/CommandLineUtilities
     */
    private static final int RC_INVALID_OPTION = 20;

    /**
     * Return code for server not found. This could happen for a number of reasons - the server really does not exist,
     * or some variable (WLP_USER_DIR) was not set correctly.
     */
    private static final int RC_SERVER_NOT_FOUND = 21;

    /** Return code for local connector URL not found. */
    private static final int RC_LOCAL_CONNECTOR_URL_NOT_FOUND = 22;

    /** Return code indicating the MBean was not found. */
    private static final int RC_MBEAN_NOT_FOUND = 23;

    /** Return code indicating the MBean reported a bad result. Caller should check the server's log for exceptions. */
    private static final int RC_MBEAN_INVALID_RESULT = 24;

    /**
     * Return code indicating that the WLP_OUTPUT_DIR/logs directory couldn't be found, likely because the environment value is set to
     * something other than what the server is using.
     */
    private static final int RC_SERVER_OUTPUT_NOT_FOUND = 25;

    /** Return code for unexpected errors. The message printed should be used to figure out what happened. */
    private static final int RC_UNEXPECTED_ERROR = 255;

    /**
     * CTOR.
     */
    protected DDLGenerationUtility() {
        this(System.out, System.err);
    }

    /**
     * CTOR.
     */
    private DDLGenerationUtility(PrintStream stdout, PrintStream stderr) {
        this.stdout = stdout;
        this.stderr = stderr;
    }

    /**
     * Get a formatted message.
     */
    private String getMessage(String key, Object... args) {
        String message = messages.getString(key);
        return (args.length == 0) ? message : MessageFormat.format(message, args);
    }

    /**
     * Print script usage.
     */
    private String getScriptUsage() {
        return getMessage("usage", SCRIPT_NAME, getMessage("action.all"));
    }

    private String getActions() {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessage("actions"));
        sb.append(getMessage("action-key.generate"));
        sb.append(getMessage("action-desc.generate"));
        sb.append(getMessage("action-key.help"));
        sb.append(getMessage("action-desc.help"));

        return sb.toString();
    }

    /**
     * Drive the logic of the program.
     *
     * @param args
     */
    protected int runProgram(String[] args) {
        // If no args, dump usage and exit.
        if (args == null || args.length == 0) {
            stdout.println(getScriptUsage());
            return 0;
        }

        String action = args[0];
        String actionTarget = args.length >= 2 ? args[1] : "";
        if (action == null || action.isEmpty() || action.toLowerCase().equals("help")) {
            if (actionTarget.isEmpty() || "help".equalsIgnoreCase(actionTarget)) {
                stdout.println(getScriptUsage());
                stdout.println(getActions());
                return 0;
            } else if ("generate".equalsIgnoreCase(actionTarget)) {
                stderr.println(getMessage("generate.help"));
                return 0;
            } else {
                stderr.println(getMessage("action.unrecognized", actionTarget));
                stdout.println(getScriptUsage());
                return RC_INVALID_OPTION;
            }
        }

        // Make sure we were provided a valid action. If not print help and return invalid
        if ("generate".equalsIgnoreCase(action) == false) {
            stderr.println(getMessage("action.unrecognized", action));
            stdout.println(getScriptUsage());
            return RC_INVALID_OPTION;
        }

        String userDir = getUserDir();
        // The server name should be the second argument.  Go find the server.  If we can't find the server, print the help text.
        String serverName = actionTarget;
        File serverDirectory = new File(userDir + File.separator + "servers" + File.separator + serverName);
        if (serverName.isEmpty() || (serverDirectory.exists() == false) || (serverDirectory.isDirectory() == false)) {
            stderr.println(getMessage("server.not.found", serverName, serverDirectory.getAbsolutePath()));
            stdout.println(getScriptUsage());
            return RC_SERVER_NOT_FOUND;
        }

        // Check the output dir.. If the value isn't the same as the value the server is using we won't find the logs directory
        File logsDir = new File(getOutputDir(serverName) + serverName + File.separator + "logs");
        if (!logsDir.exists()) {
            stderr.println(getMessage("server.output.logs.dir.not.found", serverName, logsDir.getAbsolutePath()));
            return RC_SERVER_OUTPUT_NOT_FOUND;
        }

        // The file containing the local connector URL is always in the
        // server's output directory.  Note that the output directory may be
        // different than the user directory, where the server config is.
        File jmxLocalConnectorUrlFile = new File(getOutputDir(serverName) + serverName + File.separator + "logs"
                                                 + File.separator + "state" + File.separator
                                                 + "/com.ibm.ws.jmx.local.address");
        if (jmxLocalConnectorUrlFile.exists() == false) {
            stderr.println(getMessage("local.connector.not.found", serverName));
            return RC_LOCAL_CONNECTOR_URL_NOT_FOUND;
        }

        JMXConnector connector = null;

        try {
            BufferedReader br = new BufferedReader(new FileReader(jmxLocalConnectorUrlFile));
            String urlString = br.readLine();
            br.close();

            if (urlString == null) {
                stderr.println(getMessage("local.connector.url.empty", serverName));
                return RC_UNEXPECTED_ERROR;
            }
            JMXServiceURL url = new JMXServiceURL(urlString);
            connector = JMXConnectorFactory.connect(url);

            MBeanServerConnection con = connector.getMBeanServerConnection();

            // Look up the DDL generation MBean
            ObjectName name = new ObjectName(DDLGenerationMBean.OBJECT_NAME);
            Set<ObjectInstance> objects = con.queryMBeans(name, null);
            if ((objects != null) && (objects.size() > 0)) {
                Iterator<ObjectInstance> i = objects.iterator();
                ObjectInstance o = i.next();
                ObjectName instanceName = o.getObjectName();
                @SuppressWarnings("unchecked")
                Map<String, Serializable> generateResults = (Map<String, Serializable>) con.invoke(instanceName, "generateDDL", new Object[] {}, new String[] {});

                // Check the results from the operation.
                if (generateResults == null) {
                    stderr.println(getMessage("mbean.null.result"));
                    return RC_MBEAN_INVALID_RESULT;
                }

                if (generateResults.containsKey(DDLGenerationMBean.SUCCESS)) {
                    Boolean success = (Boolean) generateResults.get(DDLGenerationMBean.SUCCESS);
                    if ((success == null) || (success.booleanValue() == false)) {
                        stderr.println(getMessage("mbean.bad.result"));
                        return RC_MBEAN_INVALID_RESULT;
                    }
                } else {
                    stderr.println(getMessage("mbean.missing.result"));
                    return RC_MBEAN_INVALID_RESULT;
                }

                String outputDirectory = null;
                if (generateResults.containsKey(DDLGenerationMBean.OUTPUT_DIRECTORY)) {
                    outputDirectory = (String) generateResults.get(DDLGenerationMBean.OUTPUT_DIRECTORY);
                    if ((outputDirectory == null) || (outputDirectory.length() <= 0)) {
                        stderr.println(getMessage("mbean.missing.output.dir"));
                        return RC_MBEAN_INVALID_RESULT;
                    } else {
                        stdout.println(getMessage("mbean.output.dir", outputDirectory));

                        if (generateResults.containsKey(DDLGenerationMBean.FILE_COUNT)) {
                            Integer fileCount = (Integer) generateResults.get(DDLGenerationMBean.FILE_COUNT);
                            if ((fileCount != null) && (fileCount.intValue() == 0)) {
                                stdout.println(getMessage("no.output.generated", serverName));
                            }
                        }
                    }
                } else {
                    stderr.println(getMessage("mbean.missing.output.dir"));
                    return RC_MBEAN_INVALID_RESULT;
                }
            } else {
                stderr.println(getMessage("mbean.not.found", serverName));
                return RC_MBEAN_NOT_FOUND;
            }
        } catch (Throwable t) {
            stderr.println(getMessage("exception.catch", t.toString()));
            t.printStackTrace(stderr);
            return RC_UNEXPECTED_ERROR;
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                    connector = null;
                } catch (Throwable t) {
                    /* Nothing... */
                }
            }
        }

        return 0;
    }

    /**
     * Main method, which wraps the instance logic and registers
     * the known tasks.
     *
     * @param args
     */
    public static void main(String[] args) {

        DDLGenerationUtility util = new DDLGenerationUtility();

        // No task handlers since this is a single function script.
        // At least it is right now.
        int rc = util.runProgram(args);

        System.exit(rc);
    }

}