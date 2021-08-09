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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.GraphQLApi;

@GraphQLApi
public class SingletonGraphQLEndpoint {

    private final List<Widget> allWidgets = new ArrayList<>();

    public SingletonGraphQLEndpoint() {
        allWidgets.add(new Widget("Notebook", 20, 2.0));
        allWidgets.add(new Widget("Pencil", 200, 0.5));
    }

    @Query("allWidgets")
    public List<Widget> getAllWidgets() {
        return allWidgets;
    }
    
    @Query("allWidgetsUnableToSerialize")
    public List<Widget> getAllWidgetsThatWeCannotSerialize() {
        return Collections.singletonList(new WidgetWithSerializationFailure("Eraser", 50, 0.3));
    }

    @Query("allWidgetsSet")
    public Set<Widget> getAllWidgetsSet() {
        return new HashSet<>(allWidgets);
    }
}
