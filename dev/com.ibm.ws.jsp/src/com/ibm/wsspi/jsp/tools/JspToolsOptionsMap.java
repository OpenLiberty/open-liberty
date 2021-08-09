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

import java.util.HashMap;
import java.util.Properties;

/**
 * @author Scott Johnson
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class JspToolsOptionsMap extends HashMap {

    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3760846757378209584L;

	/**
     * @param props
     */
    public JspToolsOptionsMap(Properties props) {
        
        super(props);
    }

    /**
     * @param arg0
     * @param arg1
     */
    /**
     * 
     */
    public JspToolsOptionsMap() {
        super();
        // TODO Auto-generated constructor stub
    }

    public void addOption(JspToolsOptionKey key, Object value){
        put(key, value);
    }
}
