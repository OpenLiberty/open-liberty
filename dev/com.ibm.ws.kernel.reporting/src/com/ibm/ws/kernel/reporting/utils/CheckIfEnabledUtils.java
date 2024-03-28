package com.ibm.ws.kernel.reporting.utils;

import java.util.Map;

public class CheckIfEnabledUtils {

	/**
	 * <p>
	 * Check server.xml to see if any are explicitly disabling CVE Reporting.
	 * </p>
	 * 
	 * @param properties Map<String,String>
	 * @return enabled boolean
	 */
	public static boolean isEnabled(Map<String, Object> properties) {

		boolean enabled = true;

		if (properties.containsKey("enabled")) {
			if (properties.get("enabled").equals(false)) {
				enabled = false;
			}
		}
		return enabled;
	}

}
