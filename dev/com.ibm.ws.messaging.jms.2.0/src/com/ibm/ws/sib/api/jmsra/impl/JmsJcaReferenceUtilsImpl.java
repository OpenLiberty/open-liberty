/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.api.jmsra.impl;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.naming.Reference;
import javax.naming.StringRefAddr;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.api.jms.JmsInternalConstants;
import com.ibm.ws.sib.api.jms.StringArrayWrapper;
import com.ibm.ws.sib.api.jmsra.JmsJcaReferenceUtils;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Implementation of utility class for processing refereces to JMS resources.
 */
public class JmsJcaReferenceUtilsImpl extends JmsJcaReferenceUtils {

    private static TraceComponent TRACE = SibTr.register(
            JmsJcaReferenceUtilsImpl.class, JmsraConstants.MSG_GROUP,
            JmsraConstants.MSG_BUNDLE);


    private static final String FFDC_PROBE_1 = "1";

    private static final String PREFIX_NULL = "NULL";

    private static final String PREFIX_BOOLEAN = "BOOL";

    private static final String PREFIX_INT = "INT";

    private static final String PREFIX_BYTE = "BYTE";

    private static final String PREFIX_SHORT = "SHORT";

    private static final String PREFIX_STRING = "STRING";

    private static final String PREFIX_FLOAT = "FLOAT";

    private static final String PREFIX_DOUBLE = "DOUBLE";

    private static final String PREFIX_LONG = "LONG";

    private static final String PREFIX_ROUTING_PATH = "ROUTINGPATH";

    private static final String PREFIX_SEPARATOR = "_";

    /**
     * This table maps between the various data types supported as values for
     * properties, and the equivalent string prefix for use when storing as a
     * reference. It is lazily initialized by the populateReference method when
     * required.
     */
    private static Map<Class,String> prefixTable = null;

    /**
     * Populates the prefix table for use when creating a Reference to a
     * ConnFactory. Creates a map between supported data types for properties
     * and the prefix used to store them.
     */
    public void populatePrefixTable() {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(TRACE, "populatePrefixTable");
        }

        // Create the storage.
        prefixTable = new HashMap<Class,String>();

        // Populate with the supported (non-null) data types.
        prefixTable.put(Boolean.class, PREFIX_BOOLEAN);
        prefixTable.put(Integer.class, PREFIX_INT);
        prefixTable.put(Byte.class, PREFIX_BYTE);
        prefixTable.put(Short.class, PREFIX_SHORT);
        prefixTable.put(String.class, PREFIX_STRING);
        prefixTable.put(Float.class, PREFIX_FLOAT);
        prefixTable.put(Double.class, PREFIX_DOUBLE);
        prefixTable.put(Long.class, PREFIX_LONG);
        prefixTable.put(StringArrayWrapper.class, PREFIX_ROUTING_PATH);

        
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(TRACE, "populatePrefixTable");
        }

    }

    /**
     * Takes a map containing a set of properties, the keys of which are
     * Strings, and the values of which are immutable objects (for instance
     * primitive wrapper classes), and returns a string encoded version of the
     * map in which the keys are the original keys prefixed with the data type,
     * and the values are string representations of the original values, but doesn't
     * include the properties in the defaultProperties map AND have that default value
     * 
     * @param raw
     *            the map to encode
     * @param defaults
     *            the default set of properties to be used (if properties in raw are set to 
     *            these defaults, they will be omitted from the encoded map)
     * @return the encoded map
     */
    public Map<String,String> getStringEncodedMap(final Map raw, final Map defaults) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getStringEncodedMap", new Object[] {raw,defaults});
        }

        final Map<String,String> encodedMap = new HashMap<String,String>();
        final Iterator propKeys = raw.keySet().iterator();

        // Look at each key in turn.
        while (propKeys.hasNext()) {

            String key = (String) propKeys.next();

            // Only store non-null keys.
            if (key != null) {

                // Retrieve the value part of the map.
                Object val = raw.get(key);

                // Initialize the lookup table if necessary.
                if (prefixTable == null) {
                    populatePrefixTable();
                }

                // Should we skip this property
                if (defaults.containsKey(key) && defaults.get(key) != null && defaults.get(key).equals(val))
                  continue;
                
                String prefix = null;
                String strForm = null;

                // Work out the correct class type and prefix, taking care of
                // null
                // value.
                if (val == null) {
                    prefix = PREFIX_NULL;
                    strForm = null;

                } else {

                    // Look up the prefix based on the class
                    prefix = prefixTable.get(val.getClass());

                    if (prefix == null) {

                        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                            SibTr.debug(TRACE,
                                    "unsupported type for property: "
                                            + val.getClass().getName());
                        }

                        // Ignore the unsupported type.
                        continue;

                    }//if prefix found.

                    // We know the object is not null, so convert it to a String
                    strForm = val.toString();

                }//if val null

                // Prepend the prefix to the key
                String prefixedKey = prefix + PREFIX_SEPARATOR + key;

                encodedMap.put(prefixedKey, strForm);

                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                    SibTr.debug(TRACE, "encoded: " + prefixedKey + " = '"
                            + strForm + "'");
                }

            }// if key not null

        }//while

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getStringEncodedMap", encodedMap);
        }
        return encodedMap;

    }

    /**
     * Turns a Map encoded in the form described above into a decoded map in
     * which the keys are the actual key names that were previously encoded, and
     * the values are the primitive wrapper objects that previously represented
     * the state.
     * 
     * @param encodedMap
     *            the map to decode
     * @param defaults
     *            the default set of properties to be used (those in the encoded will override these)
     * @return the decoded map
     */
    public Map getStringDecodedMap(final Map encodedMap, final Map defaults) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getStringDecodedMap", new Object[] {encodedMap,defaults});
        }

        final Map<String,Object> decoded = new HashMap<String,Object>();

        // Preload with the defaults - if the property exists in the input
        // it will override this default
        decoded.putAll(defaults);
        
        // Look at each property in turn.
        final Iterator keyList = encodedMap.keySet().iterator();
        while (keyList.hasNext()) {

            // These variables will point to the info to be placed
            // in the map.
            String propName = null;
            Object propVal = null;

            // Get the coded version of the name. This will start with one
            // of the prefix values. The codedName must have been non-null.
            String encodedKey = (String) keyList.next();
            String encodedVal = (String) encodedMap.get(encodedKey);

            // Extract the prefix.
            String prefix = null;

            int sepIndex = encodedKey.indexOf(PREFIX_SEPARATOR);

            if (sepIndex == -1) {
                // The separator was not found - this is really bad, and
                // suggests
                // that the encoding step was flawed.
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled())
                        SibTr.debug(TRACE, "Ignoring malformed encoded name: "
                                + encodedKey);

                continue;

            } else {
                // Extract the prefix and clean version of the name.
                prefix = encodedKey.substring(0, sepIndex);
                propName = encodedKey.substring(sepIndex
                        + PREFIX_SEPARATOR.length());

            }//if

            // Catch any number conversion errors that arise while converting
            // the
            // string to an object.
            try {

                // Decode the prefix to recreate the data type.
                if (PREFIX_NULL.equals(prefix)) {

                    // The value was null.
                    propVal = null;

                } else if (PREFIX_STRING.equals(prefix)) {

                    propVal = encodedVal;

                    // Because the value was not prefixed with PREFIX_NULL, we
                    // know that
                    // if this val is null, it was meant to be an empty
                    // string...
                    if (propVal == null) propVal = "";

                } else if (PREFIX_BOOLEAN.equals(prefix)) {

                    propVal = Boolean.valueOf(encodedVal);

                } else if (PREFIX_INT.equals(prefix)) {

                    propVal = Integer.valueOf(encodedVal);

                } else if (PREFIX_BYTE.equals(prefix)) {

                    propVal = Byte.valueOf(encodedVal);

                } else if (PREFIX_SHORT.equals(prefix)) {

                    propVal = Short.valueOf(encodedVal);

                } else if (PREFIX_FLOAT.equals(prefix)) {

                    propVal = Float.valueOf(encodedVal);

                } else if (PREFIX_DOUBLE.equals(prefix)) {

                    propVal = Double.valueOf(encodedVal);

                } else if (PREFIX_LONG.equals(prefix)) {

                    propVal = Long.valueOf(encodedVal);

                } else if (PREFIX_ROUTING_PATH.equals(prefix)) {
                    // encodedVal = array represented as one long string.
                    // This uses the Java 1.4 regex method on a string to split
                    // it into
                    // an array, with the individual strings being separated by
                    // the string passed in.
                    String[] array = encodedVal
                            .split(JmsraConstants.PATH_ELEMENT_SEPARATOR);

                    // propVal = what we want to return (a string array wrapper
                    // containing the string[])
                    String bigDestName = (String) encodedMap
                            .get(PREFIX_STRING + PREFIX_SEPARATOR
                                    + JmsInternalConstants.DEST_NAME);
                    propVal = StringArrayWrapper.create(array, bigDestName);
                } else {
                    // Did not match any of the known prefixes
                    if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                        SibTr
                                .debug(TRACE, "Ignoring unknown prefix: "
                                        + prefix);
                    }

                    continue;

                }// (if)switch on prefix type.

                // We have successfully decoded the property, so now add it to
                // the
                // temporary map of properties.
                decoded.put(propName, propVal);

                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                    SibTr.debug(TRACE, "retrieved: " + propName + " = "
                            + propVal);
                }

            } catch (final Exception exception) {

                FFDCFilter
                        .processException(
                                exception,
                                "com.ibm.ws.sib.api.jmsra.impl.JmsJcaReferenceUtilsImpl.getStringDecodedMap",
                                FFDC_PROBE_1, this);

                // Catch any NumberFormatException or similar thing that arises
                // from the attempt to convert the string to another data type.
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                    SibTr.debug(TRACE, "Error decoding string to object. ",
                            exception);
                }

                continue;

            }//try

        }//while

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getStringDecodedMap", decoded);
        }
        return decoded;

    }

    /**
     * Uses the reference passed in to extract a map of properties which have
     * been stored in this Reference.
     * 
     * @param ref
     *            the reference
     * @param defaults
     *            the default set of properties to be used (those in the reference will override these)
     * @return the map of properties
     */
    public Map getMapFromReference(final Reference ref, final Map defaults) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getMapFromReference", new Object[] {ref,defaults});
        }

        Map extractedProps = null;

        // Extract a Map of the properties from the Reference
        synchronized (ref) {

            Enumeration propsList = ref.getAll();

            // This will be set up to contain a map representing all the
            // information that was previously stored in the Reference.
            final Map<String,String> encodedMap = new HashMap<String,String>();

            // Look at each property in turn.
            while (propsList.hasMoreElements()) {

                // Get the coded version of the name. This will start with one
                // of the prefix values. The codedName must have been non-null.
                StringRefAddr refAddr = (StringRefAddr) propsList.nextElement();
                String codedName = refAddr.getType();
                String val = (String) refAddr.getContent();

                // Store the coded information in the map.
                encodedMap.put(codedName, val);

            }//while

            // Decode the encoded map.
            extractedProps = getStringDecodedMap(encodedMap, defaults);

        }//sync

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getMapFromReference", extractedProps);
        }
        return extractedProps;

    }

    /**
     * Dynamically populates the reference that it has been given using the
     * properties currently stored in this ConnectionFactory. Note that this way
     * of doing things automatically handles the adding of extra properties
     * without the need to change this code.
     * 
     * @param reference
     *            the reference to populate
     * @param properties
     *            the properties to populate the reference with
     * @param defaults
     *            the default set of properties to be used (if properties in theProps are set to 
     *            these defaults, they will be omitted from the reference)
     */
    public void populateReference(final Reference reference,
            final Map properties, final Map defaults) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "populateReference", new Object[] {
                    reference, properties, defaults});
        }

        // Make sure no-one can pull the rug from beneath us.
        synchronized (properties) {

            // Convert the map of properties into an encoded form, where the
            // keys have the necessary prefix on the front, and the values are
            // all Strings.
            Map<String,String> encodedMap = getStringEncodedMap(properties,defaults);

            for(Map.Entry<String,String> entry : encodedMap.entrySet())
            {
              reference.add(new StringRefAddr(entry.getKey(),entry.getValue()));
            }

        }//sync

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "populateReference");
        }

    }

}
