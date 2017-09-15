/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.api.jms.impl;

import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jms.ConnectionMetaData;
import javax.jms.JMSException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.ffdc.FFDCFilter;
//import com.ibm.ws.sib.utils.BuildInfo;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author matrober
 * 
 *         This class returns the ConnectionMetaData for the connection.
 */
public class JmsMetaDataImpl implements ConnectionMetaData
{

    //***************************** TRACE VARIABLES ****************************
    private static TraceComponent tc = SibTr.register(JmsMetaDataImpl.class, ApiJmsConstants.MSG_GROUP_INT, ApiJmsConstants.MSG_BUNDLE_INT);

    // ***************************** STATE VARIABLES ****************************
    private static final int jmsMajorVersion = 2;
    private static final int jmsMinorVersion = 0;

    // Initialize this information (which will be dynamically read from the
    // jar manifest) to null values.
    private static String provName = null;
    private static String provVersion = null;
    private static int provMajorVersion = -1;
    private static int provMinorVersion = -1;

    private static final String[] supportedJMSXProps = new String[] { ApiJmsConstants.JMSX_USERID,
                                                                     ApiJmsConstants.JMSX_DELIVERY_COUNT,
                                                                     ApiJmsConstants.JMSX_APPID,
                                                                     ApiJmsConstants.JMSX_GROUPID,
                                                                     ApiJmsConstants.JMSX_GROUPSEQ };
    private static final String packageName = "com.ibm.ws.sib.api.jms.impl";

    // pre-compile a few static patterns for parsing the build level
    // regex to match a valid sib build level e.g. a2c0543.120
    // this match will capture:
    // (1) - the whole build level sring
    // (2) - the numeric year and week number part yyww
    // (3) - the build sequence number. 2 or 3 digits
    private static final String regexSibBuildLevel = "(\\w+(\\d{4})\\.(\\d{2,3}))";
    private static final Pattern sibBuildLevelPattern = Pattern.compile(regexSibBuildLevel);

    // pattern for the old format of implementation version containing release and build
    // e.g.  WASX.SIB [a2c0543.120] e.g. wwww.wwww wwwdddd.ddd
    private static final Pattern sibOldVersionPattern = Pattern.compile(".*[\\w\\.]+\\s+\\[" + regexSibBuildLevel + "\\].*");

    // ******************************* CONSTRUCTORS *****************************

    /**
     * Constructor for ProtoMetaData.
     */
    public JmsMetaDataImpl() {
        super();
    }

    // ************************* INTERFACE METHODS ******************************

    /**
     * @see javax.jms.ConnectionMetaData#getJMSVersion()
     */
    @Override
    public String getJMSVersion() throws JMSException {
        return getJMSMajorVersion() + "." + getJMSMinorVersion();
    }

    /**
     * @see javax.jms.ConnectionMetaData#getJMSMajorVersion()
     */
    @Override
    public int getJMSMajorVersion() throws JMSException {
        return jmsMajorVersion;
    }

    /**
     * @see javax.jms.ConnectionMetaData#getJMSMinorVersion()
     */
    @Override
    public int getJMSMinorVersion() throws JMSException {
        return jmsMinorVersion;
    }

    /**
     * @see javax.jms.ConnectionMetaData#getJMSProviderName()
     */
    @Override
    public String getJMSProviderName() throws JMSException {
        if (JmsMetaDataImpl.provName == null) {
            // Initialize the data if it has not already been done.
            JmsMetaDataImpl.retrieveManifestData();
        }
        return JmsMetaDataImpl.provName;
    }

    /**
     * @see javax.jms.ConnectionMetaData#getProviderVersion()
     */
    @Override
    public String getProviderVersion() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getProviderVersion");
        if (JmsMetaDataImpl.provVersion == null) {
            // Initialize the data if it has not already been done.
            JmsMetaDataImpl.retrieveManifestData();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getProviderVersion", JmsMetaDataImpl.provVersion);
        return JmsMetaDataImpl.provVersion;
    }

    /**
     * @see javax.jms.ConnectionMetaData#getProviderMajorVersion()
     */
    @Override
    public int getProviderMajorVersion() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getProviderMajorVersion");
        if (JmsMetaDataImpl.provMajorVersion == -1) {
            // Initialize the data if it has not already been done.
            JmsMetaDataImpl.retrieveManifestData();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getProviderMajorVersion", JmsMetaDataImpl.provMajorVersion);
        return JmsMetaDataImpl.provMajorVersion;
    }

    /**
     * @see javax.jms.ConnectionMetaData#getProviderMinorVersion()
     */
    @Override
    public int getProviderMinorVersion() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getProviderMinorVersion");
        if (JmsMetaDataImpl.provMinorVersion == -1) {
            // Initialize the data if it has not already been done.
            JmsMetaDataImpl.retrieveManifestData();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getProviderMinorVersion", JmsMetaDataImpl.provMinorVersion);
        return JmsMetaDataImpl.provMinorVersion;
    }

    /**
     * @see javax.jms.ConnectionMetaData#getJMSXPropertyNames()
     */
    @Override
    public Enumeration getJMSXPropertyNames() throws JMSException {
        Vector v = new Vector(supportedJMSXProps.length);
        for (int i = 0; i < supportedJMSXProps.length; i++)
            v.add(supportedJMSXProps[i]);
        return v.elements();
    }

    // ******************* IMPLEMENTATION METHODS ***********************

    /**
     * This method retrieves the information stored in the jar manifest and uses
     * it to populate the implementation information to be returned to the user.
     * 
     * If the build level is not available in the manifest, the BuildInfo class
     * is used to get the value instead.
     */
    private static void retrieveManifestData() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "retrieveManifestData");

        try {
            // Set up the defaults that will be overriden if possible.
            JmsMetaDataImpl.setProblemDefaults();

            Package thisPackage = Package.getPackage(packageName);
            if (thisPackage != null) {

                // This string will contain something like "IBM".
                String tempProv = thisPackage.getImplementationVendor();

                // Only set it if it is not null.
                if (tempProv != null) {
                    JmsMetaDataImpl.provName = tempProv;
                }

                // d336782 rework of build level processing
                //
                // The format for the implementation version will depend on how the jar containing
                // our package was built.
                // The builds of sib.api.jmsImpl.jar will contain something like this:
                // Implementation-Version: WASX.SIB [o0602.101]
                // Implementation-Vendor: IBM Corp.
                // However, the product is build into a larger package com.ibm.ws.sib.server.jar
                // with entries in the manifest like this:
                // Implementation-Version: 1.0.0
                // Implementation-Vendor: IBM Corp.
                //
                // We parse the implementation version and if it has the former format
                // we use that build level, otherwise we will use the build level
                // reported by the BuildInfo class.
                //
                // We will set provVersion to the whole build level string,
                // provMajorVersion to the 4 numbers before the dot (yyww - year and week)
                // and provMinorVersion to the two or three numbers after the dot
                // (build sequence)
                String version = thisPackage.getImplementationVersion();

                if (version != null) {

                    Matcher m = sibOldVersionPattern.matcher(version);

                    if (!(m.matches())) { // is there a valid build level in the version strng?
                        // NO. New format or some unknown format. Use BuildInfo class
                        version = "1.0.0";//BuildInfo.getBuildLevel(); //this will bever be used is the assumption -lohith  //lohith liberty change

                        m = sibBuildLevelPattern.matcher(version); // re-match for just the build level
                    }

                    if (m.matches()) { // check again
                        JmsMetaDataImpl.provVersion = m.group(1); // e.g. wwwdddd.ddd
                        try {
                            JmsMetaDataImpl.provMajorVersion = Integer.valueOf(m.group(2)).intValue();
                            JmsMetaDataImpl.provMinorVersion = Integer.valueOf(m.group(3)).intValue();
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(tc, "provMajorVersion=" + JmsMetaDataImpl.provMajorVersion + "provMinorVersion=" + JmsMetaDataImpl.provMinorVersion);
                        } catch (RuntimeException e2) {
                            // No FFDC code needed
                            // This exception should never happen if the regex worked properly returning numbers for group 2 and 3
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(tc, "Unable to convert major or minor version number from build level " + version + " to int", e2);
                        }
                    }
                    else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "Unable to find a valid build level in " + version);
                    }
                }

                else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Implementation version from manifest was null");
                }

            }
            else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "The package was null - unable to retrieve information");
            } //if package not null

        } catch (RuntimeException e) {
            // No FFDC code needed
            FFDCFilter.processException(e, "com.ibm.ws.sib.api.jms.impl.JmsMetaDataImpl", "retrieveManifestData#1");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Error retrieving manifest information", e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "retrieveManifestData");
    }

    /**
     * If there is a problem retrieving the information from the manifest, then
     * this method is called to set some pretend values.
     */
    private static void setProblemDefaults() {
        //  Set up some defaults.
        JmsMetaDataImpl.provName = "IBM";
        JmsMetaDataImpl.provVersion = "1.0";
        JmsMetaDataImpl.provMajorVersion = 1;
        JmsMetaDataImpl.provMinorVersion = 0;
    }
}
