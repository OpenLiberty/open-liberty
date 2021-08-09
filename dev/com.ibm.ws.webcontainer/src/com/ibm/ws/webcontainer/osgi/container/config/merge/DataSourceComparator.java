/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.container.config.merge;

import com.ibm.ws.javaee.dd.common.DataSource;

public class DataSourceComparator extends AbstractBaseComparator<DataSource> {

    @Override
    public boolean compare(DataSource o1, DataSource o2) {
        if (o1.getName() == null) {
            if (o2.getName() != null)
                return false;
        } else if (!o1.getName().equals(o2.getName())) {
            return false;
        }
        if (o1.getClassNameValue() == null) {
            if (o2.getClassNameValue() != null)
                return false;
        } else if (!o1.getClassNameValue().equals(o2.getClassNameValue())) {
            return false;
        }
        if (o1.getDatabaseName() == null) {
            if (o2.getDatabaseName() != null)
                return false;
        } else if (!o1.getDatabaseName().equals(o2.getDatabaseName())) {
            return false;
        }
        if (o1.getServerName() == null) {
            if (o2.getServerName() != null)
                return false;
        } else if (!o1.getServerName().equals(o2.getServerName())) {
            return false;
        }
        if (o1.getUrl() == null) {
            if (o2.getUrl() != null)
                return false;
        } else if (!o1.getUrl().equals(o2.getUrl())) {
            return false;
        }
        if (o1.getUser() == null) {
            if (o2.getUser() != null)
                return false;
        } else if (!o1.getUser().equals(o2.getUser())) {
            return false;
        }
        if (o1.getPassword() == null) {
            if (o2.getPassword() != null)
                return false;
        } else if (!o1.getPassword().equals(o2.getPassword())) {
            return false;
        }
        if (o1.getInitialPoolSize() != o2.getInitialPoolSize()) {
            return false;
        }
        if (o1.getIsolationLevelValue() != o2.getIsolationLevelValue()) {
            return false;
        }
        if (o1.getLoginTimeout() != o2.getLoginTimeout()) {
            return false;
        }
        if (o1.getMaxIdleTime() != o2.getMaxIdleTime()) {
            return false;
        }
        if (o1.getMaxPoolSize() != o2.getMaxPoolSize()) {
            return false;
        }
        if (o1.getMaxStatements() != o2.getMaxStatements()) {
            return false;
        }
        if (o1.getMinPoolSize() != o2.getMinPoolSize()) {
            return false;
        }
        if (!compareProperties(o1.getProperties(), o2.getProperties())) {
            return false;
        }
        if (o1.getDescription() == null) {
            if (o2.getDescription() != null)
                return false;
        } else if (!o1.getDescription().equals(o2.getDescription())) {
            return false;
        }
        return true;
    }

}
