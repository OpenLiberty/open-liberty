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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.websphere.samples.batch.beans.AccountDataObject;
import com.ibm.websphere.samples.batch.util.BonusPayoutConstants;
import com.ibm.websphere.samples.batch.util.BonusPayoutUtils;

/**
 *
 */
@Named("ValidationProcessor")
public class ValidationProcessor implements ItemProcessor, BonusPayoutConstants {

    private final static Logger logger = Logger.getLogger(BONUSPAYOUT_LOGGER);

    @Inject
    @BatchProperty(name = "bonusAmount")
    String bonusAmountStr;

    Integer bonusAmount = null;

    @Override
    public Object processItem(Object item) throws Exception {
        AccountDataObject tableDO = (AccountDataObject) item;
        AccountDataObject fileDO = tableDO.getCompareToDataObject();

        if (fileDO == null) {
            throw new IllegalStateException("Somehow got null fileDO");
        }

        if (tableDO.getAccountNumber() != fileDO.getAccountNumber()) {
            String errorMsg = "Mismatch between DB account # " + tableDO.getAccountNumber() + " and file account # " + fileDO.getAccountNumber();
            BonusPayoutUtils.throwIllegalStateExc(errorMsg);
        } else if (!tableDO.getAccountCode().equals(fileDO.getAccountCode())) {
            String errorMsg = "Mismatch between DB account code " + tableDO.getAccountCode() + " and file account code " + fileDO.getAccountCode();
            BonusPayoutUtils.throwIllegalStateExc(errorMsg);
        }

        int originalBalance = fileDO.getBalance();
        int updatedBalance = tableDO.getBalance();

        if (originalBalance + getBonusAmount() != updatedBalance) {
            String errorMsg = "Mismatch between expected updated balance of " + Integer.toString(originalBalance + getBonusAmount()) + " actual updated balance of "
                              + updatedBalance;
            BonusPayoutUtils.throwIllegalStateExc(errorMsg);
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Verified match for records for account # " + tableDO.getAccountNumber());
        }
        return tableDO;
    }

    private int getBonusAmount() {
        if (bonusAmount == null) {
            bonusAmount = Integer.parseInt(bonusAmountStr);
        }

        return bonusAmount;
    }
}
