/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpGraphQL10.jarInWar;

import java.time.LocalDate;
import java.util.List;

import javax.annotation.PostConstruct;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import mpGraphQL10.jarInWar.inJar.EntityInJarRefdFromWar;

@GraphQLApi
public class RootInWarApi {

    @PostConstruct
    public void init() {
        EntityInWar.allWars().add(new EntityInWar("War of 1812", LocalDate.of(1812, 6, 18)));
        EntityInWar.allWars().add(new EntityInWar("Thirty Years' War", LocalDate.of(1618, 5, 23)));
    }

    @Query
    public List<EntityInWar> allWars() {
        return EntityInWar.allWars();
    }

    @Query
    public List<EntityInJarRefdFromWar> allJarsRefdFromWar() {
        return EntityInJarRefdFromWar.allJars();
    }
}
