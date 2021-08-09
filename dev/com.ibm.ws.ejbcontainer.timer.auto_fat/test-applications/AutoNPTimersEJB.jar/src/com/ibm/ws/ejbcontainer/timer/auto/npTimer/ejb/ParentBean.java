/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.timer.auto.npTimer.ejb;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TimerService;

@Stateless(name = "ParentBean")
@LocalBean
public class ParentBean {
    private static final String CLASS_NAME = ParentBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public static HashMap<String, HashSet<Class<?>>> svTimerMethodsToInvokingClasses = new HashMap<String, HashSet<Class<?>>>();

    @Resource
    private TimerService ts;

    @Schedule(hour = "*", minute = "*", second = "17", info = AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_ONE, persistent = false)
    public void method_one(Timer timer) {
        svLogger.info("The parent method_one method got called back into by a Timer associated with class " + this.getClass());
        ParentBean.updateTimerToInvokingClassMap(AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_ONE, this.getClass());
    }

    @Schedule(hour = "*", minute = "*", second = "19", info = AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_TWO, persistent = false)
    private void method_two(Timer timer) {
        svLogger.info("The parent method_two method got called back into by a Timer associated with class " + this.getClass());
        ParentBean.updateTimerToInvokingClassMap(AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_TWO, this.getClass());
    }

    @Schedule(hour = "*", minute = "*", second = "21", info = AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_THREE, persistent = false)
    private void method_three(Timer timer) {
        svLogger.info("The parent method_three method got called back into by a Timer associated with class " + this.getClass());
        ParentBean.updateTimerToInvokingClassMap(AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_THREE, this.getClass());
    }

    public HashSet<String> getTimerInfos() {
        HashSet<String> timerInfos = new HashSet<String>();
        Collection<Timer> timers = ts.getTimers();
        for (Timer oneTimer : timers) {
            timerInfos.add((String) oneTimer.getInfo());
        }
        return timerInfos;
    }

    public static synchronized void updateTimerToInvokingClassMap(String timerMethodAndClass, Class<?> invokingClass) {
        HashSet<Class<?>> invokingClasses = svTimerMethodsToInvokingClasses.get(timerMethodAndClass);
        if (invokingClasses == null) {
            // This particular timer method has not been invoked yet...so add it to
            // the Map now.
            invokingClasses = new HashSet<Class<?>>();
            svTimerMethodsToInvokingClasses.put(timerMethodAndClass, invokingClasses);
        }

        invokingClasses.add(invokingClass);
    }

    public void clearAllTimers() {
        Collection<Timer> timers = ts.getTimers();
        for (Timer timer : timers) {
            timer.cancel();
        }
    }

}
