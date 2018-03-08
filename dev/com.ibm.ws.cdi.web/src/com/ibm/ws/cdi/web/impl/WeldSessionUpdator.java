package com.ibm.ws.cdi.web.impl;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class WeldSessionUpdator implements Filter {

	private static final WELD_SESSION_ATTRIBUTE_PREFIX = "WELD_S#";

	public WeldSessionUpdator() {
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HashMap<String, Integer> hashCodeMap = null;
		// Pre check
		HttpSession session = ((HttpServletRequest)request).getSession(false);
		if (session != null) {
			hashCodeMap = new HashMap<>();
			for (Enumeration<String> e = session.getAttributeNames(); e.hasMoreElements(); ) {
				String key = e.nextElement();
				if (key.startsWith(WELD_SESSION_ATTRIBUTE_PREFIX)) {
					hashCodeMap.put(key, session.getAttribute(key).hashCode());
				}
			}
		}
		//
		chain.doFilter(request, response);
		// Post check
		if (session != null) {
			for (Enumeration<String> e = session.getAttributeNames(); e.hasMoreElements(); ) {
				String key = e.nextElement();
				if (key.startsWith(WELD_SESSION_ATTRIBUTE_PREFIX)) {
					Object o = hashCodeMap.get(key);
					int prevHash = hashCodeMap.get(key);
					if (prevHash != o.hashCode()) {
						session.setAttribute(key, o);
					}
				}
			}
		}
	}

	@Override
	public void destroy() {
	}
}

