package com.ibm.ws.commitPriority.cdi;

import javax.annotation.Resource;
import javax.inject.Named;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.transaction.UserTransaction;

import com.ibm.ws.commitPriority.common.CommitPriorityLocal;
import com.ibm.ws.commitPriority.common.CommitPriorityTestUtils;

/**
 * CDI Bean implementation class Test
 */
@Transactional(value = TxType.NOT_SUPPORTED)
@Named("managedbeaninservlet")
public class ManagedBeanInServlet implements CommitPriorityLocal {

    @Resource(name = "jdbc/derby4")
    DataSource ds1;

    @Resource(name = "jdbc/derby5")
    DataSource ds2;

    @Resource(name = "jdbc/derby6")
    DataSource ds3;

    @Resource
    private UserTransaction ut;

    @Override
    public String testMethod() throws Exception {

        return CommitPriorityTestUtils.test(ut, ds1, ds2, ds3);
    }
}