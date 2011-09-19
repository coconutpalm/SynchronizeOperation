package test.app;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class StatusFactory {
	private String bundleId = "";
	
	public StatusFactory(String bundleId) {
		this.bundleId = bundleId;
	}

	public IStatus error(String message, Throwable e) {
		return new Status(Status.ERROR, bundleId, message, e);
	}

	public IStatus warning(String message) {
		return new Status(Status.WARNING, bundleId, message);
	}
	
	public IStatus info(String message) {
		return new Status(Status.INFO, bundleId, message);
	}
}
