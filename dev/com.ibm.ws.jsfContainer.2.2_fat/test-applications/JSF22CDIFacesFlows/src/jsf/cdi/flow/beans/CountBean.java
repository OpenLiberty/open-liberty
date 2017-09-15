/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jsf.cdi.flow.beans;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 * This Bean is used to keep track of a an integer count over the life of an application
 */
@Named
@ApplicationScoped
public class CountBean {

    private int count = 0;

    public int getCount() {
        return count;
    }

    public void increment() {
        count++;
    }
}
