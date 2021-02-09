package com.ibm.websphere.samples.batch.artifacts;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import javax.batch.api.chunk.listener.AbstractItemWriteListener;
import javax.inject.Named;

import com.ibm.websphere.samples.batch.util.BonusPayoutConstants;

@Named("BatchUpdateExceptionListener")
public class BatchUpdateExceptionListener extends AbstractItemWriteListener implements BonusPayoutConstants {

    private final static Logger logger = Logger.getLogger(BONUSPAYOUT_LOGGER);

    @Override
    public void onWriteError(List<Object> items, Exception ex) throws Exception {

        boolean testing = true;
        BatchUpdateException bue = null;
        if (ex instanceof BatchUpdateException) {
            bue = (BatchUpdateException) ex;
            int[] updateCounts = bue.getUpdateCounts();
            logger.info("Caught BatchUpdateException with exception message = " + ex.getMessage());
            logger.info("Update Counts = " + updateCounts.length + "; Original chunk size of items to write = " + items.size());

            SQLException e = bue;
            while (e.getNextException() != null) {
                logger.info("Next chained exception message: " + e.getMessage() + ", SQLSTATE = " + e.getSQLState() + ", errorcode = " + e.getErrorCode());
                e = e.getNextException();
            }
        } else {
            logger.info("Found something besides BatchUpdateException, class type = " + ex.getClass());
        }

    }
}
