/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.samples.batch.artifacts;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Inject;

import com.ibm.websphere.samples.batch.beans.AccountDataObject;
import com.ibm.websphere.samples.batch.util.BonusPayoutConstants;
import javax.inject.Named;
/**
 * Add a 'bonusAmount' to each balance.
 */
@Named("BonusCreditProcessor")
public class BonusCreditProcessor implements ItemProcessor, BonusPayoutConstants {

    @Inject
    @BatchProperty(name = "bonusAmount")
    String bonusAmountStr;

    Integer bonusAmount = null;

    @Override
    public Object processItem(Object item) throws Exception {
        AccountDataObject ado = (AccountDataObject) item;

        int newBalance = ado.getBalance();
        ado.setBalance(newBalance + getBonusAmount());
        return ado;
    }

    private int getBonusAmount() {
        if (bonusAmount == null) {
            bonusAmount = Integer.parseInt(bonusAmountStr);
        }

        return bonusAmount;
    }

}
