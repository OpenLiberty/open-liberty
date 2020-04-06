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
package cdi.beans.v2;

import javax.enterprise.context.Dependent;
import javax.inject.Named;

/**
 * CDI Testing: Type for upgrade handler field injection.
 */
@Dependent
@Named
@UpgradeType
public class UpgradeFieldBean extends FieldBean {
    /**
     * Override: An upgrade handler field bean has request scope, {@link CDICaseScope#Request}.
     * 
     * @return The bean scope.
     */
    @Override
    public CDICaseScope getScope() {
        return CDICaseScope.Request;
    }
}
