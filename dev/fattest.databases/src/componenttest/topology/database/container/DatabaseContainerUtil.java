/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.database.container;

import org.testcontainers.containers.JdbcDatabaseContainer;

import componenttest.topology.database.container.util.LibertyServerUtil;
import componenttest.topology.impl.LibertyServer;


/**
 * For server utilities use {@link componenttest.topology.database.container.util.LibertyServerUtil}
 * 
 * For client utilities use {@link componenttest.topology.database.container.util.LibertyClientUtil}
 * 
 * For generic config utilities use {@link componenttest.topology.database.container.util.LibertyConfigUtil}
 *
 */
@Deprecated
public final class DatabaseContainerUtil {
    
    private DatabaseContainerUtil() {
    	//No objects should be created from this class
    }
    
    /**
     * Use {@link componenttest.topology.database.container.util.LibertyServerUtil#setupDatabaseProperties(LibertyServer, JdbcDatabaseContainer)}
     */
    @Deprecated
    public static void setupDataSourceDatabaseProperties(LibertyServer serv, JdbcDatabaseContainer<?> cont) throws CloneNotSupportedException, Exception {
    	LibertyServerUtil.setupDatabaseProperties(serv, cont);
    }

    /**
     * Use {@link componenttest.topology.database.container.util.LibertyServerUtil#setupGenericProperties(LibertyServer, JdbcDatabaseContainer)}
     */
    @Deprecated
    public static void setupDataSourceProperties(LibertyServer serv, JdbcDatabaseContainer<?> cont) throws Exception {
    	LibertyServerUtil.setupGenericProperties(serv, cont);
    }
}
