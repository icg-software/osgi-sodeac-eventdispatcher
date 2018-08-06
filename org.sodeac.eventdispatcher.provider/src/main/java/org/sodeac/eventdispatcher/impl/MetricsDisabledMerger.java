/*******************************************************************************
 * Copyright (c) 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.impl;

import java.util.HashSet;
import java.util.Set;

import org.sodeac.eventdispatcher.api.MetricsRequirement;
import org.sodeac.eventdispatcher.api.QueueComponentConfiguration;

public class MetricsDisabledMerger
{
	private QueueImpl queue = null;
	int countPreferMetrics = 0;
	int countPreferNoMetrics = 0;
	int countRequireMetrics = 0;
	
	protected MetricsDisabledMerger(QueueImpl queue, ControllerContainer remove, ControllerContainer add)
	{
		super();
		this.queue = queue;
		this.analyse(remove, add);
	}
	
	private void analyse(ControllerContainer remove, ControllerContainer add)
	{
		Set<QueueComponentConfiguration> processed = new HashSet<QueueComponentConfiguration>();
		for(ControllerContainer controllerContainer : queue.getConfigurationList())
		{
			if(controllerContainer == remove)
			{
				continue;
			}
			if(controllerContainer == add)
			{
				continue;
			}
			
			if(controllerContainer.getBoundByIdList() != null)
			{
				for(QueueComponentConfiguration.BoundedByQueueId boundedById : controllerContainer.getBoundByIdList())
				{
					if(processed.contains(boundedById))
					{
						continue;
					}
					processed.add(boundedById);
					
					if(boundedById.getQueueMetricsRequirement() == MetricsRequirement.RequireMetrics)
					{
						this.countRequireMetrics++;
					}
					else if(boundedById.getQueueMetricsRequirement() == MetricsRequirement.PreferMetrics)
					{
						countPreferMetrics++;
					}
					else if(boundedById.getQueueMetricsRequirement() == MetricsRequirement.PreferNoMetrics)
					{
						countPreferNoMetrics++;
					}
				}
			}
			if(controllerContainer.getBoundedByQueueConfigurationList() != null)
			{
				for(QueueComponentConfiguration.BoundedByQueueConfiguration boundedByConfiguration : controllerContainer.getBoundedByQueueConfigurationList())
				{
					if(processed.contains(boundedByConfiguration))
					{
						continue;
					}
					processed.add(boundedByConfiguration);
					
					if(boundedByConfiguration.getQueueMetricsRequirement() == MetricsRequirement.RequireMetrics)
					{
						this.countRequireMetrics++;
					}
					else if(boundedByConfiguration.getQueueMetricsRequirement() == MetricsRequirement.PreferMetrics)
					{
						countPreferMetrics++;
					}
					else if(boundedByConfiguration.getQueueMetricsRequirement() == MetricsRequirement.PreferNoMetrics)
					{
						countPreferNoMetrics++;
					}
				}
			}
		}
		
		if(add != null)
		{
			if(add.getBoundByIdList() != null)
			{
				for(QueueComponentConfiguration.BoundedByQueueId boundedById : add.getBoundByIdList())
				{
					if(processed.contains(boundedById))
					{
						continue;
					}
					processed.add(boundedById);
					
					if(boundedById.getQueueMetricsRequirement() == MetricsRequirement.RequireMetrics)
					{
						this.countRequireMetrics++;
					}
					else if(boundedById.getQueueMetricsRequirement() == MetricsRequirement.PreferMetrics)
					{
						countPreferMetrics++;
					}
					else if(boundedById.getQueueMetricsRequirement() == MetricsRequirement.PreferNoMetrics)
					{
						countPreferNoMetrics++;
					}
				}
			}
			if(add.getBoundedByQueueConfigurationList() != null)
			{
				for(QueueComponentConfiguration.BoundedByQueueConfiguration boundedByConfiguration : add.getBoundedByQueueConfigurationList())
				{
					if(processed.contains(boundedByConfiguration))
					{
						continue;
					}
					processed.add(boundedByConfiguration);
					
					if(boundedByConfiguration.getQueueMetricsRequirement() == MetricsRequirement.RequireMetrics)
					{
						this.countRequireMetrics++;
					}
					else if(boundedByConfiguration.getQueueMetricsRequirement() == MetricsRequirement.PreferMetrics)
					{
						countPreferMetrics++;
					}
					else if(boundedByConfiguration.getQueueMetricsRequirement() == MetricsRequirement.PreferNoMetrics)
					{
						countPreferNoMetrics++;
					}
				}
			}
		}
		
		// TODO Queue Services
	}
	
	protected boolean disableMetrics()
	{
		if(this.countRequireMetrics > 0)
		{
			return false;
		}
		return this.countPreferNoMetrics > this.countPreferMetrics;
	}
}
