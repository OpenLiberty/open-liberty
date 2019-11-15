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
package mpGraphQL10.defaultvalue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.graphql.DefaultValue;
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
        reset();
    }
    
    public void reset() {
        allWidgets.clear();
    }

    @Query("allWidgets")
    public Collection<Widget> getAllInstances() {
        System.out.println("MyGraphQLEndpoint returning: " + allWidgets);
        return allWidgets;
    }

    @Query("widgetByName")
    public Widget getWidgetByName(@Name("name") @DefaultValue("Pencil") String name) {
        switch (name) {
            case "Pencil": return new Widget("Pencil", 10, 0.5, 5.5, 0.2, 0.2);
            case "Eraser": return new Widget("Eraser", 5, 0.7, 2.2, 0.2, 0.4);
        }
        return new Widget("Unknown Widget", 0, 0.0, 0.0, 0.0, 0.0);
    }

    @Mutation("createWidget")
    public Widget createNewWidget(@Name("widget") WidgetInput input) {
        Widget w = Widget.fromWidgetInput(input);
        allWidgets.add(w);
        return w;
    }
    
    @Mutation("createWidgetByString")
    public Widget createNewWidget(@DefaultValue("Widget(Oven,12,120.1,36.2,3.3,14.0)")
                                  @Name("widgetString") String input) {
        Widget w = Widget.fromString(input);
        allWidgets.add(w);
        return w;
    }
}
