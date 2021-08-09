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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Set;

import javax.management.RuntimeMBeanException;

import com.ibm.ws.product.utility.ExecutionContext;
import com.ibm.ws.webserver.plugin.utility.utils.ParseLoginAddress;
import com.ibm.ws.webserver.plugin.utility.utils.PluginMBeanConnection;
import com.ibm.ws.webserver.plugin.utility.utils.PluginUtilityConsole;

/**
 * This will add generate option to pluginUtility script to get a plugin-cfg.xml for an application server.
 * 
 * generate --server=<user>:<password>@<host>:<port> --cluster=<clustername> --targetPath=<dir or file>
 * generate --server=<user>:<password>@<host>:<port> --targetPath=<dir or file>
 * 
 * Utility should prompt for userid and password if they are not specified.
 * Missing host and port value will fail the command
 * 
 * @author anupag
 */
public class GeneratePluginTask extends BasePluginConfigCommandTask{

	static final String ARG_CLUSTER_NAME = "--cluster";
	static final String ARG_SERVER_ADDRESS = "--server";
	static final String ARG_OPT_TARGET_PATH = "--targetPath";
	static final String MISSING_VALUE_MSG = "missingValue";

	static final String PLUGIN_CFG_FILE_NAME = "plugin-cfg.xml";
    static final String PLUGIN_CFG_FILE_DIR = File.separator+"logs"+File.separator+"state"+File.separator;

	
	protected PluginUtilityConsole commandConsole;
	protected PluginMBeanConnection connection;


	/**
	 * @param scriptName
	 * @param cmdConsole
	 */
	public GeneratePluginTask(String scriptName , PluginUtilityConsole cmdConsole) {

		super(scriptName); //"pluginUtility is scriptName"
		commandConsole = cmdConsole;
		connection = new PluginMBeanConnection(commandConsole);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.product.utility.CommandTask#getTaskName()
	 */
	@Override
	public String getTaskName() {
		return "generate";
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.product.utility.CommandTask#getTaskHelp()
	 */
	@Override
	public String getTaskHelp() {
		
		String[] Descs = {"GeneratePluginTask.required-option-desc.serverLoginAddress",
                          "GeneratePluginTask.required-option-desc.serverLoginAddressLocal1",
		                  "GeneratePluginTask.required-option-desc.serverLoginAddressLocal2",
				          "GeneratePluginTask.required-option-desc.serverLoginAddressRemote1",
				          "GeneratePluginTask.required-option-desc.serverLoginAddressRemote2",
				          "GeneratePluginTask.required-option-desc.serverLoginAddressRemote2.User",
				          "GeneratePluginTask.required-option-desc.serverLoginAddressRemote2.Password",
				          "GeneratePluginTask.required-option-desc.serverLoginAddressRemote2.Host",
				          "GeneratePluginTask.required-option-desc.serverLoginAddressRemote2.Port",};


		String footer = NL + getOption("global.options")
				+		buildScriptOptions("GeneratePluginTask.required-option-key.serverLoginAddress",Descs)
				+		buildScriptOptions("GeneratePluginTask.required-option-key.cluster",
						"GeneratePluginTask.required-option-desc.cluster")
				+       buildScriptOptions("GeneratePluginTask.optional-option-key.targetPath",
						"GeneratePluginTask.optional-option-desc.targetPath")
				+
				NL;

		return getTaskHelp("GeneratePluginTask.desc", "GeneratePluginTask.usage.options",
				null, null,
				null, footer,
				scriptName);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.product.utility.CommandTask#getTaskDescription()
	 */
	@Override
	public String getTaskDescription() {
		StringBuilder scriptHelp = new StringBuilder();
		String UseOption=getOption("GeneratePluginTask.usage.options");
		UseOption = TAB+UseOption.substring(5);
		scriptHelp.append(UseOption);
		scriptHelp.append(NL);
		scriptHelp.append(getOption("GeneratePluginTask.desc"));
		scriptHelp.append(NL);
		return scriptHelp.toString();
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.product.utility.CommandTask#getSupportedOptions()
	 */
	@Override
	public Set<String> getSupportedOptions() {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.product.utility.CommandTask#execute(com.ibm.ws.product.utility.ExecutionContext)
	 */
	@Override
	public void execute(ExecutionContext context){

		final String[] args = context.getArguments();
		
		String clusterValue = getArgumentValue(ARG_CLUSTER_NAME,args,null); 

		if(clusterValue != null){
			// Add all the arguments which need to be validated	
			this.createCollectiveArgumentsValues();
			this.validateArgumentList(args, true);
			
			String serverValues = getArgumentValue(ARG_SERVER_ADDRESS, args, null);
			if(serverValues == null){
				throw new IllegalArgumentException(getMessage(MISSING_VALUE_MSG , 
						"["+ARG_SERVER_ADDRESS + "]"));
			}

			String targetPathValue = getArgumentValue(ARG_OPT_TARGET_PATH, args, null);

			GeneratePluginForCollective collectiveTask = 
					new GeneratePluginForCollective(serverValues, clusterValue ,targetPathValue,commandConsole);
			ParseLoginAddress controllerAddress = null;
			try {
				controllerAddress = collectiveTask.parseCollectiveAddressValue();
			} catch (IllegalArgumentException e) {
				throw e;
			}		
			invokeGeneratePluginCfgMBean(controllerAddress, collectiveTask.getCluster(),targetPathValue,"collective");		
		}
		else{
			// Add all the arguments which need to be validated	
			this.createServerArgumentsValues();
			String serverValues = getArgumentValue(ARG_SERVER_ADDRESS, args, null);
			if(serverValues == null){
				throw new IllegalArgumentException(getMessage(MISSING_VALUE_MSG , 
						"["+ARG_SERVER_ADDRESS + "]" ));
			}

			this.validateArgumentList(args, true);

			String targetPathValue = getArgumentValue(ARG_OPT_TARGET_PATH, args, null);			
			GeneratePluginForServer serverTask = 
					new GeneratePluginForServer(serverValues, targetPathValue, commandConsole);
			ParseLoginAddress serverAddress = null;
			try {
				serverAddress = serverTask.parseServerAddressValue();
			} catch (IllegalArgumentException e) {
				throw e;
			}
			this.invokeGeneratePluginCfgMBean(serverAddress, null, targetPathValue,"server");
		}
		
	
	}


	/**
	 * Invokes MBean for generatePluginConfig to generate plugin configuration file
	 * 
	 * @param controllerHost
	 * @param controllerPort
	 * @param user
	 * @param password
	 * @param targetPath
	 * @return
	 */
	protected boolean invokeGeneratePluginCfgMBean(ParseLoginAddress loginAddress, String clusterName, String targetPath, String option) 
	{
		boolean success = false;
		try {
			success = connection.generatePluginConfig(loginAddress, clusterName, targetPath ,option);
			
			if (success)
				success = copyFileToTargetPath(loginAddress,clusterName,targetPath);

			if(success){
				
				if(option.equalsIgnoreCase("server")){
					commandConsole.printlnInfoMessage(getMessage("generateWebServerPluginTask.complete.server"));
				}
				else
					commandConsole.printlnInfoMessage(getMessage("generateWebServerPluginTask.complete.collective"));
			}

			else
				if(option.equalsIgnoreCase("server")){
					commandConsole.printlnInfoMessage(getMessage("generateWebServerPluginTask.fail.server"));
				}
				else
					commandConsole.printlnInfoMessage(getMessage("generateWebServerPluginTask.fail.collective"));


		} catch (RuntimeMBeanException e) {
			commandConsole.printlnErrorMessage(getMessage("common.connectionError", e.getMessage()));		

		} catch (UnknownHostException e) {
			// java.net.UnknownHostException: bad host
			commandConsole.printlnErrorMessage(getMessage("common.hostError", loginAddress.getHost()));

		} catch (ConnectException e) {
			// java.net.ConnectException: bad port
			commandConsole.printlnErrorMessage(getMessage("common.portError", loginAddress.getPort()));		

		} catch (IOException e) {
			// java.io.IOException: bad creds or some other IO error
			commandConsole.printlnErrorMessage(getMessage("common.connectionError", e.getMessage()));

		} catch (Exception e) {
			String msg = e.getMessage();
			if (msg != null) {
				//int idx = e.getMessage().indexOf(MBEAN_NOT_PRESENT_MSG_ID);
				//if (idx > -1) {
				commandConsole.printlnInfoMessage(getMessage("generateWebServerPluginTask.notEnabled"));
				//}
			}
			commandConsole.printlnErrorMessage(getMessage("common.connectionError", e.getMessage()));		
		} finally {
			try {
				connection.closeConnector();
			} catch (IOException e) {
				// java.io.IOException: some other IO error
				commandConsole.printlnErrorMessage(getMessage("common.connectionError", e.getMessage()));

			}
		}
		return success;
	}

	/**
	 * 
	 */
	private void createServerArgumentsValues(){
		//generate --server=<user>:<password>@<host>:<port> --targetPath=<dir or file>
		knownArgs.add(ARG_SERVER_ADDRESS);
		knownArgs.add(ARG_OPT_TARGET_PATH);
	}

	/**
	 * 
	 */
	private void createCollectiveArgumentsValues(){
		//generate --sever=<user>:<password>@<host>:<port> --cluster=<clustername> --targetPath=<dir or file>		
		knownArgs.add(ARG_CLUSTER_NAME);
		reqArgs.add(ARG_SERVER_ADDRESS);
		knownArgs.add(ARG_OPT_TARGET_PATH);
		knownArgs.addAll(reqArgs);
	}

	private boolean copyFileToTargetPath(ParseLoginAddress loginAddress, String clusterName, String tgtPath) throws Exception {
		
        String strTargetDir = System.getProperty("user.dir");
        String targetFileName = tgtPath;
        boolean result = true;
        
        
        String fileName=PLUGIN_CFG_FILE_NAME;
        
        if (clusterName!=null){
        	fileName=clusterName+"-"+fileName;
        }

        if (targetFileName == null) {
            targetFileName = strTargetDir + File.separator + fileName;
        } else {
            File targetPathFile = new File(targetFileName);
            if (targetPathFile.exists() && targetPathFile.isDirectory()) {
                targetFileName = targetFileName + File.separator +fileName;
            }
        }


        if (loginAddress.isLocal()) {
        	
        	try {
        	
        	    String generatedFileName = System.getenv("WLP_OUTPUT_DIR");
        	    generatedFileName += File.separator+loginAddress.getServerName();
        	    generatedFileName += PLUGIN_CFG_FILE_DIR;
        	    generatedFileName += fileName;
        	
        	    File generatedFile = new File(generatedFileName);
        	    if (generatedFile.exists() && !generatedFile.isDirectory()) {
        		    File targetFile = new File(targetFileName);
        		    if (targetFile.exists()) {
        			    if (!targetFile.delete()){
        			    	throw new Exception("Cannot delete existing target file : " + generatedFileName);
        			    }
        		    }
        		    copyFile(generatedFile,targetFile);
        	    } else {
        	    	commandConsole.printlnErrorMessage(getMessage("generateWebServerPluginTask.fail.server"));
        	    }
        	    
        	} catch (Exception exc) {
    			commandConsole.printlnErrorMessage(getMessage("error", exc.getMessage()));
    			result = false;
        	}    	
        	
        } else {
        	result = remoteCopy(clusterName, targetFileName);
        }
        
        return result;
        
	}
    private void copyFile(File sourceFile, File destFile) throws IOException {
        FileInputStream is = null;
        FileOutputStream os = null;
        try {
            is = new FileInputStream(sourceFile);
            os = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            if (is != null)
                is.close();
            if (os != null)
                os.close();
        }
    }

	private boolean remoteCopy(String clusterName,String targetFile) {
		return connection.remoteCopy(clusterName, targetFile);		
	}

}
