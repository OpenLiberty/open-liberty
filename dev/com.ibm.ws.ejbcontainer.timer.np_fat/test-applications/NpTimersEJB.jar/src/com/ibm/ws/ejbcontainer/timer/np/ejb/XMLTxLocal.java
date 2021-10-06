/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.ejb;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public interface XMLTxLocal {
    public final static DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS z");

    public Date createTimer(String info);

    public Date createIntervalTimer(String info);

    public boolean checkCancelTxSemantics();

    public void checkNoSuchObjectLocalException();

    public String getNextTimeoutString();

    public void reset();

    public void clearAllTimers();
}
