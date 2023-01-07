/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpGraphQL10.jarInWar;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EntityInWar {

    private static List<EntityInWar> wars = new ArrayList<>();
    private String warName;
    private LocalDate date;

    static List<EntityInWar> allWars() {
        return wars;
    }

    public EntityInWar() {
    }
    
    public EntityInWar(String name, LocalDate date) {
        this.warName = name;
        this.date = date;
    }

    public String getWarName() {
        return warName;
    }

    public void setWarName(String warName) {
        this.warName = warName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
