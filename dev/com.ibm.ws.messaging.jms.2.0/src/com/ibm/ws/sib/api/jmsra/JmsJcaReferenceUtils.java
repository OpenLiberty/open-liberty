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

package com.ibm.ws.sib.api.jmsra;

import java.util.Map;

import javax.naming.Reference;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Utility class for creating references for JMS resources.
 */
public abstract class JmsJcaReferenceUtils implements JmsraConstants {

    /**
     * Implementation class name.
     */
    private static final String SIB_REFERENCE_UTILS_CLASS = "com.ibm.ws.sib.api.jmsra.impl.JmsJcaReferenceUtilsImpl";

    /**
     * The singleton instance of the implementation class.
     */
    private static JmsJcaReferenceUtils _instance = null;

    private static final TraceComponent TRACE = SibTr.register(
            JmsJcaReferenceUtils.class, JmsraConstants.MSG_GROUP,
            JmsraConstants.MSG_BUNDLE);

    private static final String FFDC_PROBE_1 = "1";

   

    static {
       
        try {

            Class clazz = Class.forName(SIB_REFERENCE_UTILS_CLASS);
            _instance = (JmsJcaReferenceUtils) clazz.newInstance();

        } catch (final Exception exception) {

            // Disaster - the implementation jar file is missing
            FFDCFilter.processException(exception,
                    "com.ibm.ws.sib.api.jmsra.JmsJcaReferenceUtils.<clinit>",
                    FFDC_PROBE_1);
            SibTr.error(TRACE, "EXCEPTION_RECEIVED_CWSJR1161", new Object[] {
                    exception, SIB_REFERENCE_UTILS_CLASS});

        }
    }

    /**
     * Returns the singleton instance of this class.
     * 
     * @return the singleton instance
     */
    public static JmsJcaReferenceUtils getInstance() {
        return _instance;
    }

    /**
     * Encodes the values in the given map to strings.
     * 
     * @param raw
     *            the map to encode
     * @param defaults
     *            the default set of properties to be used (if properties in raw are set to 
     *            these defaults, they will be omitted from the encoded map)
     * @return the encoded map
     */
    public abstract Map getStringEncodedMap(Map raw, Map defaults);

    /**
     * Decodes the values in the given map from strings back to their original
     * types.
     * 
     * @param encodedMap
     *            the map to decode
     * @param defaults
     *            the default set of properties to be used (those in the encodedMap will override these)
     * @return the decoded map
     */
    public abstract Map getStringDecodedMap(Map encodedMap, Map defaults);

    /**
     * Returns the map of properties for the given reference.
     * 
     * @param ref
     *            the reference
     * @param defaults
     *            the default set of properties to be used (those in the reference will override these)
     * @return the map of properties
     */
    public abstract Map getMapFromReference(Reference ref, Map defaults);
    
    /**
     * Dynamically populates the reference that it has been given using the
   	 * properties currently stored in this Map.<p>
   	 * 
   	 * Note that this way of doing things automatically handles the adding
   	 * of extra properties without the need to change this code.
   	 * 
     * @param ref The reference to be populated.
     * @param theProps The Map contains the properties for insertion into the Reference.
     * @param defaults
     *            the default set of properties to be used (if properties in theProps are set to 
     *            these defaults, they will be omitted from the reference)
     */
    public abstract void populateReference(Reference ref, Map theProps, Map defaults);

}
