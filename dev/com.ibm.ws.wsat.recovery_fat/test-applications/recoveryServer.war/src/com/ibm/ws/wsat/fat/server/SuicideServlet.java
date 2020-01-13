package com.ibm.ws.wsat.fat.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.tx.jta.ut.util.XAResourceImpl;

@WebServlet({ "/SuicideServlet" })
public class SuicideServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		System.out.println("Get in SuicideServlet in recoveryServer.");
		XAResourceImpl.dumpState();
		Runtime.getRuntime().halt(0);
		System.out.println("Should not reach here in SuicideServlet!");
	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
	}
}