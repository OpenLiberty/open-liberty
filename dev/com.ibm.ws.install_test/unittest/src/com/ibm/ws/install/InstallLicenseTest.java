/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.install;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.ws.install.internal.InstallLicenseImpl;
import com.ibm.ws.repository.common.enums.LicenseType;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.resources.AttachmentResource;

/**
 *
 */
public class InstallLicenseTest {

    @Test
    public void testInstallLicenseImpl() throws RepositoryException {
        InstallLicenseImpl ili = new InstallLicenseImpl("type", (LicenseType) null, (AttachmentResource) null, (AttachmentResource) null, (AttachmentResource) null);
        assertEquals("InstallLicenseImpl.getId()", "type", ili.getId());
        assertEquals("InstallLicenseImpl.getType()", "", ili.getType());
        assertEquals("InstallLicenseImpl.getName()", "", ili.getName());
        assertEquals("InstallLicenseImpl.getProgramName()", "", ili.getProgramName());
        assertEquals("InstallLicenseImpl.getInformation()", "", ili.getInformation());
        assertEquals("InstallLicenseImpl.getAgreement()", "", ili.getAgreement());
        assertEquals("InstallLicenseImpl.getFeatures().size()", 0, ili.getFeatures().size());
        ili.addFeature("f1");
        assertEquals("InstallLicenseImpl.getFeatures().size()", 1, ili.getFeatures().size());

        ili = new InstallLicenseImpl("type", LicenseType.UNSPECIFIED, (AttachmentResource) null, (AttachmentResource) null, (AttachmentResource) null);
        assertEquals("InstallLicenseImpl.getType() returns UNSPECIFIED", "UNSPECIFIED", ili.getType());

        ili = new InstallLicenseImpl("type", LicenseType.ILAN, (AttachmentResource) null, (AttachmentResource) null, (AttachmentResource) null);
        assertEquals("InstallLicenseImpl.getType() returns UNSPECIFIED", "ILAN", ili.getType());
    }

    @Test
    public void testInstallLicenseImplUnspecifiedLicenseType() throws RepositoryException {
        InstallLicenseImpl ili = new InstallLicenseImpl("http://www.ibm.com/licenses/wlp-featureterms-v1", null, null);
        assertEquals("InstallLicenseImpl.getType()", "UNSPECIFIED", ili.getType());
        ili = new InstallLicenseImpl("http://www.ibm.com/licenses/WLP-featureterms-v1", null, null);
        assertEquals("InstallLicenseImpl.getType()", "UNSPECIFIED", ili.getType());
    }

}
