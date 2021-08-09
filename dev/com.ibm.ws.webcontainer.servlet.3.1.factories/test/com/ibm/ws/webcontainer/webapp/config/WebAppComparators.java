/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.webapp.config;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.javaee.dd.common.DataSource;
import com.ibm.ws.javaee.dd.common.EJBRef;
import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.javaee.dd.common.InjectionTarget;
import com.ibm.ws.javaee.dd.common.MessageDestinationRef;
import com.ibm.ws.javaee.dd.common.PersistenceContextRef;
import com.ibm.ws.javaee.dd.common.PersistenceUnitRef;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.ws.javaee.dd.common.ResourceBaseGroup;
import com.ibm.ws.javaee.dd.common.ResourceEnvRef;
import com.ibm.ws.javaee.dd.common.ResourceGroup;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.javaee.dd.common.wsclient.ServiceRef;
import com.ibm.ws.webcontainer.webapp.WebAppConfiguration;

/**
 *
 */
public class WebAppComparators {

    private static String addError(String type, String expected, String found) {

        return "For [ " + type + " ] Expected: [ " + expected + " ] Found: [ " + found + " ]";

    }

    public static List<String> compareWebAppConfiguration(WebAppConfiguration expected, WebAppConfiguration found) {
        List<String> errors = new ArrayList<String>();

        errors.addAll(WebAppComparators.compareDataSources(expected.getDataSources(), found.getDataSources()));
        errors.addAll(WebAppComparators.compareRefs("ServiceRefs", expected.getServiceRefs(), found.getServiceRefs()));
        errors.addAll(WebAppComparators.compareRefs("ResourceRefs", expected.getResourceRefs(), found.getResourceRefs()));
        errors.addAll(WebAppComparators.compareRefs("ResourceEnvRef", expected.getResourceEnvRefs(), found.getResourceEnvRefs()));
        errors.addAll(WebAppComparators.compareRefs("EnvEntry", expected.getEnvEntries(), found.getEnvEntries()));
        errors.addAll(WebAppComparators.compareRefs("EjbRef", expected.getEJBRefs(), found.getEJBRefs()));
        errors.addAll(WebAppComparators.compareRefs("MessageDestinationRef", expected.getMessageDestinationRefs(), found.getMessageDestinationRefs()));

        errors.addAll(WebAppComparators.comparePersistenceRefs("PersistanceContextRef", expected.getPersistenceContextRefs(), found.getPersistenceContextRefs()));
        errors.addAll(WebAppComparators.comparePersistenceRefs("PersistenceUnitRef", expected.getPersistenceUnitRefs(), found.getPersistenceUnitRefs()));

        return errors;
    }

    public static List<String> compareDataSource(DataSource expected, DataSource found) {

        List<String> errors = new ArrayList<String>();

        String temp = new String();
        String type = "DataSource";

        temp = WebAppComparators.compareString(type, expected.getDescription(), found.getDescription());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getName(), found.getName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getClassNameValue(), found.getClassNameValue());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getServerName(), found.getServerName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareInt(type, expected.getPortNumber(), found.getPortNumber());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getDatabaseName(), found.getDatabaseName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getUser(), found.getUser());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getPassword(), found.getPassword());
        if (temp != null)
            errors.add(temp);

        errors.addAll(WebAppComparators.compareProperties(expected.getProperties(), found.getProperties()));

        temp = WebAppComparators.compareInt(type, expected.getLoginTimeout(), found.getLoginTimeout());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareBoolean(type, expected.isTransactional(), found.isTransactional());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareInt(type, expected.getIsolationLevelValue(), found.getIsolationLevelValue());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareInt(type, expected.getInitialPoolSize(), found.getInitialPoolSize());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareInt(type, expected.getMaxPoolSize(), found.getMaxPoolSize());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareInt(type, expected.getMinPoolSize(), found.getMinPoolSize());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareInt(type, expected.getMaxIdleTime(), found.getMaxIdleTime());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareInt(type, expected.getMaxStatements(), found.getMaxStatements());
        if (temp != null)
            errors.add(temp);

        return errors;
    }

    private static String compareTrimmedString(String type, String expected, String found) {
        if (expected != null)
            expected = expected.trim();
        if (found != null)
            found = found.trim();
        return compareString(type, expected, found);
    }

    private static String compareString(String type, String expected, String found) {
        if ((expected == null) && (found == null))
            return null;
        else if (expected == null)
            return WebAppComparators.addError(type, "NULL", found);
        else if (found == null)
            return WebAppComparators.addError(type, expected, "NULL");
        else if (!expected.equals(found))
            return WebAppComparators.addError(type, expected, found);
        else if (expected.equals(found))
            return null;
        return WebAppComparators.addError(type, expected, "NON DETERMINATE");
    }

    private static String compareInt(String type, int expected, int found) {
        if (expected == found)
            return null;
        else
            return WebAppComparators.addError(type, String.valueOf(expected), String.valueOf(found));
    }

    private static String compareBoolean(String type, boolean expected, boolean found) {
        if (expected == found)
            return null;
        else
            return WebAppComparators.addError(type, String.valueOf(expected), String.valueOf(found));
    }

    public static List<String> compareDataSources(List<DataSource> sources1, List<DataSource> sources2) {
        List<String> errors = new ArrayList<String>();
        boolean found = false;
        for (DataSource source1 : sources1) {
            found = false;
            String name = source1.getName();
            List<String> localErrors = new ArrayList<String>();
            for (DataSource source2 : sources2) {
                if (name.equals(source2.getName())) {
                    found = true;
                    localErrors.addAll(compareDataSource(source1, source2));
                    break;
                }
            }
            if (!found)
                localErrors.add(WebAppComparators.addError("DataSource", name, "NOTHING"));
            errors.addAll(localErrors);
        }
        for (DataSource source2 : sources2) {
            found = false;
            String name = source2.getName();
            for (DataSource source1 : sources1) {
                if (name.equals(source1.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found)
                errors.add(WebAppComparators.addError("DataSource", "NOTHING", name));
        }
        return errors;
    }

    public static List<String> compareProperties(List<Property> sources1, List<Property> sources2) {
        List<String> errors = new ArrayList<String>();
        String type = "Property";
        boolean found = false;
        for (Property source1 : sources1) {
            found = false;
            String name = source1.getName();
            List<String> localErrors = new ArrayList<String>();
            for (Property source2 : sources2) {
                if (name.equals(source2.getName())) {
                    found = true;
                    localErrors.addAll(compareProperty(type, source1, source2));
                    break;
                }
            }
            if (!found)
                localErrors.add(WebAppComparators.addError(type, name, "NOTHING"));
            errors.addAll(localErrors);
        }
        for (Property source2 : sources2) {
            found = false;
            String name = source2.getName();
            for (Property source1 : sources1) {
                if (name.equals(source1.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found)
                errors.add(WebAppComparators.addError(type, "NOTHING", name));
        }

        return errors;
    }

    private static List<String> compareProperty(String type, Property expected, Property found) {
        List<String> errors = new ArrayList<String>();
        String temp = new String();
        temp = WebAppComparators.compareString(type, expected.getName(), found.getName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getValue(), found.getValue());
        if (temp != null)
            errors.add(temp);
        return errors;
    }

    public static List<String> compareRefs(String type, List<? extends ResourceGroup> sources1, List<? extends ResourceGroup> sources2) {
        List<String> errors = new ArrayList<String>();
        boolean found = false;
        for (ResourceGroup source1 : sources1) {
            found = false;
            String name = source1.getName();
            List<String> localErrors = new ArrayList<String>();
            for (ResourceGroup source2 : sources2) {
                if (name.equals(source2.getName())) {
                    found = true;
                    if (source1 instanceof ServiceRef)
                        localErrors.addAll(compareServiceRef((ServiceRef) source1, (ServiceRef) source2));
                    else if (source1 instanceof ResourceRef)
                        localErrors.addAll(compareResourceRef((ResourceRef) source1, (ResourceRef) source2));
                    else if (source1 instanceof ResourceEnvRef)
                        localErrors.addAll(compareResourceEnvRef((ResourceEnvRef) source1, (ResourceEnvRef) source2));
                    else if (source1 instanceof EnvEntry)
                        localErrors.addAll(compareEnvEntry((EnvEntry) source1, (EnvEntry) source2));
                    else if (source1 instanceof EJBRef)
                        localErrors.addAll(compareEjbRef((EJBRef) source1, (EJBRef) source2));
                    else if (source1 instanceof MessageDestinationRef)
                        localErrors.addAll(compareMessageDestinationRef((MessageDestinationRef) source1, (MessageDestinationRef) source2));
                    else
                        localErrors.add("UNHANDLED RESOURCE GROUP TYPE.");
                    break;
                }
            }

            if (!found)
                localErrors.add(WebAppComparators.addError(type, name, "NOTHING"));
            errors.addAll(localErrors);
        }
        for (ResourceGroup source2 : sources2) {
            found = false;
            String name = source2.getName();
            for (ResourceGroup source1 : sources1) {
                if (name.equals(source1.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found)
                errors.add(WebAppComparators.addError(type, "NOTHING", name));
        }
        return errors;

    }

    public static List<String> comparePersistenceRefs(String type, List<? extends ResourceBaseGroup> sources1, List<? extends ResourceBaseGroup> sources2) {
        List<String> errors = new ArrayList<String>();
        boolean found = false;
        for (ResourceBaseGroup source1 : sources1) {
            found = false;
            String name = source1.getName();
            List<String> localErrors = new ArrayList<String>();
            for (ResourceBaseGroup source2 : sources2) {
                if (name.equals(source2.getName())) {
                    found = true;
                    if (source1 instanceof PersistenceContextRef)
                        localErrors.addAll(comparePersistenceContextRef((PersistenceContextRef) source1, (PersistenceContextRef) source2));
                    else if (source1 instanceof PersistenceUnitRef)
                        localErrors.addAll(comparePersistenceUnitRef((PersistenceUnitRef) source1, (PersistenceUnitRef) source2));
                    else
                        localErrors.add("UNHANDLED RESOURCE GROUP TYPE.");
                    break;
                }
            }
            if (!found)
                localErrors.add(WebAppComparators.addError(type, name, "NOTHING"));
            errors.addAll(localErrors);
        }
        for (ResourceBaseGroup source2 : sources2) {
            found = false;
            String name = source2.getName();
            for (ResourceBaseGroup source1 : sources1) {
                if (name.equals(source1.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found)
                errors.add(WebAppComparators.addError(type, "NOTHING", name));
        }
        return errors;

    }

    public static List<String> compareServiceRef(ServiceRef expected, ServiceRef found) {

        List<String> errors = new ArrayList<String>();

        String temp = new String();
        String type = "ServiceRef";

        temp = WebAppComparators.compareString(type, expected.getName(), found.getName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getServiceInterfaceName(), found.getServiceInterfaceName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getServiceRefTypeName(), found.getServiceRefTypeName());
        if (temp != null)
            errors.add(temp);

        errors.addAll(WebAppComparators.compareInjectionTargets(expected.getInjectionTargets(), found.getInjectionTargets()));

        return errors;
    }

    public static List<String> compareResourceRef(ResourceRef expected, ResourceRef found) {

        List<String> errors = new ArrayList<String>();

        String temp = new String();
        String type = "ResourceRef";

        temp = WebAppComparators.compareString(type, expected.getName(), found.getName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getType(), found.getType());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareInt(type, expected.getAuthValue(), found.getAuthValue());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareInt(type, expected.getSharingScopeValue(), found.getSharingScopeValue());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareTrimmedString(type, expected.getLookupName(), found.getLookupName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getMappedName(), found.getMappedName());
        if (temp != null)
            errors.add(temp);

        errors.addAll(WebAppComparators.compareInjectionTargets(expected.getInjectionTargets(), found.getInjectionTargets()));

        return errors;
    }

    public static List<String> compareResourceEnvRef(ResourceEnvRef expected, ResourceEnvRef found) {

        List<String> errors = new ArrayList<String>();

        String temp = new String();
        String type = "ResourceEnvRef";

        temp = WebAppComparators.compareString(type, expected.getName(), found.getName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareTrimmedString(type, expected.getLookupName(), found.getLookupName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getMappedName(), found.getMappedName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getTypeName(), found.getTypeName());
        if (temp != null)
            errors.add(temp);

        errors.addAll(WebAppComparators.compareInjectionTargets(expected.getInjectionTargets(), found.getInjectionTargets()));

        return errors;
    }

    public static List<String> compareEnvEntry(EnvEntry expected, EnvEntry found) {

        List<String> errors = new ArrayList<String>();

        String temp = new String();
        String type = "EnvEntry";

        temp = WebAppComparators.compareString(type, expected.getName(), found.getName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareTrimmedString(type, expected.getLookupName(), found.getLookupName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getMappedName(), found.getMappedName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getTypeName(), found.getTypeName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getValue(), found.getValue());
        if (temp != null)
            errors.add(temp);

        errors.addAll(WebAppComparators.compareInjectionTargets(expected.getInjectionTargets(), found.getInjectionTargets()));

        return errors;
    }

    public static List<String> compareEjbRef(EJBRef expected, EJBRef found) {

        List<String> errors = new ArrayList<String>();

        String temp = new String();
        String type = "EJBRef";

        temp = WebAppComparators.compareString(type, expected.getName(), found.getName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareTrimmedString(type, expected.getLookupName(), found.getLookupName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getMappedName(), found.getMappedName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getHome(), found.getHome());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getInterface(), found.getInterface());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getLink(), found.getLink());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareInt(type, expected.getKindValue(), found.getKindValue());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareInt(type, expected.getTypeValue(), found.getTypeValue());
        if (temp != null)
            errors.add(temp);

        errors.addAll(WebAppComparators.compareInjectionTargets(expected.getInjectionTargets(), found.getInjectionTargets()));

        return errors;
    }

    public static List<String> compareMessageDestinationRef(MessageDestinationRef expected, MessageDestinationRef found) {

        List<String> errors = new ArrayList<String>();

        String temp = new String();
        String type = "MessageDestinationRef";

        temp = WebAppComparators.compareString(type, expected.getName(), found.getName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareTrimmedString(type, expected.getLookupName(), found.getLookupName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getMappedName(), found.getMappedName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getLink(), found.getLink());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getType(), found.getType());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareInt(type, expected.getUsageValue(), found.getUsageValue());
        if (temp != null)
            errors.add(temp);

        errors.addAll(WebAppComparators.compareInjectionTargets(expected.getInjectionTargets(), found.getInjectionTargets()));

        return errors;
    }

    public static List<String> comparePersistenceContextRef(PersistenceContextRef expected, PersistenceContextRef found) {

        List<String> errors = new ArrayList<String>();

        String temp = new String();
        String type = "PersistenceContextRef";

        temp = WebAppComparators.compareString(type, expected.getName(), found.getName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getMappedName(), found.getMappedName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getPersistenceUnitName(), found.getPersistenceUnitName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareInt(type, expected.getSynchronizationValue(), found.getSynchronizationValue());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareInt(type, expected.getTypeValue(), found.getTypeValue());
        if (temp != null)
            errors.add(temp);

        errors.addAll(WebAppComparators.compareProperties(expected.getProperties(), found.getProperties()));
        errors.addAll(WebAppComparators.compareInjectionTargets(expected.getInjectionTargets(), found.getInjectionTargets()));

        return errors;
    }

    public static List<String> comparePersistenceUnitRef(PersistenceUnitRef expected, PersistenceUnitRef found) {

        List<String> errors = new ArrayList<String>();

        String temp = new String();
        String type = "PersistenceUnitRef";

        temp = WebAppComparators.compareString(type, expected.getName(), found.getName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getMappedName(), found.getMappedName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getPersistenceUnitName(), found.getPersistenceUnitName());
        if (temp != null)
            errors.add(temp);

        errors.addAll(WebAppComparators.compareInjectionTargets(expected.getInjectionTargets(), found.getInjectionTargets()));

        return errors;
    }

    public static List<String> compareInjectionTargets(List<InjectionTarget> sources1, List<InjectionTarget> sources2) {
        List<String> errors = new ArrayList<String>();
        String type = "InjectionTarget";
        boolean found = false;
        for (InjectionTarget source1 : sources1) {
            found = false;
            String name = source1.getInjectionTargetName();
            List<String> localErrors = new ArrayList<String>();
            for (InjectionTarget source2 : sources2) {
                if (name.equals(source2.getInjectionTargetName())) {
                    found = true;
                    localErrors.addAll(compareInjectionTarget(type, source1, source2));
                    break;
                }
            }
            if (!found)
                localErrors.add(WebAppComparators.addError(type, name, "NOTHING"));
            errors.addAll(localErrors);
        }
        for (InjectionTarget source2 : sources2) {
            found = false;
            String name = source2.getInjectionTargetName();
            for (InjectionTarget source1 : sources1) {
                if (name.equals(source1.getInjectionTargetName())) {
                    found = true;
                    break;
                }
            }
            if (!found)
                errors.add(WebAppComparators.addError(type, "NOTHING", name));
        }

        return errors;
    }

    private static List<String> compareInjectionTarget(String type, InjectionTarget expected, InjectionTarget found) {
        List<String> errors = new ArrayList<String>();
        String temp = new String();
        temp = WebAppComparators.compareString(type, expected.getInjectionTargetName(), found.getInjectionTargetName());
        if (temp != null)
            errors.add(temp);
        temp = WebAppComparators.compareString(type, expected.getInjectionTargetClassName(), found.getInjectionTargetClassName());
        if (temp != null)
            errors.add(temp);
        return errors;
    }
}
