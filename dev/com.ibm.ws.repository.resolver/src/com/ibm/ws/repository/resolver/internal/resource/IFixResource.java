/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.resolver.internal.resource;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.resolver.ResolveContext;

import com.ibm.ws.product.utility.extension.ifix.xml.Applicability;
import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;
import com.ibm.ws.product.utility.extension.ifix.xml.Offering;
import com.ibm.ws.product.utility.extension.ifix.xml.Problem;
import com.ibm.ws.product.utility.extension.ifix.xml.Resolves;
import com.ibm.ws.product.utility.extension.ifix.xml.UpdatedFile;
import com.ibm.ws.product.utility.extension.ifix.xml.Updates;
import com.ibm.ws.repository.resolver.internal.namespace.InstallableEntityIdentityConstants;
import com.ibm.ws.repository.resources.IfixResource;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.transport.model.Asset;

/**
 * <p>
 * This is a resource for an iFix. It will give a {@link Capability} for each of the APARs it fixes and a requirement on the product(s) that it applies to.
 * </p>
 * <p>
 * There is no public constructor for this class, instead there are two factory method that can be called. If it is being constructed from an installed iFix on the file system then
 * the iFix XML should be parsed into an {@link IFixInfo} object and an instance created through {@link #createInstance(IFixInfo)}. If the iFix is in massive then the {@link Asset}
 * representation of it should be passed to {@link #createInstance(Asset)} </p>
 * <p>
 * Note: this class has a natural ordering that is inconsistent with equals. Comparing these objects will make the highest priority object for installation be less than a lower
 * priority object in order to be most useful when used in a {@link ResolveContext#findProviders(Requirement)} method invocation.</p>
 */
public class IFixResource extends ResourceImpl implements Resource, Comparable<IFixResource> {

    /** This is the date of the file that has the most recent update date in the iFix */
    private final Date iFixDate;

    /**
     * Factory method to create an instance of this class getting all of the information from the <code>iFixInfo</code> object.
     * 
     * @param iFixInfo The info about this iFix
     * @return The new instance of this class
     */
    public static IFixResource createInstance(IFixInfo iFixInfo) {
        List<Capability> capabilities = new ArrayList<Capability>();
        Resolves resolves = iFixInfo.getResolves();
        if (resolves != null) {
            List<Problem> problems = resolves.getProblems();
            if (problems != null) {
                for (Problem problem : problems) {
                    capabilities.add(new InstallableEntityCapability(problem.getDisplayId(), InstallableEntityIdentityConstants.TYPE_IFIX));
                }
            }
        }

        /*
         * uh oh. The iFix XML file is the only thing we have on the file system under the install to say that the iFix is present but it is more an artifact for installation
         * manager than us. As such the applicability section lists marketing edition of WebSphere rather than the editions that appear in the websphere properties file. We are
         * saved by the fact that an iFix JAR also includes an applies-to field that is used by the installer to check the edition (before then chucking that information away) so
         * for an installed iFix like we are dealing with here the product edition will have already been checked. However, the iFix might of been installed then the user updated
         * to a newer version of the product so the iFix is no longer applicable, we should check for this by matching the product version.
         */
        List<Requirement> requirements = new ArrayList<Requirement>();
        Applicability applicability = iFixInfo.getApplicability();
        if (applicability != null) {
            List<Offering> offerings = applicability.getOfferings();
            if (offerings != null && !offerings.isEmpty()) {
                // The iFix always applies to the same version of a product but different editions so get the version from any offering
                VersionRange range = new VersionRange(offerings.get(0).getTolerance());
                Version minimum = convertVersion(range.getLeft());
                Version maximum = convertVersion(range.getRight());
                range = new VersionRange(range.getLeftType(), minimum, maximum, range.getRightType());
                requirements.add(new ProductRequirement(range));
            }
        }

        // Finally find the date with the most recent update date
        List<Date> updateDates = new ArrayList<Date>();
        Updates updates = iFixInfo.getUpdates();
        if (updates != null) {
            Collection<UpdatedFile> updatedFiles = updates.getFiles();
            if (updatedFiles != null) {
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                for (UpdatedFile updatedFile : updatedFiles) {
                    String dateString = updatedFile.getDate();
                    if (dateString != null) {
                        try {
                            Date date = dateFormat.parse(updatedFile.getDate());
                            updateDates.add(date);
                        } catch (ParseException e) {
                            // Ignore this file
                        }
                    }
                }
            }
        }
        Collections.sort(updateDates);

        /*
         * Make sure that the iFix had at least one dated file that it updated. As all updated files have the date field set I don't it is possible to not be set but be lenient in
         * case there is an invalid XML. By setting it to new Date(0) it will mean that this IFixResource will always be sorted last so would only be selected if no other IFix
         * fixes the APARs listed for this iFix.
         */
        Date lastUpdateDate = updateDates.size() > 0 ? updateDates.get(updateDates.size() - 1) : new Date(0);
        return new IFixResource(capabilities, requirements, ResourceImpl.LOCATION_INSTALL, lastUpdateDate, null);
    }

    /**
     * Factory method to create an instance of IFixResource from an asset in Massive
     * 
     * @param asset The asset representing the iFix
     * @return The IFixResource for this asset
     */
    public static IFixResource createInstance(IfixResource massiveFix) {
        // APAR IDs are stored as a provide fix on the WLP information
        Collection<String> aparIds = massiveFix.getProvideFix();
        List<Capability> capabilities = new ArrayList<Capability>();
        for (String aparId : aparIds) {
            capabilities.add(new InstallableEntityCapability(aparId, InstallableEntityIdentityConstants.TYPE_IFIX));
        }

        // The version of the product this applies to comes from the applies to field
        List<Requirement> requirements = new ArrayList<Requirement>();
        String appliesTo = massiveFix.getAppliesTo();
        if (appliesTo != null && !appliesTo.isEmpty()) {
            requirements.add(new ProductRequirement(appliesTo));
        }

        Date iFixDate = massiveFix.getDate();

        return new IFixResource(capabilities, requirements, ResourceImpl.LOCATION_REPOSITORY, iFixDate, massiveFix);
    }

    /**
     * <p>This method will convert a version from an iFix XML file into a product version as used by Liberty. The iFix XML versions follow the IM edition syntax which is related to
     * a Liberty edition by the relationship:</p>
     * 
     * <b>Liberty Version:</b> a.b.c.d</br>
     * <b>iFix XML version:</b> a.b.c*1000+d
     * 
     * @param version The 3 part version from the iFix XML
     * @return The 4 part version
     */
    /* package */static Version convertVersion(Version version) {
        String qualifier = version.getQualifier();
        if (qualifier == null || qualifier.isEmpty()) {
            int micro = version.getMicro();
            qualifier = Integer.toString(micro % 1000);
            micro = micro / 1000;
            version = new Version(version.getMajor(), version.getMinor(),
                            micro, qualifier);
        }
        return version;
    }

    /**
     * Constructs a new instance of this class.
     * 
     * @param capabilities The capabilities of this iFix
     * @param requirements The requirements of this iFix
     * @param location The location of this iFix
     * @param iFixDate The date that this iFix was created
     * @param massiveResource The resource in massive or <code>null</code> if this is not representing a resource from massive
     */
    private IFixResource(List<Capability> capabilities, List<Requirement> requirements, String location, Date mostRecentUpdateDate, RepositoryResource massiveResource) {
        super(capabilities, requirements, location, massiveResource);

        // Default date to 0 (i.e. will always get picked last when there is a choice) if it isn't set
        if (mostRecentUpdateDate != null) {
            this.iFixDate = mostRecentUpdateDate;
        } else {
            this.iFixDate = new Date(0);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(IFixResource o) {
        // First check the locations, install location should always win
        int locationCheck = compareLocation(o);
        if (locationCheck != 0) {
            return locationCheck;
        }

        // We want most recent first so compare the dates the other way around
        return o.iFixDate.compareTo(this.iFixDate);
    }

}
