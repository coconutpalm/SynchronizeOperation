package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import junit.framework.TestCase;

public class IntegrationTestCase extends TestCase {

	// Names of launchers: Maven / Eclipse IDE defaults respectively
	private static final String LAUNCHER_EXE = "eclipse";
	private static final String ECLIPSE_EXE = "eclipse.exe";
	
	private final String SITE_URL_PROPERTY = "siteURL";

	protected List<String> installFromFixturesReturnInstalledFeatures(String fixture, String testHarnessPath)
			throws Exception {

		List<String> features;		

		Runtime runtime = Runtime.getRuntime();
		String execTargetString = testHarnessPath + File.separator + LAUNCHER_EXE;
		if(!(new File(execTargetString).exists())){
			execTargetString = testHarnessPath + File.separator + ECLIPSE_EXE;
		}
		// For remote debugging use the version including -Xdebug...
//		execTargetString += " -vmargs -D" + SITE_URL_PROPERTY + "=" + fixture  + " -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=y"; 
		execTargetString += " -vmargs -D" + SITE_URL_PROPERTY + "=" + fixture;
		
		System.out.println(execTargetString);
		
		Process p = runtime.exec(execTargetString, null, new File(testHarnessPath));
		Thread worker = new Thread(new StreamGobbler(p.getInputStream(), "STDOUT"));
		worker.setDaemon(true);
		worker.start();
		
		ExecutorService executor = Executors.newFixedThreadPool(1);
		Callable<List<String>> stderrCallable = new StreamGobbler(p.getErrorStream(), "STDERR");
		Future<List<String>> configuredFeaturesFuture = executor.submit(stderrCallable);

		features = configuredFeaturesFuture.get();
		return features;
	}

	protected int countOccurancesOfListItemsStartingWithPrefix(List<String> list, String prefix) {
		int count = 0;
		for (String string : list) {
			if (string.startsWith(prefix))
				count++;
		}
		return count;
	}

	protected List<String> installFromUpdateSiteFixtureAndCheckFeatureInstalledOK(String fixture, String featureName,
		String productExeDirectory) throws Exception {

		List<String> features = installFromFixturesReturnInstalledFeatures(fixture, productExeDirectory);

		assertEquals("Features are: [" + features + "]", 1, 
				countOccurancesOfListItemsStartingWithPrefix(features, featureName));

		return features;
	}
	
	/**
	 * A Runnable that @throws Exception for use in tests
	 */
	public interface TestRunnable {
		void run() throws Exception;
	}
	
	protected void assertFeatures(List<String> features, TestRunnable runnable) throws Exception {
		try {
			runnable.run();
		} catch (Throwable e) {
			String result = "Features: [";
			for (String feature : features) {
				result += feature + " ";
			}
			result += "]: \n" + e.getMessage();
			throw new RuntimeException(result, e);
		}
	}

	class StreamGobbler implements Callable<List<String>>, Runnable {
	    InputStream is;
	    String type;

	    StreamGobbler(InputStream is, String type) {
			this.is = is;
			this.type = type;
		}

	    public void run() {
			try {
				call();
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		public List<String> call() throws Exception {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				LinkedList<String> result = new LinkedList<String>();
				String line = null;
				while ((line = br.readLine()) != null) {
					System.out.println(type + ">" + line);
					result.addLast(line);
				}
				return result;
			} catch (IOException ioe) {
				ioe.printStackTrace();
				throw new RuntimeException(ioe.getMessage(), ioe);
			}
		}
	}

	public void testToPreventFailuresForNoTestsInTheClass()
	{
	}

}
