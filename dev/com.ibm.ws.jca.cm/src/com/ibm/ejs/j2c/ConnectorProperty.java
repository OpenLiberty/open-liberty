/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import java.io.Serializable;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Each instance holds one configuration property for a deployed Resource Adapter
 * Scope : Name server and EJB server
 * 
 * Object model : 1 per deployed resource adapter
 * 
 */

public final class ConnectorProperty implements Serializable {

    private static final long serialVersionUID = -842064029887347417L;

    private static final TraceComponent tc = Tr.register(ConnectorProperty.class, J2CConstants.traceSpec, J2CConstants.messageFile); 

    private final String name;
    private final String type;
    private Object value;

    /**
     * Constructor
     * 
     * @param name Name of the property.
     * @param type Fully qualified type of the property (ie. java.lang.String)
     * @param value A string representation of the value, passed in from SM
     */
    public ConnectorProperty(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.setValue(value);
    }

    /**
     * Get method for name
     * 
     * @return name The name of the property
     */
    String getName() {
        return name;
    }

    /**
     * Get method for type.
     * 
     * @return fully qualified type
     */
    String getType() {
        return type;
    }

    /**
     * Get method for value. The object returned should be of the same type as
     * getType()
     * 
     * @return the value for the connector property, as an object.
     */
    Object getValue() {
        return value;
    }

    /**
     * Set method for value. This method news up this.value to the appropriate type, and initializes it to newValue.
     * 
     * @param newValue The value to set.
     */
    // should be identical to version 1.3, except newValue is of type Object.
    private void setValue(Object newValue) {
        try {
            if (this.type == null)
                value = newValue;
            else if (this.type.equals("java.lang.String"))
                value = newValue;
            else if (this.type.equals("java.lang.Boolean"))
                value = Boolean.valueOf((String) newValue);
            else if (this.type.equals("java.lang.Integer"))
                value = new Integer(((String) newValue));
            else if (this.type.equals("java.lang.Double"))
                value = new Double(((String) newValue));
            else if (this.type.equals("java.lang.Byte"))
                value = new Byte(((String) newValue));
            else if (this.type.equals("java.lang.Short"))
                value = new Short(((String) newValue));
            else if (this.type.equals("java.lang.Long"))
                value = new Long(((String) newValue));
            else if (this.type.equals("java.lang.Float"))
                value = new Float(((String) newValue));
            else if (this.type.equals("java.lang.Character")) 
                value = Character.valueOf(((String) newValue).charAt(0)); 
            else
                value = newValue;

        } catch (NumberFormatException nfe) {
            Tr.warning(tc, "INCOMPATIBLE_PROPERTY_TYPE_J2CA0207", new Object[] { name, type, newValue });
        }

    }

    /**
     * toString produces a reasonably readable formatted view of the Property
     * 
     * @return a string representation of the property.
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer(100);
        String nl = ConnectorProperties.nl; 

        buf.append("[Deployed Resource Adapter Property]");
        buf.append(nl);
        buf.append("\tName:  ");
        buf.append(this.name);
        buf.append("\tType:  ");
        buf.append(this.type);
        buf.append("\tValue: ");
        buf.append(this.value.toString());
        buf.append(nl);

        return buf.toString();
    }
}