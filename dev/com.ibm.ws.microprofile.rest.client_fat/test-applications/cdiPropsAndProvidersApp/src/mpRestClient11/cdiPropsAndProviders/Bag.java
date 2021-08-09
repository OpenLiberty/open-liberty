/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient11.cdiPropsAndProviders;

import java.util.ArrayList;
import java.util.List;

class Bag {

    private static Bag INSTANCE;

    synchronized static Bag getBag() {
        if (INSTANCE == null) {
            INSTANCE = new Bag();
        }
        return INSTANCE;
    }

    synchronized static void clear() {
        INSTANCE = null;
    }

    final List<Class<?>> filtersInvoked = new ArrayList<>();

}
