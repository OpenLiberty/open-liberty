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
package ejb;

import java.io.Serializable;

/**
 * Serializable timer info.
 */
public class MyTimerInfo implements Serializable {
    private static final long serialVersionUID = -3302802216108334195L;

    public final int cancelOnExecution;
    public final String name;

    public MyTimerInfo(String name, int cancelOnExecution) {
        this.name = name;
        this.cancelOnExecution = cancelOnExecution;
    }
}
