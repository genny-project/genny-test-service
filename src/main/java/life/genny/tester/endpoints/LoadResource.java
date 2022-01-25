package life.genny.tester.endpoints;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.message.QSearchMessage;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.test.LoadTestJobs;
import life.genny.qwandaq.test.TestJob;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.KeycloakUtils;
import life.genny.tester.live.data.InternalProducer;

public class LoadResource {

	private static final Logger log = Logger.getLogger(LoadResource.class);

	Jsonb jsonBuilder = JsonbBuilder.create();

	@Inject
	InternalProducer internalProducer;
	
	@Inject
	LoadTestJobs jobLoader;
	
	@ConfigProperty(name = "genny.keycloak.url", defaultValue = "https://keycloak.gada.io")
	String baseKeycloakUrl;

	@ConfigProperty(name = "genny.keycloak.realm", defaultValue = "genny")
	String keycloakRealm;

	@ConfigProperty(name = "genny.service.username", defaultValue = "service")
	String serviceUsername;

	@ConfigProperty(name = "genny.service.password", defaultValue = "password")
	String servicePassword;

	@ConfigProperty(name = "genny.oidc.client-id", defaultValue = "backend")
	String clientId;

	@ConfigProperty(name = "genny.oidc.credentials.secret", defaultValue = "secret")
	String secret;

	GennyToken serviceToken;

	BaseEntityUtils beUtils;
	
	@Path("/api/test")
	public Response runTest() {

    	SearchEntity searchBE = new SearchEntity("SBE_PERSONS", "Person Entities")
				// Filters
				.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "PER_%")
				.addFilter("PRI_IS_USER", true)
				.addFilter("PRI_NAME", SearchEntity.StringFilter.LIKE, "Gizem %")
				// Sorts
				.addSort("PRI_CREATED", "Created", SearchEntity.Sort.DESC)
				.setPageSize(20)
				.setPageStart(0)
				// Columns
				.addColumn("PRI_NAME", "Name")
				.addColumn("PRI_EMAIL", "EMAIL");
    	QSearchMessage newSearchMessage = new QSearchMessage(searchBE);
    	newSearchMessage.setDestination("search_data");
    	newSearchMessage.setToken(serviceToken.getToken());

		// Create a new Test Job and add it to jobLoader HashMap
		new TestJob(jobLoader, searchBE.getCode());    	

    	List<TestJob> searches = new ArrayList<>();
        log.info("Running kafka test");
        log.info("Conducting " + searches.size() + " searches");
        
        //search(newSearch);
        
		/*
		 * if(searches.size() > 0) { // For each Search in searches, perform the search
		 * and capture the delta between time sent and time received SearchData current;
		 * for(int i = 0; i < searches.size(); i++) { current = searches.get(i);
		 * searches.set(i, search(current)); }
		 * 
		 * // Get Minimum Search Time across all searches Long minTimeMillis =
		 * Collections.min(searches, new Comparator<SearchData>() { public int
		 * compare(SearchData s1, SearchData s2) { return
		 * s2.duration.compareTo(s1.duration); } }).duration;
		 * log.info("Min Search Time: " + minTimeMillis);
		 * 
		 * // Get average Search Time across all searches Long averageTimeMillis =
		 * searches.stream().mapToLong(o -> o.duration).sum() / searches.size();
		 * log.info("Average Search Time: " + averageTimeMillis);
		 * 
		 * // Get Maximum Search Time across all searches Long maxTimeMillis =
		 * Collections.max(searches, new Comparator<SearchData>() { public int
		 * compare(SearchData s1, SearchData s2) { return
		 * s1.duration.compareTo(s2.duration); } }).duration;
		 * log.info("Max Search Time: " + maxTimeMillis); }
		 */
        return Response.ok().build();
	}
    
    
    /**
     * Perform a single search through Fyodor's kafka messaging system
     * @param search - an {@link SearchData} containing a {@link QSearchMessage} to send to kafka
     * @return {@link SearchData} 
     */
	/*
	 * private SearchData search(SearchData search) { log.info("Searching: " +
	 * search.payload); String serialisedPayload =
	 * jsonBuilder.toJson(search.payload); log.info(serialisedPayload);
	 * Emitter<String> searchEvents = internalProducer.getToSearchEvents();
	 * 
	 * 
	 * Instant start = Instant.now(); // Do Search Here
	 * searchEvents.send(serialisedPayload);
	 * 
	 * 
	 * Instant end = Instant.now();
	 * 
	 * Long delta = Duration.between(start, end).toMillis();
	 * 
	 * log.info("Searched for: " + search.payload); log.info("Delta: " + delta);
	 * search.duration = delta;
	 * 
	 * return search; }
	 */
    
    void onStart(@Observes StartupEvent ev) {
		serviceToken = new KeycloakUtils().getToken(baseKeycloakUrl, keycloakRealm, clientId, secret, serviceUsername, servicePassword, null);

		// Init Utility Objects
		beUtils = new BaseEntityUtils(serviceToken);

		//CacheUtils.init(cache);

		log.info("[*] Finished Startup!");
    }

	
}
