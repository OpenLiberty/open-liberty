/*******************************************************************************
 * Copyright (c) 2011, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.simplicity;

import java.io.File;

import com.ibm.websphere.simplicity.log.Log;

/**
 * This is a general container class to hold connection information. This information can be used to
 * connect to entities suchs as a WebSphere cell or a remote machine.
 */
public class ConnectionInfo implements Cloneable {

    private static Class c = ConnectionInfo.class;

    protected String profileDir;
    protected String host;
    protected Integer port;
    protected String user;
    protected String password;
    protected File keystore;

    /**
     * No argument constructor. If used, connection data must be set using the availabe setter
     * methods.
     */
    public ConnectionInfo() {
        Log.entering(c, "<clinit>");
        Log.exiting(c, "<clinit>");
    }

    /**
     * Constructor which takes in the minimal arguments needed to connect to a machine.
     *
     * @param host     The hostname of the machine
     * @param user     The administrator username of the machine
     * @param password The administartor password of the machine
     */
    public ConnectionInfo(String host, String user, String password) {
        Log.entering(c, "<clinit>", new Object[] { host, user, password });
        this.user = user;
        this.password = password;
        this.host = host;
        Log.exiting(c, "<clinit>");
    }

    /**
     * Constructor which takes in the minimal arguments needed to connect to a machine
     * using an SSH keystore and passphrase.
     *
     * @param host     The hostname of the machine
     * @param keystore The SSH keystore file
     * @param user     The username of the machine
     * @param password The password for the keystore file
     */
    public ConnectionInfo(String host, File keystore, String user, String password) {
        Log.entering(c, "<construct>", new Object[] { host, keystore, user, password });
        this.host = host;
        this.keystore = keystore;
        this.user = user;
        this.password = password;
        Log.exiting(c, "<construct>");
    }

    public String getProfileDir() {
        return profileDir;
    }

    public void setProfileDir(String dir) {
        this.profileDir = dir;
    }

    /**
     * Get the hostname of the machine to connect to
     *
     * @return The hostname of the machine to connect to
     */
    public String getHost() {
        return host;
    }

    /**
     * Set the hostname of the machine to connect to
     *
     * @param host The hostname of the machine to connect to
     */
    void setHost(String host) {
        this.host = host;
    }

    /**
     * Get the password that is to be used during the connection.
     *
     * @return The password that is to be used during the connection.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the password that is to be used during the connection.
     *
     * @param password The password that is to be used during the connection.
     */
    void setPassword(String password) {
        this.password = password;
    }

    /**
     * Get the port number that is to be used during the connection.
     *
     * @return The port number that is to be used during the connection.
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Set the port number that is to be used during the connection.
     *
     * @param port The port number that is to be used during the connection.
     */
    void setPort(Integer port) {
        this.port = port;
    }

    /**
     * Get the username that is to be used during the connection.
     *
     * @return The username that is to be used during the connection.
     */
    public String getUser() {
        return user;
    }

    /**
     * Set the username that is to be used during the connection.
     *
     * @param user The username that is to be used during the connection.
     */
    void setUser(String user) {
        this.user = user;
    }

    /**
     * Get the SSH keystore file to be used during the connection
     *
     * @return The SSH keystore that will be used to make a connection
     */
    public File getKeystore() {
        return keystore;
    }

    /**
     * Set the SSH keystore file to be used during the connection
     *
     * @param keystore The SSH keystore to use to make a connection
     */
    void setKeystore(File keystore) {
        this.keystore = keystore;
    }

    /**
     * Two ConnectionInfo Objects are equal if all their member data is equal.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof ConnectionInfo)) {
            return false;
        }

        ConnectionInfo other = (ConnectionInfo) o;
        if (((this.host == null) && (other.host != null)) || ((this.host != null) && (!this.host.equalsIgnoreCase(other.host)))
            || ((this.password == null) && (other.password != null)) || ((this.password != null) && (!this.password.equals(other.password)))
            || ((this.port == null) && (other.port != null)) || ((this.port != null) && (!this.port.equals(other.port)))
            || ((this.user == null) && (other.user != null)) || ((this.user != null) && (!this.user.equals(other.user)))
            || ((this.keystore == null) && (other.keystore != null)) || ((this.keystore != null) && (!this.keystore.equals(other.keystore)))
            || ((this.profileDir == null) && (other.profileDir != null)) || (this.profileDir != null) && (!this.profileDir.equals(other.profileDir))) {
            return false;
        }

        return true;
    }

    /**
     * Clone this ConnectionInfo
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        final String method = "clone";
        Log.entering(c, method);
        ConnectionInfo clone = (ConnectionInfo) super.clone();

        // make copies of mutable data
        if (this.keystore != null) {
            clone.setKeystore(new File(this.keystore.getAbsolutePath()));
        } else {
            clone.setKeystore(null);
        }
        if (this.port != null) {
            clone.setPort(new Integer(this.port.intValue()));
        } else {
            clone.setPort(null);
        }
        Log.exiting(c, method, clone);
        return clone;
    }

    /**
     * Custom clone method to clone all values except those specified in the method header
     *
     * @param  port                       The port to connect to
     * @param  user                       The user name to use when connecting
     * @param  password                   The password to use when connecting
     * @return                            Cloned ConnectionInfo
     * @throws CloneNotSupportedException
     */
    public Object clone(int port, String user, String password) throws CloneNotSupportedException {
        ConnectionInfo nci = (ConnectionInfo) this.clone();
        if (port != 0)
            nci.setPort(port);
        if (user != null)
            nci.setUser(user);
        if (password != null)
            nci.setPassword(password);
        return nci;
    }

}
