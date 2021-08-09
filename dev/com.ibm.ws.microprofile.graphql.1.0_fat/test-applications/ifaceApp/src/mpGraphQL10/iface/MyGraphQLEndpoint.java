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
package mpGraphQL10.iface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
@ApplicationScoped
public class MyGraphQLEndpoint {

    private final List<Widget> allWidgets = new ArrayList<>();

    public MyGraphQLEndpoint() {
        System.out.println("MyGraphQLEndpoint <init>");
    }

    @Query("allWidgets")
    public Collection<Widget> getAllInstances() {
        System.out.println("MyGraphQLEndpoint returning: " + allWidgets);
        return allWidgets;
    }
    
    @Mutation("createWidget")
    public Widget createNewWidget(@Name("widget") WidgetInput input) {
        if (!(input instanceof WidgetInput)) {
            String msg = String.format("Unexpected input type; expected WidgetInput, got {0}\n",
                                       input.getClass().getName());
            System.out.println(msg);
            throw new IllegalArgumentException(msg);
        }
        WidgetImpl impl = WidgetImpl.fromWidgetInput(input);
        allWidgets.add(impl);
        return impl;
    }
}
