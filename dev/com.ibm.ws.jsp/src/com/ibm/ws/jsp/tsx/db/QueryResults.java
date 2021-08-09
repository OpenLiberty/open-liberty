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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Vector;

import com.ibm.ws.jsp.JspCoreException;

public class QueryResults implements com.ibm.ws.jsp.tsx.TsxDynamicPropertyBeanIndex {
    // The query Results will be a vector Rows of hashtables. The vector will be
    // dynamically sized to the number of rows returned in the SQL resultset. 
    // Each Hashtable will correspond to a row. The name/value pairs in the hashtable
    // correspond to the name and values of the attibutes in the resultset.

    Vector rows = new Vector();
    // just a reference to be filled in in the constructor
    private int currRow = -1;
    private QueryRow currRowRef = null;
    /**
     * This method was created in VisualAge.
     */
    public QueryResults() {

        // do nothing
    }
    /**
     * This method was created in VisualAge.
     * @param rs java.sql.ResultSet
     */
    protected void compute(java.sql.ResultSet rs) throws SQLException {
        ResultSetMetaData rsmd;
        int numCols;
        // Zero element will be null always
        // This is to avoid any indexing problems.. 

        rsmd = rs.getMetaData();
        numCols = rsmd.getColumnCount();

        // so that we do not have to manage indexes
        // remember to subtract 1 from size of vector
        // vector indexes are valid from 1 to size-1
        while (rs.next()) {
            // process row
            QueryRow qr = new QueryRow(numCols);
            for (int i = 1; i <= numCols; i++) {
                String colVal = rs.getString(i);
                String colName = rsmd.getColumnLabel(i);
                qr.put(colName, colVal);

            } // for
            // put Hashtable reference into Vector
            rows.addElement(qr);
        } // while
        rows.trimToSize();
        //if (!isEmpty())
        //  {
        //  this.currRow=0;
        //  setCurrRowRef((QueryRow)rows.elementAt(this.currRow));
        //  }
    } // compute
    /**
     * This method was created in VisualAge.
     * Will return -1 if the the current row is not set
     * next() will set the current row to the next available row
     * @return int returns a row index
     * 
     * 
     */
    public int getCurrRow() {
        return this.currRow;
    }
    /**
     * This method was created in VisualAge.
     * @return QueryRow
     */
    protected QueryRow getCurrRowRef() throws JspCoreException {
        if (this.currRowRef == null) {
            throw new JspCoreException(JspConstants.CurrRowNotInit);
        }
        return currRowRef;
    }
    /**
     * This returns an enumeration of QueryRows
     * @return java.util.Enumeration
     */
    public Enumeration getRows() {
        return rows.elements();
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

        return (getCurrRowRef().getValue(propertyName));
    }
    /**
     * This method was created in VisualAge.
     * @return java.lang.String
     * @param propertyName java.lang.String
     */
    public String getValue(String propertyName, int index) throws JspCoreException {
        if (rows.size() > index) {
            QueryRow qr = (QueryRow) rows.elementAt(index);
            return (qr.getValue(propertyName));
        }
        else
            return (null);
    }
    /**
     * This method was created in VisualAge.
     * @return boolean
     */
    public boolean isEmpty() {
        return (size() == 0);
    }
    /**
     * This method was created in VisualAge.
     * @return boolean
     */
    public boolean next() throws JspCoreException {
        if (isEmpty()) // Query results is empty
            return false;
        if (this.currRow == size() - 1) // we are at the end 
            return false;
        setCurrRow(getCurrRow() + 1);
        return true;
    }
    /**
     * This method was created in VisualAge.
     * @param newValue int is a 0 relative row index
     */
    public void setCurrRow(int newValue) throws JspCoreException {
        int size = size();
        if ((newValue < 0) || (newValue >= size)) {
            throw new JspCoreException(JspConstants.InvalidRowIndex + String.valueOf(newValue) + String.valueOf(size - 1));
        }
        this.currRow = newValue;
        this.currRowRef = (QueryRow) rows.elementAt(newValue);
    }
    /**
     * This method was created in VisualAge.
     * @param newValue java.util.Hashtable
     */
    protected void setCurrRowRef(QueryRow qr) throws JspCoreException {
        if (qr == null) {
            throw new JspCoreException(JspConstants.InvalidCurrRowRef);
        }
        this.currRowRef = qr;
    }
    /**
     * This method was created in VisualAge.
     * @return int
     */
    public int size() {
        return (rows.size());
    }
}
