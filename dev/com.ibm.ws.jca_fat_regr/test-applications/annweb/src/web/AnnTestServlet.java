package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import javax.naming.InitialContext;
import javax.resource.cci.ConnectionFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class AnnTestServlet extends HttpServlet {

    private final String servletName = this.getClass().getSimpleName();
    final String
    // CD_1 = "com.ibm.tra.ann.ConnDefAnn1",
    CD_1_CONNFACT_INTF = "javax.resource.cci.ConnectionFactory",
                    CD_1_CONNFACT_IMPL = "com.ibm.tra.outbound.impl.J2CConnectionFactory",
                    CD_1_CONN_INTF = "javax.resource.cci.Connection",
                    CD_1_CONN_IMPL = "com.ibm.tra.outbound.impl.J2CConnection",

                    // CD_2 = "com.ibm.tra.ann.ConnDefAnn2",
                    CD_2_CONNFACT_INTF = "com.ibm.tra.outbound.base.ConnectionFactoryBase",
                    CD_2_CONNFACT_IMPL = "com.ibm.tra.outbound.impl.J2CConnectionFactory",
                    CD_2_CONN_INTF = "com.ibm.tra.outbound.base.ConnectionBase",
                    CD_2_CONN_IMPL = "com.ibm.tra.outbound.impl.J2CConnection";

    public static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println(" ---> " + servletName + " is starting " + test + "<br>");
        System.out.println(" ---> " + servletName + " is starting test: " + test);

        try {
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);
            out.println(" <--- " + test + " " + SUCCESS_MESSAGE);
            System.out.println(" <--- " + test + " " + SUCCESS_MESSAGE);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
            x.printStackTrace();
            out.println(" <--- " + test + " FAILED");
            System.out.println(" <--- " + test + " FAILED");
        } finally {
            out.flush();
            out.close();
        }
    }

    public AnnTestServlet() {

    }

    public void testSingleNoConnDefInDD(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf4");

        javax.resource.cci.Connection con = cf.getConnection();

        System.out.println("In test: testSingleNoConnDefInDD");
        System.out.println("expected CF class  name is : " + CD_1_CONNFACT_IMPL + " and actual name is " + cf.getClass());
        System.out.println("expected con class  name is : " + CD_1_CONN_IMPL + " and actual name is " + con.getClass());
        System.out.println("expected CF Interface of " + CD_1_CONNFACT_INTF + " is correct ? " + (cf instanceof javax.resource.cci.ConnectionFactory));
        System.out.println("expected Con Interface of " + CD_1_CONN_INTF + " is correct ? " + (con instanceof javax.resource.cci.Connection));

        boolean val = false;
        if ((cf.getClass().getName()).equals(CD_1_CONNFACT_IMPL) && (con.getClass().getName()).equals(CD_1_CONN_IMPL) && (cf instanceof javax.resource.cci.ConnectionFactory)
            && (con instanceof javax.resource.cci.Connection))
            val = true;

        if (val)
            System.out.println("testSingleNoConnDefInDD test passed");
        else
            throw new Exception("testSingleNoConnDefInDD test failed");

    }

    public void testSingleConnDefDiffCfInDD(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        ConnectionFactory cf1 = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");

        javax.resource.cci.Connection con1 = cf1.getConnection();

        System.out.println("In test: testSingleConnDefDiffCfInDD phase 1");
        System.out.println("expected CF class  name is : " + CD_1_CONNFACT_IMPL + " and actual name is " + cf1.getClass());
        System.out.println("expected con class  name is : " + CD_1_CONN_IMPL + " and actual name is " + con1.getClass());
        System.out.println("expected CF Interface of " + CD_1_CONNFACT_INTF + " is correct ? " + (cf1 instanceof javax.resource.cci.ConnectionFactory));
        System.out.println("expected Con Interface of " + CD_1_CONN_INTF + " is correct ? " + (con1 instanceof javax.resource.cci.Connection));

        boolean val = false;
        if ((cf1.getClass().getName()).equals(CD_1_CONNFACT_IMPL) && (con1.getClass().getName()).equals(CD_1_CONN_IMPL)
            && (cf1 instanceof javax.resource.cci.ConnectionFactory)
            && (con1 instanceof javax.resource.cci.Connection))
            val = true;

        if (val)
            System.out.println("testSingleConnDefDiffCfInDD phase1 test passed");
        else
            throw new Exception("testSingleConnDefDiffCfInDD phase1 test failed");

        ConnectionFactory cf2 = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf2");

        javax.resource.cci.Connection con2 = cf2.getConnection();

        System.out.println("In test: testSingleConnDefDiffCfInDD phase 2");
        System.out.println("expected CF class  name is : " + CD_2_CONNFACT_IMPL + " and actual name is " + cf2.getClass());
        System.out.println("expected con class  name is : " + CD_2_CONN_IMPL + " and actual name is " + con2.getClass());
        System.out.println("expected CF Interface of " + CD_2_CONNFACT_INTF + " is correct ? " + (cf2 instanceof com.ibm.tra.outbound.base.ConnectionFactoryBase));
        System.out.println("expected Con Interface of " + CD_2_CONN_INTF + " is correct ? " + (con2 instanceof com.ibm.tra.outbound.base.ConnectionBase));

        val = false;
        if ((cf2.getClass().getName()).equals(CD_2_CONNFACT_IMPL) && (con2.getClass().getName()).equals(CD_2_CONN_IMPL)
            && (cf2 instanceof com.ibm.tra.outbound.base.ConnectionFactoryBase)
            && (con2 instanceof com.ibm.tra.outbound.base.ConnectionBase))
            val = true;

        if (val)
            System.out.println("testSingleConnDefDiffCfInDD phase2 test passed");
        else
            throw new Exception("testSingleConnDefDiffCfInDD phase2 test failed");

    }

    public void testSingleNoOutboundRaInDD(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf3");

        javax.resource.cci.Connection con = cf.getConnection();

        System.out.println("CF class  name is : " + cf.getClass());
        System.out.println("Con class  name is : " + con.getClass());
        System.out.println("CF Interface is correct ? " + (cf instanceof javax.resource.cci.ConnectionFactory));
        System.out.println("Con Interface is correct ? " + (con instanceof javax.resource.cci.Connection));

        boolean val = false;
        if ((cf.getClass().getName()).equals(CD_1_CONNFACT_IMPL) && (con.getClass().getName()).equals(CD_1_CONN_IMPL) && (cf instanceof javax.resource.cci.ConnectionFactory)
            && (con instanceof javax.resource.cci.Connection))
            val = true;

        if (val)
            System.out.println("testSingleNoConnDefInDD test passed");
        else
            throw new Exception("testSingleNoConnDefInDD test failed");

    }
}