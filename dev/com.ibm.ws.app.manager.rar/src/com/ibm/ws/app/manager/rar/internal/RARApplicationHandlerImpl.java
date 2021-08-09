/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.rar.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.app.manager.AppMessageHelper;
import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.wsspi.adaptable.module.DefaultNotification;
import com.ibm.wsspi.adaptable.module.Notifier.Notification;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationHandler;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.application.handler.ApplicationMonitoringInformation;
import com.ibm.wsspi.application.handler.DefaultApplicationMonitoringInformation;
import com.ibm.wsspi.application.lifecycle.ApplicationStartBarrier;

@Component(service = { ApplicationHandler.class },
           property = { "service.vendor=IBM", "type:String=rar" })
public class RARApplicationHandlerImpl extends AppMessageHelper implements ApplicationHandler<DeployedAppInfo> {
    private static final TraceComponent _tc = Tr.register(RARApplicationHandlerImpl.class);

    /**
     * App manager messages to replace
     */
    private static final Map<String, String> MESSAGES = new HashMap<String, String>();
    static {
    	MESSAGES.put("APPLICATION_START_SUCCESSFUL", "J2CA7001.adapter.install.successful");
    	MESSAGES.put("APPLICATION_START_FAILED", "J2CA7002.adapter.install.failed");
    	MESSAGES.put("APPLICATION_UPDATE_SUCCESSFUL", "J2CA7001.adapter.install.successful");
    	MESSAGES.put("APPLICATION_UPDATE_FAILED", "J2CA7002.adapter.install.failed");
    	MESSAGES.put("CANNOT_CREATE_DIRECTORY", "J2CA7006.cannot.create.directory");
    	MESSAGES.put("APPLICATION_STOPPED", "J2CA7009.adapter.uninstalled");
    	MESSAGES.put("APPLICATION_STOP_FAILED", "J2CA7010.adapter.uninstall.failed");
    	MESSAGES.put("APPLICATION_NOT_STARTED", "J2CA7012.adapter.not.installed");
    	MESSAGES.put("DUPLICATE_APPLICATION_NAME", "J2CA7013.duplicate.adapter.name");
    	MESSAGES.put("APPLICATION_NOT_FOUND", "J2CA7014.adapter.not.found");
    	MESSAGES.put("STARTING_APPLICATION", "J2CA7018.installing.adapter");
    	MESSAGES.put("APPLICATION_NOT_UPDATED", "J2CA7020.adapter.not.updated");
    	MESSAGES.put("APPLICATION_AT_LOCATION_NOT_VALID", "J2CA7021.adapter.at.location.not.valid");
    	MESSAGES.put("APPLICATION_SLOW_STARTUP", "J2CA7022.adapter.slow.install");
    	MESSAGES.put("MONITOR_APP_STOP_FAIL", "J2CA7053.monitor.adapter.uninstall.fail");
    	MESSAGES.put("MONITOR_APP_START_FAIL", "J2CA7056.monitor.adapter.install.fail");
    	MESSAGES.put("INVALID_DELETE_OF_APPLICATION", "J2CA7059.invalid.delete.of.adapter");
    	MESSAGES.put("APPLICATION_MONITORING_FAIL", "J2CA7060.adapter.monitoring.fail");
    }

    private FutureMonitor futureMonitor;
    private DeployedAppInfoFactory deployedAppFactory;

    @Reference
    protected void setFutureMonitor(FutureMonitor fm) {
        futureMonitor = fm;
    }

    @Reference(target = "(type=rar)")
    protected void setDeployedAppFactory(DeployedAppInfoFactory factory) {
        deployedAppFactory = factory;
    }

    @Override
    public ApplicationMonitoringInformation setUpApplicationMonitoring(ApplicationInformation<DeployedAppInfo> applicationInformation) {
    	//This will allow the application to use the CompleteApplicationListener allowing recursive file monitoring
        return new DefaultApplicationMonitoringInformation(null, true);
    }

    @Override
    public Future<Boolean> install(ApplicationInformation<DeployedAppInfo> applicationInformation) {
        final Future<Boolean> result = futureMonitor.createFuture(Boolean.class);

        RARDeployedAppInfo deployedApp;
        try {
            deployedApp = (RARDeployedAppInfo) deployedAppFactory.createDeployedAppInfo(applicationInformation);
        } catch (UnableToAdaptException ex) {
            futureMonitor.setResult(result, ex);
            return result;
        }

        if (!deployedApp.deployApp(result)) {
            futureMonitor.setResult(result, false);
            return result;
        }

        return result;
    }

    @Override
    public Future<Boolean> uninstall(ApplicationInformation<DeployedAppInfo> applicationInformation) {
        RARDeployedAppInfo deployedApp = (RARDeployedAppInfo) applicationInformation.getHandlerInfo();
        if (deployedApp == null) {
            // Somebody asked us to remove an app we don't know about
            return futureMonitor.createFutureWithResult(false);
        }

        boolean success = deployedApp.uninstallApp();
        return futureMonitor.createFutureWithResult(success);
    }

	@Override
	@Trivial
	public void audit(String key, Object... params) {
		String newKey = MESSAGES.get(key);
		if (newKey == null)
			super.audit(key, params);
		else
			Tr.audit(_tc, newKey, params);
	}

	@Override
	@Trivial
	public void error(String key, Object... params) {
		String newKey = MESSAGES.get(key);
		if (newKey == null)
			super.error(key, params);
		else
			Tr.error(_tc, newKey, params);
	}

	@Override
	@Trivial
	public void info(String key, Object... params) {
		String newKey = MESSAGES.get(key);
		if (newKey == null)
			super.info(key, params);
		else
			Tr.info(_tc, newKey, params);
	}

	@Override
	@Trivial
	public void warning(String key, Object... params) {
		String newKey = MESSAGES.get(key);
		if (newKey == null)
			super.warning(key, params);
		else
			Tr.warning(_tc, newKey, params);
	}
	
    @Reference
	private void setApplicationStartBarrier(ApplicationStartBarrier applicationStartBarrier) {
    }
}
