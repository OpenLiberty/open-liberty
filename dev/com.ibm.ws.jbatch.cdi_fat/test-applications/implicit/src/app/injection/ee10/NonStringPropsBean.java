/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package app.injection.ee10;

import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import app.injection.NoActiveBatchJobException;

@Dependent
@Named("NonStringPropsBean")
public class NonStringPropsBean {

    @Inject
    private JobContext jobContext;

    @Inject
    @BatchProperty(name = "color")
    protected String color;

    @Inject
    @BatchProperty(name = "quantity")
    protected Integer quantity;

    @Inject
    @BatchProperty(name = "shortProp")
    protected Short shortProp;

    @Inject
    @BatchProperty(name = "longProp")
    protected Long longProp;

    @Inject
    @BatchProperty(name = "floatProp")
    protected Float floatProp;

    @Inject
    @BatchProperty(name = "doubleProp")
    protected Double doubleProp;

    @Inject
    @BatchProperty(name = "boolProp")
    protected Boolean boolProp;

    /**
     *
     * @return The value returned by calling getJobName() on the injected JobContext.
     * @throws NoActiveBatchJobException If and only if the JobContext is null.
     */
    public String getJobName() throws NoActiveBatchJobException {
        if (jobContext != null) {
            return jobContext.getJobName();
        } else {
            throw new NoActiveBatchJobException("From getJobName()");
        }
    }

    public String getColor() {
        return color;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Short getShortProp() {
        return shortProp;
    }

    public Long getLongProp() {
        return longProp;
    }

    public Float getFloatProp() {
        return floatProp;
    }

    public Double getDoubleProp() {
        return doubleProp;
    }

    public Boolean getBoolProp() {
        return boolProp;
    }

    @Override
    public String toString() {
        return super.toString() + ",  jobCtx= " + jobContext + ", color =" + color + ", quantity = " + quantity + ", shortProp = " + shortProp
               + ", longProp = " + longProp + ", floatProp = " + floatProp + ", doubleProp = " + doubleProp + ", boolProp = " + boolProp;
    }

}
