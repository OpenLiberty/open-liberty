package io.openliberty.org.testcontainers.generate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

/**
 * Allows external contributors a convenient way to build the custom images we use in our build
 */
public class CustomImages {
	
	public static void main(String[] args) {
	  	long start = System.currentTimeMillis();
	  	
	  	if(args == null || args[0] == null || args.length > 1) {
	  		throw new RuntimeException("CustomImages expects a single argument (projectPath) which is the path to the io.openliberty.org.testcontainers project.");
	  	}
	  	
	  	//Get data from calling script
	  	String projectPath = args[0];
	  	
	  	// Get image list if it exists
	  	File imageList = new File(projectPath, "cache/externals");
	  	if(!imageList.exists()) {
	  		throw new RuntimeException("No image list was generated.");
	  	}
	  	
	  	// Try to pull all images in the list and generate a list of images that need to be created
	  	final Set<DockerImageName> unpullableImageNames = new HashSet<>();
	  	try {
		  	Files.lines(imageList.toPath())
	  				.filter(line -> !line.startsWith("#"))
	  				.map(line -> DockerImageName.parse(line))
	  				.forEach(unpullableImageNames::add);
		  	
		  	Files.lines(imageList.toPath())
	  				.filter(line -> !line.startsWith("#"))
	  				.map(line -> DockerImageName.parse(line))
	  				.map(name -> new RemoteDockerImage(name))
	  				.forEach(image -> {
	  					try {
	  						unpullableImageNames.remove(DockerImageName.parse(image.get()));
	  					} catch (Exception e) {
	  						System.out.println("Could not pull image " + image.toString() + " because " + e.getMessage());
	  					}
	  				});
	  	} catch (Exception e) {
	  		throw new RuntimeException("Failure during pulling of images", e);
	  	}
	  	
	  	
	  	File dockerfiles = new File(projectPath, "dockerfiles");
	  	if(!dockerfiles.exists() && !dockerfiles.isDirectory()) {
	  		throw new RuntimeException("Expected dockerfiles directory not found: " + dockerfiles.getAbsolutePath());
	  	}
	  	
	  	// Try to find a Dockerfile for each unpullable image and build the image
	  	final Set<DockerImageName> unbuildableImageNames = new HashSet<>(unpullableImageNames);
	  	
	  	unpullableImageNames.stream()
	  		.map(name -> new ImageFromDockerfile(name.asCanonicalNameString(), false)
	  						.withDockerfile(
	  								new File(dockerfiles.getAbsolutePath(), name.getUnversionedPart() + "/" + name.getVersionPart() + "/Dockerfile").toPath()))
	  		.forEach(image ->  {
	  			try {
	  				unbuildableImageNames.remove(DockerImageName.parse(image.get()));
	  			} catch (Exception e) {
	  				System.out.println("Could not build image " + image.getDockerImageName() + " because " + e.getMessage());
	  			}
	  		});
	  	
	  	System.out.println("Could not pull or build " + unbuildableImageNames.size() + " image(s)");
	  	
		long end = System.currentTimeMillis();
		System.out.println( "Execution time in ms: " + ( end - start ));
	}
}
