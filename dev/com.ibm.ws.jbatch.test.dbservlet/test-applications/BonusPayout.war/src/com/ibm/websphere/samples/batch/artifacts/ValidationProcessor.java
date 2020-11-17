/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
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
