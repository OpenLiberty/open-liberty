/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.ws.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.jbatch.container.ws.BatchJobNotLocalException;
import com.ibm.jbatch.container.ws.BatchLocationService;
import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.WSRemotablePartitionExecution;
import com.ibm.websphere.kernel.server.ServerInfoMBean;

/**
 * Resolves the batch REST url based on endpoint, server, and/or system config.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class BatchLocationServiceImpl implements BatchLocationService {

    /**
     * If the batch REST url cannot be resolved because the SSL endpoint is
     * not enabled, then this value is returned from getBatchRestUrl.
     */
    protected static final String BatchRestUrlUnavailable = "SSL-ENDPOINT-UNAVAILABLE";

    /**
     * For getting the https host+port
     */
    private DynamicMBean endpointInfoMBean;

    /**
     * For resolving the hostname
     */
    private ServerInfoMBean serverInfoMBean;

    /**
     * DS injection.
     * Note: If SSL is not enabled (e.g. if the keystore is not defined), then the SSL endpoint
     * will not be started and this reference will not be satisfied; hence it's OPTIONAL.
     *
     * If the SSL endpoint is not started, then we cannot resolve the BatchRestUrl, since it
     * requires the SSL endpoint. In that case we assign the constant BatchRestUrlUnavailable
     * to all new jobexecutions.
     *
     */
    @Reference(target = "(jmx.objectname=WebSphere:feature=channelfw,type=endpoint,name=defaultHttpEndpoint-ssl)", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setEndPointInfoMBean(DynamicMBean endpointInfoMBean) {
        this.endpointInfoMBean = endpointInfoMBean;
    }

    /**
     * DS un-inject
     */
    protected void unsetEndPointInfoMBean(DynamicMBean endpointInfoMBean) {
        if (this.endpointInfoMBean == endpointInfoMBean) {
            this.endpointInfoMBean = null;
        }
    }

    /**
     * DS injection
     */
    @Reference
    protected void setServerInfoMBean(ServerInfoMBean serverInfoMBean) {
        this.serverInfoMBean = serverInfoMBean;
    }

    /**
     * @return the batch REST url for this server.
     */
    @Override
    public String getBatchRestUrl() {
        if (endpointInfoMBean == null) {
            return BatchRestUrlUnavailable;
        }

        try {
            String host = resolveHost((String) endpointInfoMBean.getAttribute("Host"));
            int port = (Integer) endpointInfoMBean.getAttribute("Port");
            return "https://" + host + ":" + port + "/ibm/api/batch";
        } catch (MBeanException mbe) {
            throw new BatchRuntimeException(mbe);
        } catch (ReflectionException re) {
            throw new BatchRuntimeException(re);
        } catch (AttributeNotFoundException anfe) {
            throw new BatchRuntimeException(anfe);
        }
    }

    /**
     * If the given host is "*", try to resolve this to a hostname or ip address
     * by first checking the configured ${defaultHostName}. If ${defaultHostName} is
     * "*", "localhost", or not specified, try obtaining the local ip address via InetAddress.
     *
     * @return the resolved host, or "localhost" if the host could not be resolved
     */
    protected String resolveHost(String host) {
        if ("*".equals(host)) {
            // Check configured ${defaultHostName}
            host = serverInfoMBean.getDefaultHostname();
            if (host == null || host.equals("localhost")) {
                // This is, as a default, not useful. Use the local IP address instead.
                host = getLocalHostIpAddress();
            }
        }
        return (host == null || host.trim().isEmpty()) ? "localhost" : host;
    }

    /**
     * @return InetAddress.getLocalHost().getHostAddress(); or null if that fails.
     */
    protected String getLocalHostIpAddress() {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws UnknownHostException {
                    return InetAddress.getLocalHost().getHostAddress();
                }
            });

        } catch (PrivilegedActionException pae) {
            // FFDC it
            return null;
        }
    }

    /**
     * @return unique identity for this server: ${defaultHostName}/${wlp.user.dir}/serverName
     */
    @Override
    public String getServerId() {
        String fixedUserDir = stripFileSeparateAtTheEnd(serverInfoMBean.getUserDirectory().trim());
        return serverInfoMBean.getDefaultHostname() + "/" + fixedUserDir + "/" + serverInfoMBean.getName();
    }

    /**
     * remove separator at the end of userDir directory if exist
     *
     * @param path
     * @return
     */
    protected String stripFileSeparateAtTheEnd(String path) {
        return (path != null && (path.endsWith("/") || path.endsWith("\\"))) ? path.substring(0, path.length() - 1) : path;
    }

    /**
     * Note: This method compares the jobexecution's REST url with this server's REST url.
     * If they are different, and the jobexecution's REST url is equal to the
     * BatchRestUrlUnavailable constant, that means the jobexecution was created
     * prior to batchManagement-1.0 being enabled. In that case, this method
     * compares the jobexecution's serverId with this server's serverId. The serverId
     * is slightly less reliable since it depends on configuration... but then again,
     * so does the REST url... so neither is that reliable but the REST url feels
     * more reliable so we prefer that one.
     *
     * @return true if the given jobexecution ran (or is running) on this server.
     */
    @Override
    public boolean isLocalJobExecution(WSJobExecution jobExecution) {
        return jobExecution.getServerId() == null
               || jobExecution.getRestUrl() == null // If server ID or rest URL were never set, we can treat as local
               || getBatchRestUrl().equals(jobExecution.getRestUrl())
               || (BatchRestUrlUnavailable.equals(jobExecution.getRestUrl())
                   && getServerId().equals(jobExecution.getServerId()));

    }

    /**
     * @return true if the given jobexecution ran (or is running) on this server.
     */
    @Override
    public boolean isLocalJobExecution(long executionId) {
        JobExecution jobExecution = BatchRuntime.getJobOperator().getJobExecution(executionId);
        return (jobExecution instanceof WSJobExecution) ? isLocalJobExecution((WSJobExecution) jobExecution) : true;
    }

    /**
     * @param partition
     * @return true if the given remotable partition ran (or is running) on this server
     */
    @Override
    public boolean isLocalRemotablePartition(WSRemotablePartitionExecution partition) {
        return getBatchRestUrl().equals(partition.getRestUrl());
    }

    /**
     * @throws BatchJobNotLocalException if the given execution did not execute here in this server.
     */
    @Override
    public JobExecution assertIsLocalJobExecution(long executionId) throws BatchJobNotLocalException {
        JobExecution jobExecution = BatchRuntime.getJobOperator().getJobExecution(executionId);
        if (jobExecution instanceof WSJobExecution && !isLocalJobExecution((WSJobExecution) jobExecution)) {
            throw new BatchJobNotLocalException((WSJobExecution) jobExecution, getBatchRestUrl(), getServerId());
        }
        return jobExecution;
    }

}
