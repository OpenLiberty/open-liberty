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
 *         NLS resources.
 * 
 */
public abstract class NLS
{
    String bundleName;

    /**
     * @param bundleName resource bundle containing the NLS messages.
     * @return NLS a newly loaded implementation.
     * @see java.util.ResourceBundle#getBundle(java.lang.String)
     */
    public static NLS getNLS(String bundleName) {
        return (NLS) Utils.getImpl("com.ibm.ws.objectManager.utils.NLSImpl",
                                   new Class[] { String.class },
                                   new Object[] { bundleName });
    } // getNLS().

    /**
     * @param bundleName resource bundle containing the NLS messages.
     * @see java.util.ResourceBundle#getBundle(java.lang.String)
     */
    NLS(String bundleName) {
        this.bundleName = bundleName;
    } // NLS().

    /**
     * Format the objects into a printable string, if possible.
     * 
     * @param key of the template.
     * @return String null or a printable string.
     */
    public String format(String key) {
        return objectsToString(key, null);
    } // format().

    /**
     * Format the objects into a printable string, if possible.
     * 
     * @param key of the template.
     * @param object to be formatted into the string.
     * @return String containg the original object or a printable string.
     */
    public String format(String key,
                         Object object) {
        return objectsToString(key, object);
    } // format().

    /**
     * Format the objects into a printable string, if possible.
     * 
     * @param key of the template.
     * @param objects array of <code>Objects</code>. to be formatted into the string.
     * @return String containg the original objects or a printable string.
     */
    public String format(String key,
                         Object[] objects) {
        return objectsToString(key, objects);
    } // formatInfo().

    /**
     * Create a simple default formatted string.
     * 
     * @param key which would look up the message in a resource bundle.
     * @param objects which would be inserted into the message.
     * @return String the formatted String.
     */
    String objectsToString(String key, Object objects) {
        java.io.StringWriter stringWriter = new java.io.StringWriter();
        stringWriter.write(key);
        stringWriter.write(":");

        if (objects == null) {
            stringWriter.write("\n");

        } else if (objects instanceof Object[]) {
            for (int i = 0; i < ((Object[]) objects).length; i++) {
                Object object = ((Object[]) objects)[i];
                if (object == null)
                    stringWriter.write("null\n");
                else {
                    stringWriter.write(((Object[]) objects)[i].toString());
                    stringWriter.write("\n");
                }
            }

        } else {
            stringWriter.write(objects.toString());
            stringWriter.write("\n");
        }

        return stringWriter.toString();
    } // objectsToString().

    /**
     * @return String the bundleName.
     */
    String getBundleName() {
        return bundleName;
    }
} // class NLS.
