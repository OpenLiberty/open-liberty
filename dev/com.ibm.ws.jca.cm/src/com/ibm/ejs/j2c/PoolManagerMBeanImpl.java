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
package com.ibm.ejs.j2c;

import java.util.Arrays;
import java.util.Hashtable;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.StandardMBean;
import javax.resource.ResourceException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean;

public class PoolManagerMBeanImpl extends StandardMBean implements ConnectionManagerMBean {
    private static final TraceComponent tc = Tr.register(PoolManagerMBeanImpl.class, J2CConstants.traceSpec, J2CConstants.messageFile);

    private transient PoolManager _pm = null;
    private transient Version jdbcRuntimeVersion;
    private transient ObjectName obn = null;
    private transient ServiceRegistration<?> reg = null;
    private final String nl = CommonFunction.nl;

    /**
     * Creates an MBean for a given PoolManager object, and registers
     * the MBean in the MBeanServer.
     *
     * @throws MalformedObjectNameException The ObjectName obtained by the PoolManager is invalid.
     */
    public PoolManagerMBeanImpl(PoolManager pm, Version jdbcRuntimeVersion) throws MalformedObjectNameException {
        super(ConnectionManagerMBean.class, false);

        _pm = pm;
        this.jdbcRuntimeVersion = jdbcRuntimeVersion;

        /* Build the ObjectName for the MBean */
        StringBuilder obnSb = new StringBuilder("WebSphere:type=" + ConnectionManagerMBean.class.getCanonicalName());
        if (_pm.gConfigProps.getJNDIName() != null) {
            obnSb.append(",jndiName=");
            obnSb.append(toObnString(_pm.gConfigProps.getJNDIName()));
        }

        if (_pm.gConfigProps.getXpathId() != null) {
            obnSb.append(",name=");
            obnSb.append(toObnString(_pm.gConfigProps.getXpathId()));
        }
        if (_pm.gConfigProps.appName != null) {
            obnSb.append(",application=");
            obnSb.append(toObnString(_pm.gConfigProps.appName));
        }
        if (_pm.gConfigProps.modName != null) {
            obnSb.append(",module=");
            obnSb.append(toObnString(_pm.gConfigProps.modName));
        }
        if (_pm.gConfigProps.compName != null) {
            obnSb.append(",component=");
            obnSb.append(toObnString(_pm.gConfigProps.compName));
        }

        try {
            obn = new ObjectName(obnSb.toString());
        } catch (MalformedObjectNameException e) {
            FFDCFilter.processException(e, getClass().getName(), "79", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Unable to create ObjectName with the string: " + obnSb.toString(), e);
            }
            throw e;
        }
    }

    public void register(BundleContext bndCtx) {
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put("jmx.objectname", this.obn.toString());

        this.reg = ConnectionManagerServiceImpl.priv.registerService(bndCtx, ConnectionManagerMBean.class.getName(), this, props);
    }

    public void unregister() {
        reg.unregister();
        reg = null;
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "invoke");

        Object o = null;

        if ("showPoolContents".equalsIgnoreCase(actionName)) {
            o = showPoolContents();
        } else if ("purgePoolContents".equalsIgnoreCase(actionName)) {
            if (params == null) {
                purgePoolContents("normal");
            } else if (params.length == 1 && params[0] instanceof java.lang.String) {
                purgePoolContents((String) params[0]);
            } else {
                throw new MBeanException(new IllegalArgumentException(Arrays.toString(params)), Tr.formatMessage(tc, "INVALID_MBEAN_INVOKE_PARAM_J2CA8060", Arrays.toString(params),
                                                                                                                 actionName));
            }
        } else {
            throw new MBeanException(new IllegalArgumentException(actionName), Tr.formatMessage(tc, "INVALID_MBEAN_INVOKE_ACTION_J2CA8061", actionName, "ConnectionManager"));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "invoke");

        return o;
    }

    @Override
    protected String getClassName(MBeanInfo info) {
        // Do not expose the internal class name (which is what the super-method gets)
        // instead, provide the interface name
        return ConnectionManagerMBean.class.getCanonicalName();
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "MBean for a Connection Manager";
    }

    @Override
    public MBeanInfo getMBeanInfo() {

        String cname = getClassName(null);
        String text = "MBean for a Connection Manager";

        String purgeParamDesc = "Use 'immediate' to purge the pool contents immediately. " +
                                "Any other string value will purge the pool contents normally.";
        // The abort option should only be visible on >= jdbc-4.1
        if (atLeastJDBCVersion(new Version(4, 1, 0)))
            purgeParamDesc = "Use 'abort' to abort all connections in the pool using Connection.abort(). " + purgeParamDesc;

        final MBeanParameterInfo p0 = new MBeanParameterInfo("arg0", String.class.getCanonicalName(), purgeParamDesc);
        MBeanParameterInfo[] purgeParams = new MBeanParameterInfo[] { p0 };

        final MBeanOperationInfo op0 = new MBeanOperationInfo("purgePoolContents", "Purges the contents of the Connection Manager.", purgeParams, Void.class.getCanonicalName(), MBeanOperationInfo.ACTION);
        final MBeanOperationInfo op1 = new MBeanOperationInfo("showPoolContents", "Displays the current contents of the Connection Manager in a human readable format.", null, String.class.getCanonicalName(), MBeanOperationInfo.INFO);
        final MBeanOperationInfo[] ops = { op0, op1 };

        final MBeanInfo nmbi = new MBeanInfo(cname, text, null, null, ops, null, null);

        return nmbi;
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        String returnValue = null;
        if (attribute.equals("size")) {
            returnValue = _pm.getTotalConnectionCount().toString();
        } else {
            throw new AttributeNotFoundException(attribute);
        }
        return returnValue;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return new StringBuffer("ConnectionManagerMBean@").append(Integer.toHexString(hashCode())).append(' ').append(obn.toString()).toString();
    }

    /**
     * <pre>
     * Example output:
     * PoolManager@2799d765
     * name=application[jdbcapp]/module[basicfat.war]/dataSource[java:comp/env/jdbc/dsfat6]/connectionManager
     * jndiName=java:comp/env/jdbc/dsfat6
     * maxPoolSize=50
     * size=6
     * waiting=0
     * unshared=1
     * &nbsp;ManagedConnection@8388c2ea=ActiveInUse thread=31
     * shared=1
     * &nbsp;ManagedConnection@1125a4bd=ActiveInTransaction thread=2B transaction=486907592 connectionHandles=1
     * available=4
     * &nbsp;ManagedConnection@e672b048=Reusable
     * &nbsp;ManagedConnection@4975c4b0=Reusable
     * &nbsp;ManagedConnection@a25213d2=Reusable
     * &nbsp;ManagedConnection@89f761d2=Reusable
     * </pre>
     */
    private String toStringExternal() {
        StringBuffer buf = new StringBuffer();
        StringBuffer unsharedBuf = new StringBuffer();
        StringBuffer sharedBuf = new StringBuffer();
        StringBuffer freeBuf = new StringBuffer();

        buf.append("PoolManager@");
        buf.append(Integer.toHexString(hashCode()));
        buf.append(nl);
        buf.append("name=");
        buf.append(obn.toString());
        buf.append(nl);
        buf.append("jndiName=");
        if (_pm.gConfigProps == null || _pm.gConfigProps.getJNDIName() == null) {
            buf.append("none");
        } else {
            buf.append(_pm.gConfigProps.getJNDIName());
        }
        buf.append(nl);
        buf.append("maxPoolSize=");
        buf.append(_pm.maxConnections);
        buf.append(nl);
        buf.append("size=");
        buf.append(_pm.totalConnectionCount.get());
        buf.append(nl);
        int numUnsharedConnections = 0, numSharedConnections = 0, numFreeConnections = 0, numWaitingConnections = 0;
        _pm.mcToMCWMapWrite.lock();
        try {
            int mcToMCWSize = _pm.mcToMCWMap.size();
            if (mcToMCWSize > 0) {
                Object[] tempObject = _pm.mcToMCWMap.values().toArray();
                com.ibm.ejs.j2c.MCWrapper mcw = null;
                for (int ti = 0; ti < mcToMCWSize; ++ti) {
                    mcw = (com.ibm.ejs.j2c.MCWrapper) tempObject[ti];
                    switch (mcw.getPoolState()) {
                        case 1: // In free pool (aka available)
                            ++numFreeConnections;
                            freeBuf.append(" ManagedConnection@");
                            freeBuf.append(Integer.toHexString(mcw.hashCode()));
                            freeBuf.append('=');
                            // Append a state string to the connection
                            freeBuf.append(translateStateString(mcw.getStateString()));
                            freeBuf.append(nl);
                            break;
                        case 2: // In shared pool
                            ++numSharedConnections;
                            sharedBuf.append(" ManagedConnection@");
                            sharedBuf.append(Integer.toHexString(mcw.hashCode()));
                            sharedBuf.append('=');
                            sharedBuf.append(translateStateString(mcw.getStateString()));
                            // Check if connection is about to be purged
                            if (mcw.isDestroyState()
                                || mcw.isStale()
                                || mcw.hasFatalErrorNotificationOccurred(_pm.freePool[0].getFatalErrorNotificationTime())
                                || ((_pm.agedTimeout != -1) && (mcw.hasAgedTimedOut(_pm.agedTimeoutMillis)))) {
                                sharedBuf.append("ToBePurged");
                            }
                            sharedBuf.append(" thread=");
                            sharedBuf.append(mcw.getThreadID());
                            sharedBuf.append(" transaction=");
                            sharedBuf.append(mcw.getTranWrapperId());
                            sharedBuf.append(" connectionHandles=");
                            sharedBuf.append(mcw.mcwHandleList.size());
                            sharedBuf.append(nl);
                            break;
                        case 3: // In unshared pool
                            ++numUnsharedConnections;
                            unsharedBuf.append(" ManagedConnection@");
                            unsharedBuf.append(Integer.toHexString(mcw.hashCode()));
                            unsharedBuf.append('=');
                            unsharedBuf.append(translateStateString(mcw.getStateString()));
                            // Check if connection is about to be purged
                            if (mcw.isStale()
                                || mcw.hasFatalErrorNotificationOccurred(_pm.freePool[0].getFatalErrorNotificationTime())
                                || ((_pm.agedTimeout != -1) && (mcw.hasAgedTimedOut(_pm.agedTimeoutMillis)))) {
                                unsharedBuf.append("ToBePurged");
                            }
                            unsharedBuf.append(" thread=");
                            unsharedBuf.append(mcw.getThreadID());
                            unsharedBuf.append(nl);
                            break;
                        case 4: // In waiter pool
                            ++numWaitingConnections;
                            break;
                        default:
                            break;
                    }
                }
            }
        } finally {
            _pm.mcToMCWMapWrite.unlock();
        }
        // Waiting info
        buf.append("waiting=");
        buf.append(numWaitingConnections);
        buf.append(nl);
        // Unshared info
        buf.append("unshared=");
        buf.append(numUnsharedConnections);
        buf.append(nl);
        if (numUnsharedConnections > 0) {
            buf.append(unsharedBuf);
        }
        //Shared info
        buf.append("shared=");
        buf.append(numSharedConnections);
        buf.append(nl);
        if (numSharedConnections > 0) {
            buf.append(sharedBuf);
        }
        // Free info
        buf.append("available=");
        buf.append(numFreeConnections);
        buf.append(nl);
        if (numFreeConnections > 0) {
            buf.append(freeBuf);
        }

        return buf.toString();
    }

    private String translateStateString(String stateStr) {
        if (stateStr.equalsIgnoreCase("state_new")) {
            return "new";
        } else if (stateStr.equalsIgnoreCase("state_active_free")) {
            return "Reusable";
        } else if (stateStr.equalsIgnoreCase("state_active_inuse")) {
            return "ActiveInUse";
        } else if (stateStr.equalsIgnoreCase("STATE_TRAN_WRAPPER_INUSE")) {
            return "ActiveInTransaction";
        } else if (stateStr.equalsIgnoreCase("STATE_INACTIVE")) {
            return "Reusable";
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "MCWrapper.getStateString() returned an unknown state");
            }
            return "Unknown";
        }
    }

    /**
     * Takes in any string value and replaces any of the reserved ObjectName
     * chars with periods (.). The 4 reserved ObjectName chars are:<br>
     * : = , "
     *
     * @param s The string to be converted to an ObjectName-safe string
     */
    private String toObnString(String s) {
        return s.replace(':', '.').replace('=', '.').replace(',', '.').replace('"', '.');
    }

    private boolean atLeastJDBCVersion(Version v) {
        return jdbcRuntimeVersion != null && jdbcRuntimeVersion.compareTo(v) >= 0;
    }

    @Override
    public void purgePoolContents(String doImmediately) throws MBeanException {
        try {
            _pm.purgePoolContents(doImmediately);
        } catch (ResourceException e) {
            throw new MBeanException(e);
        }
    }

    @Override
    public String showPoolContents() {
        return toStringExternal();
    }
}
