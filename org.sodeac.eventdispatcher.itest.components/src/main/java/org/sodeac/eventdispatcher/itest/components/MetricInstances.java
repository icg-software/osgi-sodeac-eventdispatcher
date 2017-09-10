package org.sodeac.eventdispatcher.itest.components;

import org.apache.sling.commons.metrics.MetricsService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.codahale.metrics.MetricRegistry;

@Component(immediate=true,service=MetricInstances.class)
public class MetricInstances
{
	@Reference(cardinality=ReferenceCardinality.MANDATORY,policy=ReferencePolicy.STATIC,target = "(name=sling)")
	protected volatile MetricRegistry metricRegistry;
	
	@Reference(cardinality=ReferenceCardinality.MANDATORY,policy=ReferencePolicy.STATIC )
	protected volatile MetricsService metricsService;
	
	public MetricRegistry getMetricRegistry()
	{
		return this.metricRegistry;
	}

	public MetricsService getMetricsService()
	{
		return this.metricsService;
	}
	
}
