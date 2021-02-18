/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jdbc.heritage.app;

import static org.junit.Assert.assertNotNull;

import javax.annotation.Resource;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JDBCHeritageTestServlet")
public class JDBCHeritageTestServlet extends FATServlet {
    TraceComponent tc = Tr.register(JDBCHeritageTestServlet.class);

    @Resource
    private DataSource defaultDataSource;

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * Confirm that a dataSource that is configured with heritageSettings can be injected.
     */
    @Test
    public void testInjection() throws Exception {
        assertNotNull(defaultDataSource);
    }
}