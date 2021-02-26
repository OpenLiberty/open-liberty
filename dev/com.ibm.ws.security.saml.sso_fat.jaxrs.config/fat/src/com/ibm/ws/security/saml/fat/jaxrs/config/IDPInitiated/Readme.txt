The RSSamlIDPInitiatedConfigTests.java test class is being split due to issues with the in-memory LDAP that result in Out-Of-Memeory failures in Shiboleth.
This FAT project tests the config attributes for SAML JaxRS.
The split was based on:
a) the size of the class (number of tests) was allowing the OOM to occur in code that we import into the FAT Framework.
b) moving the largest chunks of test cases that test like function out to new classes.

The RSSamlIDPInitiatedConfigTests.java test class is being split into:
RSSamlIDPInitiatedConfigCommonTests.java - common test code and test utilities 
RSSamlIDPInitiatedMapToUserRegistryConfigTests.java - variations on the mapToUserRegistry config attribute
RSSamlIDPInitiatedMiscConfigTests.java - all other config attributes
RSSamlIDPInitiatedSSLConfigTests.java - SSL config attributes

