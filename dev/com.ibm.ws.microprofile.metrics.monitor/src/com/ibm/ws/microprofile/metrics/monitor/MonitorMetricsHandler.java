/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.monitor;

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

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;

@Component(name = "com.ibm.ws.microprofile.metrics.monitor.MonitorMetricsHandler")
public class MonitorMetricsHandler {
	
	private static final TraceComponent tc = Tr.register(MonitorMetricsHandler.class);
	
    private SharedMetricRegistries sharedMetricRegistry;
    private ExecutorService execServ;
    private MappingTable mappingTable;
    private Set<MonitorMetrics> metricsSet = new HashSet<MonitorMetrics>();
	private NotificationListener listener;
    
	@Activate
    protected void activate(ComponentContext context) {
		this.mappingTable = MappingTable.getInstance();
		register();
        addMBeanListener();
        Tr.info(tc, "FEATURE_REGISTERED");
    }

    @Reference
    public void setSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistry) {
        this.sharedMetricRegistry = sharedMetricRegistry;
    }
    
    public void unsetSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistry) {
        this.sharedMetricRegistry = null;
    }
       
    @Reference
    public void setExecutorService(ExecutorService execServ) {
        this.execServ = execServ;
    }
    
    public void unsetExecutorService(ExecutorService execServ) {
        this.execServ = null;
    }
    
    @Deactivate
    protected void deactivate(ComponentContext context) {
    	MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    	if (listener != null) {
        	try {
    			mbs.removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, listener);
    		} catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deactivate exception message: ", e.getMessage());
                    FFDCFilter.processException(e, getClass().getSimpleName(), "deactivate:Exception");
                }
    		}     		
			listener = null;
    	}
    	SharedMetricRegistries.remove(MetricRegistry.Type.VENDOR.getName());
   	
    	Tr.info(tc, "FEATURE_UNREGISTERED");
    }

    private void addMBeanListener() {
    	listener = new NotificationListener() {

			@Override
			public void handleNotification(Notification notification, Object handback) {
		        MBeanServerNotification mbsn = (MBeanServerNotification) notification;
	            String objectName = mbsn.getMBeanName().toString();
		        if(MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(mbsn.getType())) {
		            Tr.debug(tc, "MBean Registered [", objectName + "]");
		            String[][] data = mappingTable.getData(objectName);
		            if (data != null) {
		            	register(objectName, data);
		            }
		        } else if(MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(mbsn.getType())) {
		        	Tr.debug(tc, "MBean Unregistered [" + objectName + "]");
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
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getCount exception message: ", e.getMessage());
                FFDCFilter.processException(e, getClass().getSimpleName(), "addMBeanListener:Exception");
            }
		}
	}

	protected void unregister(String objectName) {
 		Set<MonitorMetrics> removeSet = new HashSet<MonitorMetrics>();
    	for (MonitorMetrics mm : metricsSet) {
    		if (mm.objectName.equals(objectName)) {
    			removeSet.add(mm);
    			mm.unregisterMetrics(sharedMetricRegistry);
    			Tr.debug(tc, "Monitoring MXBean " + objectName + " was unregistered from mpMetrics.");
    		}
    	}
    	metricsSet.removeAll(removeSet);
	}

    protected void register() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        for (String sName : mappingTable.getKeys()) {
            Set<ObjectInstance> mBeanObjectInstanceSet;
            try {
                mBeanObjectInstanceSet = mbs.queryMBeans(new ObjectName(sName), null);
                if (sName.contains("ThreadPoolStats") && mBeanObjectInstanceSet.isEmpty() && execServ != null) {
                    execServ.execute(() -> {
                        final int MAX_TIME_OUT = 5000;
                        int currentTimeOut = 0;
                        Set<ObjectInstance> mBeanObjectInstanceSetTemp = mBeanObjectInstanceSet;
                        while (mBeanObjectInstanceSetTemp.isEmpty() && currentTimeOut <= MAX_TIME_OUT) {
                            try {
                                Thread.sleep(50);

                                mBeanObjectInstanceSetTemp = mbs.queryMBeans(new ObjectName(sName), null);
                                currentTimeOut += 50;
                            } catch (Exception e) {
                                if (tc.isDebugEnabled()) {
                                    Tr.debug(tc, "register exception message: ", e.getMessage());
                                    FFDCFilter.processException(e, MonitorMetricsHandler.class.getSimpleName(),
                                            "register:Exception");
                                }
                                /*
                                 * Interruption Exception or RuntimeOperationException from malformed query exit
                                 * thread.
                                 */
                                break;
                            }
                        }
                        registerMbeanObjects(mBeanObjectInstanceSetTemp);
                    });
                }
                registerMbeanObjects(mBeanObjectInstanceSet);
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "register exception message: ", e.getMessage());
                    FFDCFilter.processException(e, MonitorMetricsHandler.class.getSimpleName(), "register:Exception");
                }
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
    
	protected void register(String objectName, String[][] data) {
        MonitorMetrics metrics = null;
		if (!containMetrics(objectName))  {
			metrics = new MonitorMetrics(objectName);
			metrics.createMetrics(sharedMetricRegistry, data);
        	metricsSet.add(metrics);
        	Tr.debug(tc, "Monitoring MXBean " + objectName + " is registered to mpMetrics.");
        } else {
        	Tr.debug(tc, objectName + " is already registered.");
        }
		
	}

	private boolean containMetrics(String objectName) {
    	for (MonitorMetrics mm : metricsSet) {
    		if (mm.objectName.equals(objectName))
    			return true;
    	}
    	return false;
	}
}
