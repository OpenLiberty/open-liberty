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

import java.io.Console;
import java.io.PrintStream;

import com.ibm.ws.product.utility.DefaultCommandConsole;

public class PluginUtilityConsole extends DefaultCommandConsole {

	private final PrintStream stderr;
	private final PrintStream stdout;
	private final Console console;
	private ConsoleWrapper stdin;

	public PluginUtilityConsole(Console console, PrintStream stdout, PrintStream stderr) {
		super(console, stdout, stderr);
		this.stderr = stderr;
		this.stdout = stdout;
		this.stdin = new ConsoleWrapper(console,stderr);
		this.console = console;
	}

	public boolean isStandardOutAvailable() {
		return stdout!=null;
	}

	public boolean isStandardErrorAvailable() {
		return stderr!=null;
	}

	/**
	 * Prompt the user to enter text. 
	 *
	 * @return Entered String
	 */
	public String promptForUser(String arg) {

		String user = console.readLine(CommandUtils.getMessage("user.enterText", arg) + " ");
		return user;
	}

	/**
	 * Prompt the user to enter text. Prompts
	 * twice and compares to ensure it was entered correctly.
	 *
	 * @return Entered String
	 */
	public String promptForPassword(String arg) {

		char[] pass1 = console.readPassword(CommandUtils.getMessage("password.enterText", arg) + " ");		
		char[] pass2 = console.readPassword(CommandUtils.getMessage("password.reenterText", arg) + " ");
		
		if (pass1 == null && pass2 == null) {
            throw new IllegalArgumentException("Unable to read either entry. Aborting prompt.");
        }
		if (pass1 == null || pass2 == null) {
            stdout.println(CommandUtils.getMessage("password.readError"));
            return promptForPassword(arg);
        }
				
		String password1 =  String.valueOf(pass1);
		String password2 =  String.valueOf(pass2);
		
       if (password1.equals(password2)) {
            return password1;
        } else {
            stdout.println(CommandUtils.getMessage("password.entriesDidNotMatch"));
            return promptForPassword(arg);
        }		
	}

	public PrintStream getStdout() {
		return stdout;
	}

	public ConsoleWrapper getStdin() {
		return stdin;
	}
	
	public PrintStream getStderr() {
		return stderr;
	}

}
