/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec40.cdi.extensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.ws.security.javaeesec.cdi.beans.BasicHttpAuthenticationMechanism;
import com.ibm.ws.security.javaeesec.cdi.beans.CustomFormAuthenticationMechanism;
import com.ibm.ws.security.javaeesec.cdi.beans.FormAuthenticationMechanism;
import com.ibm.ws.security.javaeesec.cdi.extensions.HttpAuthenticationMechanismsTracker;
import com.ibm.ws.security.javaeesec.cdi.extensions.PrimarySecurityCDIExtension;
import com.ibm.ws.security.javaeesec.properties.ModuleProperties;

import io.openliberty.security.jakartasec.JakartaSec30Constants;
import io.openliberty.security.jakartasec.OpenIdAuthenticationMechanismDefinitionHolder;
import io.openliberty.security.jakartasec.cdi.beans.OidcHttpAuthenticationMechanism;
import io.openliberty.security.jakartasec.handlers.HAMModuleRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanismHandler;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;

/*-
 * CDI Extension to register beans required for Jakarta Security 4.0.
 *
 * Handles Multiple HAMs with qualifiers, the registration of a custom HAM handler
 * (if present), and HAMs with qualifiers. Those without qualifiers will be
 * handled by the standard HAM bean handling process (JavaEESecCDIExtension).
 *
 * It's important to note the extension method ordering (although not all
 * apply in this class):
 *
 * 1. @Observes @WithAnnotations ProcessAnnotatedType
 * └─ processAnnotatedHAMs()
 *     └─ processHAMList()
 *        └─ processHAMDefinition()
 *
 * 2. @Observes ProcessBean
 * └─ processBean()
 *     └─ set boolean regarding custom ham handler
 *     └─ (veto other HAMs - not applicable here!)
 *
 * 3. @Observes AfterBeanDiscovery
 * └─ afterBeanDiscovery()
 *     └─ create all qualified HAM beans
 *     └─ create internal (in-built) custom ham handler if required
 *
 * So by the time afterBeanDiscovery() is called, all your bean data should be in place.
 */

@Component(service = {},
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM", "api.classes=jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanismHandler" })
public class JakartaSecurity40CDIExtension implements Extension {

    private static final TraceComponent tc = Tr.register(JakartaSecurity40CDIExtension.class);
    private boolean httpAuthenticationMechanismHandlerRegistered = false;
    private final String applicationName;
    private final Set<Bean<?>> beansToAdd = new HashSet<Bean<?>>();

    private static PrimarySecurityCDIExtension primarySecurityCDIExtension;

    // temporary storage for qualified HAMs during CDI extension phase only
    private final List<QualifiedHAMData> qualifiedHAMs = new ArrayList<>();

    private static class QualifiedHAMData {
        private final Class<?> implClass;
        private final List<Class<?>> qualifiers;
        private final Properties props;

        QualifiedHAMData(Class<?> implClass, List<Class<?>> qualifiers, Properties props) {
            this.implClass = implClass;
            this.qualifiers = qualifiers;
            this.props = props;
        }
    }

    public JakartaSecurity40CDIExtension() {
        applicationName = HttpAuthenticationMechanismsTracker.getApplicationName();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "JakartaSecurity40CDIExtension", "Using application name [" + applicationName + "].");
        }
    }

    @SuppressWarnings("static-access")
    @Reference
    protected void setPrimarySecurityCDIExtension(PrimarySecurityCDIExtension primarySecurityCDIExtension) {
        // we need this to add to the http tracker auth mech
        this.primarySecurityCDIExtension = primarySecurityCDIExtension;
    }

    /**
     * This method processes annotations in the application which specify HAMs.
     *
     * We have to watch for the single HAM definition class, and also lists as single HAMs
     * can appear in nested lists as they are repeatable (but only if there is more than one
     * will they appear in a list).
     *
     * NOTE: ApplicationScoped annotation is included to catch List annotations which can't be
     * explicitly listed as they are nested interfaces only in Jakarta Security 4.0+.
     */
    public <T> void processAnnotatedHAMs(@WithAnnotations({ BasicAuthenticationMechanismDefinition.class,
                                                            FormAuthenticationMechanismDefinition.class,
                                                            CustomFormAuthenticationMechanismDefinition.class,
                                                            OpenIdAuthenticationMechanismDefinition.class,
                                                            ApplicationScoped.class }) @Observes ProcessAnnotatedType<T> event,
                                         BeanManager beanManager) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "instance: " + Integer.toHexString(this.hashCode()) + " BeanManager: " + Integer.toHexString(beanManager.hashCode()));
        }

        AnnotatedType<T> annotatedType = event.getAnnotatedType();
        Class<?> annotatedClass = annotatedType.getJavaClass();
        Set<Annotation> annotations = annotatedType.getAnnotations();

        // look at all annotatoins including class level
        for (Annotation annotation : annotations) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Annotation found: " + annotation);
                Tr.debug(tc, "Annotation class: ", annotation.getClass());
            }

            Class<? extends Annotation> annotationType = annotation.annotationType();
            String annotationTypeName = annotationType.getName();

            // check for HAM lists first, then singles HAMs
            if (annotationTypeName.contains("OpenIdAuthenticationMechanismDefinition$List")) {
                processHAMList(OidcHttpAuthenticationMechanism.class, "OpenId",
                               OpenIdAuthenticationMechanismDefinition.class, annotation, annotatedClass, annotatedType);
            } else if (annotationTypeName.contains("BasicAuthenticationMechanismDefinition$List")) {
                processHAMList(BasicHttpAuthenticationMechanism.class, "Basic",
                               BasicAuthenticationMechanismDefinition.class, annotation, annotatedClass, annotatedType);
            } else if (annotationTypeName.contains("CustomFormAuthenticationMechanismDefinition$List")) {
                processHAMList(CustomFormAuthenticationMechanism.class, "CustomForm",
                               CustomFormAuthenticationMechanismDefinition.class, annotation, annotatedClass, annotatedType);
            } else if (annotationTypeName.contains("FormAuthenticationMechanismDefinition$List")) {
                processHAMList(FormAuthenticationMechanism.class, "Form",
                               FormAuthenticationMechanismDefinition.class, annotation, annotatedClass, annotatedType);
            }
            // now the single HAMs
            else if (OpenIdAuthenticationMechanismDefinition.class.equals(annotationType)) {
                processHAMDefinition(OidcHttpAuthenticationMechanism.class, annotation, annotationType, annotatedClass, annotatedType);
            } else if (BasicAuthenticationMechanismDefinition.class.equals(annotationType)) {
                processHAMDefinition(BasicHttpAuthenticationMechanism.class, annotation, annotationType, annotatedClass, annotatedType);
            } else if (FormAuthenticationMechanismDefinition.class.equals(annotationType)) {
                processHAMDefinition(FormAuthenticationMechanism.class, annotation, annotationType, annotatedClass, annotatedType);
            } else if (CustomFormAuthenticationMechanismDefinition.class.equals(annotationType)) {
                processHAMDefinition(CustomFormAuthenticationMechanism.class, annotation, annotationType, annotatedClass, annotatedType);
            }
        }
    }

    /**
     * Process a List annotation containing multiple HAM definitions.
     */
    private <T> void processHAMList(Class<?> hamImplClass, String hamTypeName,
                                    Class<? extends Annotation> singleAnnotationType,
                                    Annotation listAnnotation, Class<?> annotatedClass,
                                    AnnotatedType<T> annotatedType) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Processing " + hamTypeName + "AuthenticationMechanismDefinition.List");
        }

        try {
            Method valueMethod = listAnnotation.annotationType().getMethod("value");
            Annotation[] hamAnnotations = (Annotation[]) valueMethod.invoke(listAnnotation);

            for (Annotation hamAnnotation : hamAnnotations) {
                processHAMDefinition(hamImplClass, hamAnnotation, singleAnnotationType, annotatedClass, annotatedType);
            }
        } catch (Exception e) {
            Tr.error(tc, "JAKARTASEC_CDI_ERROR_PROCESSING_HAM_LIST", hamTypeName);
            String msg = Tr.formatMessage(tc, "JAKARTASEC_CDI_ERROR_PROCESSING_HAM_LIST", hamTypeName);
            throw new DeploymentException(msg);
        }
    }

    /**
     * Process a single HAM definition annotation, and add it to our internal
     * hamDefinitions tracker if it has qualifiers.
     */
    private <T> void processHAMDefinition(Class<?> hamImplClass, Annotation annotation,
                                          Class<? extends Annotation> annotationType,
                                          Class<?> annotatedClass, AnnotatedType<T> annotatedType) {
        List<Class<?>> qualifiers = getQualifierClassesFromAnnotation(annotation, annotationType);

        // only add to hamDefinitions if it has qualifiers (if no explicit qualifier,
        //   then the Jakarta default one will have been set on the definition automatically)
        if (hasQualifiers(qualifiers)) {
            Properties props = extractHAMProperties(hamImplClass, annotation, annotationType);

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Processing HAM type [" + hamImplClass.getSimpleName()
                             + "] with qualifiers [" + qualifiers.toString()
                             + "] and props [" + props.toString() + "].");
            }

            // Determine module name from the class location
            String moduleName = null;
            try {
                String location = annotatedClass.getProtectionDomain().getCodeSource().getLocation().getFile();
                // Remove trailing slashes (common for expanded WARs/EARs)
                while (location.endsWith("/")) {
                    location = location.substring(0, location.length() - 1);
                }
                
                // For expanded WARs, location might be .../module.war/WEB-INF/classes
                // Extract the WAR name by looking for .war in the path
                int warIndex = location.indexOf(".war");
                if (warIndex > 0) {
                    // Find the start of the WAR name (after the last slash before .war)
                    int startIndex = location.lastIndexOf('/', warIndex);
                    if (startIndex >= 0) {
                        moduleName = location.substring(startIndex + 1, warIndex + 4); // +4 to include ".war"
                    } else {
                        moduleName = location.substring(0, warIndex + 4);
                    }
                } else {
                    // Fallback: extract from the last path component
                    int lastSlash = location.lastIndexOf('/');
                    if (lastSlash >= 0) {
                        moduleName = location.substring(lastSlash + 1);
                    } else {
                        moduleName = location;
                    }
                }
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Could not determine module name for class: " + annotatedClass.getName(), e);
                }
            }
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Registering HAM [" + hamImplClass.getSimpleName() + "] with module: [" + moduleName + "]");
            }
            // Register HAM with its module in the registry
            HAMModuleRegistry.register(applicationName, hamImplClass, moduleName);
            qualifiedHAMs.add(new QualifiedHAMData(hamImplClass, qualifiers, props));

            // add to tracker EARLY (during ProcessAnnotatedType phase) to prevent other extensions
            //   (such as JavaEESecCDIExtension) from vetoing the bean (and only add one of the HAM type)
            if (primarySecurityCDIExtension != null && !primarySecurityCDIExtension.existAuthMech(applicationName, hamImplClass)) {
                Set<Annotation> annotations = annotatedType.getAnnotations();
                primarySecurityCDIExtension.addAuthMech(applicationName, hamImplClass, hamImplClass, annotations, props);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Added qualified HAM to tracker early: " + hamImplClass.getSimpleName());
                }
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Processing HAM type [" + hamImplClass.getSimpleName()
                             + "] with no qualifiers - will be handled by standard CDI");
            }
            // HAMs without qualifiers are handled in other extensions
        }
    }

    /**
     * Extract properties from HAM annotation based on HAM type.
     */
    private Properties extractHAMProperties(Class<?> hamImplClass, Annotation annotation,
                                            Class<? extends Annotation> annotationType) {
        Properties props = new Properties();

        try {
            // OpenID HAM
            if (hamImplClass != null && OidcHttpAuthenticationMechanism.class.equals(hamImplClass)) {
                props.put(JakartaSec30Constants.OIDC_ANNOTATION,
                          new OpenIdAuthenticationMechanismDefinitionHolder((OpenIdAuthenticationMechanismDefinition) annotation));
            }
            // Basic HAM
            else if (BasicAuthenticationMechanismDefinition.class.equals(annotationType)) {
                Method realmNameMethod = annotationType.getMethod("realmName");
                String realmName = (String) realmNameMethod.invoke(annotation);
                props.put(JavaEESecConstants.REALM_NAME, realmName);
            }
            // Form/CustomForm HAM
            else if (FormAuthenticationMechanismDefinition.class.equals(annotationType) ||
                     CustomFormAuthenticationMechanismDefinition.class.equals(annotationType)) {
                Method loginToContinueMethod = annotationType.getMethod("loginToContinue");
                Annotation ltcAnnotation = (Annotation) loginToContinueMethod.invoke(annotation);
                props = parseLoginToContinue(ltcAnnotation);
            }
        } catch (Exception e) {
            String simpleAnnotationTypeName = annotationType.getSimpleName();
            Tr.error(tc, "JAKARTASEC_CDI_ERROR_EXTRACTING_HAM_PROPERTIES", simpleAnnotationTypeName);
            String msg = Tr.formatMessage(tc, "JAKARTASEC_CDI_ERROR_EXTRACTING_HAM_PROPERTIES", simpleAnnotationTypeName);
            throw new DeploymentException(msg);
        }

        return props;
    }

    /**
     * Parse LoginToContinue annotation to extract properties - specific to *FormHAMs.
     */
    private Properties parseLoginToContinue(Annotation ltcAnnotation) throws Exception {
        Properties props = new Properties();
        Class<? extends Annotation> ltcType = ltcAnnotation.annotationType();

        Method loginPageMethod = ltcType.getMethod("loginPage");
        String loginPage = (String) loginPageMethod.invoke(ltcAnnotation);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, loginPage);

        Method errorPageMethod = ltcType.getMethod("errorPage");
        String errorPage = (String) errorPageMethod.invoke(ltcAnnotation);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, errorPage);

        Method useForwardToLoginMethod = ltcType.getMethod("useForwardToLogin");
        boolean useForwardToLogin = (boolean) useForwardToLoginMethod.invoke(ltcAnnotation);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN, useForwardToLogin);

        Method useForwardToLoginExpressionMethod = ltcType.getMethod("useForwardToLoginExpression");
        String useForwardToLoginExpression = (String) useForwardToLoginExpressionMethod.invoke(ltcAnnotation);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION, useForwardToLoginExpression);

        return props;
    }

    /**
     * For the *HamDefinition annotations, extract the qualifiers value.
     */
    @FFDCIgnore(Exception.class)
    private List<Class<?>> getQualifierClassesFromAnnotation(Annotation annotation,
                                                             Class<? extends Annotation> annotationType) {
        try {
            Method qualifiersMethod = annotationType.getMethod(JavaEESecConstants.QUALIFIERS);
            Class<?>[] qualifiers = (Class<?>[]) qualifiersMethod.invoke(annotation);

            if (qualifiers != null && qualifiers.length > 0) {
                return new ArrayList<>(Arrays.asList(qualifiers));
            }
        } catch (Exception e) {
            // no qualifiers which is okay
        }
        return Collections.emptyList();
    }

    /**
     * Does a list of HAM classes have a qualifier (i.e. "Admin" or "User")?
     */
    private boolean hasQualifiers(List<Class<?>> qualifiers) {
        return qualifiers != null && !qualifiers.isEmpty();
    }

    /**
     * Check if a qualifier class is a default qualifier for HAMs (JS 4.0+).
     */
    private boolean isDefaultQualifier(Class<?> qualifierClass) {
        String qualifierName = qualifierClass.getName();
        return qualifierName.contains("$BasicAuthenticationMechanism") ||
               qualifierName.contains("$FormAuthenticationMechanism") ||
               qualifierName.contains("$CustomFormAuthenticationMechanism") ||
               qualifierName.contains("$OpenIdAuthenticationMechanism");
    }

    /**
     * Create a custom qualifier annotation instance from a qualifier class.
     */
    private Annotation createQualifierAnnotation(Class<?> qualifierClass) {

        // For custom application-defined qualifiers
        return new AnnotationLiteral() {
            private static final long serialVersionUID = 1L;

            @Override
            public Class<? extends Annotation> annotationType() {
                return (Class<? extends Annotation>) qualifierClass;
            }
        };

    }

    public void processBean(@Observes ProcessBean<?> processBean, BeanManager beanManager) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "instance: " + Integer.toHexString(this.hashCode()) + " BeanManager: " + Integer.toHexString(beanManager.hashCode()));
        }
        if (!httpAuthenticationMechanismHandlerRegistered) {
            if (isHttpAuthenticationMechanismHandler(processBean)) {
                httpAuthenticationMechanismHandlerRegistered = true;
            }
        }
    }

    protected boolean isHttpAuthenticationMechanismHandler(ProcessBean<?> processBean) {
        Bean<?> bean = processBean.getBean();
        // Skip interface class
        if (bean.getBeanClass() != HttpAuthenticationMechanismHandler.class) {
            Set<Type> types = bean.getTypes();
            if (types.contains(HttpAuthenticationMechanismHandler.class)) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "found a custom HttpAuthenticationMechanismHandler : " + bean.getBeanClass());
                return true;
            }
        }
        return false;
    }

    public <T> void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "afterBeanDiscovery : instance : " + Integer.toHexString(this.hashCode()));
            Tr.debug(tc, "Number of qualified HAM definitions: " + qualifiedHAMs.size());
            Tr.debug(tc, "afterBeanDiscovery called for application: " + applicationName);
        }

        try {
            verifyConfiguration();

            // Note: Qualified HAMs are already added to tracker during ProcessAnnotatedType phase
            // to prevent old extensions from vetoing them

            // Create QualifiedHAMBean for each HAM definition with qualifiers
            for (QualifiedHAMData qualifiedHAM : qualifiedHAMs) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Creating QualifiedHAMBean for class ["
                                 + qualifiedHAM.implClass.getCanonicalName() + "] with qualifiers ["
                                 + qualifiedHAM.qualifiers.toString() + "].");
                }

                // Build qualifier annotations
                List<Annotation> qualifierAnnotations = new ArrayList<>();
                qualifierAnnotations.add(new AnnotationLiteral<Any>() {
                    private static final long serialVersionUID = 1L;
                });

                for (Class<?> qClass : qualifiedHAM.qualifiers) {
                    Annotation qualifier = createQualifierAnnotation(qClass);
                    if (qualifier != null) {
                        qualifierAnnotations.add(qualifier);
                    }
                }

                // Create and add QualifiedHAMBean
                Set<Annotation> qualifierSet = new HashSet<>(qualifierAnnotations);
                QualifiedHAMBean qualifiedHAMBean = new QualifiedHAMBean(beanManager, qualifiedHAM.implClass, qualifierSet, qualifiedHAM.props);
                beansToAdd.add(qualifiedHAMBean);

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Added QualifiedHAMBean for " + qualifiedHAM.implClass.getName() +
                                 " with qualifiers: " + qualifierSet);
                }
            }

            // Register default HttpAuthenticationMechanismHandler if needed
            if (!httpAuthenticationMechanismHandlerRegistered) {
                beansToAdd.add(new HttpAuthenticationMechanismHandlerBean(beanManager));
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "registering the default HttpAuthenticationMechanismHandler.");
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "HttpAuthenticationMechanismHandler is not registered because a custom HttpAuthenticationMechanismHandler has been registered");
            }
        } catch (DeploymentException de) {
            afterBeanDiscovery.addDefinitionError(de);
        }

        // Add all beans
        for (Bean<?> bean : beansToAdd) {
            afterBeanDiscovery.addBean(bean);
        }
    }

    /**
     * Verify the configuration after all the beans have been discovered.
     *
     * - if explicitly qualified HAMs exist, then ensure there is a custom
     * ham handler (spec requirement)
     **/
    private void verifyConfiguration() throws DeploymentException {

        // only custom qualified HAM defs require a custom handler
        boolean hasCustomQualifiedHAMs = qualifiedHAMs.stream().anyMatch(hamDef -> hamDef.qualifiers.stream().anyMatch(q -> !isDefaultQualifier(q)));

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Have custom qualified HAMs is [" + hasCustomQualifiedHAMs + "].");
            Tr.debug(tc, "Have a custom HAM handler [" + httpAuthenticationMechanismHandlerRegistered + "].");
        }

        if (hasCustomQualifiedHAMs && !httpAuthenticationMechanismHandlerRegistered) {
            Tr.error(tc, "JAKARTA_SECURITY_CDI_ERROR_CUSTOM_QUALIFIERS_NO_HAM_HANDLER");
            String msg = Tr.formatMessage(tc, "JAKARTA_SECURITY_CDI_ERROR_CUSTOM_QUALIFIERS_NO_HAM_HANDLER");
            throw new DeploymentException(msg);
        }
    }
}
