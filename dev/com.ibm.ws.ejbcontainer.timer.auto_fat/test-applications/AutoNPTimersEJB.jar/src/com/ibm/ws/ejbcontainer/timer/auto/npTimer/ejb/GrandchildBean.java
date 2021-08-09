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
import java.util.HashSet;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TimerService;

@Stateless(name = "GrandchildBean")
@LocalBean
public class GrandchildBean extends ChildBean {
    private static final String CLASS_NAME = GrandchildBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @Resource
    private TimerService ts;

    @Override
    public void method_three(Timer timer) {
        svLogger.info("The grandchild method_three method got called back into by a Timer associated with class " + this.getClass());
        ParentBean.updateTimerToInvokingClassMap(AutoCreatedTimerDriverBean.GRANDCHILD_BEAN_METHOD_THREE, this.getClass());
    }

    // This method overrides the public ChildBean.method_one() timer method.
    //
    // This method is intentionally NOT declared a timer via annotation or
    // xml.  In other words, this is just plain-old-java method (it takes the
    // Timer param so it overrides the actual timer method declared in ChildBean).
    //
    // The net effect is that this should reduce the number of Timer instances
    // associated with the GrandchildBean by 1.  Without this method, there
    // would have been a Timer instance associated with GrandchildBean that was
    // calling back into the ChildBean.method_one() method...but because of this
    // override, that Timer instance will not be created.
    @Override
    public void method_one(Timer timer) {
        svLogger.info("The grandchild method_one method got called back into.");
    }

    @Override
    public HashSet<String> getTimerInfos() {
        HashSet<String> timerInfos = new HashSet<String>();
        Collection<Timer> timers = ts.getTimers();
        for (Timer oneTimer : timers) {
            timerInfos.add((String) oneTimer.getInfo());
        }
        return timerInfos;
    }
}
