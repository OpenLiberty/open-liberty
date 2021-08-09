/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpGraphQL10.voidQuery;

/**
 * Model object used for determining the object ID of the query object
 */
public class QueryInfo {

    private String instanceId;

    public QueryInfo() {}

    public QueryInfo(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public String toString() {
        return "QueryInfo(query instanceId=" + instanceId;
    }
    
    @Override
    public boolean equals(Object o) {
        return (o instanceof QueryInfo) && ((QueryInfo)o).instanceId.equals(this.instanceId);
    }
}
