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
package concurrent.mp.fat.web;

import java.io.Serializable;
import java.util.concurrent.Executor;

public class SerializableContextSnapshot implements Executor, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public void execute(Runnable runnable) {
        runnable.run();
    }
}
