package life.genny.tester.live.data;

import java.time.Instant;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.persistence.EntityManager;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import life.genny.qwandaq.CodedEntity;
import life.genny.qwandaq.data.GennyCache;
import life.genny.qwandaq.message.QBulkMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.message.QSearchMessage;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.test.LoadTestJobs;
import life.genny.qwandaq.test.TestJob;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.KeycloakUtils;

@ApplicationScoped
public class InternalConsumer {

	private static final Logger log = Logger.getLogger(InternalConsumer.class);

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

	@Inject
	EntityManager entityManager;

	@Inject
	InternalProducer producer;

	@Inject
	LoadTestJobs jobLoader;
	
	@Inject 
	GennyCache cache;

	GennyToken serviceToken;

	BaseEntityUtils beUtils;

    void onStart(@Observes StartupEvent ev) {
		serviceToken = new KeycloakUtils().getToken(baseKeycloakUrl, keycloakRealm, clientId, secret, serviceUsername, servicePassword, null);

		// Init Utility Objects
		beUtils = new BaseEntityUtils(serviceToken);

		//CacheUtils.init(cache);

		log.info("[*] Finished Startup!");
    }

    /**
     * onConsume: set end Instant in relevant {@link TestJob}
     * @param searchData - {@link QSearchMessage} payload
     */
    @Incoming("search_data")
    public void getTestData(String searchData) {
    	log.info("Received incoming test data... ");
		log.debug(searchData);

		JsonbConfig config = new JsonbConfig();
		Jsonb jsonb = JsonbBuilder.create(config);
		QBulkMessage bulkMSG = jsonb.fromJson(searchData, QBulkMessage.class);
		
	
		// Deserialize with null values to avoid deserialisation errors
		GennyToken userToken = new GennyToken(bulkMSG.getToken());
		CodedEntity searchBE = null;
		for(QDataBaseEntityMessage dbMsg : bulkMSG.getMessages()) {
			if(dbMsg.getItems().length == 1) {
				if(dbMsg.getItems()[0].getCode().startsWith("SBE_")) {
					searchBE = dbMsg.getItems()[0];
				}
			}
		}
		
		if(searchBE == null) {
			log.error("SearchEntity is null !");
			return;
		}
		
		String jobUuid = searchBE.getCode();
		TestJob job = jobLoader.getJob(jobUuid);
		if(job == null) {
			log.error("Could not find job: " + jobUuid);
			return;
		}
		job.setEnd(Instant.now());
    }
}
