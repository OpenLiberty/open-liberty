/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.pmi.stat;

public interface WSStatTypes {
    public static final String JVM = "jvmRuntimeModule#";

    // With the move of web container to a custom pmi solution,
    // the core stat type for their servlet stats has changed.  
    // The following declaration has changed, accordingly.
    public static final String SERVLET = "com.ibm.ws.wswebcontainer.stats.servletStats";
    //public static final String SERVLET = "webAppModule#webAppModule.servlets";

    public static final String EJB_ENTITY = "beanModule#ejb.entity";
    public static final String EJB_STATELESS = "beanModule#ejb.stateless";
    public static final String EJB_STATEFUL = "beanModule#ejb.stateful";
    public static final String EJB_MESSAGEDRIVEN = "beanModule#ejb.messageDriven";

    public static final String JDBC_PROVIDER = "connectionPoolModule";
    public static final String J2C_PROVIDER = "j2cModule";

    public static final String TRANSACTION = "transactionModule";

    public static final int STATISTIC_UNDEFINED = 0;
    public static final int STATISTIC_COUNT = 1;
    public static final int STATISTIC_DOUBLE = 2;
    public static final int STATISTIC_AVERAGE = 3;
    public static final int STATISTIC_TIME = 4;
    public static final int STATISTIC_RANGE = 5;
    public static final int STATISTIC_BOUNDARY = 6;
    public static final int STATISTIC_BOUNDEDRANGE = 7;
}
