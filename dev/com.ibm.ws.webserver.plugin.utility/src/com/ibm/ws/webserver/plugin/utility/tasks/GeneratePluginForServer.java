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

import com.ibm.ws.webserver.plugin.utility.utils.ParseLoginAddress;
import com.ibm.ws.webserver.plugin.utility.utils.PluginUtilityConsole;

/**
 * 
 * 
 * @author anupag
 */
public class GeneratePluginForServer{

	String inputAddress = null;
	String targetPath = null;

	protected PluginUtilityConsole commandConsole;

	public GeneratePluginForServer(){

	}

	public GeneratePluginForServer(String input, String targetPathValue, PluginUtilityConsole commandConsole2) {
		this.inputAddress = input;	
		this.commandConsole = commandConsole2;
		this.targetPath = targetPathValue;
	}

	/**
	 * Parse <user>:<password>@<host>:<port>
	 * 
	 * @return
	 * @throws IllegalArgumentException
	 */
	protected ParseLoginAddress parseServerAddressValue() throws IllegalArgumentException {

		ParseLoginAddress serverAddress = new ParseLoginAddress(this.inputAddress, this.commandConsole);
		// parse <user>:<password>@<host>:<port>
		serverAddress.parseLoginAddressValue("--server");
		
		return serverAddress;
	}
	
	public String getTargetPath() {
		return targetPath;
	}

	public void setTargetPath(String targetPath) {
		this.targetPath = targetPath;
	}
	
}
