/*******************************************************************************
 * Copyright (c) 2020-2023 IBM Corporation and others.
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
package test.corba.bean.jar;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import shared.TestRemote;

@Stateless
@Remote
public class TestBean implements TestRemote {
    public TestBean() {}

    @Override
    public String toString() {
        return "TestBean []";
    }
}
