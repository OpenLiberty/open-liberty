/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.management;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.common.PersistenceUnitRef;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;

public class JPAPUnitInjectionBinding extends AbstractJPAInjectionBinding<PersistenceUnit>
{
    private static final TraceComponent tc = Tr.register(JPAPUnitInjectionBinding.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME); //d447863

    private boolean ivPuFromXML;

    /**
     * XML based constructor.
     * 
     * @param pUnitRef XML representation of persistence unit metadata.
     * @param nameSpaceConfig component name space configuration metadata.
     */
    // d662814
    public JPAPUnitInjectionBinding(PersistenceUnitRef pUnitRef,
                                    ComponentNameSpaceConfiguration nameSpaceConfig)
        throws InjectionException
    {
        super(newPersistenceUnit(pUnitRef.getName(), pUnitRef.getPersistenceUnitName()),
              pUnitRef.getName(),
              pUnitRef.getPersistenceUnitName(),
              nameSpaceConfig);
        String pUnitName = pUnitRef.getPersistenceUnitName();
        setInjectionClassType(EntityManagerFactory.class);

        if (pUnitName != null && pUnitName.length() > 0)
        {
            ivPuFromXML = true;
        }
    }

    /**
     * Annotation based constructor.
     * 
     * @param pUnit persistence unit annotation.
     * @param nameSpaceConfig component name space configuration metadata.
     */
    public JPAPUnitInjectionBinding(PersistenceUnit pUnit,
                                    ComponentNameSpaceConfiguration nameSpaceConfig)
        throws InjectionException
    {
        super(pUnit, pUnit.name(), pUnit.unitName(), nameSpaceConfig);
        this.setInjectionClassType(EntityManagerFactory.class);
    }

    /**
     * Extract the fields from the PersistenceUnitRef, and verify they match the
     * values in the current binding object and/or annotation exactly.
     * 
     * @param pUnitRef reference with same name to merge
     * @throws InjectionException if the fields of the two references are not
     *             compatible.
     */
    // d658856
    public void merge(PersistenceUnitRef pUnitRef) throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "merge : " + pUnitRef);

        PersistenceUnit thisPu = getAnnotation();
        String thisUnitName = thisPu.unitName();
        String mergedUnitName = pUnitRef.getPersistenceUnitName();

        if (isTraceOn && tc.isDebugEnabled())
        {
            Tr.debug(tc, "new=" + getJndiName() + ":" + mergedUnitName);
            Tr.debug(tc, "cur=" + getJndiName() + ":" + thisUnitName);
        }

        // Merge the persistence unit name. Either one must be not set or they
        // must match exactly.
        if (thisUnitName == null || thisUnitName.equals(""))
        {
            if (mergedUnitName != null)
            {
                setAnnotation(newPersistenceUnit(getJndiName(),
                                                 mergedUnitName));
                ivPuId.setPuName(mergedUnitName);
                ivPuFromXML = true; // d662814
            }
        }
        else if (mergedUnitName != null && !mergedUnitName.equals(""))
        {
            if (!thisUnitName.equals(mergedUnitName))
            {
                Tr.error(tc, "CONFLICTING_XML_VALUES_CWWJP0041E",
                         ivJ2eeName.getModule(),
                         ivJ2eeName.getApplication(),
                         "persistence-unit-name",
                         "persistence-unit-ref",
                         "persistence-unit-ref-name",
                         getJndiName(),
                         thisUnitName,
                         mergedUnitName);
                String exMsg = "CWWJP0041E: The " + ivJ2eeName.getModule() +
                               " module of the " + ivJ2eeName.getApplication() +
                               " application has conflicting configuration data in the XML" +
                               " deployment descriptor. Conflicting " + "persistence-unit-name" +
                               " element values exist for multiple " + "persistence-unit-ref" +
                               " elements with the same " + "persistence-unit-ref-name" +
                               " element value : " + getJndiName() + ". The conflicting " +
                               "persistence-unit-name" + " element values are " + thisUnitName +
                               " and " + mergedUnitName + ".";
                throw new InjectionConfigurationException(exMsg);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "merge : " + this);
    }

    @Override
    public void merge(PersistenceUnit annotation, Class<?> instanceClass, Member member) throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "merge : " + annotation + ", " + instanceClass + ", " + this);

        // EJB Core 16.10.2.1 Overriding Rules
        // The following rules apply to how a deployment descriptor entry may override a
        //  PersistenceUnit annotation:
        // - The relevant deployment descriptor entry is located based on the JNDI name
        //      used with the annotation (either defaulted or provided explicitly).
        // - The persistence-unit-name overrides the unitName element of the annotation. The
        //      Application Assembler or Deployer should exercise caution in changing this value,
        //      if specified, as doing so is likely to break the application.
        // - The injection target, if specified, must name exactly the annotated field or
        //      property method.

        // When this method is called, this cookie (typically from xml) and the input merged
        //  cookie (from annotation) has meet the following conditions:
        //  1) both has the same jndi name,
        //  2) both has the same annotation type.

        // Only need to check values if not overridden by XML.             d662814
        if (!this.ivPuFromXML)
        {
            PersistenceUnit thisPu = getAnnotation();

            //d447863 start
            String thisUnitName = thisPu.unitName();
            String mergedUnitName = annotation.unitName();

            if (mergedUnitName != null && mergedUnitName.length() > 0)
            {
                if (!isComplete() && (thisUnitName == null || thisUnitName.length() == 0))
                {
                    setAnnotation(newPersistenceUnit(thisPu.name(),
                                                     mergedUnitName));
                }
                else if (!mergedUnitName.equals(thisUnitName))
                {
                    // Error - conflicting persistence unit specified         d662814
                    Tr.error(tc, "CONFLICTING_ANNOTATION_VALUES_CWWJP0042E",
                             ivNameSpaceConfig.getDisplayName(),
                             ivNameSpaceConfig.getModuleName(),
                             ivNameSpaceConfig.getApplicationName(),
                             "unitName",
                             "@PersistenceUnit",
                             "name",
                             getJndiName(),
                             thisUnitName,
                             mergedUnitName);
                    String exMsg = "The " + ivNameSpaceConfig.getDisplayName() +
                                   " bean in the " + ivNameSpaceConfig.getModuleName() +
                                   " module of the " + ivNameSpaceConfig.getApplicationName() +
                                   " application has conflicting configuration data" +
                                   " in source code annotations. Conflicting " +
                                   "unitName" + " attribute values exist for multiple " +
                                   "@PersistenceUnit" + " annotations with the same " +
                                   "name" + " attribute value : " + getJndiName() +
                                   ". The conflicting " + "unitName" +
                                   " attribute values are " + thisUnitName +
                                   " and " + mergedUnitName + ".";
                    throw new InjectionConfigurationException(exMsg);
                }
            }
            //d447863 end
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "merge : " + this);
    }

    @Override
    public void mergeSaved(InjectionBinding<PersistenceUnit> injectionBinding) // d681743
    throws InjectionException
    {
        JPAPUnitInjectionBinding pUnitBinding = (JPAPUnitInjectionBinding) injectionBinding;
        PersistenceUnit pUnitBindingAnn = pUnitBinding.getAnnotation();
        PersistenceUnit ann = getAnnotation();

        mergeSavedValue(ann.unitName(), pUnitBindingAnn.unitName(), "persistence-unit-name");
    }

    /**
     * This transient PersistencUnit annotation class has no default value.
     * i.e. null is a valid value for some fields.
     */
    private static PersistenceUnit newPersistenceUnit(final String fJndiName, final String fUnitName)
    {
        return new PersistenceUnit()
        {
            @Override
            public String name()
            {
                return fJndiName;
            }

            @Override
            public Class<? extends Annotation> annotationType()
            {
                return javax.persistence.PersistenceUnit.class;
            }

            @Override
            public String unitName()
            {
                return fUnitName;
            }

            @Override
            public String toString()
            {
                return "JPA.PersistenceUnit(name=" + fJndiName +
                       ", unitName=" + fUnitName + ")";
            }
        };
    }
}
