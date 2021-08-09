/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.security;

import java.security.cert.X509Certificate;

/**
 * Interface for custom X.509 certificate mapping. Implementing classes are required to define
 * a zero-argument constructor so that they can be instantiated during loading.
 *
 * <p/>
 * Support of this interface is currently limited to basic and LDAP registries.
 *
 * <p/>
 * To make a X509CertificateMapper implementation available to Liberty as an OSGi service there are two
 * options.
 *
 * <ol>
 * <li>Basic Extensions using Liberty Libraries (BELL)</li>
 * <p/>
 * The BELL feature uses the Java ServiceLoader facility to load an OSGi service from a library. Your
 * JAR file must contain both the X509CertificateMapper implementation class and the provider-configuration
 * file. The following list shows the files that might go into a JAR file:
 *
 * <pre>
 * myLibrary.jar
 * -- com/acme/CustomLdapMapper.class
 * -- com/acme/AnotherCustomCertificateMapper.class
 * -- META-INF/services/com.ibm.websphere.security.X509CertificateMapper
 * </pre>
 *
 * The provider-configuration file lists all the X509CertificateMapper implementations to be provided as an
 * OSGi service. For example, for myLibrary.jar, the META-INF/services/com.ibm.websphere.security.X509CertificateMapper
 * provider-configuration file has a list of services, with each service on its own line. It *must* also specify
 * the ID for each instance by inserting a comment line prior to each implementing class that contains a key value pair
 * where the key is 'x509.certificate.mapper.id' and the value is a unique ID that can be used to reference the instance
 * from a user registry in the server.xml.
 *
 * <pre>
 * # x509.certificate.mapper.id=mapper1
 * com.acme.CustomMapper
 *
 * # x509.certificate.mapper.id=mapper2
 * com.acme.AnotherCustomMapper
 * </pre>
 *
 * Once the JAR has been packaged, update the server.xml configuration to include the "bells-1.0" feature, the library
 * that points to the JAR and the BELL configuration that points to the library. Finally, associate the user registry
 * to a X509CertificateMapper implementation by changing the 'certificateMapMode' to 'CUSTOM' and setting the
 * 'certificateMapperId' to the value of the 'x509.certificate.mapper.id' of the instance of the mapper to use.
 *
 * <p/>
 * Below is an example of associating 'mapper1' to and LDAP registry using the BELL feature.
 *
 * <pre>
 * &lt;server&gt;
 *    &lt;featureManager&gt;
 *       &lt;feature&gt;ldapRegistry-3.0&lt;/feature&gt;
 *       &lt;feature&gt;bells-1.0&lt;/feature&gt;
 *    &lt;/featureManager&gt;
 *
 *    &lt;!--
 *       Create a library for the JAR file that contains
 *       the CertificateMapper implementation.
 *    --&gt;
 *    &lt;library id="mylibrary"&gt;
 *       &lt;file name="${shared.resource.dir}/libs/myLibrary.jar"&gt;
 *    &lt;/library&gt;
 *
 *    &lt;!-- Load the library in a BELL. --&gt;
 *    &lt;bell libraryRef="mylibrary" /&gt;
 *
 *    &lt;!-- Configure the registry with the custom X509CertificateMapper. --&gt;
 *    &lt;ldapRegistry ...
 *       certificateMapMode="CUSTOM"
 *       certificateMapperId="mapper1"
 *       ... /&gt;
 * &lt;/server&gt;
 * </pre>
 *
 * <p/>
 *
 * <li>Registering with a user feature</li>
 * <p/>
 * If there is a pre-existing user feature or you prefer to create a user feature, you can create an new OSGi service
 * in you user feature that implements the X509CertificateMapper. The service *must* define the property
 * 'x509.certificate.mapper.id' with a unique ID that can be used to reference the instance from a user registry in the
 * server.xml.
 *
 * <p/>
 * When the user feature has been installed in Liberty, add the user feature to the feature list in the server.xml
 * configuration file. Finally, associate the user registry to a X509CertificateMapper implementation by changing the
 * 'certificateMapMode' to 'CUSTOM' and setting the 'certificateMapperId' to the value of the 'x509.certificate.mapper.id'
 * of the instance of the mapper to use.
 *
 * <p/>
 * Below is an example of associating 'mapper1' to an LDAP registry using a user feature.
 *
 * <pre>
 * &lt;server&gt;
 *    &lt;featureManager&gt;
 *       &lt;feature&gt;ldapRegistry-3.0&lt;/feature&gt;
 *       &lt;feature&gt;user:myFeature-1.0&lt;/feature&gt;
 *    &lt;/featureManager&gt;
 *
 *    &lt;!-- Configure the registry with the custom X509CertificateMapper. --&gt;
 *    &lt;ldapRegistry ...
 *       certificateMapMode="CUSTOM"
 *       certificateMapperId="mapper1"
 *       ... /&gt;
 * &lt;/server&gt;
 * </pre>
 *
 * </ol>
 */
public interface X509CertificateMapper {

    /**
     *
     * Map the X.509 certificate. Implementations of this method must be thread-safe.
     *
     * <p/>A {@link X509CertificateMapper} for an LDAP registry should return a string that is one of either:
     * <ol>
     * <li>a distinguished name (DN). For example: uid=user1,o=ibm,c=us</li>
     * <li>an LDAP search filter surrounded by parenthesis. For example: (uid=user1)</li>
     * </ol>
     *
     * <p/>A {@link X509CertificateMapper} for a basic registry should return a string that corresponds
     * to the user's name in the registry. For example: user1
     *
     * @param certificates The certificate chain containing the certificate to map.
     * @return The registry specific string returned used by the repository to search for the user.
     * @throws CertificateMapNotSupportedException If certificate mapping is not supported.
     * @throws CertificateMapFailedException If the certificate could not be mapped.
     */
    public String mapCertificate(X509Certificate[] certificates) throws CertificateMapNotSupportedException, CertificateMapFailedException;
}
