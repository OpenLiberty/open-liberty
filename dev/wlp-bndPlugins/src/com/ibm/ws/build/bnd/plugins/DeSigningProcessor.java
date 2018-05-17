package com.ibm.ws.build.bnd.plugins;

import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.service.Plugin;
import aQute.service.reporter.Reporter;

public class DeSigningProcessor implements AnalyzerPlugin, Plugin {

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		Manifest m = analyzer.getJar().getManifest();
		// Clear all the non-main sections in the original manifest
		// This will remove any signing (and any other non-main sections, but
		// ...)
		Map<String, Attributes> sections = m.getEntries();
		if (!sections.isEmpty()) {
			System.out
					.println("Stripping out "
							+ sections.size()
							+ " non-main sections from the original manifest before wrapping.");
			sections.clear();
		}

		// return false, there is no need to recalc the classpath
		return false;
	}

	@Override
	public void setProperties(Map<String, String> map) {
		// no-op
	}

	private Reporter reporter;

	@Override
	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

}
