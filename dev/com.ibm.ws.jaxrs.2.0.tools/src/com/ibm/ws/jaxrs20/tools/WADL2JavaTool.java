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
package com.ibm.ws.jaxrs20.tools;


import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.tools.common.CommandInterfaceUtils;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.wadlto.WADLToJava;

import com.ibm.ws.jaxrs20.tools.internal.JaxRsToolsConstants;
import com.ibm.ws.jaxrs20.tools.internal.JaxRsToolsUtil;

public class WADL2JavaTool {
	private static final PrintStream err = System.err;
	private static final PrintStream out = System.out;
	 public static void main(String[] args) throws ClassNotFoundException, PrivilegedActionException {
 
	// check if version exists.		 
		if(JaxRsToolsUtil.isParamExists(Arrays.asList(args), Arrays.asList(JaxRsToolsConstants.PARAM_VERSION))) {
	        out.println(JaxRsToolsConstants.IBM_WADL_TOOL_VERSION);
	        System.exit(0);
		}	 

    
		 
	        AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
	            public Boolean run()
	                            throws Exception
	            {
	                System.setProperty(StaxUtils.ALLOW_INSECURE_PARSER, "1"); // sets the bla property without throwing an exception -> ok
	        		System.setProperty("javax.xml.accessExternalSchema", "all");
	                return Boolean.TRUE;
	            }
	        });
	        CommandInterfaceUtils.commandCommonMain();
	        WADLToJava w2j = new WADLToJava(args);
	        try {

	            w2j.run(new ToolContext());

	        } 
	        catch (ToolException ex) {
	            err.println();
	            err.println("WADLToJava Error: " + ex.getMessage());
	            err.println();
	            //IF verbose enabled
	            if(JaxRsToolsUtil.isParamExists(Arrays.asList(args), Arrays.asList(JaxRsToolsConstants.PARAM_VERBOSE))) {
	            	ex.printStackTrace();
	            }
	            System.exit(1);
	            
	        } 
	        catch (Exception ex) {
	            err.println("WADLToJava Error: " + ex.getMessage());
	            err.println();
	            System.exit(1);
	            //IF verbose enabled
	            if(JaxRsToolsUtil.isParamExists(Arrays.asList(args), Arrays.asList(JaxRsToolsConstants.PARAM_VERBOSE))) {
	            	ex.printStackTrace();
	            }

	        }

	       System.exit(0);

	 }
}
