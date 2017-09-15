/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.util;

import java.io.StringWriter;
import java.io.PrintWriter;

public class ExceptionUtility {

    //---------------------------------------------------------------------
    // Get stacktrace as string
    //---------------------------------------------------------------------
	static public String getStackTrace(Throwable oThrowable) {
		if (oThrowable == null)
			return null;
		StringWriter oStringWriter = new StringWriter();
		PrintWriter  oPrintWriter  = new PrintWriter(oStringWriter);
		oThrowable.printStackTrace(oPrintWriter);

		return oStringWriter.toString();
	}
}


