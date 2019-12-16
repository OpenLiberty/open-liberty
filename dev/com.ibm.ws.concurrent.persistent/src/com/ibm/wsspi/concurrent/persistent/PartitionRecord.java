/*******************************************************************************
 * Copyright (c) 2014,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.concurrent.persistent;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Represents a partition entry in the persistent store.
 */
@Trivial
public class PartitionRecord {
    /**
     * End of line character.
     */
    private static final String EOLN = String.format("%n");

    /**
     * Bits corresponding to each specified attribute.
     */
    private static final int ID = 0x1,
                    EXECUTOR = 0x2,
                    HOSTNAME = 0x4,
                    LSERVER = 0x8,
                    USERDIR = 0x10,
                    EXPIRY = 0x20,
                    STATES = 0x40;

    /**
     * Bits indicating which attributes are set.
     */
    private int attrs;

    /**
     * Id, JNDI name, or config.displayId of the persistent executor.
     */
    private String executor;

    /**
     * Expiry timestamp for the partition entry. Currently unused.
     */
    private long expiry;

    /**
     * Host name.
     */
    private String hostName;

    /**
     * Unique identifier for the partition.
     */
    private long id;

    /**
     * Liberty server name.
     */
    private String libertyServer;

    /**
     * Bits that represent configured state. Currently unused.
     */
    private long states;

    /**
     * Value of ${wlp.user.dir}
     */
    private String userDir;

    /**
     * Construct an empty partition record.
     *
     * @param allAttributesAreSpecified indicates whether all attributes should be considered specified or unspecified.
     */
    public PartitionRecord(boolean allAttributesAreSpecified) {
        attrs = allAttributesAreSpecified ? 0xffff : 0;
    }

    /**
     * Deep comparison of attributes for equality. Only specified attributes are compared.
     * Both instances must specify the same sets of attributes.
     *
     * @param obj instance with which to compare.
     * @return true if equal, otherwise false.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof PartitionRecord) {
            PartitionRecord other = (PartitionRecord) obj;
            return attrs == other.attrs
                   && ((attrs & ID) == 0 || id == other.id)
                   && ((attrs & EXECUTOR) == 0 || match(executor, other.executor))
                   && ((attrs & HOSTNAME) == 0 || match(hostName, other.hostName))
                   && ((attrs & LSERVER) == 0 || match(libertyServer, other.libertyServer))
                   && ((attrs & USERDIR) == 0 || match(userDir, other.userDir))
                   && ((attrs & EXPIRY) == 0 || expiry == other.expiry)
                   && ((attrs & STATES) == 0 || states == other.states);
        }
        return false;
    }

    /**
     * Returns the name (Id, JNDI name, or config.displayId) of the persistent executor.
     *
     * @return the name (Id, JNDI name, or config.displayId) of the persistent executor.
     */
    public String getExecutor() {
        if ((attrs & EXECUTOR) == 0)
            throw new IllegalStateException();
        else
            return executor;
    }

    /**
     * Returns the expiry timestamp for the partition entry.
     *
     * @return the expiry timestamp for the partition entry.
     */
    public long getExpiry() {
        if ((attrs & EXPIRY) == 0)
            throw new IllegalStateException();
        else
            return expiry;
    }

    /**
     * Returns the host name.
     *
     * @return the host name.
     */
    public String getHostName() {
        if ((attrs & HOSTNAME) == 0)
            throw new IllegalStateException();
        else
            return hostName;
    }

    /**
     * Returns the unique identifier for the partition.
     *
     * @return the unique identifier for the partition.
     */
    public final long getId() {
        if ((attrs & ID) == 0)
            throw new IllegalStateException();
        else
            return id;
    }

    /**
     * Returns the Liberty server name.
     *
     * @return the Liberty server name.
     */
    public String getLibertyServer() {
        if ((attrs & LSERVER) == 0)
            throw new IllegalStateException();
        else
            return libertyServer;
    }

    /**
     * Returns bits that represent the configured state of the executor.
     *
     * @return bits that represent the configured state of the executor.
     */
    public long getStates() {
        if ((attrs & STATES) == 0)
            throw new IllegalStateException();
        else
            return states;
    }

    /**
     * Returns the value of ${wlp.user.dir}.
     *
     * @return the value of ${wlp.user.dir}.
     */
    public String getUserDir() {
        if ((attrs & USERDIR) == 0)
            throw new IllegalStateException();
        else
            return userDir;
    }

    /**
     * Hash code for the partition record is computed from the id.
     *
     * @return the hash code.
     */
    @Override
    public final int hashCode() {
        return (int) id;
    }

    /**
     * Returns true if the Executor attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasExecutor() {
        return (attrs & EXECUTOR) != 0;
    }

    /**
     * Returns true if the Expiry attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasExpiry() {
        return (attrs & EXPIRY) != 0;
    }

    /**
     * Returns true if the Host attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasHostName() {
        return (attrs & HOSTNAME) != 0;
    }

    /**
     * Returns true if the Id attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasId() {
        return (attrs & ID) != 0;
    }

    /**
     * Returns true if the Server attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasLibertyServer() {
        return (attrs & LSERVER) != 0;
    }

    /**
     * Returns true if the States attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasStates() {
        return (attrs & STATES) != 0;
    }

    /**
     * Returns true if the UserDir attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasUserDir() {
        return (attrs & USERDIR) != 0;
    }

    /**
     * Utility method to compare for equality.
     *
     * @param obj1 first object.
     * @param obj2 second object.
     * @return true if equal or both null. False otherwise.
     */
    private static final boolean match(Object obj1, Object obj2) {
        return obj1 == obj2 || obj1 != null && obj1.equals(obj2);
    }

    /**
     * Sets the name (Id, JNDI name, or config.displayId) of the persistent executor.
     *
     * @param executor the name of the persistent executor.
     */
    public final void setExecutor(String executor) {
        this.executor = executor;
        attrs |= EXECUTOR;
    }

    /**
     * Sets the expiry timestamp for the partition entry.
     *
     * @param expiry the expiry timestamp for the partition entry.
     */
    public final void setExpiry(long expiry) {
        this.expiry = expiry;
        attrs |= EXPIRY;
    }

    /**
     * Sets the host name.
     *
     * @param host the host name.
     */
    public final void setHostName(String host) {
        this.hostName = host;
        attrs |= HOSTNAME;
    }

    /**
     * Sets the unique identifier for the partition.
     *
     * @param id unique identifier for the partition.
     */
    public final void setId(long id) {
        this.id = id;
        attrs |= ID;
    }

    /**
     * Sets the server name.
     *
     * @param server the server name.
     */
    public final void setLibertyServer(String server) {
        this.libertyServer = server;
        attrs |= LSERVER;
    }

    /**
     * Sets the bits that represent configured state.
     *
     * @param expiry the expiry timestamp for the partition entry.
     */
    public final void setStates(long states) {
        this.states = states;
        attrs |= STATES;
    }

    /**
     * Sets the value obtained from ${wlp.user.dir}.
     *
     * @param userDir value obtained from ${wlp.user.dir}.
     */
    public final void setUserDir(String userDir) {
        this.userDir = userDir;
        attrs |= USERDIR;
    }

    /**
     * Returns a textual representation of this instance.
     *
     * @return a textual representation of this instance.
     */
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder(200).append("PartitionRecord");
        if ((attrs & ID) != 0)
            output.append('[').append(id).append(']');
        output.append('@').append(Integer.toHexString(System.identityHashCode(this)));
        if ((attrs & EXECUTOR) != 0)
            output.append(EOLN).append("EXECUTOR=").append(executor);
        if ((attrs & HOSTNAME) != 0)
            output.append(EOLN).append("HOSTNAME=").append(hostName);
        if ((attrs & LSERVER) != 0)
            output.append(EOLN).append("LSERVER=").append(libertyServer);
        if ((attrs & USERDIR) != 0)
            output.append(EOLN).append("USERDIR=").append(userDir);
        if ((attrs & EXPIRY) != 0)
            output.append(EOLN).append("EXPIRY=").append(expiry);
        if ((attrs & STATES) != 0)
            output.append(EOLN).append("STATES=").append(states);

        return output.toString();
    }

    /**
     * Unsets the Executor attribute.
     */
    public final void unsetExecutor() {
        attrs &= ~EXECUTOR;
    }

    /**
     * Unsets the Expiry attribute.
     */
    public final void unsetExpiry() {
        attrs &= ~EXPIRY;
    }

    /**
     * Unsets the Host attribute.
     */
    public final void unsetHostName() {
        attrs &= ~HOSTNAME;
    }

    /**
     * Unsets the Id attribute.
     */
    public final void unsetId() {
        attrs &= ~ID;
    }

    /**
     * Unsets the Server attribute.
     */
    public final void unsetLibertyServer() {
        attrs &= ~LSERVER;
    }

    /**
     * Unsets the States attribute.
     */
    public final void unsetStates() {
        attrs &= ~STATES;
    }

    /**
     * Unsets the UserDir attribute.
     */
    public final void unsetUserDir() {
        attrs &= ~USERDIR;
    }
}
