/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package jaxrs21.fat.atinject;

import javax.enterprise.context.Dependent;

@Dependent
public class ConstructorInjectedObject extends AbstractInjectedObject {

    /* (non-Javadoc)
     * @see jaxrs21.fat.atinject.AbstractInjectedObject#getInjectionTargetType()
     */
    @Override
    public String getInjectionTargetType() {
        return "constructor";
    }

}
