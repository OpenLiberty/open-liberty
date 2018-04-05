package com.ibm.ws.cdi.test.dependentscopedproducer.servlets;
import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi.test.dependentscopedproducer.NonNullBeanThree;

//This servlet should return a resource injection exception when accessed. 
@WebServlet("/failAppSteryotypedMethod")
public class AppScopedSteryotypedServlet extends HttpServlet {
	
    @Inject NonNullBeanThree nullBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		PrintWriter pw = response.getWriter();

		nullBean.toString(); //calling a method as a proxy gets injected.
		if (nullBean == null) {
			pw.write("You shouldn't see this. Test Failed");
		}
		
		pw.flush();
		pw.close();
	}

}
