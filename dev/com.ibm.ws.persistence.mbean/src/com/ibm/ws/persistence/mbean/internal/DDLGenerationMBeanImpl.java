/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.persistence.mbean.internal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.DynamicMBean;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.persistence.mbean.DDLGenerationMBean;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.kernel.service.utils.PathUtils;
import com.ibm.wsspi.kernel.service.utils.ServiceAndServiceReferencePair;
import com.ibm.wsspi.logging.TextFileOutputStreamFactory;
import com.ibm.wsspi.persistence.DDLGenerationParticipant;

@Component(service = { DDLGenerationMBean.class, DynamicMBean.class },
                immediate = true,
                property = { "service.vendor=IBM",
                            "jmx.objectname=" + DDLGenerationMBean.OBJECT_NAME })
public class DDLGenerationMBeanImpl extends StandardMBean implements DDLGenerationMBean {
    private static final String OUTPUT_DIR = WsLocationConstants.SYMBOL_SERVER_OUTPUT_DIR + "ddl" + File.separator;

    private static final String KEY_GENERATOR = "generator";

    private final ConcurrentServiceReferenceSet<DDLGenerationParticipant> generators = new ConcurrentServiceReferenceSet<DDLGenerationParticipant>(
                    KEY_GENERATOR);

    private final AtomicReference<WsLocationAdmin> locationService = new AtomicReference<WsLocationAdmin>();

    /**
     * Service constructor. Invokes super() for StandardMBean.
     * 
     * @throws NotCompliantMBeanException
     */
    public DDLGenerationMBeanImpl() throws NotCompliantMBeanException {
        super(DDLGenerationMBean.class);
    }

    /**
     * Method which registers a DDL generator. All OSGi services providing this
     * interface will be set here.
     */
    @Reference(name = KEY_GENERATOR, service = DDLGenerationParticipant.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void setGenerator(ServiceReference<DDLGenerationParticipant> ref) {
        generators.addReference(ref);
    }

    /**
     * Method which will unregister a DDL generator.
     */
    protected void unsetGenerator(ServiceReference<DDLGenerationParticipant> ref) {
        generators.removeReference(ref);
    }

    @Reference(name = "locationService")
    protected void setLocationService(WsLocationAdmin locationService) {
        this.locationService.set(locationService);
    }

    protected void unsetLocationService(WsLocationAdmin locationService) {
        this.locationService.set(null);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        generators.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        generators.deactivate(cc);
    }

    /**
     * Trigger DDL generation for anyone who needs to generate DDL.
     */
    @Override
    synchronized public Map<String, Serializable> generateDDL() {
        Map<String, Serializable> returnMap = new HashMap<String, Serializable>();
        WsResource ddlOutputDirectory = locationService.get().resolveResource(OUTPUT_DIR);
        if (ddlOutputDirectory.exists() == false) {
            ddlOutputDirectory.create();
        }

        // Try to put the canonical path to the DDL output directory in the results.  
        // If we can't, then put the symbolic name.
        try {
            returnMap.put(OUTPUT_DIRECTORY, ddlOutputDirectory.asFile().getCanonicalPath());
        } catch (IOException ioe) {
            returnMap.put(OUTPUT_DIRECTORY, OUTPUT_DIR);
        }
        boolean success = true;
        int fileCount = 0;

        Map<String, DDLGenerationParticipant> participants = new HashMap<String, DDLGenerationParticipant>();
        Iterator<ServiceAndServiceReferencePair<DDLGenerationParticipant>> i = generators.getServicesWithReferences();
        while (i.hasNext()) {
            // We'll request the DDL be written to a file whose name is chosen by the component providing the service.
            ServiceAndServiceReferencePair<DDLGenerationParticipant> generatorPair = i.next();
            DDLGenerationParticipant generator = generatorPair.getService();
            String rawId = generator.getDDLFileName();

            // Remove any restricted characters from the file name, and make sure
            // that the resulting string is not empty.  If it's empty, supply a
            // default name.
            String id = (rawId != null) ? PathUtils.replaceRestrictedCharactersInFileName(rawId) : null;
            if ((id == null) || (id.length() == 0)) {
                throw new IllegalArgumentException("Service " + generator.toString() + " DDL file name: " + rawId);
            }

            participants.put(id, generator);
        }

        for (Map.Entry<String, DDLGenerationParticipant> entry : participants.entrySet()) {
            String id = entry.getKey();
            DDLGenerationParticipant participant = entry.getValue();

            // The path to the file is in the server's output directory.
            WsResource ddlOutputResource = locationService.get().resolveResource(OUTPUT_DIR + id + ".ddl");
            if (ddlOutputResource.exists() == false) {
                ddlOutputResource.create();
            }

            // Use the text file output stream factory to create the file so that
            // it is readable on distributed and z/OS platforms.  Overwrite the
            // file if it already exists.  We have to specify the encoding explicitly
            // on the OutputStreamWriter or Findbugs gets upset.  We always specify
            // UTF-8 because the output DDL might have UNICODE characters.  We use
            // a TextFileOutputStreamFactory in an attempt to make the file readable
            // on z/OS.  The file will be tagged as 'ISO8859-1' on z/OS, allowing at
            // least some of the characters to be printable.  The z/OS chtag command
            // does not appear to honor 'UTF-8' as an encoding, even though iconv
            // supports it.  The data on the disk will be correct in any case, the
            // customer may need to FTP it to a distributed machine, or use iconv,
            // to be able to view the data.
            try {
                TextFileOutputStreamFactory f = TrConfigurator.getFileOutputStreamFactory();
                OutputStream os = f.createOutputStream(ddlOutputResource.asFile(), false);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                participant.generate(bw);

                // The JPA code may close the stream for us.  Just make sure it's
                // closed so that we flush any data out.
                bw.close();
                fileCount++;
            } catch (Throwable t) {
                // We'll get an FFDC here... indicate that we had trouble. 
                success = false;
            }
        }

        returnMap.put(SUCCESS, Boolean.valueOf(success));
        returnMap.put(FILE_COUNT, Integer.valueOf(fileCount));
        return returnMap;
    }

    /** {@inheritDoc} */
    @Override
    protected final String getDescription(MBeanInfo info) {
        return "Generates DDL for components using the PersistenceService.";
    }

    /** {@inheritDoc} */
    @Override
    protected final String getDescription(MBeanOperationInfo info) {
        String description = "Unknown operation";

        if (info != null) {
            String operationName = info.getName();
            if (operationName != null) {
                if (operationName.equals("generateDDL")) {
                    description = "Generates DDL for components using the PersistenceService.";
                }
            }
        }

        return description;
    }

    /** {@inheritDoc} */
    @Override
    protected final String getParameterName(MBeanOperationInfo opInfo, MBeanParameterInfo parmInfo, int sequence) {
        String name = "Unknown";

        if ((opInfo != null) && (parmInfo != null) && (sequence >= 0)) {
            String operation = opInfo.getName();
            if (operation != null) {
                if (operation.equals("generateDDL")) {
                    /* No parameters (yet). */
                }
            }
        }

        return name;
    }

    /** {@inheritDoc} */
    @Override
    protected final String getDescription(MBeanOperationInfo opInfo, MBeanParameterInfo parmInfo, int sequence) {
        String description = "Unknown";

        if ((opInfo != null) && (parmInfo != null) && (sequence >= 0)) {
            String parmName = getParameterName(opInfo, parmInfo, sequence);
            if (parmName != null) {
                /* No parameters (yet). */
            }
        }

        return description;
    }

}
