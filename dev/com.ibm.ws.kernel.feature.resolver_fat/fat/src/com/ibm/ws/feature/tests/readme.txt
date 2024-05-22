[FEATURE RESOLUTION FAT NOTES][TFB][22-May-2024][START]>

Part 1: Feature testing

When testing feature updates, the following tests are recommended to be run:

Unit tests:

  dev/com.ibm.ws.kernel.feature
  
FATs:

  com.ibm.ws.kernel.feature.resolver_fat
  io.openliberty.jakartaee9.internal_fat
  io.openliberty.jakartaee10.internal_fat
  io.openliberty.jakartaee11.internal_fat

---

Part 2: Updating baseline data:

A typical resolution failure obtained from subclasses of the
feature resolution unit test "FeatureResolutionUnitTestBase"
is a change to what features are resolved.

Baseline test cases are recorded in several data files under
"com.ibm.ws.kernel.feature.resolver_fat/publish/verify".

Cases for all public features:

    singleton_expected.xml
    singleton_expected_WL.xml

Cases which combine a single versioned servlet feature with single
versionless feature:

    servlet_expected.xml
    servlet_expected_WL.xml

For example, the resolution of  "acmeCA-2.0" as a server feature is
present in "singleton_expected.xml":

    <case>
        <name>com.ibm.websphere.appserver.acmeCA-2.0_SERVER</name>
        <description>Singleton [ com.ibm.websphere.appserver.acmeCA-2.0 ] [ SERVER ]</description>
        <input>
            <server/>
            <kernel>com.ibm.websphere.appserver.kernelCore-1.0</kernel>
            <kernel>com.ibm.websphere.appserver.logging-1.0</kernel>
            <root>com.ibm.websphere.appserver.acmeCA-2.0</root>
        </input>
        <output>
            <resolved>com.ibm.websphere.appserver.certificateCreator-2.0</resolved>
            <resolved>json-1.0</resolved>
            <resolved>com.ibm.websphere.appserver.eeCompatible-6.0</resolved>
            <resolved>io.openliberty.servlet.api-3.1</resolved>
            <resolved>com.ibm.websphere.appserver.javaeeddSchema-1.0</resolved>
            <resolved>com.ibm.websphere.appserver.servlet-servletSpi1.0</resolved>
            <!-- ... additional resolved features ... -->
        </output>
    </case>

This shows a single case described as the resolution of the singleton "acmCA-2.0"
as a server feature.  This example has omitted most of the resolved features.

The meaning of a feature resolution case is that the the input features are
expected to resolve to the output features. 

Each case has an "input" element, which provides resolution settings, a list of
kernel features, and a list of root features.  The root features are generally
the most import input and are what distinguish cases.

Each case has an "output" element which has a full listing of the resolved
features.  The output element lists all public and private internal features
which were resolved from the inputs.

Usually, two data files are read, one with open-liberty specific data,
and one with WAS liberty specific data.  When multiple data files are
read, later read files overlay the earlier read files, using case name
as key.

Failures are usually because one or more features was added or removed to the
resolution result.  The necessary correction will be to update the base line
data with the added or removed features.  That is, adding or removing
"resolved" elements from cases which failed.  The correction is made directly
to the case file.

  * If the feature update which triggers the failure was made in
    open-liberty, both data files should be updated.
    
  * If the feature update which triggers the failure was made in
    WAS-liberty, only the WAS liberty data files should be updated.

As a convenience, the FAT test log contains suggested XML text which may be
used to update the baseline data file.");

Alternatively, the entire case data can be regenerated using
"VersionlessResolutionTest".  See section 3 of these notes for more information
about regenerating the case data.

---

Part 3: Generating entirely new baseline data

Several of the feature resolution FATs follow the pattern of:

1) Generating baseline data based on a case generator.
2) Running unit tests which perform resolution and validate the results using the baseline data.

1: Baseline generation

Baseline data is created by running "VersionlessResolutionTest", which starts the server with
parameters which cause baseline data to be generated.

Parameter names are specified in "VerifyEnv".

    String RESULTS_SINGLETON_PROPERTY_NAME = "featureVerify_results_singleton";
    String DURATIONS_SINGLETON_PROPERTY_NAME = "featureVerify_durations_singleton";

    String RESULTS_SERVLET_PROPERTY_NAME = "featureVerify_results_servlet";
    String DURATIONS_SERVLET_PROPERTY_NAME = "featureVerify_durations_servlet";

Parameters are provided for resolution results, and for resolution timing data.

When any of these parameter values is specified as a server environment variable, baseline results
are generated and written to the file as specified through the environment variable.  Baseline
data is generated as a callout during usual feature resolution.  Server class "FeatureManager"
calls out to helper "FeatureResolverBaseline".

Currently, baseline data is written to the autoFVT output directory, for example:   

dev/com.ibm.ws.kernel.feature.resolver_fat/build/libs/autoFVT/output/verify:
  servlet_actual.xml
  servlet_durations.txt
  singleton_actual.xml
  singleton_durations.txt

After generation, the data must be copied from the autoFVT directory to a project directory,
for example:

dev/com.ibm.ws.kernel.feature.resolver_fat/publish/verify/
  servlet_durations.txt
  servlet_actual.xml
  singleton_durations.txt
  singleton_actual.xml

Code references:

Baseline data is generated by setting server environment variables a specified by "VerifyEnv":

dev/com.ibm.ws.kernel.feature.core/com/ibm/ws/kernel/feature/internal/util/
  VerifyEnv.java

Baseline data generation is triggerred from "VersionlessResolutionTest":

dev/com.ibm.ws.kernel.feature.resolver_fat/fat/src/com/ibm/ws/feature/tests/
  VersionlessResolutionTest.java

The actual generation is performed by a callout from "FeatureManager" to "FeatureResolverBaseline":

dev/com.ibm.ws.kernel.feature.core/com/ibm/ws/kernel/feature/internal/
  FeatureManager.java
  FeatureResolverBaseline.java

2: Resolution verification unit tests

The baseline data is used by unit tests.  Here, a common base implementation class provides
general capabilities of reading baseline case data, of performing resolutions for each of
the cases, and for comparing the actual resolution results with the expected resolution results.

Cases currently exist for:

1) All singleton public features (which are not versionless features).
2) All pairs of a versioned servlet feature with a versionless feature.

These unit tests used the pre-generated baseline case data:

  servlet_expected.xml
  singleton_expected.xml 

Other cases are expected to be created.

Code references: 

dev/com.ibm.ws.kernel.feature.resolver_fat/fat/src/com/ibm/ws/feature/tests/
  FeatureResolutionUnitTestBase.java
  BaselineServletUnitTest.java
  BaselineSingletonUnitTest.java

[FEATURE RESOLUTION FAT NOTES][TFB][22-May-2024][END]
