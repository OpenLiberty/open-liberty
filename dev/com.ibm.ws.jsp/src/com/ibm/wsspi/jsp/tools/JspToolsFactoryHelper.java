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
package com.ibm.wsspi.jsp.tools;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author kaspar
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class JspToolsFactoryHelper {

	static String TOOLS_FACTORY ="com.ibm.ws.jsp.tools.JspToolsFactoryImpl";
	
	static private Logger logger;
	private static final String CLASS_NAME="com.ibm.wsspi.jsp.tools.JspToolsFactoryHelper";
	static {
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}

    /**
     * 
     */
    public JspToolsFactoryHelper() {
        super();
    }

    public static JspToolsFactory getJspToolsFactory() {
        JspToolsFactory factory=null;
        try {
            factory = (JspToolsFactory) Class.forName(TOOLS_FACTORY).newInstance();
        }
        catch (ClassNotFoundException e) {
			logger.logp(Level.WARNING, CLASS_NAME, "getJspToolsFactory", "Failed to find class: "+ TOOLS_FACTORY , e);
        } catch (InstantiationException e) {
			logger.logp(Level.WARNING, CLASS_NAME, "getJspToolsFactory", "Failed to instantiate class: "+ TOOLS_FACTORY , e);
        } catch (IllegalAccessException e) {
			logger.logp(Level.WARNING, CLASS_NAME, "getJspToolsFactory", "Failed to access class: "+ TOOLS_FACTORY , e);
        }
        return factory; 
    }

}
