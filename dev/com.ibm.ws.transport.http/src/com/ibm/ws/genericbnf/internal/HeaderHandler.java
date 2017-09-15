/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.genericbnf.internal;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.genericbnf.HeaderStorage;

/**
 * Utility class for interacting with headers that take the form of delimiter
 * separated lists. Each element in the list could be just a 'value' or a
 * 'key=value' combination. For the key=value pairs, it also handles quoted
 * values such as [no-cache: "set-cookie, set-cookie2"]. That would be parsed
 * as one key entry for 'no-cache' and two sub-values.
 * 
 * This will parse starting values, and allow the caller to modify the list
 * by adding or removing values.
 * 
 */
public class HeaderHandler {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HeaderHandler.class, GenericConstants.GENERIC_TRACE_NAME, null);

    /** separator used by this list */
    private char mySep = ',';
    /** number of individual values found for the header */
    private int num_items = 0;
    /** storage of the key=value pairs */
    private Map<String, List<String>> values = new Hashtable<String, List<String>>(5);
    /** storage of the simple 'value' instances (no key) */
    private List<String> genericValues = new LinkedList<String>();
    /** Particular header instance for this handler */
    private String headerName = null;

    /**
     * Constructor based on the input message and the target header name.
     * 
     * @param msg
     * @param sep
     * @param name
     */
    public HeaderHandler(HeaderStorage msg, char sep, String name) {
        this.headerName = name;
        this.mySep = sep;
        if (msg.containsHeader(name)) {
            Iterator<HeaderField> it = msg.getHeaders(name).iterator();
            while (it.hasNext()) {
                parse(it.next().asString());
            }
        }
    }

    /**
     * Constructor based on the input message and the target header name.
     * 
     * @param msg
     * @param sep
     * @param name
     */
    public HeaderHandler(HeaderStorage msg, char sep, HeaderKeys name) {
        this.headerName = name.getName();
        this.mySep = sep;
        if (msg.containsHeader(name)) {
            Iterator<HeaderField> it = msg.getHeaders(name).iterator();
            while (it.hasNext()) {
                parse(it.next().asString());
            }
        }
    }

    /**
     * Add the given element to the generic no-key storage. The value must be
     * in lowercase form by the time of this call.
     * 
     * @param value - if null then nothing is stored
     */
    private void addElement(String value) {
        if (null == value) {
            return;
        }
        this.num_items++;
        this.genericValues.add(value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "addElement: " + value + " num: " + this.num_items);
        }
    }

    /**
     * Add the given key=value pair into storage. Both key and value must be
     * in lowercase form. If this key already exists, then this value will be
     * appended to the existing values.
     * 
     * @param key
     * @param value - if null then an empty string value is stored
     */
    private void addElement(String key, String value) {
        this.num_items++;
        List<String> vals = this.values.get(key);
        if (null == vals) {
            vals = new LinkedList<String>();
        }
        if (null == value) {
            vals.add("\"\"");
        } else {
            vals.add(value);
        }
        this.values.put(key, vals);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "addElement: " + key + "=" + value + " num: " + this.num_items);
        }
    }

    /**
     * Parse the input string for all value possibilities.
     * 
     * @param input
     */
    private void parse(String input) {
        char[] data = input.toCharArray();
        int start = 0;
        int hard_stop = data.length - 1;
        while (start < data.length) {
            if (this.mySep == data[start]) {
                start++;
                continue;
            }
            // look for the '=', end of data, or the separator
            int end = start;
            String key = null;
            boolean insideQuotes = false;
            while (end < data.length) {
                boolean extract = false;
                if ('"' == data[end]) {
                    insideQuotes = !insideQuotes;
                } else if (this.mySep == data[end]) {
                    extract = true;
                    end--;
                } else if ('=' == data[end]) {
                    // found a key
                    key = extractString(data, start, end - 1);
                    end++; // past the '='
                    start = end;
                    continue;
                }
                // if we're on the last character then always extract and quit
                if (end == hard_stop) {
                    extract = true;
                }

                // if we need to, extract the value and continue
                if (extract) {
                    String value = extractString(data, start, end);
                    if (null == key) {
                        addElement(value);
                    } else {
                        addElement(key, value);
                    }
                    // at this point, end is pointing to the last char of the val
                    end = end + 2; // jump past delim
                    start = end;
                    if (!insideQuotes) {
                        break; // out of while
                    }
                    continue;
                }
                end++;
            }
        }
    }

    /**
     * Extract a string from the input array based on the start and end markers.
     * This will strip off any leading and trailing white space or quotes.
     * 
     * @param data
     * @param start
     * @param end
     * @return String (lowercase converted)
     */
    private String extractString(char[] data, int start, int end) {
        // skip leading whitespace and quotes
        while (start < end &&
               (' ' == data[start] || '\t' == data[start] || '"' == data[start])) {
            start++;
        }
        // ignore trailing whitespace and quotes
        while (end >= start &&
               (' ' == data[end] || '\t' == data[end] || '"' == data[end])) {
            end--;
        }
        // check for nothing but whitespace
        if (end < start) {
            return null;
        }
        int len = end - start + 1;
        String rc = Normalizer.normalize(new String(data, start, len), Normalizer.NORMALIZE_LOWER);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "extractString: [" + rc + "]");
        }
        return rc;
    }

    /**
     * Add the generic value to this handler with no required key.
     * 
     * @param inputValue
     * @return boolean (true means success adding)
     */
    public boolean add(String inputValue) {
        String value = Normalizer.normalize(inputValue, Normalizer.NORMALIZE_LOWER);
        if (!contains(this.genericValues, value)) {
            addElement(value);
            return true;
        }
        return false;
    }

    /**
     * Add the given key=value pair to this handler.
     * 
     * @param inputKey
     * @param inputValue
     * @return boolean (true means success adding)
     */
    public boolean add(String inputKey, String inputValue) {
        String key = Normalizer.normalize(inputKey, Normalizer.NORMALIZE_LOWER);
        String value = Normalizer.normalize(inputValue, Normalizer.NORMALIZE_LOWER);
        if (!contains(this.values.get(key), value)) {
            addElement(key, value);
            return true;
        }
        return false;
    }

    /**
     * Remove the given item from the input list, if present, and update the
     * item counter appropriately.
     * 
     * @param list
     * @param item
     * @return boolean (true means removed)
     */
    private boolean remove(List<String> list, String item) {
        if (null != list) {
            if (list.remove(item)) {
                this.num_items--;
                return true;
            }
        }
        return false;
    }

    /**
     * Remove this specific key=value pair from storage. If this key exists with
     * other values, then those will not be touched, only the target value.
     * 
     * @param inputKey
     * @param inputValue
     * @return boolean (true means success removing)
     */
    public boolean remove(String inputKey, String inputValue) {
        String key = Normalizer.normalize(inputKey, Normalizer.NORMALIZE_LOWER);
        String value = Normalizer.normalize(inputValue, Normalizer.NORMALIZE_LOWER);
        boolean rc = remove(this.values.get(key), value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "remove: " + key + "=" + value + " rc=" + rc);
        }
        return rc;
    }

    /**
     * Remove the target value from the generic no-key storage.
     * 
     * @param inputValue
     * @return boolean (true means success removing)
     */
    public boolean remove(String inputValue) {
        String value = Normalizer.normalize(inputValue, Normalizer.NORMALIZE_LOWER);
        boolean rc = remove(this.genericValues, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "remove: " + value + " rc=" + rc);
        }
        return rc;
    }

    /**
     * Remove an entire key from storage, regardless of how many values it may
     * contain.
     * 
     * @param inputKey
     * @return int (number of items removed by this action)
     */
    public int removeKey(String inputKey) {
        String key = Normalizer.normalize(inputKey, Normalizer.NORMALIZE_LOWER);
        int num_removed = 0;
        List<String> vals = this.values.remove(key);
        if (null != vals) {
            num_removed = vals.size();
        }
        this.num_items -= num_removed;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "removeKey: key=" + key + " " + num_removed);
        }
        return num_removed;
    }

    /**
     * Query whether the target list contains the item.
     * 
     * @param list
     * @param item
     * @return boolean
     */
    private boolean contains(List<String> list, String item) {
        return (null == list) ? false : list.contains(item);
    }

    /**
     * Query whether the target value is contained in this handler.
     * 
     * @param inputValue
     * @return boolean
     */
    public boolean contains(String inputValue) {
        String value = Normalizer.normalize(inputValue, Normalizer.NORMALIZE_LOWER);
        boolean rc = contains(this.genericValues, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "contains: value [" + inputValue + "] rc=" + rc);
        }
        return rc;
    }

    /**
     * Query whether the target key exists with any values in this handler.
     * 
     * @param inputKey
     * @return boolean
     */
    public boolean containsKey(String inputKey) {
        String key = Normalizer.normalize(inputKey, Normalizer.NORMALIZE_LOWER);
        List<String> list = this.values.get(key);
        boolean rc = (null == list) ? false : !list.isEmpty();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "containsKey: key=" + inputKey + " rc=" + rc);
        }
        return rc;
    }

    /**
     * Query whether this specific key=value pair is contained in this handler.
     * 
     * @param inputKey
     * @param inputValue
     * @return boolean
     */
    public boolean contains(String inputKey, String inputValue) {
        String key = Normalizer.normalize(inputKey, Normalizer.NORMALIZE_LOWER);
        String value = Normalizer.normalize(inputValue, Normalizer.NORMALIZE_LOWER);
        boolean rc = contains(this.values.get(key), value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "contains: " + inputKey + "=" + inputValue + " rc=" + rc);
        }
        return rc;
    }

    /**
     * Access an iterator of all the no-key values found in this handler.
     * 
     * @return Iterator<String> (empty list of none present)
     */
    public Iterator<String> getValues() {
        return this.genericValues.iterator();
    }

    /**
     * Access an iterator of all values for the target key.
     * 
     * @param inputKey
     * @return Iterator (empty list if none present)
     */
    public Iterator<String> getValues(String inputKey) {
        String key = Normalizer.normalize(inputKey, Normalizer.NORMALIZE_LOWER);
        List<String> vals = this.values.get(key);
        if (null != vals) {
            return vals.iterator();
        }
        return new LinkedList<String>().iterator();
    }

    /**
     * Take the current data in this handler and create the properly formatted
     * string that would represent the header.
     * 
     * @return String (empty string if no values are present)
     */
    public String marshall() {
        if (0 == this.num_items) {
            return "";
        }
        boolean shouldPrepend = false;
        StringBuilder output = new StringBuilder(10 * this.num_items);
        // walk through the list of simple values (no key=value) first
        Iterator<String> i = this.genericValues.iterator();
        while (i.hasNext()) {
            if (shouldPrepend) {
                output.append(this.mySep);
                output.append(' ');
            }
            output.append(i.next());
            shouldPrepend = true;
        }
        // now walk through the list of key=value pairs, where value may actually
        // be multiple values
        i = this.values.keySet().iterator();
        while (i.hasNext()) {
            String key = i.next();
            List<String> vals = this.values.get(key);
            if (null == vals)
                continue;
            int size = vals.size();
            if (0 == size) {
                // ignore an empty key
                continue;
            }
            if (shouldPrepend) {
                output.append(this.mySep);
                output.append(' ');
            }
            output.append(key);
            output.append('=');
            if (1 == size) {
                output.append(vals.get(0));
            } else {
                // multiple values need to be quote wrapped
                shouldPrepend = false;
                output.append('"');
                for (int count = 0; count < size; count++) {
                    if (shouldPrepend) {
                        output.append(this.mySep);
                        output.append(' ');
                    }
                    output.append(vals.get(count));
                    shouldPrepend = true;
                }
                output.append('"');
            }
            shouldPrepend = true;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "marshalling [" + output.toString() + "]");
        }
        return output.toString();
    }

    /**
     * Query the number of items currently in storage for this handler.
     * 
     * @return int
     */
    public int numValues() {
        return this.num_items;
    }

    /**
     * Query the name of the header that this handler is wrapping.
     * 
     * @return String
     */
    public String getHeaderName() {
        return this.headerName;
    }

    /**
     * Clear everything out of storage for this handler.
     */
    public void clear() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Clearing this header handler: " + this);
        }
        this.num_items = 0;
        this.values.clear();
        this.genericValues.clear();
    }

    /**
     * Standard toString debug method.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return super.toString() + "; num items=" + numValues();
    }

}
