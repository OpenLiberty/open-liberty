package com.ibm.ws.remoteEJB.ejb;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.ws.remoteEJB.shared.TestBeanRemote;

@Stateless(name = "TestBean")
@Remote(TestBeanRemote.class)
public class TestBean implements TestBeanRemote {

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String getProperty(String var) {
//        System.getProperties().entrySet().stream().forEach(e -> System.out.println("Prop: " + e.getKey() + " -> " + e.getValue()));
//        System.getenv().entrySet().stream().forEach(e -> System.out.println("Env: " + e.getKey() + " -> " + e.getValue()));

        return System.getProperty(var);
    }

    private String tranID() throws SystemException {
        Transaction t = TransactionManagerFactory.getTransactionManager().getTransaction();

        if (null != t) {
            String strID = t.toString();
            System.out.println("Tran ID: " + strID);
            int start = strID.indexOf("#tid=") + 5;
            int end = strID.indexOf(",");
            strID = strID.substring(start, end);
            System.out.println("tid: " + strID);
            return strID;
        }

        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public String mandatory() throws SystemException {
        return tranID();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String required() throws SystemException {
        return tranID();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String requiresNew() throws SystemException {
        return tranID();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String supports() throws SystemException {
        return tranID();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String notSupported() throws SystemException {
        return tranID();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public String never() throws SystemException {
        return tranID();
    }
}