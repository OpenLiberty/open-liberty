/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webserver.plugin.utility.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.ibm.websphere.filetransfer.FileTransferMBean;
import com.ibm.ws.webserver.plugin.runtime.interfaces.PluginConfigRequester;

/**
 * Helper class to generate JMXconnection
 * 
 * @author anupag
 *
 */
public class PluginMBeanConnection extends CommonMBeanConnection {

	/**
	 * PluginConfigMBean Object Name
	 */
	static final String PLUGIN_CONFIG_MBEAN = PluginConfigRequester.OBJECT_NAME;
	
	/**
	 * FileTransferMBean Object Name
	 */
	static final String FILE_TRANSFER_MBEAN = FileTransferMBean.OBJECT_NAME;
	
	private static final String CONNECTOR_ADDRESS_FILE_NAME = "com.ibm.ws.jmx.local.address";


	private final ObjectName objectName;
    private final PluginUtilityConsole console;
    JMXConnector connector = null;
    MBeanServerConnection mbsc = null;

	public PluginMBeanConnection(PluginUtilityConsole console) {	
		super(console.getStdin(),console.getStderr());
		
		this.console = console;

		try {
			this.objectName = new ObjectName(PLUGIN_CONFIG_MBEAN);
		} catch (MalformedObjectNameException ex) {
			throw new IllegalStateException("Staticly defined MBean name " + PLUGIN_CONFIG_MBEAN + " was not valid...", ex);
		}
	}


	public boolean generatePluginConfig(ParseLoginAddress loginAddress, String clusterName, String targetPath, String option) throws Exception {
		
		if (loginAddress.isLocal()){
			console.printlnInfoMessage(CommandUtils.getMessage("generateWebServerPluginTask.start.server.local",loginAddress.getServerName()));
			connector = getLocalJMXConnection(loginAddress.getServerName());  
		} else {
			console.printlnInfoMessage(CommandUtils.getMessage("generateWebServerPluginTask.start.server.remote",loginAddress.getHost()+":"+loginAddress.getPort()));
		    connector = getJMXConnector(loginAddress.getHost(), Integer.parseInt(loginAddress.getPort()), 
		    		loginAddress.getUserName(), loginAddress.getPassword());
		}    
		mbsc = connector.getMBeanServerConnection();
		boolean success = false;
		if(mbsc != null){
			if(option.equalsIgnoreCase("collective")) {
				Object[] params = new Object[] { clusterName };
				String[] signature = new String[] { "java.lang.String"};
					
				success = (Boolean) mbsc.invoke(objectName, "generateClusterPlugin", params, signature);
			}
			else{
				Object[] params = new Object[] {};
				String[] signature = new String[] {};

				success = (Boolean) mbsc.invoke(objectName, "generateAppServerPlugin", params, signature);
			}
		}		

		return success;

	}


	private JMXConnector getLocalJMXConnection(String serverName) throws IOException {
		
		   JMXConnector localConnector=null;
		   
		   				   		
		   String serverOutputDir = System.getenv("WLP_OUTPUT_DIR");
	       String serverRoot = serverOutputDir + File.separator + serverName + File.separator;

	        if (serverName == null || serverName.length() == 0) {
	            throw new IOException(CommandUtils.getMessage("serverNotFound", serverName, serverOutputDir));
	        }
	        
	        serverRoot = serverRoot.replaceAll("\\\\", "/");
	        String connectorFile = serverRoot + "logs"+ File.separator + "state"+ File.separator + CONNECTOR_ADDRESS_FILE_NAME;

	        File file = new File(connectorFile);
	        if (file.exists()) {
	            String connectorAddr = null;
	            BufferedReader br = null;
	            try {
	                br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
	                connectorAddr = br.readLine();
	            } catch (IOException e) {
	                throw new IOException(CommandUtils.getMessage("jmx.local.connector.file.invalid", file.getAbsolutePath()));
	            } finally {
	                try {
	                    if (br != null) {
	                        br.close();
	                    }
	                } catch (IOException e) {
	                    // ignore
	                }
	            }
	            if (connectorAddr != null) {
	                JMXServiceURL url = new JMXServiceURL(connectorAddr);
	                localConnector = JMXConnectorFactory.connect(url);
	            } else {
	                throw new IOException(CommandUtils.getMessage("jmx.local.connector.file.invalid", file.getAbsolutePath()));
	            }
	        } else {
	            throw new IOException(CommandUtils.getMessage("jmx.local.connector.file.notfound", file.getAbsolutePath()));
	        }

		return localConnector;
	}
	
	public boolean remoteCopy(String clusterName, String targetPath) {
		boolean success = false;
		ObjectName fileTransferObjectName;
	
		try {
			fileTransferObjectName = new ObjectName(FILE_TRANSFER_MBEAN);

			// ${server.output.dir} should point to the connected controller directory
			String sourceFile = "${server.output.dir}"+ File.separator + "logs"+ File.separator + "state"+ File.separator;
			if (clusterName != null) {
				sourceFile += clusterName + "-";
			}
			sourceFile += "plugin-cfg.xml";

			String[] signature = { "java.lang.String", "java.lang.String" };
			Object[] params = { sourceFile, targetPath };

			mbsc.invoke(fileTransferObjectName, "downloadFile", params, signature);

			//check to see if the file was copied over
			File copiedFile = new File(targetPath);
			if (copiedFile.exists()) {
				success = true;
			}
		} catch (MalformedObjectNameException ex) {
			throw new IllegalStateException("Staticly defined MBean name " + FILE_TRANSFER_MBEAN + " was not valid...", ex);
		} catch (Exception e) {
			console.printlnErrorMessage(CommandUtils.getMessage("error", e.getMessage()));			
			success = false;
		}

		return success;
	}

	public void closeConnector() throws IOException {
		if (connector != null) {
			connector.close();
		}
	}

}
