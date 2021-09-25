/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.v32.web.ejb;

import javax.ejb.Stateful;

import com.ibm.ws.ejbcontainer.timer.np.v32.shared.AbstractTestBean;
import com.ibm.ws.ejbcontainer.timer.np.v32.shared.TestBean;

/**
 * Bean implementation for a basic Stateful session bean. It contains methods
 * to test the TimerService getAllTimers API. <p>
 **/
@Stateful
public class StatefulTestBean extends AbstractTestBean implements TestBean {
    // No overrides needed - use abstract implementation as provided.
}
