package test.app;

import static test.app.optionmonad.None.none;
import static test.app.optionmonad.Some.some;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.SynchronizeOperation;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import test.app.optionmonad.Option;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements IApplication {
	private final String SITE_URLs = "siteURL";
	private static final String JUSTUPDATED = "justUpdated";
	private StatusFactory status = new StatusFactory(Activator.PLUGIN_ID);

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext context) throws Exception {
		Display display = PlatformUI.createDisplay();
		try {
			log(status.info("Starting..."));
			String siteURLs = System.getProperty(SITE_URLs);
			if (null == siteURLs) {
				siteURLs = "";
			}
			if (synchronizeWith(siteURLs)) return IApplication.EXIT_RESTART;
			return IApplication.EXIT_OK;
		} finally {
			display.dispose();
		}
	}

	/**
	 * Synchronize with the specified (space-delimited) set of URLs.
	 * 
	 * @param siteURLs 
	 * @return true if we need to restart
	 * @throws ProvisionException
	 */
	private boolean synchronizeWith(final String siteURLs) throws ProvisionException {
		Option<IProvisioningAgent> maybeAgent = getProvisioningAgent();
		final IProvisioningAgent agent = maybeAgent.getOrThrow(new ProvisionException(maybeAgent.getStatus()));
		
		/*
		 * Check to make sure that we are not restarting after an update. If we
		 * are, then there is no need to check for updates again.
		 */
		final IPreferenceStore prefStore = Activator.getDefault()
				.getPreferenceStore();
		if (prefStore.getBoolean(JUSTUPDATED)) {
			log(status.info("Restarting after update; continuing..."));
			prefStore.setValue(JUSTUPDATED, false);
			return false;
		}

		/*
		 * Create a runnable to execute the update. We'll show a dialog during
		 * the process and then return when the runnable is complete.
		 */
		log(status.info("Kicking off update"));
		final boolean[] restartRequired = new boolean[] { false };
		IRunnableWithProgress runnable = new IRunnableWithProgress() {

			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				try {
					IStatus opStatus = synchronizeWith(agent, monitor, siteURLs);
					if (opStatus.getSeverity() != IStatus.ERROR) {
						prefStore.setValue(JUSTUPDATED, true);
						restartRequired[0] = true;
						log(opStatus);
						log(status.info("Everything looks good here.  Time to restart"));
					} else {
						log(status.info("Ooops, got an error.  Details after the break..."));
						log(opStatus);
					}
				} catch (Exception e) {
					log(status.info("Whaaats up Doc?..."));
					log(status.error(e.getMessage(), e));
				} catch (Throwable t) {
					log(status.info("Something is weeewy scweeewy heeah, Doc..."));
					log(status.error(t.getMessage(), t));
				}
			}
		};

		/*
		 * Execute the runnable and wait for it to complete.
		 */
		try {
			new ProgressMonitorDialog(null).run(true, true, runnable);
			return restartRequired[0];
		} catch (InvocationTargetException e) {
			log(status.error(e.getMessage(), e));
		} catch (InterruptedException e) {
			log(status.error("oops, got interrupted", e));
		}
		return false;
	}

	private void logInstalledFeatures(IProvisioningAgent agent) {
		IProfileRegistry pr = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		IProfile currentProfile = pr.getProfile(IProfileRegistry.SELF);
		IQueryResult<IInstallableUnit> roots = currentProfile.query(new UserVisibleRootQuery(), new NullProgressMonitor());
		Set<IInstallableUnit> ius = roots.toUnmodifiableSet();
		for (IInstallableUnit iu : ius) {
			System.err.println(iu.getId() + '_' + iu.getVersion());
		}
	}

	/*
	 * Needs error checking, cancel checking, etc.
	 */
	protected IStatus synchronizeWith(IProvisioningAgent agent,
			IProgressMonitor monitor, String updateSiteURLs) {
		ProvisioningSession session = new ProvisioningSession(agent);

		// Get update site uris from system argument.
		String siteUrlPackage = System.getProperty(SITE_URLs);
		if (siteUrlPackage == null || siteUrlPackage.isEmpty()) {
			return new Status(Status.ERROR, Activator.PLUGIN_ID,
					"No site URL specified. Pass -DsiteUrl as a system argument.");
		}
		
		log(status.info("Synch repos: " + siteUrlPackage));
		
		String[] siteUrlStrings = siteUrlPackage.split(",");
		URI[] siteURIs = new URI[siteUrlStrings.length];
		for (int i = 0; i < siteURIs.length; i++) {
			try {
				siteURIs[i] = new URI(siteUrlStrings[i]);
			} catch (URISyntaxException e) {
				return status.error(e.getMessage(), e);
			}
		}
		
		// Convert URIs into something we can query...
		Option<IQueryable<IInstallableUnit>> maybeNewIURepos = createIUQueryable(siteURIs, agent, monitor);
		if (!maybeNewIURepos.hasValue()) {
			return maybeNewIURepos.getStatus();
		}
		IQueryable<IInstallableUnit> allTheIUs = maybeNewIURepos.get();
		
		/*
		 * Query the metadata repository(ies) for the feature(s) to install.
		 */
		Collection<IInstallableUnit> toInstall = allTheIUs.query(QueryUtil.createIUGroupQuery(), new NullProgressMonitor()).toUnmodifiableSet();		
		log(status.info("Everything to synchronize:"));
		logQueryResults(toInstall);

		/*
		 * Run the operation modally so that a status can be returned to
		 * the caller.
		 */
		SubMonitor sub = SubMonitor.convert(monitor, "Installing new features...", 200);

		SynchronizeOperation operation = new SynchronizeOperation(session, toInstall);
		IStatus opStatus = operation.resolveModal(sub.newChild(100));
		if (opStatus.isOK()) {
			ProvisioningJob job = operation.getProvisioningJob(null);
			opStatus = job.runModal(sub.newChild(100));
			if (opStatus.getSeverity() == IStatus.CANCEL)
				throw new OperationCanceledException();
		}
		
		logInstalledFeatures(agent);
		return opStatus;
	}

	private void logQueryResults(Collection<IInstallableUnit> toInstall) {
		StringBuffer iuNames = new StringBuffer("IUs:\n\n ");
		for (IInstallableUnit iu : toInstall) {
			iuNames.append(iu.getId() + iu.getVersion() + "\n ");
		}
		log(status.info(iuNames.toString()));
	}

	private Option<IQueryable<IInstallableUnit>> createIUQueryable(URI[] p2Sites, IProvisioningAgent agent, IProgressMonitor monitor) {
		IMetadataRepositoryManager metadataManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);

		Collection<IMetadataRepository> metadataReposList = new LinkedList<IMetadataRepository>();
		for (URI uri : p2Sites) {
			log(status.info("Adding metadata repo at: " + uri.toString()));
			try {
				IMetadataRepository metadataRepo = metadataManager.loadRepository(uri, monitor);
				artifactManager.loadRepository(uri, monitor); // load the repo: this makes it available to us
				metadataReposList.add(metadataRepo);
			} catch (ProvisionException e) {
				return none(status.error(e.getMessage(), e));
			} catch (OperationCanceledException e) {
				return none(status.error(e.getMessage(), e));
			}
		}
		
		return some(QueryUtil.compoundQueryable(metadataReposList));
	}

	private Option<IProvisioningAgent> getProvisioningAgent() {
		BundleContext context = Activator.getContext();
		ServiceReference serviceReference = context.getServiceReference(IProvisioningAgentProvider.SERVICE_NAME);
		if (serviceReference == null) {
			return none(status.error("No service reference found.  This application is not set up for updates.", new RuntimeException()));
		}
		IProvisioningAgentProvider agentProvider = (IProvisioningAgentProvider) context.getService(serviceReference);
		IProvisioningAgent agent = null;
		try {
			agent = agentProvider.createAgent(null); 	// The URI here is the site.  "null" means, use the running system.
		} catch (ProvisionException e) {
			return none(status.error(e.getMessage(), e));
		}
		return some(agent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
		if (!PlatformUI.isWorkbenchRunning())
			return;
		final IWorkbench workbench = PlatformUI.getWorkbench();
		final Display display = workbench.getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				if (!display.isDisposed())
					workbench.close();
			}
		});
	}

	private void log(IStatus status) {
		Activator.getDefault().getLog().log(status);
	}
}
