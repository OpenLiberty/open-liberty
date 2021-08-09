/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.resourceinfoatstartup;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.sql.DataSource;

@ApplicationPath("/")
@Stateless
@LocalBean
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