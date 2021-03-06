package test;

import java.util.List;

public class TestSynchronizeOperation extends IntegrationTestCase {
	// Default paths.  Change this to where you exported the test product.
	private static String FIXTURE_LOCATION = "c:/temp/test.app";
	private static String FIXTURE_LOCATION_BIN = FIXTURE_LOCATION + "/eclipse";
	private static String FIXTURE_LOCATION_REPO = "\"file:///" + FIXTURE_LOCATION + "/repository ";

	// The variables we use to find the test product during a Maven/Tycho build.
	private static final String FIXTURE_LOCATION_PROP = "osgi.install.area";
	private static final String RELATIVE_LOCATION_WIN32 = "test.app.product/target/products/test.app.product/win32/win32/x86";
	private static final String RELATIVE_LOCATION_WIN32_64 = "test.app.product/target/products/test.app.product/win32/win32/x86_64";
	private static final String RELATIVE_LOCATION_LINUX = "test.app.product/target/products/test.app.product/linux/gtk/x86";

	// This assumes that the whole workspace is located inside a Jetty or similar web root
	public static final String FIXTURE_ROOT = "http://localhost:8080/";
	protected static final String SITE_BASE = FIXTURE_ROOT + "payload.site";
//	protected static String SITE400_FIXTURE = SITE_BASE + "_4.0.0";
//	protected static String SITE401_FIXTURE = SITE_BASE + "_4.0.1";
//	protected static String SITE500_FIXTURE = SITE_BASE + "_5.0.0";
	// convention: Base repo is the 0th item, additional repos after that
	protected static String SITE400_FIXTURE = FIXTURE_LOCATION_REPO + ',' + SITE_BASE + "_4.0.0";
	protected static String SITE401_FIXTURE = FIXTURE_LOCATION_REPO + ',' + SITE_BASE + "_4.0.1";
	protected static String SITE500_FIXTURE = FIXTURE_LOCATION_REPO + ',' + SITE_BASE + "_5.0.0";
	
	private void setPaths() {
		/*
		 * If we're running from within Maven, then recalculate the paths.  Otherwise just use the hard-coded
		 * values above.
		 */
		String systemPropertyFixtureLocation = System.getProperty(FIXTURE_LOCATION_PROP);
		if (null == systemPropertyFixtureLocation || "".equals(systemPropertyFixtureLocation)) {
			System.err.println("NOTFOUND: " + FIXTURE_LOCATION_PROP + " env var; using default: " + FIXTURE_LOCATION);
			return;
		}
		systemPropertyFixtureLocation = systemPropertyFixtureLocation.substring("file:".length());
		boolean win32 = false;
		if (systemPropertyFixtureLocation.indexOf(":") > 0) {
		    systemPropertyFixtureLocation = systemPropertyFixtureLocation.substring(1);
		    win32 = true;
		}
		systemPropertyFixtureLocation = systemPropertyFixtureLocation.substring(0, 
				systemPropertyFixtureLocation.length() - "test/target/work/".length());
		System.err.println("ROOT : " + systemPropertyFixtureLocation);
		if (win32) {
			String word_size= System.getProperty("sun.arch.data.model");
			if ("32".equals(word_size)) {
				FIXTURE_LOCATION = systemPropertyFixtureLocation + RELATIVE_LOCATION_WIN32;
			} else {
				FIXTURE_LOCATION = systemPropertyFixtureLocation + RELATIVE_LOCATION_WIN32_64;
			}
		} else {
		    FIXTURE_LOCATION = systemPropertyFixtureLocation + RELATIVE_LOCATION_LINUX;
		}
		System.err.println("FOUND:  " + systemPropertyFixtureLocation);
		FIXTURE_LOCATION_BIN = FIXTURE_LOCATION;
		FIXTURE_LOCATION_REPO = "file:///" + systemPropertyFixtureLocation + "test.app.product/target/repository";
		
//		SITE400_FIXTURE = SITE_BASE + "_4.0.0";
//		SITE401_FIXTURE = SITE_BASE + "_4.0.1";
//		SITE500_FIXTURE = SITE_BASE + "_5.0.0";
		SITE400_FIXTURE = FIXTURE_LOCATION_REPO + ',' + SITE_BASE + "_4.0.0";
		SITE401_FIXTURE = FIXTURE_LOCATION_REPO + ',' + SITE_BASE + "_4.0.1";
		SITE500_FIXTURE = FIXTURE_LOCATION_REPO + ',' + SITE_BASE + "_5.0.0";
	}
	
	public void test_InstallSomething() throws Exception {
		setPaths();
		
		//Add 4.0.0 version
		List<String> features = installFromFixturesReturnInstalledFeatures(SITE400_FIXTURE, FIXTURE_LOCATION_BIN);
		System.out.println(features.toString());
		assertEquals("v4.0.0 installed", 1, countOccurancesOfListItemsStartingWithPrefix(features, "payload.feature.feature.group_4.0.0"));

		//Update to 5.0.0
		features = installFromFixturesReturnInstalledFeatures(SITE500_FIXTURE, FIXTURE_LOCATION_BIN);
		System.out.println(features.toString());
		assertEquals("v4.0.0 no longer installed", 0, countOccurancesOfListItemsStartingWithPrefix(features, "payload.feature.feature.group_4.0.0"));
		assertEquals("v5.0.0 installed", 1, countOccurancesOfListItemsStartingWithPrefix(features, "payload.feature.feature.group_5.0.0"));
		
		//Downgrade to 4.0.0
		features = installFromFixturesReturnInstalledFeatures(SITE400_FIXTURE, FIXTURE_LOCATION_BIN);
		System.out.println(features.toString());
		assertEquals("v4.0.0 installed", 1, countOccurancesOfListItemsStartingWithPrefix(features, "payload.feature.feature.group_4.0.0"));
		assertEquals("v5.0.0 no longer installed", 0, countOccurancesOfListItemsStartingWithPrefix(features, "payload.feature.feature.group_5.0.0"));
		
		//Remove all payload
		features = installFromFixturesReturnInstalledFeatures(FIXTURE_LOCATION_REPO, FIXTURE_LOCATION_BIN);
		System.out.println(features.toString());
		assertEquals("No payload installed", 0, countOccurancesOfListItemsStartingWithPrefix(features, "payload.feature.feature.group"));
	}
}
