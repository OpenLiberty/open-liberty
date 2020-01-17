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
package com.ibm.websphere.security.oauth20.store;

import java.util.Collection;

/**
 * Interface for storing and accessing OAuth artifacts, such as clients, tokens,
 * and consents, that are necessary for an OAuth flow. The implementation is
 * responsible for securing the data at motion and rest.
 *
 * <p/>
 * Implementing classes are required to define a zero-argument constructor so that
 * they can be instantiated during loading.
 *
 * <p/>
 * To make a OAuthStore implementation available to Liberty as an OSGi service there are two
 * options.
 *
 * <ol>
 * <li>Basic Extensions using Liberty Libraries (BELL)</li>
 * <p/>
 * The BELL feature uses the Java ServiceLoader facility to load an OSGi service from a library. Your
 * JAR file must contain both the OAuthStore implementation class and the provider-configuration
 * file. The following list shows the files that might go into a JAR file:
 *
 * <pre>
 * myLibrary.jar
 * -------------
 * -- com/acme/CustomOAuthStore1.class
 * -- com/acme/CustomOAuthStore2.class
 * -- META-INF/services/com.ibm.websphere.security.oauth20.store.OAuthStore
 * </pre>
 *
 * The provider-configuration file lists all the OAuthStore implementations to be provided as an
 * OSGi service. For example, for myLibrary.jar, the META-INF/services/com.ibm.websphere.security.oauth20.store.OAuthStore
 * provider-configuration file has a list of services, with each service on its own line. It *must* also specify
 * the ID for each instance by inserting a comment line prior to each implementing class that contains a key value pair
 * where the key is 'oauth.store.id' and the value is a unique ID that can be used to reference the instance
 * from a OAuth provider in the server.xml.
 *
 * <pre>
 * # oauth.store.id=customOAuthStore1
 * com.acme.CustomOAuthStore1
 *
 * # oauth.store.id=customOAuthStore2
 * com.acme.CustomOAuthStore2
 * </pre>
 *
 * Once the JAR has been packaged, update the server.xml configuration to include the "bells-1.0" feature, the library
 * that points to the JAR and the BELL configuration that points to the library. Finally, associate the OAuth provider
 * to an OAuthStore implementation by adding a 'customStore' element to the 'oauthProvider' element and setting the
 * 'storeId' attribute to the value of the 'oauth.store.id' of the implementation of the OAuthStore to use.
 *
 * <p/>
 * Below is an example of associating 'customOAuthStore1' to an OAuth provider using the BELL feature.
 *
 * <pre>
 * &lt;server&gt;
 *    &lt;featureManager&gt;
 *       &lt;feature&gt;oauth-2.0&lt;/feature&gt;
 *       &lt;feature&gt;bells-1.0&lt;/feature&gt;
 *    &lt;/featureManager&gt;
 *
 *    &lt;!--
 *       Create a library for the JAR file that contains
 *       the OAuthStore implementation.
 *    --&gt;
 *    &lt;library id="mylibrary"&gt;
 *       &lt;file name="${shared.resource.dir}/libs/myLibrary.jar"&gt;
 *    &lt;/library&gt;
 *
 *    &lt;!-- Load the library in a BELL. --&gt;
 *    &lt;bell libraryRef="mylibrary" /&gt;
 *
 *    &lt;!-- Configure the OAuth provider with the custom OAuthStore implementation. --&gt;
 *    &lt;oauthProvider ...&gt;
 *        &lt;customStore storeId="customOAuthStore1" /&gt;
 *    &lt;/oauthProvider&gt;
 * &lt;/server&gt;
 * </pre>
 *
 * <p/>
 *
 * <li>Registering with a user feature</li>
 * <p/>
 * You can create a new OSGi service that implements the OAuthStore in a user feature. The service *must* define the property
 * 'oauth.store.id' with a unique ID that can be used to reference the implementation from a OAuth provider in the
 * server.xml. An example component XML file defining the component service might look like this:
 *
 * <pre>
 * OSGI-INF/com.acme.CustomOAuthStore1.xml
 * ---------------------------------------
 * &lt;component name="CustomOAuthStore1"&gt;
 *    &lt;implementation class="com.acme.CustomOAuthStore1"/&gt;
 *    &lt;service&gt;
 *       &lt;provide interface="com.ibm.websphere.security.oauth20.store.OAuthStore"/&gt;
 *    &lt;/service&gt;
 *    &lt;property name="service.vendor" type="String" value="ACME"/&gt;
 *    &lt;property name="oauth.store.id" type="String" value="customOAuthStore1"/&gt;
 * &lt;/component&gt;
 * </pre>
 *
 * <p/>
 * When the user feature has been installed in Liberty, add the user feature to the feature list in the server.xml
 * configuration file. Finally, associate the OAuth provider to an OAuthStore implementation by adding a 'customStore' element
 * to the 'oauthProvider' element and setting the 'storeId' attribute to the value of the 'oauth.store.id' of the implementation
 * of the OAuthStore to use.
 *
 * <p/>
 * Below is an example of associating 'customOAuthStore1' to an OAuth provider using a user feature.
 *
 * <pre>
 * &lt;server&gt;
 *    &lt;featureManager&gt;
 *       &lt;feature&gt;oauth-2.0&lt;/feature&gt;
 *       &lt;feature&gt;user:myFeature-1.0&lt;/feature&gt;
 *    &lt;/featureManager&gt;
 *
 *    &lt;!-- Configure the OAuth provider with the custom OAuthStore. --&gt;
 *    &lt;oauthProvider ...&gt;
 *       &lt;customStore storeId="customOAuthStore1" /&gt;
 *    &lt;/oauthProvider&gt;
 * &lt;/server&gt;
 * </pre>
 *
 * </ol>
 */
public interface OAuthStore {

    /**
     * Creates an {@link OAuthClient} entry in the store.
     *
     * @param oauthClient the {@link OAuthClient} object representing the client to create in the store
     *
     * @throws OAuthStoreException if the store is not able to create the {@link OAuthClient} entry
     */
    void create(OAuthClient oauthClient) throws OAuthStoreException;

    /**
     * Creates an {@link OAuthToken} entry in the store.
     *
     * @param oauthToken the {@link OAuthToken} object representing the token to create in the store
     *
     * @throws OAuthStoreException if the store is not able to create the {@link OAuthToken} entry
     */
    void create(OAuthToken oauthToken) throws OAuthStoreException;

    /**
     * Creates an {@link OAuthConsent} entry in the store.
     *
     * @param oauthConsent the {@link OAuthConsent} object representing the consent to create in the store
     *
     * @throws OAuthStoreException if the store is not able to create the {@link OAuthConsent} entry
     */
    void create(OAuthConsent oauthConsent) throws OAuthStoreException;

    /**
     * Reads the {@link OAuthClient} entry matching the given providerId and clientId arguments from the store.
     *
     * @param providerId the id of the OAuth provider the client is registered with
     * @param clientId the id of the client entry to find in the store
     *
     * @return the {@link OAuthClient} entry or <code>null</code> if no matching entry exists
     *
     * @throws OAuthStoreException if the store is not able to read an {@link OAuthClient} entry
     */
    OAuthClient readClient(String providerId, String clientId) throws OAuthStoreException;

    /**
     * Reads all the {@link OAuthClient} entries matching the given providerId and attribute arguments from the store.
     *
     * @param providerId the id of the OAuth provider the client is registered with
     * @param attribute an attribute of the client to match when reading the entry from the underlying store. If
     *                  null, the method should return all clients for the specified provider.
     *
     * @return the collection of {@link OAuthClient} entries or <code>null</code> if no matching entries exist
     *
     * @throws OAuthStoreException if the store is not able to read the {@link OAuthClient} entries
     */
    Collection<OAuthClient> readAllClients(String providerId, String attribute) throws OAuthStoreException;

    /**
     * Reads the {@link OAuthToken} entry matching the given the providerId and lookupKey arguments from the store.
     *
     * @param providerId the id of the OAuth provider that issued the token
     * @param lookupKey the lookup key of the token entry to find in the store
     *
     * @return the {@link OAuthToken} entry or <code>null</code> if no matching entry exists
     *
     * @throws OAuthStoreException if the store is not able to read an {@link OAuthToken} entry
     */
    OAuthToken readToken(String providerId, String lookupKey) throws OAuthStoreException;

    /**
     * Reads all the {@link OAuthToken} entries matching the given providerId and username arguments from the store.
     *
     * @param providerId the id of the OAuth provider that issued the tokens
     * @param username the user the tokens were issued for
     *
     * @return the {@link OAuthToken} entries or <code>null</code> if no matching entries exist
     *
     * @throws OAuthStoreException if the store is not able to read the {@link OAuthToken} entries
     */
    Collection<OAuthToken> readAllTokens(String providerId, String username) throws OAuthStoreException;

    /**
     * Counts the {@link OAuthToken} entries matching the given providerId, username, and clientId arguments in the store.
     *
     * @param providerId the id of the OAuth provider that issued the tokens
     * @param username the user the tokens were issued for
     * @param clientId the id of the client the tokens were issued to
     *
     * @return the number of tokens the user was issued for the client with the given clientId from the provider with the given providerId
     *
     * @throws OAuthStoreException if the store is not able to count the {@link OAuthToken} entries
     */
    int countTokens(String providerId, String username, String clientId) throws OAuthStoreException;

    /**
     * Reads the {@link OAuthConsent} entry matching the given providerId, username, clientId, and resource arguments from the store.
     *
     * @param providerId the id of the OAuth provider from which consent was given
     * @param userame the user that gave consent
     * @param clientId the id of the client granted consent to access the resource
     * @param resource the resource the client was granted consent to
     *
     * @return the {@link OAuthConsent} entries or <code>null</code> if no matching entry exists
     *
     * @throws OAuthStoreException if the store is not able to read an {@link OAuthConsent} entry
     */
    OAuthConsent readConsent(String providerId, String username, String clientId, String resource) throws OAuthStoreException;

    /**
     * Updates an {@link OAuthClient} entry in the store. If the entry does not exist, this operation
     * will no-op.
     *
     * @param oauthClient the {@link OAuthClient} object representing the client to update in the store
     *
     * @throws OAuthStoreException if the store is not able to update the {@link OAuthClient} entry
     */
    void update(OAuthClient oauthClient) throws OAuthStoreException;

    /**
     * Updates an {@link OAuthToken} entry in the store. If the entry does not exist, this operation
     * will no-op.
     *
     * @param oauthToken the {@link OAuthToken} object representing the token to update in the store
     *
     * @throws OAuthStoreException if the store is not able to update the {@link OAuthToken} entry
     */
    void update(OAuthToken oauthToken) throws OAuthStoreException;

    /**
     * Updates an {@link OAuthConsent} entry in the store. If the entry does not exist, this operation
     * will no-op.
     *
     * @param oauthConsent the {@link OAuthConsent} object representing the consent to update in the store
     *
     * @throws OAuthStoreException if the store is not able to update the {@link OAuthConsent} entry
     */
    void update(OAuthConsent oauthConsent) throws OAuthStoreException;

    /**
     * Deletes an {@link OAuthClient} entry matching the providerId and clientId arguments from the store.
     *
     * @param providerId the id of the OAuth provider the client is registered with
     * @param clientId the id of the client entry to delete from the store
     *
     * @throws OAuthStoreException if the store is not able to delete the {@link OAuthClient} entry
     */
    void deleteClient(String providerId, String clientId) throws OAuthStoreException;

    /**
     * Deletes an {@link OAuthToken} entry matching the providerId and lookupKey arguments from the store.
     *
     * @param providerId the id of the OAuth provider that issued the token
     * @param lookupKey the lookup key of the token entry to delete from the store
     *
     * @throws OAuthStoreException if the store is not able to delete the {@link OAuthToken} entry
     */
    void deleteToken(String providerId, String lookupKey) throws OAuthStoreException;

    /**
     * Deletes the {@link OAuthToken} entries for the providerId from the store whose expiration fields are less than the given timestamp argument.
     *
     * @param providerId the id of the OAuth provider that issued the token
     * @param timestamp the time in milliseconds since the epoch to compare the token entry expiration with to delete the entry from the store
     *
     * @throws OAuthStoreException if the store is not able to delete the {@link OAuthToken} entries
     */
    void deleteTokens(String providerId, long timestamp) throws OAuthStoreException;

    /**
     * Deletes an {@link OAuthConsent} entry matching the providerId, username, and clientId arguments from the store.
     *
     * @param providerId the id of the OAuth provider from which consent was given
     * @param username the user that gave consent
     * @param clientId the id of the client for which to delete the user consent entry from the store
     * @param resource the resource the client was granted consent to
     *
     * @throws OAuthStoreException if the store is not able to delete the {@link OAuthConsent} entry
     */
    void deleteConsent(String providerId, String username, String clientId, String resource) throws OAuthStoreException;

    /**
     * Deletes the {@link OAuthConsent} entries for the providerId from the store whose expiration fields are less than the given timestamp argument.
     *
     * @param providerId the id of the OAuth provider from which consent was given
     * @param timestamp the time in milliseconds since the epoch to compare the consent entry expiration with to delete the entry from the store
     *
     * @throws OAuthStoreException if the store is not able to delete the {@link OAuthConsent} entries
     */
    void deleteConsents(String providerId, long timestamp) throws OAuthStoreException;

}
