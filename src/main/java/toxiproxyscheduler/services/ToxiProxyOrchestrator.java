package toxiproxyscheduler.services;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import toxiproxyscheduler.ProxyMeta;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ToxiProxyOrchestrator {

	private static final Logger log = LoggerFactory.getLogger(ToxiProxyOrchestrator.class);
	private static final Duration TIMEOUT = Duration.ofSeconds(30);

	private final DockerClient dockerClient;
	private final Map<String, Instant> proxyContainers;

	public ToxiProxyOrchestrator() {
		try {
			this.dockerClient = DefaultDockerClient.fromEnv().build();
		} catch (DockerCertificateException e) {
			throw new RuntimeException(e);
		}
		pullLatestToxiProxy();
		this.proxyContainers = new ConcurrentHashMap<>();
		addShutdownHook();
	}

	private void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> proxyContainers.forEach((k, v) -> destroyContainer(k))));
	}

	private void destroyContainer(String id) {
		try {
			dockerClient.stopContainer(id, 10);
		} catch (DockerException | InterruptedException e) {
			log.error("Failed to stop the container {}", id, e);
		}

		try {
			dockerClient.removeContainer(id);
		} catch (DockerException | InterruptedException e) {
			log.error("Failed to remove the container {}", id, e);
		}
		log.info("Proxy container is destroyed {}", id);
	}

	private void pullLatestToxiProxy() {
		try {
			dockerClient.pull("shopify/toxiproxy:2.1.4");
		} catch (DockerException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Scheduled(fixedRate = 30000)
	public void keepAliveCheck() {
		proxyContainers.forEach((id, lastTouch) -> {
			if (Duration.between(lastTouch, Instant.now()).compareTo(TIMEOUT) < 0) {
				return;
			}
			log.info("Proxy container didn't get keep alived within the timeout {}", id);
			destroyContainer(id);
			proxyContainers.remove(id);
		});

	}

	public ProxyMeta startProxy() {
		try {
			// Bind container port 443 to an automatically allocated available host port.
			Map<String, List<PortBinding>> portBindings = new HashMap<>();

			String exposedPort = "8474/tcp";
			List<PortBinding> randomPort = new ArrayList<>();
			randomPort.add(PortBinding.randomPort("0.0.0.0"));
			portBindings.put(exposedPort, randomPort);

			HostConfig hostConfig = HostConfig.builder().portBindings(portBindings).build();
			// Create container with exposed ports
			ContainerConfig containerConfig = ContainerConfig.builder()
					.image("shopify/toxiproxy:2.1.4")
					.hostConfig(hostConfig)
					.build();

			ContainerCreation creation = dockerClient.createContainer(containerConfig);
			String id = creation.id();
			log.info("Proxy is created in the docker container with id {}", id);
			proxyContainers.put(id, Instant.now());
			dockerClient.startContainer(id);
			NetworkSettings networkSettings = dockerClient.inspectContainer(id).networkSettings();
			return new ProxyMeta(id, networkSettings.ports().get(exposedPort).get(0).hostPort());
		} catch (DockerException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void touch(String containerId) {
		proxyContainers.computeIfPresent(containerId, (k, v) -> Instant.now());
	}

	public void destroy(String containerId) {
		if (proxyContainers.remove(containerId) == null) {
			return;
		}
		destroyContainer(containerId);
	}
}
