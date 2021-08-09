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
package com.ibm.ws.jsp;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * @author Scott Johnson
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class BasicLogFormatter extends Formatter {

    private String lineSeparator = (String) java.security.AccessController.doPrivileged(
    	            new java.security.PrivilegedAction() {public Object run() 
    	            {return System.getProperty("line.separator");}});


    /* (non-Javadoc)
     * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
     */
    public String format(LogRecord record) {
        StringBuffer sb = new StringBuffer();
        String message = formatMessage(record);
        sb.append(message);
        sb.append(lineSeparator);
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
            sb.append(sw.toString());
            } catch (Exception ex) {
            	if (record.getThrown().getCause() != null){
					sb.append(record.getThrown().getCause().getLocalizedMessage());
            	}else{
					sb.append(record.getThrown().getLocalizedMessage());
            	}
            	
            }
        }
        return sb.toString();
    }

}
