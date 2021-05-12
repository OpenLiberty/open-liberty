/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.jdbc;

import java.sql.SQLException;
import java.util.Vector;

import com.ibm.adapter.HandleStates;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.rsadapter.FFDCLogger;

/**
 * This class is the super class of jdbc connection class and statement classes.
 * <p>
 */
public abstract class JdbcObject implements HandleStates {

    /** Indicates the state of the JDBC object. */
    public int state = ACTIVE;

    private static final TraceComponent tc = Tr.register(JdbcObject.class);

    /**
     * Parent wrapper that created this JDBC object. For example, the parent of
     * a Statement wrapper is a Connection wrapper. The parent of a Connection
     * wrapper is null, since a JDBC Connection has no parent.
     */
    JdbcObject parentWrapper;

    /** Single child wrapper belonging to this wrapper. */
    JdbcObject childWrapper;

    /** List of (open) child wrappers belonging to this wrapper. */
    Vector childWrappers;

    /** Object to synchronize on. */
    Object syncObject;

    /**
     * The Connection wrapper should override this method to handle the getting
     * and starting of a LocalTransaction when appropriate. This method should
     * only be called when we are certain this wrapper is not closed.
     *
     * @throw SQLException if an error occurs or the current state is invalid.
     */
    void beginTransactionIfNecessary() throws SQLException {
        getConnectionWrapper().beginTransactionIfNecessary();
    }

    /**
     * Default close method for all JDBC wrappers, accessible to users. This
     * method closes all child wrappers and invokes the closeWrapper method
     * implemented by the wrapper subclass. If the wrapper is already closed, a
     * message stating the wrapper has already been closed is logged to the
     * trace.
     *
     * @throws SQLException
     *             the first error to occur while closing the object.
     */
    public void close() throws SQLException {
        TraceComponent tc = getTracer();

        if (tc.isEntryEnabled())
            Tr.entry(tc, "close", this);

        // Make sure we only get closed once.

        synchronized (this) {
            if (state == CLOSED) // already closed, just return
            {
                if (tc.isEventEnabled())
                    Tr.event(tc, "Already closed.");
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "close");
                return;
            }

            state = CLOSED;
        }

        if (tc.isEventEnabled())
            Tr.event(tc, "state --> " + getStateString());

        // Close all children. Then close the current wrapper, saving the first
        // exception
        // encountered. Others are logged.

        closeChildWrappers();
        SQLException sqlX = closeWrapper();

        parentWrapper = null;
        childWrappers = null;

        if (sqlX == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "close");
        } else {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "close", sqlX);
            throw sqlX;
        }
    }

    /**
     * Close all child objects of this wrapper. Errors that occur while closing
     * child objects should not be thrown. Errors may be logged instead.
     */
    public final void closeChildWrappers() {
        // Close any child wrappers in the child wrapper list.

        if (childWrappers != null && !childWrappers.isEmpty()) {
            JdbcObject wrapper = null;

            // Children remove themselves from the childWrappers list as they
            // are closed.
            for (int i = childWrappers.size(); i > 0;)
                try {
                    wrapper = (JdbcObject) childWrappers.get(--i);
                    wrapper.close();
                } catch (SQLException closeX) // can't fail here, need to keep
                                              // closing
                {
                    if (tc.isDebugEnabled())
                        Tr.debug(getTracer(), "ERR_CLOSING_CHILD",
                                 new Object[] { wrapper, closeX });
                }
        }

        // Close this object's single child wrapper, if one exists.

        if (childWrapper != null)
            try {
                childWrapper.close();
            } catch (SQLException closeX) {
                if (tc.isDebugEnabled())
                    Tr.debug(getTracer(), "ERR_CLOSING_CHILD", new Object[] {
                                                                              childWrapper, closeX });
            }
    }

    /**
     * Perform any wrapper-specific close logic. This method is called by the
     * default JdbcObject close method.
     *
     * @return SQLException the first error to occur while closing the object.
     */
    abstract SQLException closeWrapper();

    /**
     * @return the Connection wrapper for this object, or null if none is
     *         available.
     */
    public abstract JdbcObject getConnectionWrapper();

    /**
     * @return the underlying JDBC implementation object which we are wrapping.
     */
    abstract Object getJDBCImplObject();

    /**
     * @return the current state.
     */
    public final int getState() {
        return state;
    }

    /**
     * @return the JdbcObject state string corresponding to the current state.
     */
    public final String getStateString() {
        return STATE_STRINGS[state];
    }

    /**
     * @return the trace component for the JDBC Object subclass.
     */
    abstract TraceComponent getTracer();

    // FFDC for the JDBC wrappers will work as follows.
    // Three methods are used:
    //
    // introspectSelf - from the FFDCSelfIntrospectable interface. This method
    // delegates back
    // to the root of the wrapper tree to display FFDC information for the
    // entire tree
    // (using the introspectAll method on the root ancestor).
    //
    // introspectAll - this method displays all FFDC information for the current
    // wrapper and
    // its subtree. It invokes introspectWrapperSpecificInfo to retrieve any
    // wrapper
    // specific information.
    //
    // introspectWrapperSpecificInfo - this method displays any FFDC information
    // specific to
    // the current wrapper only. No information for child or parent wrappers is
    // included.
    //
    // Most wrappers will find it useful only to override the third method,
    // introspectWrapperSpecificInfo. The Connection wrapper will also want to
    // override the
    // introspectSelf method.

    /**
     * Collects generic FFDC information applicable to all JDBC wrappers.
     * Formats this information to the provided FFDC logger. FFDC information
     * for all child wrappers should be included in the result.
     *
     * @param info
     *            FFDCLogger on which to record the FFDC information.
     */
    void introspectAll(FFDCLogger info) {
        info.append(this.toString());

        // Allow any wrapper specific info to be inserted first.

        introspectWrapperSpecificInfo(info);

        // Display generic information.

        info.append("Wrapper State: ", getStateString());
        info.append("Parent wrapper:", parentWrapper);

        info.append("Child wrappers:");
        info.indent(childWrapper);

        if (childWrappers != null)
            try {
                for (int i = 0; i < childWrappers.size(); i++)
                    info.indent(childWrappers.get(i));
            } catch (Throwable th) {
                // No FFDC code needed; closed on another thread; ignore.
            }

        info.eoln();

        info.append("Synchronization Object:", syncObject);

        if (childWrapper != null)
            try {
                info
                                .append("____________________________________________________________");
                info.append("Child Wrapper");
                childWrapper.introspectAll(info);
            } catch (NullPointerException nullX) {
                // No FFDC code needed; closed on another thread; ignore.
            }

        if (childWrappers != null)
            try {
                for (int i = 0; i < childWrappers.size(); i++) {
                    info
                                    .append("____________________________________________________________");
                    info.append("Child Wrapper #" + (i + 1));
                    ((JdbcObject) childWrappers.get(i)).introspectAll(info);
                }
            } catch (Throwable th) {
                // No FFDC code needed; closed on another thread; ignore.
            }
    }

    /**
     * @return relevant FFDC information for the JDBC object, formatted as a
     *         String array.
     */
    public String[] introspectSelf() {
        // The default implementation for JDBC objects just delegates to the
        // Connection
        // wrapper, which will display the entire hierarchy.

        com.ibm.ws.rsadapter.FFDCLogger info = new com.ibm.ws.rsadapter.FFDCLogger(500, this);

        JdbcObject connWrapper = null;

        try {
            connWrapper = getConnectionWrapper();
        } catch (NullPointerException nullX) {
            // No FFDC code needed; wrapper is closed.
        }

        if (connWrapper == null || connWrapper == this)
            introspectAll(info);
        // Information for this wrapper (and child objects) only.

        else {
            info.append("Displaying FFDC information for wrapper hierarchy,");
            info.append("beginning from the Connection...");
            info.append(connWrapper.introspectSelf());
        }

        return info.toStringArray();
    }

    /**
     * Collects FFDC information specific to this JDBC wrapper. Formats this
     * information to the provided FFDC logger. This method is used by
     * introspectAll to collect any wrapper specific information.
     *
     * @param info
     *            FFDCLogger on which to record the FFDC information.
     */
    void introspectWrapperSpecificInfo(FFDCLogger info) {}

    /**
     * @param runtimeX
     *            a RuntimeException which occurred, indicating the wrapper may
     *            be closed.
     *
     * @return the RuntimeException to throw if it isn't.
     */
    abstract RuntimeException runtimeXIfNotClosed(RuntimeException runtimeX) throws SQLException;
}
