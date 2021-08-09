/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

@Stateless
public class StatelessTimedBean implements StatelessTimedLocal {
    @Resource
    private TimerService ivTimerService;

    @PostConstruct
    public void create() {
    }

    @Timeout
    public void timeout(Timer timer) {
    }

    // Create a timer to fire in 60 seconds
    @Override
    public Timer createTimer(boolean persistent) {
        if (persistent) {
            return ivTimerService.createTimer(60000, "StatelessTimedBean");
        }

        return ivTimerService.createSingleActionTimer(60000, new TimerConfig("StatelessTimedBean", false));
    }
}
