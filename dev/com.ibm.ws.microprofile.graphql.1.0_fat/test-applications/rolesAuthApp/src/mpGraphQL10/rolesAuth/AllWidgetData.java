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
package mpGraphQL10.rolesAuth;

import java.util.List;
import java.util.Set;

public class AllWidgetData {

    private List<Widget> allWidgets;

    public List<Widget> getAllWidgets() {
        return allWidgets;
    }

    public void setAllWidgets(List<Widget> allWidgets) {
        this.allWidgets = allWidgets;
    }
}
