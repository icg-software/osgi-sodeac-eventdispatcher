package org.sodeac.eventdispatcher.itest.components.configurable;

import java.util.Arrays;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.sodeac.eventdispatcher.api.IQueueController;
import org.sodeac.eventdispatcher.api.QueueComponentConfiguration;
import org.sodeac.multichainlist.LinkerBuilder;

@Component(service=IQueueController.class)
public class ConfigurableQueueController implements IQueueController
{
	public static final String QUEUE_ID = "configurationtest1";

	@Override
	public List<QueueComponentConfiguration> configureQueueController()
	{
		return Arrays.asList(new QueueComponentConfiguration[] 
		{
			new QueueComponentConfiguration.BoundedByQueueId(QUEUE_ID),
			new QueueComponentConfiguration.ChainDispatcherRuleConfiguration("chain1rule", (event) -> true).setLinksToAdd(LinkerBuilder.newBuilder().linkIntoChain("chain1"))
		});
	}

}
