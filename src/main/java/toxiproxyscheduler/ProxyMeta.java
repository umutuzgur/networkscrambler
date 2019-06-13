package toxiproxyscheduler;

public class ProxyMeta {

	private final String containerId;
	private final String port;

	public ProxyMeta(String containerId, String port) {
		this.containerId = containerId;
		this.port = port;
	}

	public String getContainerId() {
		return containerId;
	}

	public String getPort() {
		return port;
	}
}
