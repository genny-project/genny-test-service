package life.genny.tester.models;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class TestMetrics {
	public final Long minTime;
	public final Long aveTime;
	public final Long maxTime;
	public final Integer searchesPerformed;
	
	public TestMetrics(Long minTime, Long aveTime, Long maxTime, Integer searchesPerformed) {
		this.minTime = minTime;
		this.aveTime = aveTime;
		this.maxTime = maxTime;
		
		this.searchesPerformed = searchesPerformed;
	}
}