package com.ibm.ws.build.bnd.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.aries.util.manifest.ManifestProcessor;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.service.Plugin;
import aQute.service.reporter.Reporter;

public class ExternalPackageProcessor implements AnalyzerPlugin, Plugin {

    private static final String EXTERNALS_MF = "externals.mf";
    private static final String SPI_ATTRIBUTE_NAME = "ibm-spi-type";
    private static final String API_ATTRIBUTE_NAME = "ibm-api-type";
    private static final String SPI_HEADER = "IBM-SPI-Package";
    private static final String API_HEADER = "IBM-API-Package";
    private static final String TYPE_ATTR = "type";

    @Override
    public boolean analyzeJar(Analyzer analyzer) throws Exception {

        File externalsMF = analyzer.getFile(EXTERNALS_MF);
        if (externalsMF != null && externalsMF.exists()) {
            System.out.println("Custom bnd plugin: " + getClass().getName() + " detected " + EXTERNALS_MF + " file @ " + externalsMF.getPath());
            Manifest mf = ManifestProcessor.parseManifest(new FileInputStream(externalsMF));
            if (mf != null) {            	
                Map<String, String> manifestEntries = ManifestProcessor.readManifestIntoMap(mf);
                if (manifestEntries.isEmpty()){
                	reporter.error("The externals.mf file was empty", new Object[0]);
                };
                String spiString = manifestEntries.get(SPI_HEADER);
                String apiString = manifestEntries.get(API_HEADER);
                //compare the externals.mf list with the exports
                Parameters exports = analyzer.getExportPackage();
                addAttribute(SPI_ATTRIBUTE_NAME, new Parameters(spiString), exports);
                addAttribute(API_ATTRIBUTE_NAME, new Parameters(apiString), exports);
                //set the new string with the modified attributes
                analyzer.setExportPackage(exports.toString());
            }
        }
        //return false, there is no need to recalc the classpath
        return false;
    }

    private void addAttribute(String attributeName, Parameters externalsParams, Parameters exports) {
        Set<String> pkgs = externalsParams.keySet();

        for (String key : exports.keySet()) {
            if (pkgs.contains(key)) {
                Attrs externalsAttrs = externalsParams.get(key);
                String type = externalsAttrs.get(TYPE_ATTR);
                if (type == null) {
                    //set a default
                    if (SPI_ATTRIBUTE_NAME.equals(attributeName)) {
                        //spi defaults to spi
                        type = "spi";
                    } else if (API_ATTRIBUTE_NAME.equals(attributeName)) {
                        //api defaults to api
                        type = "api";
                    } else {
                        reporter.error("Could not default type attribute for package {0}", key);
                    }
                }
                System.out.println("Adding attribute: " + attributeName + "=" + type + " for package " + key);
                //add the attribute
                Attrs exportAttrs = exports.get(key);
                exportAttrs.put(SPI_ATTRIBUTE_NAME, type);
            }
        }
    }

    @Override
    public void setProperties(Map<String, String> map) {
        //no-op
    }

    private Reporter reporter;

    @Override
    public void setReporter(Reporter reporter) {
        this.reporter = reporter;
    }

}
