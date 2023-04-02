/*******************************************************************************
 * Copyright (c) 2010, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.resourceinfoatstartup;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.enterprise.context.Dependent;
import javax.sql.DataSource;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/")
@Stateless
@LocalBean
@Dependent
public class App extends Application {

    @Resource(description = "Application Data Source", name = "jdbc/TestDataSource")
    private DataSource datasource;

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();

        if (datasource == null) {
            throw new RuntimeException("was not injected");
        } else {
            System.out.println("App(Set) - datasource " + datasource.toString());
        }

        return classes;
    }
    
}