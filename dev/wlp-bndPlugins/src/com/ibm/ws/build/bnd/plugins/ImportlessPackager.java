package com.ibm.ws.build.bnd.plugins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.service.Plugin;
import aQute.service.reporter.Reporter;

public abstract class ImportlessPackager implements AnalyzerPlugin, Plugin {

    private static boolean scanAgain = true;
    private static final List<String> addedPackages = new ArrayList<String>();

    private static int iteration = 0;

    private static final File errorMarker = new File("build/lib/ERROR");
	private Set<String> excludes = new HashSet<String>();				// packages to exclude
	private Set<String> excludePrefixes = new HashSet<String>();		// prefixes of the packages to exclude
	private  Set<TypeRef> importedReferencedTypes = new HashSet<TypeRef>();	// newly added class types because of the class dependency imports 
	static private  Set<TypeRef> allReferencedTypes = new HashSet<TypeRef>();	// all class types that need to be exported

    @Override
    public boolean analyzeJar(Analyzer analyzer) throws Exception {

        try {
        if (scanAgain) {

            //this will only have an effect on the first scan, because subsequent scanAgain
            //will use the errorMarker to decide if to scan again
            resetErrorMarker();

            List<String> newlyAddedPackages = new ArrayList<String>();

            System.out.println("ImportlessPackager plugin: iteration " + iteration);
            // set up exclude filter
            setupFilters(analyzer);
            // collect dependency packages
            Set<PackageRef> importedPackages = collectDependencies(analyzer);
            
            Jar outputJar = analyzer.getJar();
            //loop through the referred packages
            for (PackageRef ref : importedPackages) {

                String packageName = ref.getFQN();

                System.out.println("Seeking package " + packageName);

                boolean foundPackage = false;
                // locate the package in the classpath and add it the the export
                for (Jar src : analyzer.getClasspath()) {
               		if (src.getPackages().contains(packageName)) {
               			foundPackage = true;
                        System.out.println("Found matching pkg " + packageName + " from " +src.getSource().getAbsolutePath());
                         //only want to include the package itself, not any sub-packages
                         //can provide an Instruction to bnd for this, but it gets a bit weird:
                         //match the package name plus a / since everything in the package will
                         //have the package path followed by a separator.
                         //We use [/|/] to match the single / because bnd needs something to serve
                         //as a wildcard to enable regex matching (otherwise it just does an equals match)
                         //we can't use the other wildcards e.g. * ? because bnd processes them into
                         //.?, .* to do an any character match
                         //If we don't want any sub package content we can't allow any more slashes
                         //so use [^/]+ (i.e. not / 1 or more times)
                         outputJar.addAll(src, new Instruction(ref.getPath() + "[/|/][^/]+"));

                         newlyAddedPackages.add(packageName);

                         //we don't break the outer loop in case of split packages (ick!)
                         //since we might find some additional content for this package in another jar
                     }
                 }
                 if (!foundPackage) {
                	 //we couldn't find the package on the classpath, fail the build
                     //with a helpful message

                     String errorMsg = "Package " + packageName + " not found for inclusion in jar. Is the package available on the projects classpath?";
                     error(errorMsg);
                }
            }

            //add all the newly added packages to our global list so we don't check them again
            if (newlyAddedPackages.isEmpty()) {
                //no new packages, no further scanning required
                scanAgain = false;
            } else {
                addedPackages.addAll(newlyAddedPackages);
                iteration++;
                if (iteration > LAST_ITERATION) {
                    //there are new packages but we've run out of iterations, fail the build
                    error("Maximum number of plugin iterations reached, but there were still new packages to analyze. Consider adding more iterations.");
                }
            }

            //don't bother scanning again if we've already had an error
            if (scanAgain == true && errorMarker.exists())
                scanAgain = false;

        }
        } catch(Exception ex) {
            ex.printStackTrace();
            System.out.println(ex.getMessage());
        }

        //tell bnd to reanalyze the classpath if we are going to scan again
        return scanAgain;
    }

    @Override
    public void setProperties(Map<String, String> map) {
    //no-op
    }

    private Reporter reporter;

    @Override
    public void setReporter(Reporter reporter) {
        this.reporter = reporter;
    }

    private void resetErrorMarker() throws IOException {
        if (errorMarker.exists()) {
            boolean deleted = errorMarker.delete();
            if (!deleted) {
                //if we couldn't delete the marker we don't have a good state to start from
                error("Could not delete error marker file at start of build.");
            }
        }
    }

    private void error(String errorMsg) throws IOException {
        //Unfortunately this does squat all in BND at the moment because there is a bug in the bnd ant task
        //When we upgrade to a newer BND version it should fail when the reporter.error is called
        System.out.println("ERROR ERROR ERROR: " + errorMsg);
        //temporarily work around, by writing a file out for ant to find
        errorMarker.getParentFile().mkdirs();
        errorMarker.createNewFile();
        //doesn't matter if it wasn't created - it probably already existed which is fine
        System.out.println("ERROR ERROR ERROR: " + errorMsg);
        reporter.error(errorMsg, new Object[0]);
    }

    /*
     * Placeholder plugins to allow workaround of bnd limitation where plugins are not called again after changes
     * Each of these are listed in the bnd.bnd -plugin header. I hope 10 iterations will be enough, but if not more can be
     * added. Just add another static class to this list and the -plugin header and increase the LAST_ITERATION integer to
     * match
     */

    public static final class ImportlessPackagerLevel0 extends ImportlessPackager {

    }

    public static final class ImportlessPackagerLevel1 extends ImportlessPackager {

    }

    public static final class ImportlessPackagerLevel2 extends ImportlessPackager {

    }

    public static final class ImportlessPackagerLevel3 extends ImportlessPackager {

    }

    public static final class ImportlessPackagerLevel4 extends ImportlessPackager {

    }

    public static final class ImportlessPackagerLevel5 extends ImportlessPackager {

    }

    public static final class ImportlessPackagerLevel6 extends ImportlessPackager {

    }

    public static final class ImportlessPackagerLevel7 extends ImportlessPackager {

    }

    public static final class ImportlessPackagerLevel8 extends ImportlessPackager {

    }

    public static final class ImportlessPackagerLevel9 extends ImportlessPackager {

    }

    //So we can issue a warning if there are still new packages but we don't have any scans left
    private static final int LAST_ITERATION = 9;
    /**
		// for each class in classSpace, use addDirectReferences() to add imported classes to allReferencedTypes
		// add the packages imported to the analyzer and run another analysis
		// Only search the directly referenced classes on the class in the allReferencedTypes
     * 
     * @param analyzer
     * @return
     * @throws Exception
     */
	private Set<PackageRef> collectDependencies(Analyzer analyzer) throws Exception {
		// collect all classed directly exported by the bnd.bnd file
		Map <TypeRef, Clazz> classSpace = analyzer.getClassspace();
		Set<TypeRef> knownTypeRefs = classSpace.keySet();
		if (allReferencedTypes.isEmpty())
			allReferencedTypes.addAll(knownTypeRefs);
	
		// go through the known classes and find their unknown dependencies
		for (TypeRef typeRef : knownTypeRefs) {
			if (!allReferencedTypes.contains(typeRef))			// unused class in the referenced package
				continue;
			// collect the imports from a class and add them to newReferencedTypes
			Clazz classInstance = classSpace.get(typeRef);
			collectClassDependencies(classInstance, analyzer);
		}
		// collect the newly added packages by going through the referenced classes
		return collectPackageDependencies();
	}
	/**
	 * Collect the imports from a class and add the imported classes to the map of all known classes
	 * importedReferencedTypes is updated to contain newly added imports
	 * allReferencedTypes is updated to avoid the duplicated process
	 * 
	 * @param classInstance
	 * @param analyzer
	 * @throws Exception
	 */
	private void collectClassDependencies (Clazz classInstance, Analyzer analyzer) throws Exception {
		// retrieve the imports from the known class path
		Set<TypeRef> importedClasses = classInstance.parseClassFile();
		for (TypeRef importedClass:importedClasses) {
			if (canBeSkipped(importedClass))		// validate the import
				continue;
			// find the class in the classpath
			Clazz classInstanceImported = analyzer.findClass(importedClass); 
			if (classInstanceImported == null)
				error( "Referenced class " + importedClass.getFQN() + " not found for inclusion in jar. It is imported by " + classInstance.getAbsolutePath());

			// update the imports map
			importedReferencedTypes.add(importedClass);
			allReferencedTypes.add(importedClass);
			// collect dependencies introduced by the imports
			collectClassDependencies(classInstanceImported, analyzer);
		}
	}
	/**
	 * check whether a imported class should be considered in further dependency check
	 * @param importedClass
	 * @return
	 */
	private boolean canBeSkipped (TypeRef importedClass) {
		// skip known imported classes and the ones in JRE
		if (allReferencedTypes.contains(importedClass) || importedReferencedTypes.contains(importedClass) || importedClass.isJava())
			return true;
		// Skip the imported classes which are excluded 
		String classPackage = importedClass.getPackageRef().getFQN();
		for (String excludePrefix: excludePrefixes) {		// skip by the package prefix
			if (classPackage.startsWith(excludePrefix))
				return true;
		}
		if (excludes.contains(classPackage))				// skip by the full package name
			return true;
		return classPackage.length()<2;						// special situation for the primitive array 
	}
	/**
	 * Read the excluded packages from importless.packager.excludes in bnd.bnd
	 * It can contain  javax.security.auth or javax.xml*
	 * Put them into two groups
	 * @param exclude
	 */
	private void setupFilters(Analyzer analyzer) {
		String exclude = analyzer.getProperty("importless.packager.excludes");
		String[] excludesAll = exclude.split(",");
		for (String excludesIndividual:excludesAll) {
			if (excludesIndividual.endsWith("*"))
				excludePrefixes.add(excludesIndividual.substring(0,excludesIndividual.length()-1));
			else 
				excludes.add(excludesIndividual);
		}
		System.out.println("excludePrefixes: " + excludePrefixes +" excludes: " + excludes);
	}
	/**
	 * Collect the referenced packages information from the referenced classes information
	 * @return referencedPackages
	 */
	private Set<PackageRef> collectPackageDependencies() {
		Set<PackageRef> referencedPackages = new HashSet<PackageRef> ();
		for (TypeRef newReferencedType:importedReferencedTypes) {
			PackageRef packageRef = newReferencedType.getPackageRef();
			if (referencedPackages.contains(packageRef))			// package already known
				continue;
			referencedPackages.add(packageRef);
			System.out.println("Add package: " + packageRef.getFQN());
		}
		return referencedPackages;
	}

}