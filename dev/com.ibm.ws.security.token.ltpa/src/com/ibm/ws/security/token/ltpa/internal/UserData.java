/*******************************************************************************
 * Copyright (c) 1997, 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.ws.common.internal.encoder.Base64Coder;

/**
 * Encapsulates the user data that gets stored in the LTPA Token.
 * At a minimum, this will contain accessId ("u");
 */
class UserData implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    private final String _userID;
    private final Map<String, ArrayList<String>> _attributes;
    private transient String _toString;

    /**
     * UserData constructor with an user identifier.
     *
     * @param userid An user identifier
     */
    protected UserData(String userid) {
        this._attributes = new HashMap<String, ArrayList<String>>(10);
        this._userID = userid;
        addAttributeShort("u", userid);
        this._toString = null;
    }

    /**
     * UserData constructor with a Map of construction information.
     *
     * @param A map of construction information
     */
    protected UserData(Map<String, ArrayList<String>> table) {
        this._attributes = table;
        ArrayList<String> accessIDArray = this._attributes.get("u");
        if (accessIDArray != null && accessIDArray.size() > 0) {
            this._userID = accessIDArray.get(0);
        } else {
            this._userID = null;
        }
        this._toString = null;
    }

    /**
     * Get the attribute value based on the named value. A string array
     * is returned containing all values of the attribute previously set.
     *
     * @param key The name of a attribute
     * @return A list of the attribute values corresponding with the specified key
     */
    protected final String[] getAttributes(String key) {
        ArrayList<String> array = this._attributes.get(key);
        if (array != null && array.size() > 0) {
            String[] type = new String[array.size()];
            return array.toArray(type);
        }
        return null;
    }

    /**
     * Add the attribute name/value pair to a String[] list of values for
     * the specified key. Once an attribute is set, it cannot only be
     * appended to but not overwritten. Returns the previous value(s)
     * set for key, not including the current value being set, or null
     * if not previously set.
     *
     * @param key The name of a attribute
     * @param value The value of the attribute
     * @return String[] A list of the attribute values previously binded with the
     *         specified key
     **/
    protected final String[] addAttribute(String key, String value) {
        this._toString = null;
        ArrayList<String> array = this._attributes.get(key);
        String[] old_array = null;
        if (array != null) {
            if (array.size() > 0) {
                old_array = new String[array.size()];
                old_array = array.toArray(old_array);
                if (key.equals("u")) {
                    /**
                     * Do not set multiple values for accessID
                     **/
                    return old_array;
                }
            }
        } else {
            array = new ArrayList<String>();
            this._attributes.put(key, array);
        }
        // Add the String to the array list
        array.add(value);
        return old_array;
    }

    /**
     * Add the attribute name/value pair to a String[] list of values for
     * the specified key. Once an attribute is set, it cannot only be
     * appended to but not overwritten. This method does not return any
     * values.
     *
     * @param key The name of a attribute
     * @param value The value of the attribute
     **/
    private final void addAttributeShort(String key, String value) {
        this._toString = null; // reset toString variable
        ArrayList<String> array = this._attributes.get(key);
        if (array != null) {
            if (array.size() > 0) {
                if (key.equals("u")) {
                    return;
                }
            }
        } else {
            array = new ArrayList<String>();
            this._attributes.put(key, array);
        }
        // Add the String to the array list
        array.add(value);
    }

    /**
     * Remove the list of attributes
     *
     * @param attributes The list of a attributes
     **/
    protected final void removeAttributes(String... attributes) {
        this._toString = null; // reset toString variable
        int i = 0;
        if (attributes != null) {
            while (i < attributes.length) {
                ArrayList<String> array = this._attributes.get(attributes[i]);
                if (array != null) {
                    this._attributes.remove(attributes[i]);
                }
                i++;
            }
        }
    }

    /**
     * Get the id (accessId) that is the value of the "id"
     *
     * @return The user identifier
     */
    protected final String getID() {
        return this._userID;
    }

    /**
     * Return the names of all attributes in the token.
     *
     * @return A list of the names of all attributes
     */
    protected final Enumeration<String> getAttributeNames() {
        return Collections.enumeration(this._attributes.keySet());
    }

    /**
     * Return string to bytes.
     *
     * @return The byte representation of the UserData
     */
    protected final byte[] toBytes() {
        return Base64Coder.getBytes(toString());
    }

    /**
     * Return the String form.
     *
     * @return The string representation of the UserData
     */
    @Override
    public final String toString() {
        if (this._toString == null) {
            this._toString = LTPATokenizer.createUserData(this._attributes);
        }
        return this._toString;
    }

    /**
     * Creates a copy of this userdata area
     *
     * @return A clone of the UserData
     */
    @Override
    public final Object clone() {
        Map<String, ArrayList<String>> table = new HashMap<String, ArrayList<String>>(this._attributes.size());

        for (Entry<String, ArrayList<String>> entry : this._attributes.entrySet()) {
            ArrayList<String> newList = new ArrayList<String>(entry.getValue());
            table.put(entry.getKey(), newList);
        }
        return new UserData(table);
    }

}
