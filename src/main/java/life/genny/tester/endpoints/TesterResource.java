package life.genny.tester.endpoints;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.runtime.StartupEvent;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.message.QSearchMessage;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.test.LoadTestJobs;
import life.genny.qwandaq.test.TestJob;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.KeycloakUtils;
import life.genny.tester.live.data.InternalProducer;
import life.genny.tester.utils.GenericMessage;
import life.genny.tester.utils.SearchGenerator;

@Path("/api/test")
public class TesterResource {

	private static final Logger log = Logger.getLogger(TesterResource.class);

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

	@GET
	@Path("/{code}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSpecificJob(final @PathParam("code") String jobCode) {
		log.info("Looking for job: " + jobCode);
		TestJob job = jobLoader.getJob(jobCode);
		if(job == null)
			return Response.status(Response.Status.NOT_FOUND).entity("Could not find job: " + jobCode).build();
		
		// Remove the token from the json before sending it off
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = null;
		try {
			node = (ObjectNode)mapper.readTree(job.getSearchJSON());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		node.remove("token");
		
		return Response.status(Response.Status.OK).entity(node.toString()).build();
	}
	
	@GET
	@Path("random/{count}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response runRandomConsecutiveTests(final @PathParam("count") long count) {
		SearchEntity testSearch = SearchGenerator.generateRandomNameSearch();
		String sourceCode = testSearch.getCode();
		List<String> jobCodes = new ArrayList<String>();
		for(long i = 0; i < count; i++) {
			testSearch.setCode(sourceCode);
			jobCodes.add(search(testSearch).getCode());
		}
		
		return Response.status(Response.Status.OK).entity(jobCodes).build();
	}
	
	@GET
	@Path("random")
	@Produces(MediaType.APPLICATION_JSON)
	public Response runRandomTest() {
		SearchEntity randomSearch = SearchGenerator.generateRandomNameSearch();
		TestJob job = search(randomSearch);
		log.info("Sending out: " + job.toString(true));
		return Response.ok().entity(job.getCode()).build();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("multi/random/{count}")
	public Response runMultipleRandomTests(@PathParam("count") final long count) {
		List<SearchEntity> searches = SearchGenerator.generateRandomNameSearches(count);
		List<String> jobCodes = new ArrayList<String>();
		for(SearchEntity search : searches) {
			String jobCode = search(search).getCode();
			jobCodes.add(jobCode);
		}
		
		log.info("Sent " + searches.size() + " requests");		
		return Response.ok().entity(new GenericMessage<String>(jobCodes)).build();
	}

	@GET
	@Path("single")
	public Response runSingleTest() {
		SearchEntity searchBE = new SearchEntity(SearchGenerator.SEARCH_PREFIX + "PERSONS", "Person Entities")
				// Filters
				.addFilter("PRI_IS_USER", true)
				.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "PER_%")
				.addFilter("PRI_NAME", SearchEntity.StringFilter.LIKE, "Gizem %")
				// Sorts
				.addSort("PRI_CREATED", "Created", SearchEntity.Sort.DESC)
				// Columns
				.addColumn("PRI_NAME", "Name")
				.addColumn("PRI_EMAIL", "EMAIL")
				.setPageSize(20)
				.setPageStart(0);

		// Create a new Test Job and add it to jobLoader HashMap
		TestJob currentJob = search(searchBE);
		return Response.ok().entity(currentJob.getCode()).build();
	}
    
    /**
     * Perform a single search through Fyodor's kafka messaging system
     * @param qSearch - an {@link SearchData} containing a {@link QSearchMessage} to send to kafka
     * @return {@link SearchData} 
     */
	private TestJob search(SearchEntity searchBE) {
    	QSearchMessage newSearchMessage = new QSearchMessage(searchBE);
    	newSearchMessage.setDestination("search_data");
    	newSearchMessage.setToken(serviceToken.getToken());

		TestJob job = new TestJob(jobLoader, newSearchMessage.getSearchEntity());
		internalProducer.getToSearchEvents().send(job.getSearchJSON());
		
		return job;
	}

    void onStart(@Observes StartupEvent ev) {
		serviceToken = new KeycloakUtils().getToken(baseKeycloakUrl, keycloakRealm, clientId, secret, serviceUsername, servicePassword, null);

		// Init Utility Objects
		beUtils = new BaseEntityUtils(serviceToken);

		//CacheUtils.init(cache);

		log.info("[*] Finished Startup!");
    }
}
