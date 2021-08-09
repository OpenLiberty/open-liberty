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
package web.derby;

import java.util.concurrent.Executor;

import javax.annotation.Resource;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.sql.DataSource;

@Stateful
public class StatefulBeanInWebApp implements CloseableExecutorBean, Executor {
    @Resource(name = "java:comp/env/jdbc/dsref", lookup = "jdbc/sharedLibDataSource")
    DataSource ds;

    @Override
    @Remove
    public void close() {
        System.out.println("EJB remove method (close) invoked");
    }

    @Override
    public void execute(Runnable runnable) {
        runnable.run();
    }
}
