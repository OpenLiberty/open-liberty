/*******************************************************************************
 * Copyright (c) 2001, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.jdbc;

import java.io.Closeable; 
import java.io.IOException; 
import java.sql.Array; 
import java.sql.Blob; 
import java.sql.Clob; 
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException; 
import java.sql.SQLXML; 
import java.sql.Wrapper;
import java.util.ArrayList; 
import java.util.LinkedList; 
import java.util.List; 

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.rsadapter.AdapterUtil; 
import com.ibm.ws.rsadapter.FFDCLogger;

/**
 * <p>This class is the superclass of the following JDBC wrappers:</p>
 * 
 * <ul>
 * <li>Connection
 * <li>Statement
 * <li>PreparedStatement
 * <li>CallableStatement
 * <li>ResultSet
 * <li>DatabaseMetaData
 * </ul>
 * 
 * <p>It is not used directly, but only through the above subclasses. This class implements
 * the common behavior of these wrappers, including a common close method.</p>
 */
public abstract class WSJdbcObject extends WSJdbcWrapper implements FFDCSelfIntrospectable {
    /** Indicates the state of the JDBC object. */
    protected State state = State.ACTIVE; 

    /**
     * Parent wrapper that created this JDBC object. For example, the parent of a Statement
     * wrapper is a Connection wrapper. The parent of a Connection wrapper is null, since a
     * JDBC Connection has no parent.
     */
    public WSJdbcObject parentWrapper; 

    /** Single child wrapper belonging to this wrapper. */
    protected WSJdbcObject childWrapper; 

    /** List of (open) child wrappers belonging to this wrapper. */
    protected ArrayList<Wrapper> childWrappers; 

    // Maintain lists of data structures that the application might forget to release,
    // so we can clean up after them when the wrapper is discarded. 

    /**
     * Indicates whether or not to track and free the following resources when closed:
     * Array, Blob, Clob, NClob, SQLXML, InputStream, Reader
     * 
     */
    protected boolean freeResourcesOnClose;

    /** Arrays created by this object. */
    protected List<Array> arrays; 

    /** BLOBs created by this object. */
    protected List<Blob> blobs; 

    /** CLOBs and NCLOBs created by this object. */
    protected List<Clob> clobs; 

    /** InputStreams and Readers created by this object. */
    protected List<Closeable> resources; 

    /** SQLXMLs created by this object. */
    protected List<SQLXML> xmls; 

    /**
     * The Connection wrapper should override this method to handle the getting and starting of
     * a LocalTransaction when appropriate. This method should only be called when we are
     * certain this wrapper is not closed.
     * 
     * @throw SQLException if an error occurs or the current state is invalid.
     */
    public void beginTransactionIfNecessary() throws SQLException {
        getConnectionWrapper().beginTransactionIfNecessary();
    }

    /**
     * Default close method for all JDBC wrappers, accessible to users. This method closes all
     * child wrappers and invokes the closeWrapper method implemented by the wrapper subclass.
     * If the wrapper is already closed, a message stating the wrapper has already been closed
     * is logged to the trace.
     * 
     * @throws SQLException the first error to occur while closing the object.
     */
    public void close() throws SQLException {
        close(false); 
    }

    /**
     * Default close method for all JDBC wrappers, accessible to users. This method closes all
     * child wrappers and invokes the closeWrapper method implemented by the wrapper subclass.
     * If the wrapper is already closed, a message stating the wrapper has already been closed
     * is logged to the trace.
     * 
     * @param closeWrapperOnly boolean flag to indicate that only wrapper-closure activities
     *            should be performed, but close of the underlying object is unnecessary.
     * 
     * @throws SQLException the first error to occur while closing the object.
     */
    protected void close(boolean closeWrapperOnly) throws SQLException 
    {
        TraceComponent tc = getTracer();

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "close"); 

        // Make sure we only get closed once.

        if (state == State.CLOSED) // already closed, just return
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "close", "Already closed");
            return;
        }

        state = State.CLOSED;

        if (isTraceOn && tc.isEventEnabled())
            Tr.event(this, tc, "state --> " + state.name()); 

        // Close all children.  Then close the current wrapper, saving the first exception
        // encountered.  Others are logged.

        closeChildWrappers();
        SQLException sqlX = freeResourcesOnClose ? closeResources() : null; 
        SQLException sqlX2 = closeWrapper(closeWrapperOnly); 
        sqlX = sqlX == null ? sqlX2 : sqlX; 

        // When JDBC event listeners are enabled, the connection error notification is sent
        // prior to raising the error, which means close is invoked prior to mapException.
        // The reference to the parent wrapper must be kept so that exception mapping can
        // still be performed. 
        childWrappers = null;

        ifcToDynamicWrapper.clear();
        dynamicWrapperToImpl.clear();

        if (sqlX != null) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "close", sqlX); 
            throw sqlX;
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "close"); 
    }

    /**
     * Close all child objects of this wrapper. Errors that occur while closing child objects
     * should not be thrown. Errors may be logged instead.
     */
    final void closeChildWrappers() 
    {
        TraceComponent tc = getTracer(); 
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        // Close any child wrappers in the child wrapper list.

        if (childWrappers != null && !childWrappers.isEmpty()) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "Closing child wrappers"); 
            WSJdbcObject wrapper = null;

            // Children remove themselves from the childWrappers list as they are closed.
            for (int i = childWrappers.size(); i > 0;)
                try {
                    wrapper = (WSJdbcObject) childWrappers.get(--i);
                    wrapper.close();
                } catch (SQLException closeX) // can't fail here, need to keep closing
                {
                    FFDCFilter.processException(closeX,
                                                "com.ibm.ws.rsadapter.jdbc.WSJdbcObject.closeChildWrappers", "554", this);
                } catch (IndexOutOfBoundsException ioobX) 
                {
                    if (isTraceOn && tc.isDebugEnabled()) 
                    {
                        Tr.debug(this, tc, "ArrayIndexOutOfBoundsException is caught during closeChildWrappers() of the WSJdbcObject");
                        Tr.debug(this, tc, "Possible causes:");
                        Tr.debug(this, tc, "multithreaded access of JDBC objects by the Application");
                        Tr.debug(this, tc, "Application is closing JDBC objects in a finalize()");
                        Tr.debug(this, tc, "Exception is: ", ioobX);
                    }
                    throw ioobX;
                }

        }

        // Close this object's single child wrapper, if one exists.

        if (childWrapper != null)
            try {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Closing child wrapper"); 
                childWrapper.close();
            } catch (SQLException closeX) {
                FFDCFilter.processException(closeX,
                                            "com.ibm.ws.rsadapter.jdbc.WSJdbcObject.closeChildWrappers", "573", this);
            }
    }

    /**
     * Clean up after applications in case they don't clean up after themselves.
     * This method releases the following resources which the application server keeps
     * track of,
     * 
     * Array, BLOB, CLOB, NCLOB, SQLXML, InputStream, Reader
     * 
     * This method assumes the resource lists are initialized. 
     * Only invoke it if freeResourcesOnClose is TRUE. 
     * 
     * @return the first failure to occur, or NULL if all resources are cleaned up,
     *         or if there is nothing to clean up.
     */
    protected SQLException closeResources() {
        TraceComponent tc = getTracer();
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "closeResources", this); 

        SQLException result = null;

        // InputStream and Reader

        for (Closeable resource : resources)
            try {
                if (resource != null)
                    resource.close(); 
            } catch (IOException ioX) {
                FFDCFilter.processException(
                                            ioX, getClass().getName() + ".closeResources", "316", this);
                if (result == null)
                    result = AdapterUtil.toSQLException(ioX);
            }

        resources.clear();

        // The remaining cleanup options are only available in JDBC 4.0 or higher.
        if (mcf.jdbcDriverSpecVersion >= 40) {
            if (mcf.doArrayCleanup)
                for (Array ra : arrays)
                    try {
                        if (ra != null)
                            ra.free(); 
                    } catch (SQLFeatureNotSupportedException supportX) {
                        // No FFDC code needed.
                        mcf.doArrayCleanup = false;
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(this, tc, "Array.free is not supported.", supportX); 
                    } catch (SQLException sqlX) {
                        FFDCFilter.processException(
                                                    sqlX, getClass().getName() + ".closeResources", "325", this);
                        if (result == null)
                            result = sqlX;
                    }

            if (mcf.doBlobCleanup)
                for (Blob blob : blobs)
                    try {
                        if (blob != null)
                            blob.free(); 
                    } catch (SQLFeatureNotSupportedException supportX) {
                        // No FFDC code needed.
                        mcf.doBlobCleanup = false;
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(this, tc, "Blob.free is not supported.", supportX); 
                    } catch (SQLException sqlX) {
                        FFDCFilter.processException(
                                                    sqlX, getClass().getName() + ".closeResources", "348", this);
                        if (result == null)
                            result = sqlX;
                    }

            if (mcf.doClobCleanup)
                for (Clob clob : clobs)
                    try {
                        if (clob != null)
                            clob.free(); 
                    } catch (SQLFeatureNotSupportedException supportX) {
                        // No FFDC code needed.
                        mcf.doClobCleanup = false;
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(this, tc, "Clob.free is not supported.", supportX); 
                    } catch (SQLException sqlX) {
                        FFDCFilter.processException(
                                                    sqlX, getClass().getName() + ".closeResources", "367", this);
                        if (result == null)
                            result = sqlX;
                    }

            if (mcf.doXMLCleanup)
                for (SQLXML xml : xmls)
                    try {
                        if (xml != null)
                            xml.free(); 
                    } catch (SQLFeatureNotSupportedException supportX) {
                        // No FFDC code needed.
                        mcf.doXMLCleanup = false;
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(this, tc, "SQLXML.free is not supported.", supportX); 
                    } catch (SQLException sqlX) {
                        FFDCFilter.processException(
                                                    sqlX, getClass().getName() + ".closeResources", "386", this);
                        if (result == null)
                            result = sqlX;
                    }
        }

        arrays.clear();
        blobs.clear();
        clobs.clear();
        xmls.clear();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "closeResources", result); 
        return result;
    }

    /**
     * Perform any wrapper-specific close logic. This method is called by the default
     * WSJdbcObject close method.
     * 
     * @param closeWrapperOnly boolean flag to indicate that only wrapper-closure activities
     *            should be performed, but close of the underlying object is unnecessary.
     * 
     * @return SQLException the first error to occur while closing the object.
     */
    protected abstract SQLException closeWrapper(boolean closeWrapperOnly); 

    /**
     * @return the Connection wrapper for this object, or null if none is available.
     */
    protected abstract WSJdbcObject getConnectionWrapper(); 

    /**
     * @return the current state.
     */
    public final State getState() {
        return state;
    }

    /**
     * @return the trace component for the JDBC Object subclass.
     */
    protected abstract TraceComponent getTracer(); 

    /**
     * Initialize the parent wrapper field and copy some fields from the parent wrapper.
     * 
     * @param parent the parent wrapper, or NULL if initializing a connection wrapper. 
     */
    final void init(WSJdbcObject parent) {
        if (parent != null) {
            parentWrapper = parent;
            dsConfig = parent.dsConfig; 
            freeResourcesOnClose = parent.freeResourcesOnClose;
        }

        if (freeResourcesOnClose) // then initialize data structures to track them
        {
            arrays = new LinkedList<Array>();
            blobs = new LinkedList<Blob>();
            clobs = new LinkedList<Clob>();
            resources = new LinkedList<Closeable>();
            xmls = new LinkedList<SQLXML>();
        }
    }

    // FFDC for the JDBC wrappers will work as follows.
    // Three methods are used:
    //
    // introspectSelf - from the FFDCSelfIntrospectable interface. This method delegates back
    //   to the root of the wrapper tree to display FFDC information for the entire tree
    //   (using the introspectAll method on the root ancestor).
    //
    // introspectAll - this method displays all FFDC information for the current wrapper and
    //   its subtree. It invokes introspectWrapperSpecificInfo to retrieve any wrapper
    //   specific information.
    //
    // introspectWrapperSpecificInfo - this method displays any FFDC information specific to
    //   the current wrapper only. No information for child or parent wrappers is included.
    //
    // Most wrappers will find it useful only to override the third method,
    // introspectWrapperSpecificInfo.  The Connection wrapper will also want to override the
    // introspectSelf method.

    /**
     * Collects generic FFDC information applicable to all JDBC wrappers. Formats this
     * information to the provided FFDC logger. FFDC information for all child wrappers should
     * be included in the result.
     * 
     * @param info FFDCLogger on which to record the FFDC information.
     */
    protected void introspectAll(FFDCLogger info) 
    {
        info.append(this.toString());

        // Allow any wrapper specific info to be inserted first.

        introspectWrapperSpecificInfo(info);

        // Display generic information.

        info.append("Wrapper State: ", state.name());
        info.append("Parent wrapper:", parentWrapper);

        info.append("Child wrapper:");
        info.indent(childWrapper);

        if (childWrappers != null) {
            try {
                info.append("# of Child Wrappers " + childWrappers.size());
                info.append("Child wrappers:"); 

                for (int i = 0; i < childWrappers.size(); i++) {
                    info.indent(childWrappers.get(i));
                }
            } catch (Throwable th) {
                // No FFDC code needed; closed on another thread; ignore.
            }
        }//end if 
        info.eoln();

    }

    /**
     * @return relevant FFDC information for the JDBC object, formatted as a String array.
     */
    public String[] introspectSelf() {
        // The default implementation for JDBC objects just delegates to the Connection
        // wrapper, which will display the entire hierarchy.

        com.ibm.ws.rsadapter.FFDCLogger info = new com.ibm.ws.rsadapter.FFDCLogger(this); 

        WSJdbcObject connWrapper = null;

        try {
            connWrapper = getConnectionWrapper();
        } catch (NullPointerException nullX) {
            // No FFDC code needed; wrapper is closed.
        }

        if (connWrapper == null || connWrapper == this)
            introspectAll(info); // Information for this wrapper (and child objects) only.

        else {
            introspectAll(info);            
            info.append(connWrapper.introspectSelf());
        }

        return info.toStringArray();
    }

    /**
     * Collects FFDC information specific to this JDBC wrapper. Formats this information to
     * the provided FFDC logger. This method is used by introspectAll to collect any wrapper
     * specific information.
     * 
     * @param info FFDCLogger on which to record the FFDC information.
     */
    protected void introspectWrapperSpecificInfo(FFDCLogger info) 
    {}

    /**
     * @return true if this object is closed, otherwise false.
     */
    public final boolean isClosed() {
        return state == State.CLOSED;
    }

    /**
     * @param runtimeX a RuntimeException which occurred, indicating the wrapper may be closed.
     * 
     * @throws RuntimeException if the wrapper is closed and exception mapping is disabled. 
     * 
     * @return the RuntimeException to throw if it isn't.
     */
    abstract protected RuntimeException runtimeXIfNotClosed(RuntimeException runtimeX)
                    throws SQLException; 
}
