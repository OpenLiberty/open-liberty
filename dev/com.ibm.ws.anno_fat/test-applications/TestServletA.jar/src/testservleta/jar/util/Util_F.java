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
package testservleta.jar.util;

/**
 * Simple utility class: Present to force the jar over the write limit, which defaults to 16.
 */
@Util_Anno(value = true)
public class Util_F {
    public Util_F() {
        super();
    }
}
