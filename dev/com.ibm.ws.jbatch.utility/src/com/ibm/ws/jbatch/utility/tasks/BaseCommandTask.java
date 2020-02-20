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
package com.ibm.ws.jbatch.utility.tasks;

import java.util.Collections;
import java.util.List;

import com.ibm.ws.jbatch.utility.JBatchUtilityTask;
import com.ibm.ws.jbatch.utility.utils.ResourceBundleUtils;
import com.ibm.ws.jbatch.utility.utils.StringUtils;
import com.ibm.ws.jbatch.utility.utils.TaskIO;

/**
 * Common routines for command tasks.
 */
public abstract class BaseCommandTask implements JBatchUtilityTask {

    /**
     * The platform-dependent newline char.
     */
    protected final String NL = System.getProperty("line.separator");
    
    /**
     * The basename of the shell script associated with this task.
     */
    protected final String scriptName;
    
    /**
     * This task's task name.
     */
    protected final String taskName;
    
    /**
     * Wrapper around the three IO streams: stdin, stdout, stderr.
     */
    private TaskIO taskIO;
    
    /**
     * CTOR.
     */
    public BaseCommandTask(String taskName, String scriptName) {
        this.taskName = taskName;
        this.scriptName = scriptName;
    }

    /**
     * @return the formatted message from the resource bundle with the given key
     */
    protected String getMessage(String key, Object... args) {
        return ResourceBundleUtils.getMessage(key, args);
    }

    /**
     * @return the formatted option message from the resource bundle with the given key
     */
    protected String getOption(String key, Object... args) {
        return ResourceBundleUtils.getOption(key, args);
    }
    
    /**
     * @return task name
     */
    @Override
    public String getTaskName() {
        return taskName;
    }
    
    /**
     * @return task description
     */
    @Override
    public String getTaskDescription() {
        return getOption(getTaskName() + ".desc");
    }
    
    /**
     * @param taskIO - wrapper around stdin/stdout/stderr.
     */
    protected void setTaskIO(TaskIO taskIO) {
        this.taskIO = taskIO;
    }
    
    /**
     * @return the TaskIO wrapper around stdin/stdout/stderr.
     */
    protected TaskIO getTaskIO() {
        return taskIO;
    }
    
    /**
     * Constructs the options segment for a script's help screen for the given
     * optionKeyPrefix and optionDescPrefix.
     * 
     * @param optionKeyPrefix - e.g "connect.required-key."
     * @param optionDescPrefix - e.g "connect.required-desc."
     * 
     * @return formatted output String for the set of options
     */
    protected String buildOptionsMessage(String optionKeyPrefix, String optionDescPrefix) {
        StringBuilder scriptOptions = new StringBuilder();
        if ( !StringUtils.isEmpty(optionKeyPrefix) && !StringUtils.isEmpty(optionDescPrefix) ) {

            List<String> optionKeys = getNlsOptionKeys( optionKeyPrefix );

            if (optionKeys.size() > 0) {
                
                // Print each option and it's associated descriptive text
                for (String optionKey : optionKeys) {
                    String option = optionKey.substring(optionKeyPrefix.length());
                    scriptOptions.append(NL);
                    scriptOptions.append(ResourceBundleUtils.getOptions().getString(optionKey));
                    scriptOptions.append(NL);
                    scriptOptions.append(ResourceBundleUtils.getOptions().getString(optionDescPrefix + option));
                    scriptOptions.append(NL);
                }
            }
        }

        return scriptOptions.toString();
    }
    
    /**
     * @param nlsOptionKeyPrefix - e.g "connect.required-key."
     * 
     * @return the list of nls message keys (e.g "connect.required-key.--user") 
     *         that start with the given prefix.
     */
    public List<String> getNlsOptionKeys( String nlsOptionKeyPrefix ) {
        return StringUtils.filterPrefix( Collections.list(ResourceBundleUtils.getOptions().getKeys()), 
                                         nlsOptionKeyPrefix );
    }
    
    /**
     * @param nlsOptionKeyPrefix - e.g "connect.required-key."
     * 
     * @return a list of option names (e.g. "--user").  The option names are pulled from the
     *         nls message keys that start with the given prefix; i.e key={nlsOptionKeyPrefix}{optionName}
     */
    public List<String> getNlsOptionNames( String nlsOptionKeyPrefix ) {
        return StringUtils.trimPrefix( getNlsOptionKeys( nlsOptionKeyPrefix ), nlsOptionKeyPrefix );
    }
    

    /**
     * @return the formatted usage message.
     */
    protected String getUsage(String usageMsgKey, Object... args) {
        return getOption("global.usage")
                        + NL 
                        + getOption( usageMsgKey, args )
                        + NL ;
    }
        
    /**
     * @return the formatted description message.
     */
    protected String getDesc(String descMsgKey) {
        return getOption("global.description")
                + NL
                + getOption( descMsgKey )
                + NL;
    }

    /**
     * @return the formatted 'required options' message.
     */
    protected String collateRequiredOptions(List<String> formattedOptionMsgs) {
        
        StringBuilder sb = new StringBuilder().append( getOption("global.required") );
        
        for (String msg : formattedOptionMsgs) {
            sb.append(msg);
        }
        
        return sb.toString();
    }
    
    /**
     * @return the formatted 'optional options' message.
     */
    protected String collateOptionalOptions(List<String> formattedOptionMsgs) {
        
        StringBuilder sb = new StringBuilder().append( getOption("global.options") );
        
        for (String msg : formattedOptionMsgs) {
            sb.append(msg);
        }
        
        return sb.toString();
    }

    /**
     * @return all messages joined with a NL.
     */
    protected String joinMsgs(String... msgs) {
        StringBuilder sb = new StringBuilder();
        for (String msg : msgs) {
            sb.append(msg).append(NL);
        }
        return sb.toString();
    }

}
