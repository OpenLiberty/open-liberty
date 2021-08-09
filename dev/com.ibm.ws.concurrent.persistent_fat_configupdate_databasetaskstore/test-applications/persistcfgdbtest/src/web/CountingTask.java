/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.Serializable;
import java.util.concurrent.Callable;

public class CountingTask implements Callable<Integer>, Serializable {
    private static final long serialVersionUID = -1270868080931250437L;

    private int count;

    @Override
    public Integer call() {
        int value = ++count;
        System.out.println("CountingTask execution " + value);
        return value;
    }
}
