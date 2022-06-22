package com.ibm.ws.transactionalEJB.web;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import com.ibm.tx.jta.ut.util.TestServletBase;

/**
 * Servlet implementation class TransactionalEJBTest
 *
 * App should never deploy because TestEJB has @Transactional annotation
 */
@SuppressWarnings("serial")
@WebServlet("/transactionalEJB")
public class TransactionalEJBTestServlet extends TestServletBase {

    public void testNoTransactionalEJB() throws NamingException {
        @SuppressWarnings("unused")
        final TestEJB t = (TestEJB) new InitialContext().lookup("java:module/TestEJB");
    }
}