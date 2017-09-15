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
package com.ibm.ws.webserver.plugin.utility.utils;

import com.ibm.ws.webserver.plugin.utility.utils.CommandUtils;
import com.ibm.ws.webserver.plugin.utility.utils.PluginUtilityConsole;

/**
 * This class will parse String <user>:<password>@<host>:<port>
 * 
 * @author anupag
 */
public class ParseLoginAddress{

	String inputAddress = null;
	//static final String MISSING_VALUE_MESSAGE = "missingValue";
	static final String INVALID_PORT_ARG_MESSAGE = "invalidPortArg";	
	static final String MISSING_USER_VALUE_MESSAGE = "missingUsernameValue"; //=Missing value for [user] in argument.
	static final String MISSING_PASS_VALUE_MESSAGE = "missingPasswordValue";//=Missing value for [password] in argument.
	static final String MISSING_HOST_VALUE_MESSAGE = "missingHostValue"; //=Missing value for [host] in argument.
	static final String MISSING_PORT_VALUE_MESSAGE = "missingPortValue"; //=Missing value for [port] in argument.
	static final String MISSING_HOSTPORT_VALUE_MESSAGE = "missingHostorPortValue"; //=Missing value for [host or port] in argument.

	// these part of address provided to connection

	String userName= null;
	String password= null;
	String host= null;
	String port= null;
	Boolean isLocal = true;
	String serverName = null;

	protected PluginUtilityConsole commandConsole;

	public ParseLoginAddress(){

	}

	public ParseLoginAddress(String input, PluginUtilityConsole commandConsole2) {
		this.inputAddress = input;	
		this.commandConsole = commandConsole2;
	}


	/**
	 * <user>:<password>@<host>:<port>
	 * 
	 * First ":" before last "@" delimits the user name and password.
	 * Last @ delimits user name and password from host and port.  @ is not a valid character for a host or port so that is good.
	 * 
	 * @param option
	 */
	public void parseLoginAddressValue(String option) throws IllegalArgumentException {

		//<user>:<password>@<host>:<port>
		int uphpdelimiter= inputAddress.lastIndexOf("@");

		if(uphpdelimiter >= 0){ // @found
			isLocal = false;
			String userpass = inputAddress.substring(0, uphpdelimiter);
			String hostport = inputAddress.substring(uphpdelimiter+1, inputAddress.length());

			if(userpass.length() > 1){				
				int userindex = userpass.indexOf(":");
				if (userindex >= 0 ) { // :found
					userName = userpass.substring(0, userindex);
					if(userName.length() == 0 ) {
						this.promptForUser(option);
					}
					password = userpass.substring(userindex +1 , userpass.length());
					if(password.length()==0){						 						
						this.promptForPassword(option);	
					} 
				}
				else{
					// : notfound
					this.promptForUser(option);
					this.promptForPassword(option);										
				}
			}
			else{
				this.promptForUser(option);
				this.promptForPassword(option);	
			}

			if(hostport.length() > 1){
				int hostindex = hostport.indexOf(":");
				if (hostindex >= 0 ) { // :found
					host = hostport.substring(0, hostindex);
					if(host.length() == 0 ) {
						// host not found
						this.throwIAE(MISSING_HOST_VALUE_MESSAGE, option);						
					}
				}
				else{
					// : not found
					this.throwIAE(MISSING_HOSTPORT_VALUE_MESSAGE, option);					
				}

				port = hostport.substring(hostindex +1 , hostport.length());
				if(port.length()==0){
					this.throwIAE(MISSING_PORT_VALUE_MESSAGE, option);					
				}				
				try {//Ensure port is a number
					Double.parseDouble(port);
				} catch (NumberFormatException e) {					
					throw new NumberFormatException(CommandUtils.getMessage(INVALID_PORT_ARG_MESSAGE, port, option));
				}
			}
			else{
				// @ found but no value after that
				this.throwIAE(MISSING_HOSTPORT_VALUE_MESSAGE, option);							
			}
		}
		else{// @ not found
			isLocal = true;
			serverName = inputAddress;
		}


	}

	public boolean isLocal() {
		return isLocal;
	}
	
	
	public String getServerName() {
		return serverName;
	}
	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @param host the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return the port
	 */
	public String getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(String port) {
		this.port = port;
	}

	/**
	 * @param option
	 */
	private void promptForUser(String option){
		//Prompt for user 
		userName = commandConsole.promptForUser("userName");
		if(userName == null) {
			throw new IllegalArgumentException(CommandUtils.getMessage(MISSING_USER_VALUE_MESSAGE, option));						
		}
	}

	/**
	 * @param option
	 */
	private void promptForPassword(String option){
		//Prompt for password 
		password = commandConsole.promptForPassword("password");
		if(password == null) {
			throw new IllegalArgumentException(CommandUtils.getMessage(MISSING_PASS_VALUE_MESSAGE, option));
		}
	}
	
	/**
	 * @param msg
	 * @param option
	 */
	private void throwIAE(String msg, String option) {
		throw new IllegalArgumentException(CommandUtils.getMessage(msg, option));
	}

}
