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
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
@ApplicationScoped
public class ApplicationScopedGraphQLEndpoint {

    private final static List<QueryInfo> allInstances = new ArrayList<>();

    public ApplicationScopedGraphQLEndpoint() {
        System.out.println("ApplicationScopedGraphQLEndpoint <init>");
    }

    @PostConstruct
    public void addInstance() {
        QueryInfo qi = new QueryInfo(this.toString());
        System.out.println("ApplicationScopedGraphQLEndpoint Adding instance: " + qi);
        allInstances.add(qi);
    }

    @Query("allQueryInstancesAppScope")
    public List<QueryInfo> getAllInstances() {
        System.out.println("ApplicationScopedGraphQLEndpoint returning: " + allInstances);
        return allInstances;
    }
}
