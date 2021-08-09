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
package mpGraphQL10.types;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
@ApplicationScoped
public class MyGraphQLEndpoint {

    private final Map<Long, WidgetImpl> allWidgets = new HashMap<>();

    public MyGraphQLEndpoint() {
        System.out.println("MyGraphQLEndpoint <init>");
    }

    @Query("allWidgets")
    public Collection<WidgetImpl> getAllInstances() {
        Collection<WidgetImpl> widgets = allWidgets.values();
        System.out.println("MyGraphQLEndpoint returning: " + widgets);
        return widgets;
    }
    
    @Mutation("createWidget")
    public WidgetImpl createNewWidget(@Name("widget") WidgetInput input) {
        if (!(input instanceof WidgetInput)) {
            String msg = String.format("Unexpected input type; expected WidgetInput, got {0}\n",
                                       input.getClass().getName());
            System.out.println(msg);
            throw new IllegalArgumentException(msg);
        }
        WidgetImpl impl = WidgetImpl.fromWidgetInput(input);
        allWidgets.put(impl.getWidgetId(), impl);
        return impl;
    }
}
