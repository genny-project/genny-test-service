package life.genny.tester.endpoints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import life.genny.qwandaq.test.LoadTestJobs;
import life.genny.qwandaq.test.TestJob;
import life.genny.tester.models.TestMetrics;
import life.genny.tester.utils.GenericMessage;

@Path("/api/metrics")
public class MetricResource {
	private static final Logger log = Logger.getLogger(MetricResource.class);
	
	@Inject
	TesterResource testerResource;
	
	@Inject
	LoadTestJobs jobLoader;
	
	@GET
	@Path("/status")
	public Response getStatus() {
		return Response.ok().entity("Connected").build();
	}

	@GET
	@Path("/{code}")
	public Response getSingleTestMetrics(@PathParam("code") final String code) {
		TestJob job = jobLoader.getJob(code);
		if(job == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		
		return Response.status(Response.Status.OK).entity(job.getDuration()).build();
	}
	
	@GET
	@Path("/summary")
	public Response getTestMetrics() {  
		log.info("Getting summary from: " + jobLoader.hashCode());
		Collection<TestJob> jobs = jobLoader.getJobs().values();
		log.info("Found " + jobs.size() + " jobs");
		if(jobs.size() == 0) {
			log.info("No Tests Run");
			return Response.noContent().build();
		}
		
		TestMetrics metrics = getMetrics(jobs);
		return Response.ok().entity(metrics).build();
	}
	
	@POST
	@Path("/summary")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTestMetricsBatch(String[] jobCodes) {
		List<TestJob> jobs = new ArrayList<TestJob>();

		// Add each job in the job codes array to the list
		for(String jobCode : jobCodes) {
			TestJob job = jobLoader.getJob(jobCode);
			if(job == null)
				log.error("Could not find job: " + jobCode);
			
			jobs.add(job);
		}
		
		TestMetrics metrics = getMetrics(jobs);
		return Response.status(Response.Status.OK).entity(metrics).build();
	}
	
	@PUT
	@Path("/clear")
	public Response clearTestMetrics() {
		int jobCount = jobLoader.getJobs().size();
		jobLoader.getJobs().clear();
		log.info("Cleared " + jobCount + " test jobs");
		return Response.ok().build();
	}
	
	@GET
	@Path("/max")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMaxTime() {
		Collection<TestJob> jobs = jobLoader.getJobs().values();
		TestJob maxTimeJob = Collections.max(jobs, new Comparator<TestJob>() {
			public int compare(TestJob t1, TestJob t2) {
				return t1.getDuration().compareTo(t2.getDuration());
			}
		});
		
		return Response.ok().entity(maxTimeJob).build();
	}
	
	@GET
	@Path("/min")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMinTime() {
		Collection<TestJob> jobs = jobLoader.getJobs().values();
		TestJob minTimeJob = Collections.min(jobs, new Comparator<TestJob>() {
			public int compare(TestJob t1, TestJob t2) {
				return t1.getDuration().compareTo(t2.getDuration());
			}
		});
		
		return Response.ok().entity(minTimeJob).build();
	}
	
	@GET
	@Path("/output/json/{code}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSearchJson(final @PathParam("code") String jobCode) {
		Response jobResponse = getSpecificJob(jobCode);
		if(jobResponse.getStatus() != Response.Status.OK.getStatusCode())
			return jobResponse;
		
		TestJob job = (TestJob)jobResponse.getEntity();
		
		return Response.status(Response.Status.OK).entity(job.getSearchJSON()).build();
	}

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
	
	@POST
	@Path("/output")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSpecificJobs(final String[] jobCodes) {
		List<TestJob> jobs = new ArrayList<TestJob>();
		
		// Add each job in the job codes array to the list
		for(String jobCode : jobCodes) {
			TestJob job = jobLoader.getJob(jobCode);
			if(job == null)
				log.error("Could not find job: " + jobCode);
			
			jobs.add(job);
		}
		
		return Response.status(Response.Status.OK).entity(jobs).build();
	}
	
	@GET
	@Path("/output/{count}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response printXJobs(final @PathParam("count") long maxCount) {
		
		List<TestJob> strings = new ArrayList<>();
		long count = 0;
		for(TestJob job : jobLoader.getJobs().values()) {
			if(count < maxCount) {
				strings.add(job);
				log.info(job.toString());
				count++;
			} else
				break;
		}
		
		return Response.ok().entity(new GenericMessage<TestJob>(strings)).build();
	}
	
	private TestMetrics getMetrics(Collection<TestJob> jobs) {
		// Get Minimum Search Time across all searches
		Long minTimeMillis = Collections.min(jobs, new Comparator<TestJob>() {
			public int compare(TestJob t1, TestJob t2) {
				return t1.getDuration().compareTo(t2.getDuration());
			}
		}).getDuration();
		
		log.info("Min Search Time: " + minTimeMillis);

		// Get average Search Time across all searches
		Long averageTimeMillis = jobs.stream().mapToLong(o -> o.getDuration()).sum() / jobs.size();
		
		log.info("Average Search Time: " + averageTimeMillis);

		// Get Maximum Search Time across all searches
		Long maxTimeMillis = Collections.max(jobs, new Comparator<TestJob>() {
			public int compare(TestJob t1, TestJob t2) {
				return t1.getDuration().compareTo(t2.getDuration());
			}
		}).getDuration();
		log.info("Max Search Time: " + maxTimeMillis);
		
		return new TestMetrics(minTimeMillis, averageTimeMillis, maxTimeMillis, jobs.size());
	}
	
}
