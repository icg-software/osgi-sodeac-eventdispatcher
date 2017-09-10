/*******************************************************************************
 * Copyright (c) 2017 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.sodeac.eventdispatcher.api.IGauge;
import org.sodeac.eventdispatcher.api.IMetrics;

import com.codahale.metrics.Gauge;

public class MetricImpl implements IMetrics
{
	public MetricImpl(QueueImpl queue )
	{
		super();
		this.queue = queue;
		this.queueGauges = new HashMap<String,Gauge<?>>();
	}
	
	private long lastHeartBeat = -1;
	
	private QueueImpl queue = null;
	private Map<String,Gauge<?>> queueGauges = null;
	
	
	@Override
	public long getLastHeartBeat()
	{
		return this.lastHeartBeat;
	}

	@Override
	public void heartBeat()
	{
		this.lastHeartBeat = System.currentTimeMillis();
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public <T> T getGaugeValue(String name, Class<T> type)
	{
		// TODO check synchronize ??
		Gauge gauge = null;
		if((gauge = this.queueGauges.get(name)) != null)
		{
			return (T)gauge.getValue();
		}
		SortedMap<String,Gauge> matches = this.queue.getEventDispatcher().getMetricRegistry().getGauges(new MetricFilterByName(name));
		
		if(matches.isEmpty())
		{
			return null;
		}
		gauge = matches.get(name);
		if(gauge == null)
		{
			return null;
		}
		return (T)gauge.getValue();
	}

	@Override
	public void registerQueueGauge(String name, IGauge<?> gauge)
	{
		if(gauge instanceof Gauge)
		{
			
		}
		else
		{
			// TODO Wrap
		}
		// TODO Auto-generated method stub
		
	}

	
	// TODO register Sling or O
	
}
