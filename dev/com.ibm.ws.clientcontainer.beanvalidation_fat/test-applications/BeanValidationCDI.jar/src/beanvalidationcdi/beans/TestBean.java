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
package beanvalidationcdi.beans;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateOnExecution;

/**
 * Simple test CDI managed bean that can be injected into other CDI managed
 * beans.
 */
@ApplicationScoped
public class TestBean {

    public String getSomething() {
        return "something";
    }

    @ValidateOnExecution(type = ExecutableType.ALL)
    public String testMethodParameterValidation(@NotNull String x) {
        return x + "addedInMethod";
    }

    @Size(max = 10)
    @ValidateOnExecution(type = ExecutableType.ALL)
    public String testMethodReturnValidation(String x) {
        return x + "modified";
    }
}
