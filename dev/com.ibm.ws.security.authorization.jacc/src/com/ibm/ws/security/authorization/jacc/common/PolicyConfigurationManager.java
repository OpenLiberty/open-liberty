/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc.common;

import java.security.Policy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyContextException;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.security.authorization.jacc.ejb.EJBSecurityPropagator;

@Component(service = { ApplicationStateListener.class })
public class PolicyConfigurationManager implements ApplicationStateListener {
    private static final TraceComponent tc = Tr.register(PolicyConfigurationManager.class);
    private static final Map<String, List<PolicyConfiguration>> pcConfigsMap = new ConcurrentHashMap<String, List<PolicyConfiguration>>();
    private static final Map<String, List<String>> pcModulesMap = new ConcurrentHashMap<String, List<String>>();
    private static final Map<String, List<String>> pcEjbMap = new ConcurrentHashMap<String, List<String>>();
    private static final List<String> pcRunningList = new ArrayList<String>();

    private static PolicyConfigurationFactory pcf = null;
    private static Policy policy = null;
    private static EJBSecurityPropagator esp = null;

    // for listener..
    public PolicyConfigurationManager() {}

    public static void initialize(Policy policy, PolicyConfigurationFactory pcf) {
        PolicyConfigurationManager.policy = policy;
        PolicyConfigurationManager.pcf = pcf;
        pcConfigsMap.clear();
        pcModulesMap.clear();
        pcRunningList.clear();
    }

    public static void setEJBSecurityPropagator(EJBSecurityPropagator esp) {
        PolicyConfigurationManager.esp = esp;
    }

    public static void linkConfiguration(String appName, PolicyConfiguration pc) throws PolicyContextException {
        List<PolicyConfiguration> pcs = pcConfigsMap.get(appName);
        if (pcs != null) {
            pc.linkConfiguration(pcs.get(0));
        } else {
            pcs = new ArrayList<PolicyConfiguration>();
            pcConfigsMap.put(appName, pcs);
        }
        pcs.add(pc);
    }

    public static void addModule(String appName, String contextId) {
        List<String> ctxIds = pcModulesMap.get(appName);
        if (ctxIds == null) {
            ctxIds = new ArrayList<String>();
            pcModulesMap.put(appName, ctxIds);
        }
        if (!ctxIds.contains(contextId)) {
            ctxIds.add(contextId);
        }
        // if the application is aready running (most likely due to the defer load is eanbled),
        // commit immediately.
        if (isApplicationRunning(appName)) {
            processEJBs(appName);
            commitModules(appName);
        }
    }

    public static boolean containModule(String appName, String contextId) {
        List<String> ctxIds = pcModulesMap.get(appName);
        if (ctxIds != null && ctxIds.contains(contextId)) {
            return true;
        }
        return false;
    }

    public static void removeModule(String appName, String contextId) {
        List<String> ctxIds = pcModulesMap.get(appName);
        if (ctxIds != null) {
            int index = ctxIds.indexOf(contextId);
            if (index >= 0) {
                ctxIds.remove(index);
                if (ctxIds.isEmpty()) {
                    // if it is empty, delete it from the map.
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Removing application : " + appName);
                    pcModulesMap.remove(appName);
                }
            }
        }
    }

    public static void addEJB(String appName, String contextId) {
        // ejb modules are not processed yet, so store the propagater for propagating the data upon starting application.
        List<String> ctxIds = pcEjbMap.get(appName);
        if (ctxIds == null) {
            ctxIds = new ArrayList<String>();
            pcEjbMap.put(appName, ctxIds);
        }
        if (!ctxIds.contains(contextId)) {
            ctxIds.add(contextId);
        }
        addModule(appName, contextId);
    }

    public static boolean isApplicationRunning(String appName) {
        return pcRunningList.contains(appName);
    }

    protected static void processEJBs(String appName) {
        List<String> ctxIds = pcEjbMap.get(appName);
        if (ctxIds != null) {
            for (String contextId : ctxIds) {
                if (esp != null) {
                    esp.processEJBRoles(pcf, contextId);
                } else {
                    Tr.error(tc, "JACC_NO_EJB_PLUGIN");
                }
            }
            pcEjbMap.remove(appName);
        }
    }

    private static void commitModules(String appName) {
        List<PolicyConfiguration> pcs = pcConfigsMap.get(appName);
        if (pcs != null) {
            for (PolicyConfiguration pc : pcs) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Comitting PolicyConfigurations : " + pc);
                try {
                    pc.commit();
                } catch (PolicyContextException pce) {
                    String ctxId = null;
                    try {
                        ctxId = pc.getContextID();
                    } catch (PolicyContextException e) {
                        ctxId = "<<UNKNOWN>>";
                    }
                    Tr.error(tc, "JACC_GET_POLICYCONFIGURATION_FAILURE", new Object[] { ctxId, pce });
                }
            }
            policy.refresh();
        }
    }

    private static void removeModules(String appName) {
        List<String> ctxIds = pcModulesMap.get(appName);
        if (ctxIds != null) {
            for (String ctxId : ctxIds) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "contextID : " + ctxId);
                PolicyConfiguration pc = null;
                try {
                    pc = pcf.getPolicyConfiguration(ctxId, false);
                    if (pc != null) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Deleting PolicyConfigurations : " + pc);
                        pc.delete();
                    }
                } catch (PolicyContextException pce) {
                    Tr.error(tc, "JACC_GET_POLICYCONFIGURATION_FAILURE", new Object[] { ctxId, pce });
                }
            }
            policy.refresh();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "refresh is invoked after deleting PolicyConfigurations");
            pcModulesMap.remove(appName);
            pcConfigsMap.remove(appName);
        }
    }

    @Override
    public void applicationStarting(ApplicationInfo appInfo) {
        // do nothing.
    }

    @Override
    public void applicationStarted(ApplicationInfo appInfo) {

        String appName = appInfo.getDeploymentName();
        if (appName != null) {
            processEJBs(appName);
            commitModules(appName);
            pcRunningList.add(appName);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "commit all modules and refresh is invoked after the application is started. AppName : " + appName);
        }
    }

    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        // do nothing.
    }

    @Override
    public void applicationStopped(ApplicationInfo appInfo) {

        String appName = appInfo.getDeploymentName();
        if (appName != null) {
            removeModules(appName);
            pcRunningList.remove(appName);
        }
    }

}
