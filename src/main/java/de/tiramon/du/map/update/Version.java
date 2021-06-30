package de.tiramon.du.map.update;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Class to extract the version of the Jar containing a specific class.
 */
public class Version {
	private final static String UNKOWN_INFORMATION = "unkown";

	/**
	 * Get the Manifest from the Jar file containing the given class.
	 * 
	 * @param theClass the class the version of the jar should be searched for
	 * @return manifest of the jar, that contains the given class
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static Manifest getManifest(Class<?> theClass) throws IOException {

		// Find the path of the compiled class
		String classPath = theClass.getResource(theClass.getSimpleName() + ".class").toString();

		// Find the path of the lib which includes the class
		if (classPath.lastIndexOf('!') == -1) {
			return null;
		}
		String libPath = classPath.substring(0, classPath.lastIndexOf('!'));
		if (libPath.endsWith("/BOOT-INF/classes")) {
			libPath = libPath.substring(0, libPath.lastIndexOf('!'));
		}
		// Find the path of the file inside the lib jar
		String filePath = libPath + "!/META-INF/MANIFEST.MF";

		// We look at the manifest file, getting three attributes out of it
		return new Manifest(new URL(filePath).openStream());
	}

	/**
	 * Returns the version of the jar that contains the given class. Will return 'unknown' when there is no manifest in the jar.
	 * 
	 * @param theClass the class the version of the jar should be searched for
	 * @return
	 */
	public static String getVersion(Class<?> theClass) {
		try {
			Manifest manifest = getManifest(theClass);
			return getVersion(manifest);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the version 'Implementation-Version' in the given manifest. Will return 'unknown' when there is no manifest.
	 * 
	 * @param manifest manifest of the jar to analyze
	 * @return
	 */
	public static String getVersion(Manifest manifest) {
		if (manifest == null) {
			return UNKOWN_INFORMATION;
		}
		Attributes attr = manifest.getMainAttributes();
		return attr.getValue("Implementation-Version");
	}

	/**
	 * Returns the vendor 'Implementation-Vendor-Id' in the given manifest. Will return 'unknown' when there is no manifest.
	 * 
	 * @param manifest manifest of the jar to analyze
	 * @return
	 */
	public static String getVendor(Manifest manifest) {
		if (manifest == null) {
			return UNKOWN_INFORMATION;
		}
		Attributes attr = manifest.getMainAttributes();
		return attr.getValue("Implementation-Vendor-Id");
	}

	/**
	 * Returns the title 'Implementation-Title' in the given manifest. Will return 'unknown' when there is no manifest.
	 * 
	 * @param manifest manifest of the jar to analyze
	 * @return
	 */
	public static String getTitle(Manifest manifest) {
		if (manifest == null) {
			return UNKOWN_INFORMATION;
		}
		Attributes attr = manifest.getMainAttributes();
		return attr.getValue("Implementation-Title");
	}
}
