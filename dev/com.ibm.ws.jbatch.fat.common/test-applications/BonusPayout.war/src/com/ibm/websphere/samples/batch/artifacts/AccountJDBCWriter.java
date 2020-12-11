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

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.logging.Logger;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.api.chunk.ItemWriter;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

import com.ibm.websphere.samples.batch.beans.AccountDataObject;
import com.ibm.websphere.samples.batch.util.BonusPayoutConstants;
import com.ibm.websphere.samples.batch.util.BonusPayoutUtils;

/**
 *
 */
@Named("AccountJDBCWriter")
public class AccountJDBCWriter extends AbstractItemWriter implements ItemWriter, BonusPayoutConstants {

    private final static Logger logger = Logger.getLogger(BONUSPAYOUT_LOGGER);

    @Inject
    @BatchProperty
    private String dsJNDI;

    @Inject
    @BatchProperty
    private String tableName;

    @Inject
    private JobContext jobCtx;

    private DataSource ds = null;

    @Override
    public void open(Serializable checkpoint) throws Exception {
        ds = BonusPayoutUtils.lookupDataSource(dsJNDI);
        // Validate table name
        BonusPayoutUtils.validateTableName(tableName);
    }

    /*
     * Follow get-use-close pattern w.r.t. Connection
     */
    @Override
    public void writeItems(List<Object> items) throws Exception {
        Connection conn = ds.getConnection();
        String sql = "INSERT INTO " + tableName + " VALUES (?,?,?,?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        for (Object obj : items) {
            AccountDataObject ado = AccountDataObject.class.cast(obj);
            ps.setInt(1, ado.getAccountNumber());
            ps.setInt(2, ado.getBalance());
            ps.setLong(3, jobCtx.getInstanceId());
            ps.setString(4, ado.getAccountCode());
            ps.addBatch();
        }
        logger.fine("Adding: " + items.size() + " items to table name: " + tableName + " via batch update");
        ps.executeBatch();
        logger.fine("Executed batch update.");
        ps.clearBatch();
        ps.close();
        conn.close();
    }

}
