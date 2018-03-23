package com.ibm.ws.cdi.test.dependentscopedproducer.servlets;
import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi.test.dependentscopedproducer.DependentSterotype;
import com.ibm.ws.cdi.test.dependentscopedproducer.NullBean;
import com.ibm.ws.cdi.test.dependentscopedproducer.NullBeanThree;
import com.ibm.ws.cdi.test.dependentscopedproducer.NullBeanTwo;
import com.ibm.ws.cdi.test.dependentscopedproducer.SingularNullBeanHolder;
import com.ibm.ws.cdi.test.dependentscopedproducer.producers.NullBeanProducer;

@WebServlet("/")
public class MyServlet extends HttpServlet {
	
	@Inject NullBean nullBean;
	@Inject @DependentSterotype NullBeanTwo nullBeanTwo;
	@Inject NullBeanThree nullBeanThree;
	@Inject SingularNullBeanHolder holder;

	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		PrintWriter pw = response.getWriter();
		
		boolean passed = true;

		if (nullBean == null) {
			pw.write("nullBean was null ");
			if (NullBeanProducer.isNullOne()) {
				pw.write("and it should be null");
			} else {
				pw.write("but it shouldn't be null");
				passed = false;
			}
		} else {
			pw.write("nullBean was not null null");
			if (! NullBeanProducer.isNullOne()) {
				pw.write("and it shouldn't be null");
			} else {
				pw.write("but it should be null");
				passed = false;
			}
		}
		
		pw.write(System.getProperty("line.separator"));
		
		if (nullBeanTwo == null) {
			pw.write("nullBeanTwo was null null");
			if (NullBeanProducer.isNullTwo()) {
				pw.write("and it should be null");
			} else {
				pw.write("but it shouldn't be null");
				passed = false;
			}
		} else {
			pw.write("nullBeanTwo was not null ");
			if (! NullBeanProducer.isNullTwo()) {
				pw.write("and it shouldn't be null");
			} else {
				pw.write("but it should be null");
				passed = false;
			}
		}
		
		pw.write(System.getProperty("line.separator"));
		
		if (nullBeanThree == null) {
			pw.write("nullBeanThree was null ");
			if (NullBeanProducer.isNullThree()) {
				pw.write("and it should be null");
			} else {
				pw.write("but it shouldn't be null");
				passed = false;
			}
		} else {
			pw.write("nullBeanThree was null ");
			if (! NullBeanProducer.isNullThree()) {
				pw.write("and it shouldn't be null");
			} else {
				pw.write("but it should be null");
				passed = false;
			}
		}
		
		pw.write(System.getProperty("line.separator"));
		
		//pw.write(holder.getMessage());
		
		//pw.write(System.getProperty("line.separator"));
		
		//passed = passed && holder.passed();
		
		if (passed) {
			pw.write( "Test Passed! ");
		} else {
			pw.write( "Test Failed! ");
		}
		
		pw.flush();
		pw.close();
	}
	
}
