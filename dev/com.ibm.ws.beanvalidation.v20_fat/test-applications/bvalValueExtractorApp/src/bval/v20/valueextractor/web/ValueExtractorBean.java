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
package bval.v20.valueextractor.web;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

@RequestScoped
public class ValueExtractorBean {

    @Min(1)
    protected IntWrapper intWrapper = new IntWrapper(5);

    @NotNull
    protected StringWrapper stringWrapper = new StringWrapper("");

    @ValidateOnExecution(type = { javax.validation.executable.ExecutableType.ALL })
    public void testCustomValueExtractorFromXml(List<@NotNull String> decMaxInclusiveFalseList) {
        System.out.println("Validated testCustomValueExtractorFromXml");
    }

    @ValidateOnExecution(type = { javax.validation.executable.ExecutableType.ALL })
    public void testCustomValueExtractorFromServiceReg(@DecimalMax("10") DoubleWrapper doubleWrapper) {
        System.out.println("Validated testCustomValueExtractorFromServiceReg");
    }
}