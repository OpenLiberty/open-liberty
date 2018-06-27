/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection.dsdann.ejb;

import java.util.logging.Logger;

import javax.annotation.sql.DataSourceDefinition;
import javax.ejb.Singleton;

@DataSourceDefinition(name = "java:module/ann_SingletonModLevelDS",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource40",
                      databaseName = "dsdAnnTest",
                      loginTimeout = 1825,
                      properties = { "createDatabase=create" })
@Singleton
public class DSDSingletonBean {
    private static String CLASSNAME = DSDSingletonBean.class.getName();
    private static Logger svLogger = Logger.getLogger(CLASSNAME);

    public void test() {
        svLogger.info("--> Called the Singleton bean.");
    }
}