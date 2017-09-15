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
package com.ibm.ws.webserver.plugin.utility.tasks;

import java.text.MessageFormat;
import java.util.Set;

import com.ibm.ws.product.utility.CommandTask;
import com.ibm.ws.product.utility.ExecutionContext;

public class HelpTask extends BasePluginConfigCommandTask {
	
	final String scriptName;
	
	ExecutionContext context;

	public HelpTask(String scriptName) {
		super(scriptName);
		this.scriptName = scriptName;
	}

	@Override
	public String getTaskName() {
		// TODO Auto-generated method stub
		return "help";
	}

	@Override
	public String getTaskHelp() {
		
		StringBuilder scriptHelp = new StringBuilder();
        scriptHelp.append(NL);
        scriptHelp.append(getMessage("usage",scriptName));
        scriptHelp.append(NL);
        scriptHelp.append(NL);
        scriptHelp.append(getOption("global.actions"));
        scriptHelp.append(NL);    
        for (CommandTask task : context.getCommandTaskRegistry().getCommandTasks()) {
        	if (!task.getTaskName().equals(getTaskName())) {
        		scriptHelp.append(NL);
        		scriptHelp.append(task.getTaskDescription());
                scriptHelp.append(NL);
             }
        }
        scriptHelp.append(getTaskDescription());
        scriptHelp.append(NL);
        scriptHelp.append(NL);
        scriptHelp.append(NL);

        return MessageFormat.format(scriptHelp.toString(), scriptName);
	}

	@Override
	public String getTaskDescription() {
		StringBuilder scriptHelp = new StringBuilder();
		String UsageOption = getOption("help.usage.options");
	    UsageOption = TAB + UsageOption.substring(5);
	    scriptHelp.append(NL);
        scriptHelp.append(UsageOption);
        scriptHelp.append(NL);
        scriptHelp.append(getOption("help.desc"));
        scriptHelp.append(NL);
        return scriptHelp.toString();
	}

	@Override
	public Set<String> getSupportedOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void execute(ExecutionContext context) {
		
		this.context = context;
		String[] args = context.getArguments();
				
		boolean helpProvided=false;
		
		if (args.length>0) {
			for (String taskName : args) {
		        for (CommandTask task : context.getCommandTaskRegistry().getCommandTasks()) {
			        if (task.getTaskName().equals(taskName) && !taskName.equals(getTaskName())) {
				        helpProvided=true;
			    	    context.getCommandConsole().printlnInfoMessage(task.getTaskHelp());
			        }	
			    }
			}    
		}
		
		if (!helpProvided) {
			context.getCommandConsole().printInfoMessage(getTaskHelp());
		}
		
	}

}
