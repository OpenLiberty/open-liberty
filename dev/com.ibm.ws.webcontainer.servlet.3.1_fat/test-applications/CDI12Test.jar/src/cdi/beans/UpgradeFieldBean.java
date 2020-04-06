/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi.beans;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 *
 */
@RequestScoped
@Named
@UpgradeType
public class UpgradeFieldBean extends FieldBean {

    @Override
    public String getData() {
        return this.getClass() + ":" + value;
    }

}
