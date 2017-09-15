package com.ibm.ws.objectManager.utils;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * @author Andrew_Banks
 * 
 *         NLS resources for WAS.
 * 
 */
public class NLSImpl
                extends NLS {
    private static final Class cclass = NLSImpl.class;

    private static final String defaultMessage = "Default Message: {0} {1} {2} {3} {4} {5} {6} {7} {8} {9}";
    private com.ibm.ejs.ras.TraceNLS traceNLS;

    /**
     * @param bundleName resource bundle containing the NLS messages.
     * @see java.util.ResourceBundle#getBundle(java.lang.String)
     */
    public NLSImpl(String bundleName) {
        super(bundleName);
        traceNLS = com.ibm.ejs.ras.TraceNLS.getTraceNLS(bundleName);
    } // NLSImpl().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.NLS#format(java.lang.String)
     */
    public String format(String key) {
        if (traceNLS != null) {
            return traceNLS.getString(key);
        } else {
            return key;
        }
    } // format().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.NLS#format(java.lang.String, java.lang.Object)
     */
    public String format(String key,
                         Object object) {
        if (traceNLS != null) {
            return traceNLS.getFormattedMessage(key, new Object[] { object }, defaultMessage);
        } else {
            return objectsToString(key, object);
        }
    } // format().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.NLS#format(java.lang.String, java.lang.Object[])
     */
    public String format(String key,
                         Object[] objects) {
        if (traceNLS != null) {
            return traceNLS.getFormattedMessage(key,
                                                objects,
                                                defaultMessage);
        } else {
            return objectsToString(key, objects);
        }
    } // format().
} // class NLSImpl.