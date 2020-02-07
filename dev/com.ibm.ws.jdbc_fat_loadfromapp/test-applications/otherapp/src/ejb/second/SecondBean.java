/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ejb.second;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.concurrent.Callable;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.sql.DataSource;

@Stateless
public class SecondBean implements Callable<String> {
    @Resource(name = "java:comp/env/jdbc/dsref", lookup = "jdbc/sharedLibDataSource")
    DataSource ds;

    @Override
    public String call() throws Exception {
        try (Connection con = ds.getConnection()) {
            DatabaseMetaData mdata = con.getMetaData();
            return mdata.getUserName();
        }
    }
}
