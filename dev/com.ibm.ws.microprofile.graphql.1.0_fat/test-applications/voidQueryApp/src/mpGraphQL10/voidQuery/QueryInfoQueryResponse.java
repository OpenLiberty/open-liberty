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

import java.util.List;

public class QueryInfoQueryResponse {

    public static class AllQueryInfoData {

        private List<QueryInfo> allQueryInstances;

        public List<QueryInfo> getAllQueryInstancesRequestScope() {
            return allQueryInstances;
        }

        public void setAllQueryInstancesRequestScope(List<QueryInfo> allQueryInfos) {
            this.allQueryInstances = allQueryInfos;
        }

        public List<QueryInfo> getAllQueryInstancesAppScope() {
            return allQueryInstances;
        }

        public void setAllQueryInstancesAppScope(List<QueryInfo> allQueryInfos) {
            this.allQueryInstances = allQueryInfos;
        }

        public List<QueryInfo> getAllQueryInstances() {
            return allQueryInstances;
        }
    }

    private AllQueryInfoData data;

    public AllQueryInfoData getData() {
        return data;
    }

    public void setData(AllQueryInfoData data) {
        this.data = data;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("QueryInfoQueryResponse[");
        if (data == null || data.getAllQueryInstancesRequestScope() == null) {
            sb.append("null");
        } else {
            for (QueryInfo q : data.getAllQueryInstancesRequestScope()) {
                sb.append(" ").append(q);
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
