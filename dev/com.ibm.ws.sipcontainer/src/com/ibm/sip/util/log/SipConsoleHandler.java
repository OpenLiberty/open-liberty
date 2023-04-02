/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.sip.util.log;

import java.util.logging.ConsoleHandler;

/**
 * @author Nitzan, Jun 23, 2005
 * Use systemout instead of systemerr as the the outputstream of the console handler. 
 * To be used in the logger property file, on WAS versions that do not internaly support 
 * 1.4 JDK logger 
 */

public class SipConsoleHandler extends ConsoleHandler {
    /**
     * Ctor
     */
	public SipConsoleHandler(){
		super();
		setOutputStream( System.out);
	}
}
