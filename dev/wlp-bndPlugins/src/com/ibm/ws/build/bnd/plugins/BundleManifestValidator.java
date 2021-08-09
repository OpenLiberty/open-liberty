package com.ibm.ws.build.bnd.plugins;

import java.util.Map;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.service.Plugin;
import aQute.bnd.version.Version;
import aQute.service.reporter.Reporter;

public class BundleManifestValidator implements AnalyzerPlugin, Plugin {
    
    class BndBundleVersionError extends Error {
        private static final long serialVersionUID = 5897241501002517451L;

        public BndBundleVersionError(String detailMessage) {
            super(detailMessage);
        }
    }
    
    @Override
    public boolean analyzeJar(Analyzer analyzer) throws Exception {
        Version v = new Version(analyzer.getBundleVersion());
        
        if(v.getQualifier() == null && Boolean.valueOf(analyzer.getProperty("allow.bundle-version.override")).booleanValue() == false) {
            throw new BndBundleVersionError("ERROR ERROR ERROR: The current XML and BND file for bundle " + analyzer.getBundleSymbolicName() + " will result in an iFixed bundle that does NOT have a Bundle-Version header with a qualifier. This is a serious serviceability issue! To rectify this issue, you should import bundle.props and set bVersion. If this is a TEST bundle, set allow.bundle-version.override=true in the bnd file.");
        }
        //Don't recalculate the classpath
        return false;
    }
    
    @Override
    public void setProperties(Map<String, String> map) {
      //No-op
    }

    @Override
    public void setReporter(Reporter processor) {
      //No-op
    }
    
}
