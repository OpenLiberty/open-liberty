/*******************************************************************************
 * Copyright (c) 2015,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.internal;

import java.io.File;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.concurrent.persistent.TaskState;
import com.ibm.websphere.concurrent.persistent.mbean.PersistentExecutorMBean;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.boot.jmx.service.MBeanMessageHelper;
import com.ibm.wsspi.concurrent.persistent.PartitionRecord;

/**
 * Implementation of the Persistent Executor MBean
 */
public class PersistentExecutorMBeanImpl extends StandardMBean implements PersistentExecutorMBean {
    private static final TraceComponent tc = Tr.register(PersistentExecutorMBeanImpl.class);

    private transient PersistentExecutorImpl _pe = null;
    private transient ObjectName obn = null;
    private transient ServiceRegistration<?> reg = null;

    private final AtomicLong exceptionCounter = new AtomicLong();

    public PersistentExecutorMBeanImpl(PersistentExecutorImpl pe) throws MalformedObjectNameException {
        super(PersistentExecutorMBean.class, false);

        _pe = pe;

        Config config = _pe.configRef.get();

        /* Build the ObjectName for the MBean */
        StringBuilder obnSb = new StringBuilder("WebSphere:feature=persistentExecutor,type=PersistentExecutorMBean");
        if (config.xpathId != null) {
            obnSb.append(",name=");
            obnSb.append(toObnString(config.xpathId));
        }
        if (config.jndiName != null)
        {
            obnSb.append(",jndiName=");
            obnSb.append(toObnString(config.jndiName));
        }
        if (config.id != null)
        {
            obnSb.append(",id=");
            obnSb.append(toObnString(config.id));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "ObjectName created with the string: " + obnSb.toString());
        }

        obn = new ObjectName(obnSb.toString());

    }

    public void register(BundleContext bndCtx) {
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put("jmx.objectname", this.obn.toString());

        this.reg = bndCtx.registerService(PersistentExecutorMBean.class.getName(), this, props);
    }

    public void unregister() {
        if (reg != null) {
            reg.unregister();
            reg = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.concurrent.persistent.mbean.PersistentExecutorMBean#findPartitonInfo(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public String[][] findPartitionInfo(String hostName, String userDir, String libertyServerName, String executorIdentifier) throws Exception {

        try {
            return _pe.findPartitionInfo(hostName, userDir, libertyServerName, executorIdentifier);
        } catch (Exception e) {
            throw buildAndLogException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.concurrent.persistent.mbean.PersistentExecutorMBean#findTaskIds(long, java.lang.String, boolean, java.lang.Long, java.lang.Integer)
     */
    @Override
    public Long[] findTaskIds(long partition, String state, boolean inState, Long minId, Integer maxResults) throws Exception {

        try {
            TaskState ts = TaskState.valueOf(state);
            return _pe.findTaskIds(partition, ts, inState, minId, maxResults);
        } catch (Exception e) {
            throw buildAndLogException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.concurrent.persistent.mbean.PersistentExecutorMBean#transfer(java.lang.Long, long)
     */
    @Override
    public int transfer(Long maxTaskId, long oldPartitionId) throws Exception {

        try {
            return _pe.transfer(maxTaskId, oldPartitionId);
        } catch (Exception e) {
            throw buildAndLogException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.concurrent.persistent.mbean.PersistentExecutorMBean#removePartitionInfo(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public int removePartitionInfo(String hostName, String userDir, String libertyServerName, String executorIdentifier) throws Exception {

        try {
            return _pe.removePartitionInfo(hostName, userDir, libertyServerName, executorIdentifier);
        } catch (Exception e) {
            throw buildAndLogException(e);
        }
    }

    @Override
    protected String getClassName(MBeanInfo info) {
        // Do not expose the internal class name (which is what the super-method gets)
        // instead, provide the interface name
        return PersistentExecutorMBean.class.getCanonicalName();
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "MBean for a Persistent Executor";
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

    /**
     * Builds a new client appropriate exception to replace the caught exception. This tells the
     * client to look in the logs for further information.
     * <br>
     * Logs the information from the original exception.
     * 
     * @param e internal exception which is caught
     * @return new client-appropriate exception
     * @throws Exception
     */
    private Exception buildAndLogException(Exception e) throws Exception {
        PartitionRecord pr = new PartitionRecord(false);
        pr.setExecutor(_pe.name);
        List<PartitionRecord> records = _pe.taskStore.find(pr);
        PartitionRecord record = records.get(0);

        String id = "PersistentExecutorMBean-".concat(String.valueOf(exceptionCounter.getAndIncrement()));

        String logDir = record.getUserDir().concat("servers/").concat(record.getLibertyServer()).concat("/logs/");
        Exception ex = new Exception(MBeanMessageHelper.getUnableToPerformOperationMessage(record.getLibertyServer(), record.getHostName(),
                                                                                           logDir.replace('/', File.separatorChar), id));

        Object[] serverStrings = { id, Utils.stackTraceToString(e) };
        Tr.error(tc, "CWWKC1559.mbean.operation.failure", serverStrings);

        return ex;
    }

}
