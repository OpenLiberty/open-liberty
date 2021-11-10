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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;

@Stateless
@Local
public class SLSBAnnotationTxBean extends AbstractAnnotationTxBean implements SLSBAnnotationTxLocal {

    public final static String CLASSNAME = SLSBAnnotationTxBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    @Override
    @Timeout
    public void myTimeout(Timer timer) {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "myTimeout", timer);
        }

        super.myTimeout(timer);

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "myTimeout", timer);
        }
    }
}
