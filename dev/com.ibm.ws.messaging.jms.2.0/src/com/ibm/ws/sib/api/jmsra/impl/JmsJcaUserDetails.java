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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Utility class to provide a single object holding a user name and password.
 */
class JmsJcaUserDetails implements FFDCSelfIntrospectable {

    /**
     * The user name.
     */
    private String _userName;

    /**
     * The password.
     */
    private String _password;

    private static TraceComponent TRACE = SibTr.register(
            JmsJcaUserDetails.class, JmsraConstants.MSG_GROUP,
            JmsraConstants.MSG_BUNDLE);


    /**
     * Constructor.
     */
    JmsJcaUserDetails() {

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "JmsJcaUserDetails");
            SibTr.exit(this, TRACE, "JmsJcaUserDetails");
        }

    }

    /**
     * Constructor.
     *
     * @param userName
     *            the user name
     * @param password
     *            the password
     */
    JmsJcaUserDetails(final String userName, final String password) {

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "JmsJcaUserDetails", new Object[] {
                    userName, (password == null) ? null : "*****" });
        }

        _userName = userName;
        _password = password;

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "JmsJcaUserDetails");
        }

    }

    /**
     * Returns the password.
     *
     * @return String
     */
    String getPassword() {
        return _password;
    }

    /**
     * Returns the userName.
     *
     * @return String
     */
    String getUserName() {
        return _userName;
    }

    /**
     * Compares the contents of the two objects to see if they are the same.
     *
     * @param other
     *            the object to compare us to
     * @return true if the objects are equals and false otherwise
     */
    public boolean equals(final Object other) {

        if (!(other instanceof JmsJcaUserDetails)) {
            return false;
        }

        if (this == other) {
            return true;
        }

        String otherUserName = ((JmsJcaUserDetails) other).getUserName();
        String otherPassword = ((JmsJcaUserDetails) other).getPassword();

        final boolean userNamesMatch = (_userName == null) ? (otherUserName == null)
                : _userName.equals(otherUserName);
        final boolean passwordsMatch = (_password == null) ? (otherPassword == null)
                : _password.equals(otherPassword);

        return userNamesMatch && passwordsMatch;

    }

    /**
     * Returns a hash code based on the user name and password.
     *
     * @return the hash code
     */
    public int hashCode() {

        int hash = 13;
        hash = 23 * hash + (_userName == null ? 0 : _userName.hashCode());
        hash = 23 * hash + (_password == null ? 0 : _password.hashCode());
        return hash;

    }

    /**
     * Returns a string representation of this object.
     *
     * @return the string describing this object
     */
    public String toString() {

        final StringBuffer sb = new StringBuffer("[");
        sb.append(this.getClass().getName());
        sb.append("@");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" <userName=");
        sb.append(_userName);
        sb.append("> <password=");
        sb.append((_password == null) ? null : "*****");
        sb.append(">]");
        return sb.toString();

    }

    /**
     * Returns an array of strings for use by FFDC when introspecting
     *
     * @return an array of string that will be added to the FFDC log
     */
    public String[] introspectSelf()
    {
        return new String[] { toString() };
    }

}
