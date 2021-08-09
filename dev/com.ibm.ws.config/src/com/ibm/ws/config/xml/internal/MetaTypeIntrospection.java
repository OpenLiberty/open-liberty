/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.config.xml.internal.MetaTypeRegistry.EntryAction;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.PidReference;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.RegistryEntry;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;
import com.ibm.ws.config.xml.internal.metatype.ExtendedObjectClassDefinition;
import com.ibm.wsspi.logging.Introspector;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           property = {
                        Constants.SERVICE_VENDOR + "=" + "IBM"
           })
public class MetaTypeIntrospection implements Introspector {

    private final static String NAME = "MetaTypeIntrospection";
    private final static String DESC = "Introspect currently defined metatypes.";
    @Reference
    private MetaTypeRegistry metaTypeRegistry;

    @Override
    public String getIntrospectorName() {
        return NAME;
    }

    @Override
    public String getIntrospectorDescription() {
        return DESC;
    }

    @Override
    public void introspect(final PrintWriter ps) throws Exception {

        // Get Registry Entries
        Collection<RegistryEntry> collection = metaTypeRegistry.getAllRegistryEntries();

        RegistryEntry[] entries = new RegistryEntry[collection.size()];
        entries = collection.toArray(entries);

        // Sort registry entries by bundle
        Arrays.sort(entries, new Comparator<RegistryEntry>() {
            @Override
            public int compare(RegistryEntry arg0, RegistryEntry arg1) {
                int retVal = Long.valueOf(arg0.getBundleId()).compareTo(arg1.getBundleId());
                if (retVal == 0)
                    return arg0.getPid().compareTo(arg1.getPid());

                return retVal;
            }
        });

        long currentBundleId = -1;
        String currentPid = null;
        for (final RegistryEntry re : entries) {
            // Get rid of duplicates -- We store by alias and full PID, but we only need one of them.
            if (re.getPid().equals(currentPid))
                continue;
            else
                currentPid = re.getPid();

            ExtendedObjectClassDefinition ocd = re.getObjectClassDefinition();

            if (re.getBundleId() != currentBundleId) {
                currentBundleId = re.getBundleId();
                ps.println("Bundle: " + re.getBundleName());
            }

            ps.println("\tID: " + ocd.getID());
            ps.println("\tName: " + ocd.getName());
            if (ocd.getDescription() != null)
                ps.println("\tDescription: " + ocd.getDescription());
            ps.println("\tPID: " + re.getPid());
            ps.println("\tIs Factory: " + re.isFactory());

            if (re.getAlias() != null)
                ps.println("\tAlias: " + re.getAlias());

            if (re.getExtends() != null)
                ps.println("\tExtends: " + re.getExtends());

            if (re.getExtendsAlias() != null)
                ps.println("\tExtendsAlias: " + re.getExtendsAlias());

            if (ocd.getObjectClass() != null)
                ps.println("\tObjectClass: " + ocd.getObjectClass());

            if (ocd.getParentPID() != null)
                ps.println("\tParent PID: " + ocd.getParentPID());

            if (re.getChildAlias() != null)
                ps.println("\tChild Alias: " + re.getChildAlias());

            if (re.getDefaultId() != null)
                ps.println("\tDefault ID: " + re.getDefaultId());

            if (ocd.getExcludedChildren() != null)
                ps.println("\tExcluded Children: " + ocd.getExcludedChildren());

            if (ocd.getLocalization() != null)
                ps.println("\tLocalization: " + ocd.getLocalization());

            if (ocd.getXsdAny() != 0)
                ps.println("\tXSD Any Elements Allowed: " + ocd.getXsdAny());

            if (!re.getReferencingEntries().isEmpty()) {
                ps.println("\tReferenced by:");
                for (PidReference ref : re.getReferencingEntries()) {
                    ps.println("\t\tparent: " + ref.getReferencingEntry().getPid() + ", accessed by: " + ref.getAccessor()
                               + (ref.isParentFirst() ? ", parent-first" : ", child-first"));
                }
            }

            printAttributes(ps, ocd);

            re.traverseHierarchyPreOrder(new EntryAction<Void>() {

                private int depth;
                private String parentPid = re.getPid();

                @Override
                public boolean entry(RegistryEntry registryEntry) {
                    if (depth == 0) {
                        ps.println("\tHierarchy:");
                        depth++;
                    }
                    if (parentPid.equals(registryEntry.getExtends())) {
                        //depth unchanged
                    } else if (parentPid.equals(registryEntry.getPid())) {
                        //depth decreased
                        depth--;
                        parentPid = registryEntry.getExtends();
                    } else {
                        //depth  increased
                        depth++;
                        parentPid = registryEntry.getPid();
                    }
                    for (int i = -1; i < depth; i++) {
                        ps.print("\t");
                    }
                    ps.println(registryEntry.getPid());
                    return true;
                }

                @Override
                public Void getResult() {
                    return null;
                }
            });

            ps.println("");
        }

    }

    /**
     * @param ps
     * @param ocd
     */
    private void printAttributes(PrintWriter ps, ExtendedObjectClassDefinition ocd) {
        ps.println("");
        ps.println("\tAttributes: ");
        for (ExtendedAttributeDefinition ad : ocd.getAttributeMap().values()) {
            ps.println("");
            ps.println("\t\tId: " + ad.getID());
            ps.println("\t\tName: " + ad.getName());
            ps.println("\t\tCardinality: " + ad.getCardinality());
            ps.println("\t\tDescription: " + ad.getDescription());
            ps.println("\t\tType: " + ad.getType());

            if (ad.getDefaultValue() != null && ad.getDefaultValue().length > 0) {
                ps.print("\t\tDefault Value: [");
                for (String value : ad.getDefaultValue()) {
                    ps.print(value + " ");
                }
                ps.println("]");
            }

            if (ad.getGroup() != null)
                ps.println("\t\tUI Group: " + ad.getGroup());

            if (ad.getReferencePid() != null)
                ps.println("\t\tReference: " + ad.getReferencePid());
            if (ad.getService() != null)
                ps.println("\t\tService: " + ad.getService());

            if (ad.getRename() != null)
                ps.println("\t\tRename: " + ad.getRename());

            if (ad.isUnique())
                ps.println("\t\tValue must be unique across: " + ad.getUniqueCategory());

            if (ad.getVariable() != null)
                ps.println("\t\tValue derived from variable: " + ad.getVariable());

            if (ad.isFinal())
                ps.println("\t\tFinal = True, Value can not be specified from user input.");

            if (ad.isFlat())
                ps.println("\t\tFlat: True");

            if (ad.getCopyOf() != null)
                ps.println("\t\tCopy of Attribute: " + ad.getCopyOf());

            if (ad.resolveVariables())
                ps.println("\t\tVariable Substitution: " + (ad.resolveVariables() ? "immediate" : "deferred"));
        }

    }

}
