/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ejb1;

import javax.ejb.Stateless;

@Stateless(name = "SampleSessionBean")
public class SampleSessionImpl implements SampleSessionRemote, SampleSessionLocal {
    @Override
    public String greeting(String name) {
        return "Hello " + name;
    }
}
