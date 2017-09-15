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
package com.ibm.ws.ejbcontainer.timer.persistent.osgi.internal;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.transaction.TransactionManager;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.websphere.concurrent.persistent.TaskState;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.ejbcontainer.mbean.EJBPersistentTimerInfo;
import com.ibm.websphere.ejbcontainer.mbean.EJBPersistentTimerServiceMXBean;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.persistent.ejb.TimerStatus;
import com.ibm.ws.concurrent.persistent.ejb.TimersPersistentExecutor;
import com.ibm.ws.ejbcontainer.util.ParsedScheduleExpression;
import com.ibm.ws.ejbcontainer.util.ScheduleExpressionParser;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

@Component(property = "jmx.objectname=WebSphere:feature=ejbPersistentTimer,type=EJBPersistentTimerService,name=EJBPersistentTimerService")
public class EJBPersistentTimerServiceMXBeanImpl implements EJBPersistentTimerServiceMXBean {
    private static final TraceComponent tc = Tr.register(EJBPersistentTimerServiceMXBeanImpl.class);
    private static final Character ESCAPE = '\\';

    private EJBPersistentTimerRuntimeImpl persistentTimerRuntime;
    private J2EENameFactory j2eeNameFactory;

    @Reference
    protected void setEJBPersistentTimerRuntime(EJBPersistentTimerRuntimeImpl persistentTimerRuntime) {
        this.persistentTimerRuntime = persistentTimerRuntime;
    }

    @Reference
    protected void setJ2EENameFactory(J2EENameFactory j2eeNameFactory) {
        this.j2eeNameFactory = j2eeNameFactory;
    }

    @Trivial
    private static void requiresNonNull(String s, String name) {
        if (s == null) {
            throw new IllegalArgumentException(name);
        }
    }

    @Trivial
    private static void requiresJ2EENamePiece(String s, String name) {
        requiresNonNull(s, name);
        if (s.indexOf('#') != -1) {
            throw new IllegalArgumentException(name);
        }
    }

    @Trivial
    private static void requiresJ2EEName(String appName) {
        requiresJ2EENamePiece(appName, "appName");
    }

    @Trivial
    private static void requiresJ2EEName(String appName, String moduleURI) {
        requiresJ2EEName(appName);
        requiresJ2EENamePiece(moduleURI, "moduleURI");
    }

    @Trivial
    private static void requiresJ2EEName(String appName, String moduleURI, String ejbName) {
        requiresJ2EEName(appName, moduleURI);
        requiresJ2EENamePiece(ejbName, "ejbName");
    }

    private static RuntimeException handleError(Throwable t) {
        // Don't nest the actual throwable in case the client can't deserialize.
        return new RuntimeException(t.toString());
    }

    @Trivial
    private TimersPersistentExecutor getPersistentExecutor() {
        return persistentTimerRuntime.getPersistentExecutor();
    }

    @Trivial
    private PersistentTimerTaskHandlerImpl getTaskHandler(TimerStatus<?> taskStatus) throws ClassNotFoundException, IOException {
        return (PersistentTimerTaskHandlerImpl) taskStatus.getTimer();
    }

    @FFDCIgnore(Throwable.class)
    @Sensitive
    private static String getUserInfoString(PersistentTimerTaskHandlerImpl taskHandler) {
        try {
            Serializable info = taskHandler.getUserInfo();
            if (info != null) {
                return info.toString();
            }
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "unable to get user info string", t);
        }

        return null;
    }

    private EJBPersistentTimerInfo[] getTimers(List<TimerStatus<?>> tasks) throws ClassNotFoundException, IOException {
        EJBPersistentTimerInfo[] results = new EJBPersistentTimerInfo[tasks.size()];

        for (int i = 0; i < tasks.size(); i++) {
            TimerStatus<?> task = tasks.get(i);
            PersistentTimerTaskHandlerImpl taskHandler = getTaskHandler(task);
            J2EEName timerJ2EEName = taskHandler.getJ2EEName();

            EJBPersistentTimerInfo timerMXBean = new EJBPersistentTimerInfo();
            results[i] = timerMXBean;

            timerMXBean.setId(String.valueOf(task.getTaskId()));
            timerMXBean.setApplication(timerJ2EEName.getApplication());
            timerMXBean.setModule(timerJ2EEName.getModule());
            timerMXBean.setEJB(timerJ2EEName.getComponent());
            timerMXBean.setNextTimeout(task.getNextExecutionTime().getTime());
            timerMXBean.setInfo(getUserInfoString(taskHandler));

            ParsedScheduleExpression parsedScheduleExpr = taskHandler.getParsedSchedule();
            if (parsedScheduleExpr != null) {
                timerMXBean.setScheduleExpression(ScheduleExpressionParser.toString(parsedScheduleExpr.getSchedule()));
            }

            timerMXBean.setAutomaticTimerMethod(taskHandler.getAutomaticTimerMethodName());
        }

        return results;
    }

    @Override
    public EJBPersistentTimerInfo[] getTimers(String appName) {
        try {
            requiresJ2EEName(appName);

            return getTimers(getPersistentExecutor().findTimerStatus(appName, null, null, TaskState.ANY, true, null, null));
        } catch (Throwable t) {
            throw handleError(t);
        }
    }

    @Override
    public EJBPersistentTimerInfo[] getTimers(String appName, String moduleURI) {
        try {
            requiresJ2EEName(appName, moduleURI);

            String pattern = PersistentTimerTaskHandlerImpl.getTaskNameModulePattern(moduleURI);
            return getTimers(getPersistentExecutor().findTimerStatus(appName, pattern, ESCAPE, TaskState.ANY, true, null, null));
        } catch (Throwable t) {
            throw handleError(t);
        }
    }

    @Override
    public EJBPersistentTimerInfo[] getTimers(String appName, String moduleURI, String ejbName) {
        try {
            requiresJ2EEName(appName, moduleURI, ejbName);

            String pattern = PersistentTimerTaskHandlerImpl.getTaskNameBeanPattern(j2eeNameFactory.create(appName, moduleURI, ejbName));
            return getTimers(getPersistentExecutor().findTimerStatus(appName, pattern, ESCAPE, TaskState.ANY, true, null, null));
        } catch (Throwable t) {
            throw handleError(t);
        }
    }

    @Override
    public boolean cancelTimer(String id) {
        try {
            requiresNonNull(id, "id");

            return getPersistentExecutor().removeTimer(Long.parseLong(id));
        } catch (Throwable t) {
            throw handleError(t);
        }
    }

    @Trivial
    private TransactionManager getTransactionManager() {
        return EmbeddableTransactionManagerFactory.getTransactionManager();
    }

    @Override
    public boolean cancelTimers(String appName) {
        try {
            requiresJ2EEName(appName);

            return getPersistentExecutor().removeTimers(appName, null, null, TaskState.ANY, true) != 0;
        } catch (Throwable t) {
            throw handleError(t);
        }
    }

    @Override
    public boolean cancelTimers(String appName, String moduleURI) {
        try {
            requiresJ2EEName(appName, moduleURI);

            String pattern = PersistentTimerTaskHandlerImpl.getTaskNameModulePattern(moduleURI);
            return getPersistentExecutor().removeTimers(appName, pattern, ESCAPE, TaskState.ANY, true) != 0;
        } catch (Throwable t) {
            throw handleError(t);
        }
    }

    @Override
    public boolean cancelTimers(String appName, String moduleURI, String ejbName) {
        try {
            requiresJ2EEName(appName, moduleURI, ejbName);

            String pattern = PersistentTimerTaskHandlerImpl.getTaskNameBeanPattern(j2eeNameFactory.create(appName, moduleURI, ejbName));
            return getPersistentExecutor().removeTimers(appName, pattern, ESCAPE, TaskState.ANY, true) != 0;
        } catch (Throwable t) {
            throw handleError(t);
        }
    }

    @Override
    public boolean containsAutomaticTimers(String appName) {
        try {
            String pattern = PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyPattern(appName, null);
            Map<String, String> properties = getPersistentExecutor().findProperties(pattern, ESCAPE);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "properties=" + properties);
            return !properties.isEmpty();
        } catch (Throwable t) {
            throw handleError(t);
        }
    }

    @Override
    public boolean containsAutomaticTimers(String appName, String moduleURI) {
        try {
            requiresJ2EEName(appName, moduleURI);

            String propertyName = PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyName(appName, moduleURI);
            String property = getPersistentExecutor().getProperty(propertyName);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "property=" + property);
            return property != null;
        } catch (Throwable t) {
            throw handleError(t);
        }
    }

    private boolean removeAutomaticTimersImpl(String appName, String moduleURI) throws Exception {
        boolean success = false;

        TransactionManager tm = getTransactionManager();
        tm.begin();
        try {
            TimersPersistentExecutor persistentExecutor = getPersistentExecutor();

            String propertyPattern = PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyPattern(appName, moduleURI);
            int numProperties = persistentExecutor.removeProperties(propertyPattern, ESCAPE);

            String taskPattern = moduleURI == null ? null :
                            PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameModulePattern(moduleURI);
            int numTimers = persistentExecutor.removeTimers(appName, taskPattern, ESCAPE, TaskState.ANY, true);

            boolean result = numProperties != 0 || numTimers != 0;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "result=" + result + "; removed " + numProperties + " properties and " + numTimers + " timers");

            success = true;
            return result;
        } finally {
            if (success) {
                tm.commit();
            } else {
                tm.rollback();
            }
        }
    }

    @Override
    public boolean removeAutomaticTimers(String appName) {
        try {
            requiresJ2EEName(appName);
            return removeAutomaticTimersImpl(appName, null);
        } catch (Throwable t) {
            throw handleError(t);
        }
    }

    @Override
    public boolean removeAutomaticTimers(String appName, String moduleURI) {
        try {
            requiresJ2EEName(appName);
            return removeAutomaticTimersImpl(appName, moduleURI);
        } catch (Throwable t) {
            throw handleError(t);
        }
    }
}
