/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logstash.collector.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.collector.ClientPool;
import com.ibm.ws.collector.Collector;
import com.ibm.ws.collector.Target;
import com.ibm.ws.collector.TaskManager;
import com.ibm.ws.http.logging.internal.ConfigurationSetterLogstash;
import com.ibm.ws.logging.collector.CollectorJsonUtils;
import com.ibm.ws.logstash.collector.LogstashRuntimeVersion;
import com.ibm.ws.lumberjack.LumberjackEvent;
import com.ibm.ws.lumberjack.LumberjackEvent.Entry;
import com.ibm.wsspi.collector.manager.Handler;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;

/**
 * Logstash collector reads data from relevant sources and sends it
 * to a Logstash instance using Lumberjack protocol
 */

@Component(name = LogstashCollector.COMPONENT_NAME, service = { Handler.class }, configurationPolicy = ConfigurationPolicy.REQUIRE, property = { "service.vendor=IBM" })
public class LogstashCollector extends Collector {

    private static final TraceComponent tc = Tr.register(LogstashCollector.class, "logstashCollector",
                                                         "com.ibm.ws.logstash.collector.internal.resources.LoggingMessages");

    public static final String COMPONENT_NAME = "com.ibm.ws.logstash.collector.internal.LogstashCollector";

    private static final String SERVER_HOST_NAME_KEY = "sourceHostName";
    private static final String SERVER_NAME_KEY = "sourceServerName";
    private static final String SERVER_DEFAULT_HOST_NAME_KEY = "sourceDefaultHostName";

    private static final String VAR_DEFAULTHOSTNAME = "${defaultHostName}";
    private static final String VAR_WLPSERVERNAME = "${wlp.server.name}";
    private static final String VAR_WLPUSERDIR = "${wlp.user.dir}";
    private static final String ENV_VAR_CONTAINERHOST = "${env.CONTAINER_HOST}";
    private static final String ENV_VAR_CONTAINERNAME = "${env.CONTAINER_NAME}";

    /** Client pool size **/
    private static final int POOL_SIZE = 4;

    /** Server service references */
    private static final String SSL_SUPPORT_SERVICE = "sslSupportService";
    private static final String LOGSTASH_VERSION_SERVICE = "logstashVersionService";
    private static final String VARIABLE_REGISTRY_SERVICE = "variableRegistryService";
    private final AtomicServiceReference<SSLSupport> sslSupportServiceRef = new AtomicServiceReference<SSLSupport>(SSL_SUPPORT_SERVICE);
    private final AtomicServiceReference<LogstashRuntimeVersion> logstashVersionServiceRef = new AtomicServiceReference<LogstashRuntimeVersion>(LOGSTASH_VERSION_SERVICE);
    private final AtomicServiceReference<VariableRegistry> variableRegistryServiceRef = new AtomicServiceReference<VariableRegistry>(VARIABLE_REGISTRY_SERVICE);

    /** Server instance details */
    protected String serverName = "";
    protected String serverUserDir = "";
    protected String serverHostName = "";

    private TaskManager taskMgr = null;

    private String logstashVersion;

    private String jsonAccessLogFields;

    public ConfigurationSetterLogstash configSetter;

    @Override
    @Reference(name = EXECUTOR_SERVICE, service = ExecutorService.class)
    protected void setExecutorService(ServiceReference<ExecutorService> executorService) {
        executorServiceRef.setReference(executorService);
    }

    @Override
    protected void unsetExecutorService(ServiceReference<ExecutorService> executorService) {
        executorServiceRef.unsetReference(executorService);
    }

    @Reference(name = SSL_SUPPORT_SERVICE, service = SSLSupport.class)
    protected void setSslSupportService(ServiceReference<SSLSupport> sslSupport) {
        sslSupportServiceRef.setReference(sslSupport);
    }

    protected void updatedSslSupportService(ServiceReference<SSLSupport> sslSupport) {
        sslSupportServiceRef.setReference(sslSupport);
        if (taskMgr != null)
            taskMgr.updateConfig();
    }

    protected void unsetSslSupportService(ServiceReference<SSLSupport> sslSupport) {
        sslSupportServiceRef.unsetReference(sslSupport);
    }

    @Reference(name = LOGSTASH_VERSION_SERVICE, service = LogstashRuntimeVersion.class)
    protected void setLogstashVersionService(ServiceReference<LogstashRuntimeVersion> logstashRuntimeVersion) {
        logstashVersionServiceRef.setReference(logstashRuntimeVersion);
    }

    protected void unsetLogstashVersionService(ServiceReference<LogstashRuntimeVersion> logstashRuntimeVersion) {
        logstashVersionServiceRef.unsetReference(logstashRuntimeVersion);
    }

    @Reference(name = VARIABLE_REGISTRY_SERVICE, service = VariableRegistry.class)
    protected void setVariableRegistryService(ServiceReference<VariableRegistry> variableRegistry) {
        variableRegistryServiceRef.setReference(variableRegistry);
    }

    protected void unsetVariableRegistryService(ServiceReference<VariableRegistry> variableRegistry) {
        variableRegistryServiceRef.unsetReference(variableRegistry);
    }

    @Override
    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> configuration) {
        super.activate(cc, configuration);
        sslSupportServiceRef.activate(cc);
        logstashVersionServiceRef.activate(cc);
        variableRegistryServiceRef.activate(cc);
        setLogstashVersion();
        setJsonAccessLogFields(configuration);
        if (configSetter != null) {
            configSetter.setConfig(jsonAccessLogFields);
        }
        //Get the server instance details
        setServerInfo(configuration);
        setConfigInfo(configuration);
        validateSources(configuration);
        if (taskMgr != null) {
            taskMgr.updateConfig();
            //Start the task manager
            executorServiceRef.getService().submit(taskMgr);
        }
    }

    @Override
    @Deactivate
    protected void deactivate(ComponentContext cc, int reason) {
        taskMgr.close();
        super.deactivate(cc, reason);
        sslSupportServiceRef.deactivate(cc);
        logstashVersionServiceRef.deactivate(cc);
        variableRegistryServiceRef.deactivate(cc);
    }

    @Override
    @Modified
    protected void modified(Map<String, Object> configuration) {
        super.modified(configuration);
        setServerInfo(configuration);
        setConfigInfo(configuration);
        validateSources(configuration);
        setJsonAccessLogFields(configuration);
        if (configSetter != null) {
            configSetter.setConfig(jsonAccessLogFields);
        }
        if (taskMgr != null) {
            taskMgr.updateConfig();
        }
    }

    private void validateSources(Map<String, Object> config) {
        if (config.containsKey(SOURCE_LIST_KEY)) {
            String[] sourceList = (String[]) config.get(SOURCE_LIST_KEY);
            if (sourceList != null) {
                for (String source : sourceList) {
                    if (getSourceName(source.trim()).isEmpty()) {
                        Tr.warning(tc, "LOGSTASH_SOURCE_UNKNOWN", source);
                    }
                }
            }
        }
    }

    private void setLogstashVersion() {
        logstashVersion = logstashVersionServiceRef.getService().getVersion().toString();
    }

    private void setConfigInfo(Map<String, Object> config) {
        taskMgr.setConfigInfo(config);
    }

    private void setJsonAccessLogFields(Map<String, Object> configuration) {
        jsonAccessLogFields = (String) configuration.get("jsonAccessLogFields");
    }

    @Reference
    public void setConfigurationSetter(ConfigurationSetterLogstash configSetter) {
        this.configSetter = configSetter;
    }

    private void setServerInfo(Map<String, Object> configuration) {
        String serverName = (String) configuration.get(SERVER_NAME_KEY);
        String serverHostName = (String) configuration.get(SERVER_HOST_NAME_KEY);
        String serverDefaultHostName = (String) configuration.get(SERVER_DEFAULT_HOST_NAME_KEY);
        String serverUserDir = variableRegistryServiceRef.getService().resolveString(VAR_WLPUSERDIR);

        if (serverName.trim().isEmpty()) {
            serverName = variableRegistryServiceRef.getService().resolveString(ENV_VAR_CONTAINERNAME);
            if (ENV_VAR_CONTAINERNAME.equals(serverName)) {
                serverName = variableRegistryServiceRef.getService().resolveString(VAR_WLPSERVERNAME);
            }
            //None of the variables resolved, set the server name back to an empty string.
            if (VAR_WLPSERVERNAME.equals(serverName)) {
                serverName = "";
            }
        }
        this.serverName = serverName;

        if (VAR_WLPUSERDIR.equals(serverUserDir)) {
            serverUserDir = "";
        }
        this.serverUserDir = serverUserDir;

        if (serverHostName.trim().isEmpty()) {
            serverHostName = variableRegistryServiceRef.getService().resolveString(ENV_VAR_CONTAINERHOST);
            if (ENV_VAR_CONTAINERHOST.equals(serverHostName)) {
                serverHostName = serverDefaultHostName;
            }
            //defaultHostName variable did not resolve or has resolved to "localhost"
            if (VAR_DEFAULTHOSTNAME.equals(serverHostName) || serverHostName.equals("localhost")) {
                try {
                    serverHostName = AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                        @Override
                        public String run() throws UnknownHostException {
                            return InetAddress.getLocalHost().getCanonicalHostName();
                        }
                    });

                } catch (PrivilegedActionException pae) {
                    serverHostName = "";
                }
            }
        }
        this.serverHostName = serverHostName;
    }

    @Override
    public String getHandlerName() {
        return COMPONENT_NAME;
    }

    @Override
    public Object formatEvent(String source, String location, Object event, String[] tags, int maxFieldLength) {

        String eventType = CollectorJsonUtils.getEventType(source, location);
        String jsonStr = CollectorJsonUtils.jsonifyEvent(event, eventType, serverName, serverUserDir, serverHostName, logstashVersion, tags, maxFieldLength);
        LumberjackEvent<String, String> lumberjackEvent = null;
        if (!jsonStr.isEmpty()) {

            lumberjackEvent = new LumberjackEvent<String, String>();
            lumberjackEvent.add(new Entry<String, String>("line", jsonStr));
            lumberjackEvent.add(new Entry<String, String>("type", eventType));
        }
        return lumberjackEvent;
    }

    /** {@inheritDoc} */
    @Override
    public Target getTarget() {
        if (taskMgr == null) {
            taskMgr = new TaskManager(sslSupportServiceRef, executorServiceRef, POOL_SIZE) {
                @Override
                public ClientPool createClientPool(String sslConfig, SSLSupport sslSupport, int numClients) throws SSLException {
                    return new LogstashClientPool(sslConfig, sslSupport, numClients);
                }
            };
        }
        return taskMgr;
    }

}
