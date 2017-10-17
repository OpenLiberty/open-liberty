package com.ibm.ws.cdi12.test.ejb.timer;

import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi12.test.ejb.timer.view.EjbSessionBeanLocal;

/**
 * Servlet implementation class TestEjbTimerServlet
 */
@WebServlet("/Timer")
public class TestEjbTimerServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    @EJB
    EjbSessionBeanLocal bean;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public TestEjbTimerServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream os = response.getOutputStream();
        os.println(String.format("session = %d request = %d", bean.getSesCount(), bean.getReqCount()));
        os.println();
        os.println(bean.getStack());
        bean.incCountersViaTimer();
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
    }

}
