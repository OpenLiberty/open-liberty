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
package mpGraphQL10.jarInWar.inJar;

import java.util.List;

import javax.annotation.PostConstruct;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
public class RootInJarApi {

    @PostConstruct
    public void init() {
        EntityInJar.allJars().add(new EntityInJar("Peanut Butter", LidTightness.EASY_TO_REMOVE));
        EntityInJar.allJars().add(new EntityInJar("Jam", LidTightness.KINDA_HARD));
    }

    @Query
    public List<EntityInJar> allJars() {
        return EntityInJar.allJars();
    }
}
