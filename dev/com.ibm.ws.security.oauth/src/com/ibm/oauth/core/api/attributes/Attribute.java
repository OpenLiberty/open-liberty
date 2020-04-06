/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.api.attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * An <code>Attribute</code> consists of an attribute by name, type and list
 * of value(s). An attribute should be uniquely identified by its name and type.
 * The name is considered as required.
 */
public class Attribute {
    String _name;
    String _type;
    List<String> _values;

    /**
     * Construct an attribute based on it's name, type and an array of values.
     * Name should be non-null, type is optional and can be null. The values may
     * be empty/null.
     * 
     * @param name
     *            name of the attribute to construct
     * @param type
     *            type of attribute - typically a URN
     * @param values
     *            array of String values
     */
    public Attribute(String name, String type, String[] values) {
        init(name, type, values);
    }

    /**
     * Construct an attribute based on it's name, type and a single string
     * values. Name should be non-null, type is optional and can be null. The
     * values may be empty/null.
     * 
     * @param name
     *            name of the attribute to construct
     * @param type
     *            type of attribute - typically a URN
     * @param value
     *            single String value
     */
    public Attribute(String name, String type, String value) {
        init(name, type, new String[] { value });
    }

    /**
     * Returns the name of the attribute
     * 
     * @return name of attribute
     */
    public String getName() {
        return _name;
    }

    /**
     * Returns the attribute type string. May be null if no type was specified
     * 
     * @return type string for the attribute
     */
    public String getType() {
        return _type;
    }

    /**
     * The list of string values. This List is returned by reference so any
     * modifications to the list also modify the attribute.
     * 
     * @return reference to list of string values of the attribute. If the
     *         attribute has no values, an empty list is returned.
     */
    public List<String> getValues() {
        return _values;
    }

    /**
     * The string values as an immutable array.
     * 
     * @return array of string values of the attribute. If the attribute has no
     *         values, an empty array is returned.
     */
    public String[] getValuesArray() {
        return (String[]) _values.toArray(new String[_values.size()]);
    }

    /**
     * Returns the first string value for the attribute if any. If the list has
     * no values, null is returned.
     * 
     * @return
     */
    public String getValue() {
        String result = null;
        if (_values != null && _values.size() > 0) {
            result = _values.get(0);
        }
        return result;
    }

    /**
     * Resets the values of the attribute to the list of strings in the passed
     * array. All existing values are removed.
     * 
     * @param values
     *            new list of values for the attribute
     */
    public void setValues(String[] values) {
        _values.clear();
        init(_name, _type, values);
    }

    /**
     * Used internally to set and reset attribute members
     * 
     * @param name
     * @param type
     * @param values
     */
    private void init(String name, String type, String[] values) {
        _name = name;
        _type = type;
        _values = new ArrayList<String>();
        if (values != null && values.length > 0) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    _values.add(values[i]);
                }
            }
        }
    }

    /**
     * String representation of an attribute useful for debug tracing
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("{name: " + _name);
        sb.append(" type: " + _type);
        sb.append(" values: [");
        if (isSensitive(_name)) {
            sb.append("*****");
        } else {
            for (ListIterator<String> li = _values.listIterator(); li.hasNext();) {
                sb.append(li.next());
                if (li.hasNext()) {
                    sb.append(",");
                }
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    private boolean isSensitive(String name) {
        return "client_secret".equals(name);
    }
}
