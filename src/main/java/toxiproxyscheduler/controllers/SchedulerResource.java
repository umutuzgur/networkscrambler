package toxiproxyscheduler.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import toxiproxyscheduler.ProxyMeta;
import toxiproxyscheduler.services.ToxiProxyOrchestrator;

@RestController
@RequestMapping("/v1/proxy")
public class SchedulerResource {

	private final ToxiProxyOrchestrator toxiProxyOrchestrator;

	@Autowired
	public SchedulerResource(ToxiProxyOrchestrator toxiProxyOrchestrator) {
		this.toxiProxyOrchestrator = toxiProxyOrchestrator;
	}

	@PostMapping(value = "/start", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public ProxyMeta startProxyContainer() {
		return toxiProxyOrchestrator.startProxy();
	}

	@PostMapping(value = "/touch", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public void startProxyContainer(@RequestParam(value = "id") String containerId) {
		toxiProxyOrchestrator.touch(containerId);
	}

	@DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public void destroyProxyContainer(@RequestParam(value = "id") String containerId) {
		toxiProxyOrchestrator.destroy(containerId);
	}

}
