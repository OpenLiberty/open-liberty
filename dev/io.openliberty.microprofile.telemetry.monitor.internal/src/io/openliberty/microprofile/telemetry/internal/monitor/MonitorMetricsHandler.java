/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.monitor;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.kernel.productinfo.ProductInfo;

import io.openliberty.microprofile.telemetry.internal.monitor.internal.MappingTable;
import io.openliberty.microprofile.telemetry.internal.monitor.internal.MonitorMetrics;

@Component(service = MonitorMetricsHandler.class, name = "io.openliberty.microprofile.telemetry.internal.monitor.MonitorMetricsHandler", property = {
		"service.vendor=IBM" }, immediate = true)
public class MonitorMetricsHandler {

	private static final TraceComponent tc = Tr.register(MonitorMetricsHandler.class);

	protected ExecutorService execServ;
	protected MappingTable mappingTable;
	protected Set<MonitorMetrics> mmonitorMetricsSet = new HashSet<MonitorMetrics>();
	protected NotificationListener listener;

	@Activate
	protected void activate(ComponentContext context) {
		Tr.debug(tc, "DEBUGGING: > activate()");
		this.mappingTable = MappingTable.getInstance();
		register();
		addMBeanListener();
	}

	@Reference
	public void setExecutorService(ExecutorService execServ) {
		Tr.debug(tc, "DEBUGGING: > setExecutorService()");
		this.execServ = execServ;
	}

	public void unsetExecutorService(ExecutorService execServ) {
		Tr.debug(tc, "DEBUGGING: > unsetExecutorService()");
		this.execServ = null;
	}

	@Deactivate
	protected void deactivate(ComponentContext context) {
		Tr.debug(tc, "DEBUGGING: > deactivate()");
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		if (listener != null) {
			try {
				mbs.removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, listener);
			} catch (Exception e) {
				if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
					Tr.debug(tc, "deactivate exception message: ", e.getMessage());
				}
				FFDCFilter.processException(e, getClass().getSimpleName(), "deactivate:Exception");
			}
			listener = null;
		}
	}

	protected void addMBeanListener() {
		listener = new NotificationListener() {

			@Override
			public void handleNotification(Notification notification, Object handback) {
				Tr.debug(tc, "DEBUGGING: >> handleNotification()");
				MBeanServerNotification mbsn = (MBeanServerNotification) notification;
				String objectName = mbsn.getMBeanName().toString();
				Tr.debug(tc, "DEBUGGING: > handleNotification() objectName: " + objectName);
				if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(mbsn.getType())) {
					if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
						Tr.debug(tc, "MBean Registered [", objectName + "]");
					}
					String[][] data = mappingTable.getData(objectName);
					if (data != null) {
						register(objectName, data);
					}
				} else if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(mbsn.getType())) {
					if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
						Tr.debug(tc, "MBean Unregistered [" + objectName + "]");
					}
					if (mappingTable.contains(objectName)) {
						unregister(objectName);
					}
				}
			}
		};

		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			mbs.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, listener, null, null);
		} catch (InstanceNotFoundException e) {
			if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
				Tr.debug(tc, "Exception while adding listener: ", e.getMessage());
			}
			FFDCFilter.processException(e, getClass().getSimpleName(), "addMBeanListener:Exception");
		}
	}

	protected void unregister(String objectName) {
		Set<MonitorMetrics> removeSet = new HashSet<MonitorMetrics>();
		for (MonitorMetrics mm : mmonitorMetricsSet) {
			if (mm.getObjectName().equals(objectName)) {
				// TODO : remove
				removeSet.add(mm);
				mm.unregisterMetrics();
				if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
					Tr.debug(tc, "Monitoring MXBean " + objectName + " was unregistered.");
				}

			}
		}
		mmonitorMetricsSet.removeAll(removeSet);
	}

	protected void register() {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		Tr.debug(tc, "DEBUGGING: > register()");
		for (String sName : mappingTable.getKeys()) {
			Tr.debug(tc, "DEBUGGING: > register() Checking: " + sName);
			Set<ObjectInstance> mBeanObjectInstanceSet;
			try {
				mBeanObjectInstanceSet = mbs.queryMBeans(new ObjectName(sName), null);
				if (sName.contains("ThreadPoolStats") && mBeanObjectInstanceSet.isEmpty() && execServ != null) {
					Tr.debug(tc, "DEBUGGING: > register() Couldn't find MBean with name: " + sName);
					execServ.execute(() -> {
						final int MAX_TIME_OUT = 50;
						int currentTimeOut = 0;
						Set<ObjectInstance> mBeanObjectInstanceSetTemp = mBeanObjectInstanceSet;
						Tr.debug(tc, "DEBUGGING: > register() retrying for: " + sName);
						while (mBeanObjectInstanceSetTemp.isEmpty() && currentTimeOut <= MAX_TIME_OUT) {
							try {
								Thread.sleep(50);

								mBeanObjectInstanceSetTemp = mbs.queryMBeans(new ObjectName(sName), null);
								Tr.debug(tc, "DEBUGGING: > register() queryMBeans: " + sName);
								currentTimeOut += 50;
							} catch (Exception e) {
								if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
									Tr.debug(tc, "register exception message: ", e.getMessage());
								}
								FFDCFilter.processException(e, MonitorMetricsHandler.class.getSimpleName(),
										"register:Exception");
								/*
								 * Interruption Exception or RuntimeOperationException from malformed query exit
								 * thread.
								 */
								break;
							}
						}
						// Log timeout result
						if (mBeanObjectInstanceSetTemp.isEmpty()) {
							Tr.debug(tc, "DEBUGGING: ThreadPoolStats MBean could't be found even after retries.");
						} else {
							Tr.debug(tc, "DEBUGGING: ThreadPoolStats found after retries.");
						}
						registerMbeanObjects(mBeanObjectInstanceSetTemp);
					});
				} else {
					Tr.debug(tc, "DEBUGGING: > register() Found MBean with name: " + sName);
				}
				registerMbeanObjects(mBeanObjectInstanceSet);
			} catch (Exception e) {
				if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
					Tr.debug(tc, "register exception message: ", e.getMessage());

				}
				FFDCFilter.processException(e, MonitorMetricsHandler.class.getSimpleName(), "register:Exception");
			}
		}
	}

	private synchronized void registerMbeanObjects(Set<ObjectInstance> mBeanObjectInstanceSet) {
		for (ObjectInstance objInstance : mBeanObjectInstanceSet) {
			String objectName = objInstance.getObjectName().toString();
			String[][] data = mappingTable.getData(objectName);
			if (data != null) {
				register(objectName, data);
			}
		}
	}

	protected synchronized void register(String objectName, String[][] data) {
 
		MonitorMetrics monitorMetricsInsts = null;
		if (!containMetrics(objectName)) {
			monitorMetricsInsts = new MonitorMetrics(objectName);
			monitorMetricsInsts.createMetrics(data);
			mmonitorMetricsSet.add(monitorMetricsInsts);
			if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
				Tr.debug(tc, "Monitoring MXBean " + objectName + " is registered to mpTelemetry.");
			}

		} else {
			if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
				Tr.debug(tc, objectName + " is already registered.");
			}
		}
	}

	protected boolean containMetrics(String objectName) {
		for (MonitorMetrics mm : mmonitorMetricsSet) {
			if (mm.getObjectName().equals(objectName))
				return true;
		}
		return false;
	}

}
