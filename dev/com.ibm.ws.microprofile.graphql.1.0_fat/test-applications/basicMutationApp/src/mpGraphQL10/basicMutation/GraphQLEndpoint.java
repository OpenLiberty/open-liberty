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
package mpGraphQL10.basicMutation;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;


import mpGraphQL10.types.WidgetImpl;
import mpGraphQL10.types.WidgetInput;

@GraphQLApi
@ApplicationScoped
public class GraphQLEndpoint {

    private final static List<Widget> allWidgets = new ArrayList<>();

    public GraphQLEndpoint() {
        System.out.println("ApplicationScopedGraphQLEndpoint <init>");
    }

    @Query("allWidgets")
    public List<Widget> getAllInstances() {
        System.out.println("GraphQLEndpoint returning: " + allWidgets);
        return allWidgets;
    }

    @Mutation("createWidget")
    @Description("Create a new widget for sale.")
    public Widget createNewWidget(@Name("widget") Widget inputWidget) {
        Widget newWidget = new Widget(inputWidget);
        allWidgets.add(newWidget);
        return newWidget;
    }

    @Mutation
    public List<Widget> setQuantityOnAllWidgets(int newQuantity) {
        for (Widget w : allWidgets) {
            w.setQuantity(newQuantity);
        }
        return allWidgets;
    }
}
