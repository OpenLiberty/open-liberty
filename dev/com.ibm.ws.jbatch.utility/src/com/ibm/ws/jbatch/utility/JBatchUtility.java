/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.utility;

import java.io.IOException;
import java.io.PrintStream;

import com.ibm.ws.jbatch.utility.tasks.GetJobLogTask;
import com.ibm.ws.jbatch.utility.tasks.HelpTask;
import com.ibm.ws.jbatch.utility.tasks.ListJobsTask;
import com.ibm.ws.jbatch.utility.tasks.PurgeTask;
import com.ibm.ws.jbatch.utility.tasks.RestartTask;
import com.ibm.ws.jbatch.utility.tasks.StatusTask;
import com.ibm.ws.jbatch.utility.tasks.StopTask;
import com.ibm.ws.jbatch.utility.tasks.SubmitTask;
import com.ibm.ws.jbatch.utility.utils.ArgumentRequiredException;
import com.ibm.ws.jbatch.utility.utils.ConsoleWrapper;
import com.ibm.ws.jbatch.utility.utils.InvalidArgumentValueException;
import com.ibm.ws.jbatch.utility.utils.ObjectUtils;
import com.ibm.ws.jbatch.utility.utils.ResourceBundleUtils;
import com.ibm.ws.jbatch.utility.utils.StringUtils;
import com.ibm.ws.jbatch.utility.utils.TaskIO;
import com.ibm.ws.jbatch.utility.utils.TaskList;
import com.ibm.ws.jbatch.utility.utils.UnrecognizedArgumentException;

/**
 * The basic outline of the flow is as follows:
 * <ol>
 * <li>If no arguments are specified, print usage.</li>
 * <li>If one argument is specified, and it is help, print general verbose help.</li>
 * <li>If two or more arguments are specified, if task name is help, print
 * verbose help for 2nd argument (expected to be task name).</li>
 * <li>If the task name is not known, print usage.</li>
 * <li>All other cases, invoke task.</li>
 * </ol>
 */
public class JBatchUtility {

    /**
     * TODO: can we avoid hard-coding the script name like this?
     */
    static final String SCRIPT_NAME = "batchManager";
    
    private final ConsoleWrapper stdin;
    private final PrintStream stdout;
    private final PrintStream stderr;
    
    /**
     * The set of tasks registered with this utility.
     */
    private TaskList tasks = new TaskList();

    /**
     * CTOR.
     */
    protected JBatchUtility() {
        this(new ConsoleWrapper(System.console(), System.err),
             System.out,
             System.err);
    }
    
    /**
     * CTOR.
     */
    private JBatchUtility(ConsoleWrapper stdin, PrintStream stdout, PrintStream stderr) {
        this.stdin = stdin;
        this.stdout = stdout;
        this.stderr = stderr;
        
        // The help task is always registered.
        registerTask(new HelpTask(SCRIPT_NAME, tasks));
    }

    /**
     * Register a task into the CollectiveUtility script.
     * The order in which the tasks are registered will
     * affect the usage statement.
     * 
     * @param task
     * 
     * @return true
     */
    private boolean registerTask(JBatchUtilityTask task) {
        return tasks.add(task);
    }
    
    /**
     * @return the task with the given name.
     */
    private JBatchUtilityTask getTask(String name) {
        return tasks.forName(name);
    }

    /**
     * @return the HelpTask
     */
    private HelpTask getHelpTask() {
        return (HelpTask) getTask("help");
    }

    /**
     * Drive the logic of the program.
     * 
     * @param args
     */
    protected int runProgram(String[] args) {
        
        // Verify input/output.
        if (stdin == null) {
            stderr.println(ResourceBundleUtils.getMessage("error.missingIO", "stdin"));
            return 254;
        }
        if (stdout == null) {
            stderr.println(ResourceBundleUtils.getMessage("error.missingIO", "stdout"));
            return 253;
        }
        if (stderr == null) {
            stdout.println(ResourceBundleUtils.getMessage("error.missingIO", "stderr"));
            return 252;
        }

        // If no args, dump help and exit.
        if (args.length == 0) {
            stdout.println(getHelpTask().getScriptUsage());
            return 0;
        }
        
        // Massage the task name if it looks similar to "help".
        if (looksLikeHelp(args[0])) {
            args[0] = getHelpTask().getTaskName();
        }

        // Lookup the requested task.  Bail if not found.
        JBatchUtilityTask task = getTask(args[0]);
        if (task == null) {
            stderr.println(ResourceBundleUtils.getMessage("task.unknown", args[0]));
            stderr.println(getHelpTask().getScriptUsage());
            return 0;
        } 
            
        // Run the task.
        try {
            return task.handleTask(stdin, stdout, stderr, args);
        } catch (ArgumentRequiredException are) {
            return handleArgumentRequiredException(task, are);
           
        } catch(UnrecognizedArgumentException ure) {
            return handleUnrecognizedArgumentException(task, ure);
            
        } catch(InvalidArgumentValueException iave) {
            return handleInvalidArgumentValueException(task, iave);
            
        } catch(IOException ioe){
        	return handleIOException(ioe);
        	
        } catch (Exception e) {
            return handleException(e);
        }

    }
    
    /**
     * @return Print out the exception stacktrace and return 255
     */
    private int handleException(Exception e) {
        stderr.println();
        stderr.println(TaskIO.getTimestamp() + " " + ResourceBundleUtils.getMessage("error", ObjectUtils.firstNonNull( e.getMessage(), "")));
        e.printStackTrace(stderr);
        return 255;
    }
    
    /**
     * Prints out the argument.invalid.value message along with the task usage
     * 
     * @return 22
     */
    private int handleInvalidArgumentValueException(JBatchUtilityTask task, InvalidArgumentValueException iave) {
        stderr.println();
        stderr.println(ResourceBundleUtils.getMessage("argument.invalid.value", 
                                                      iave.getArgName(),
                                                      iave.getInvalidValue(),
                                                      iave.getPermittedValues() ));
        stderr.println(ResourceBundleUtils.getMessage("for.task.usage", SCRIPT_NAME, "help", task.getTaskName()));
 
        return 22;
    }

    /**
     * Prints out the argument.required message along with the task usage
     * 
     * @return 20
     */
    protected int handleArgumentRequiredException(JBatchUtilityTask task, ArgumentRequiredException are) {
        stderr.println();
        stderr.println(ResourceBundleUtils.getMessage("argument.required", are.getArgName()));
        stderr.println(ResourceBundleUtils.getMessage("for.task.usage", SCRIPT_NAME, "help", task.getTaskName()));
        return 20;
    }

    /**
     * Prints out the argument.unrecognized message along with the task usage
     * 
     * @return 21
     */
    protected int handleUnrecognizedArgumentException(JBatchUtilityTask task, UnrecognizedArgumentException ure) {
        stderr.println();
        if ( StringUtils.isEmpty(ure.getExpectedArg()) ) {
            stderr.println(ResourceBundleUtils.getMessage("argument.unrecognized", ure.getUnrecognizedArg() ));
        } else {
            stderr.println(ResourceBundleUtils.getMessage("argument.unrecognized.expected", 
                                                          ure.getUnrecognizedArg(),
                                                          ure.getExpectedArg()));
        }
        stderr.println(ResourceBundleUtils.getMessage("for.task.usage", SCRIPT_NAME, "help", task.getTaskName()));
        return 21;
    }
    
    /**
     * Prints out the jbatch.rest CWWKY0151E message if found
     * 
     * @return 23
     */
    protected int handleIOException(IOException ioe) {
        //If the CWWKY0151E message is part of the message print it, otherwise handle as a normal exception
    	//the CWWKY0151E message occurs when a status request is entered with an invalid jobInstanceID
    	if (ioe.getMessage().contains("CWWKY0151E")){
	    	stderr.println();
	        stderr.println(ioe.getMessage());
	        return 23;
        
        } else { 
        	return handleException(ioe);
        }
    }
    
    /**
     * strip off any leading punctuation or other noise, see if the rest appears to be a "help" request.
     * note that the string is already trim()'d by command-line parsing unless user explicitly escaped a space 
     */
    protected boolean looksLikeHelp(String taskname) {
        return getHelpTask().getTaskName().equalsIgnoreCase(stripLeadingNonLetters(taskname) );
    }
    
    /**
     * @return the given string, with any leading punctuation or other non-letter noise stripped away.
     */
    protected String stripLeadingNonLetters(String str) {
        if (str == null) {
            return "";
        }
        
        int start = 0, len = str.length();
        while (start < len && !Character.isLetter(str.charAt(start))) {
            ++start;
        }
        
        return str.substring(start);
    }

    /**
     * Main method, which wraps the instance logic and registers
     * the known tasks.
     * 
     * @param args
     */
    public static void main(String[] args) {

        JBatchUtility util = new JBatchUtility();
        
        // Register task handlers
        util.registerTask(new SubmitTask(SCRIPT_NAME));
        util.registerTask(new StopTask(SCRIPT_NAME));
        util.registerTask(new RestartTask(SCRIPT_NAME));
        util.registerTask(new StatusTask(SCRIPT_NAME));
        util.registerTask(new GetJobLogTask(SCRIPT_NAME));
        util.registerTask(new ListJobsTask(SCRIPT_NAME));
        util.registerTask(new PurgeTask(SCRIPT_NAME));
        
        // Process the command
        int rc = util.runProgram(args);
        
        System.exit(rc);
    }
}
