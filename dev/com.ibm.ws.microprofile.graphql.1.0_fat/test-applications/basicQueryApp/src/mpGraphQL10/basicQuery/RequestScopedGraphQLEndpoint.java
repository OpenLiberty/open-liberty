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
package mpGraphQL10.basicQuery;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
@RequestScoped
public class RequestScopedGraphQLEndpoint {

    private final static List<QueryInfo> allInstances = new ArrayList<>();

    public RequestScopedGraphQLEndpoint() {
        System.out.println("RequestScopedGraphQLEndpoint <init>");
    }

    @PostConstruct
    public void addInstance() {
        QueryInfo qi = new QueryInfo(this.toString());
        System.out.println("RequestScopedGraphQLEndpoint Adding instance: " + qi);
        allInstances.add(qi);
    }

    @Query("allQueryInstancesRequestScope")
    public List<QueryInfo> getAllInstances() {
        System.out.println("RequestScopedGraphQLEndpoint returning: " + allInstances);
        return allInstances;
    }
}
