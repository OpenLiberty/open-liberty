/*******************************************************************************
 * Copyright (c) 1997, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.tsx.db;

import java.util.Hashtable;

import com.ibm.ws.jsp.JspCoreException;

public class QueryRow {
    Hashtable row = null;
    /**
     * This method was created in VisualAge.
     */
    public QueryRow() {}
    /**
     * This method was created in VisualAge.
     * @param colCount int
     */
    protected QueryRow(int colCount) {
        row = new Hashtable(colCount);
    }
    /**
     * This method was created in VisualAge.
     * @return java.lang.String
     * @param propertyName java.lang.String
     */
    public String getValue(String propertyName) throws JspCoreException {
        // use current row 
        // get the value from hashtable and give it. Return null if property
        // not found ?? should we throw an error here ??
        String val = (String) row.get(propertyName);
        /*      if(val == null)
              {// invalid attribute
                 throw new JasperException((JspConstants.InvalidAttrName)+propertyName);
              }
              else
              {*/
        return val;
        //}
    }
    /**
     * This method was created in VisualAge.
     * @param name java.lang.String
     * @param val java.lang.String
     */
    protected void put(String name, String val) {
        if (val == null) {
            row.put(name, "");
        }
        else {
            row.put(name, val);
        }
    }
}
