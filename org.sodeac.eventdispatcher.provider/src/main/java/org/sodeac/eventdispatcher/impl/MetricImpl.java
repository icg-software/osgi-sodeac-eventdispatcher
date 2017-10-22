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
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.sodeac.eventdispatcher.api.ICounter;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IGauge;
import org.sodeac.eventdispatcher.api.IHistogram;
import org.sodeac.eventdispatcher.api.IMeter;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.ITimer;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

public class MetricImpl implements IMetrics
{
	public MetricImpl(QueueImpl queue, IPropertyBlock qualityValues, String jobId)
	{
		super();
		this.queue = queue;
		this.dispatcher = (EventDispatcherImpl)queue.getEventDispatcher();
		this.gaugeIndex = new HashMap<String,IGauge<?>>();
		this.meterIndex = new HashMap<String,IMeter>();
		this.timerIndex = new HashMap<String,ITimer>();
		this.counterIndex = new HashMap<String,ICounter>();
		this.histogramIndex = new HashMap<String,IHistogram>();
		this.qualityValues = qualityValues;
		this.jobId = jobId;
		
		this.metricsLock = new ReentrantReadWriteLock(true);
		this.metricsReadLock = this.metricsLock.readLock();
		this.metricsWriteLock = this.metricsLock.writeLock();
	}
	
	public MetricImpl(EventDispatcherImpl dispatcher, IPropertyBlock qualityValues)
	{
		super();
		this.queue = null;
		this.dispatcher = dispatcher;
		this.gaugeIndex = new HashMap<String,IGauge<?>>();
		this.meterIndex = new HashMap<String,IMeter>();
		this.timerIndex = new HashMap<String,ITimer>();
		this.counterIndex = new HashMap<String,ICounter>();
		this.histogramIndex = new HashMap<String,IHistogram>();
		this.qualityValues = qualityValues;
		this.jobId = null;
		
		this.metricsLock = new ReentrantReadWriteLock(true);
		this.metricsReadLock = this.metricsLock.readLock();
		this.metricsWriteLock = this.metricsLock.writeLock();
	}
	
	private long lastHeartBeat = -1;
	
	private String jobId = null;
	private QueueImpl queue = null;
	private EventDispatcherImpl dispatcher = null;
	private IPropertyBlock qualityValues = null;
	private Map<String,IGauge<?>> gaugeIndex = null;
	private Map<String,IMeter> meterIndex = null;
	private Map<String,ITimer> timerIndex = null;
	private Map<String,ICounter> counterIndex = null;
	private Map<String,IHistogram> histogramIndex = null;
	private ReentrantReadWriteLock metricsLock;
	private ReadLock metricsReadLock;
	private WriteLock metricsWriteLock;
	
	
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
	public <T> IGauge<T> getGauge(Class<T> type, String... names)
	{
		String key = this.queue == null ? 
				MetricRegistry.name(MetricRegistry.name(IEventDispatcher.class.getName(),names),"Gauge") :  
				MetricRegistry.name(IQueue.class.getName(),MetricRegistry.name(queue.getQueueId() + ((jobId == null) ? "": "." + jobId), names),"Gauge");

		try
		{
			this.metricsReadLock.lock();
			IGauge gauge = this.gaugeIndex.get(key);
			if(gauge != null)
			{
				return gauge;
			}
		}
		finally 
		{
			this.metricsReadLock.unlock();
		}
		
		SortedMap<String,Gauge> matches = this.dispatcher.getMetricRegistry().getGauges(new MetricFilterByName(key));
		
		if(matches.isEmpty())
		{
			return null;
		}
		Gauge chgauge = matches.get(key);
		if(chgauge == null)
		{
			return null;
		}
		
		IGauge gauge = null;
		if(chgauge instanceof IGauge<?>)
		{
			try
			{
				this.metricsWriteLock.lock();
				gauge = this.gaugeIndex.get(key);
				if(gauge != null)
				{
					return gauge;
				}
				
				this.gaugeIndex.put(key, (IGauge<T>) chgauge);
				return  (IGauge<T>) chgauge;
			}
			finally 
			{
				this.metricsWriteLock.unlock();
			}
		}
		
		try
		{
			this.metricsWriteLock.lock();
			gauge = this.gaugeIndex.get(key);
			if(gauge != null)
			{
				return gauge;
			}
			
			this.gaugeIndex.put(key, (IGauge<T>) new CodahaleGaugeWrapper(chgauge));
			return (IGauge<T>)this.gaugeIndex.get(key);
		}
		finally 
		{
			this.metricsWriteLock.unlock();
		}
		
	}

	@Override
	public IGauge<?> registerGauge(IGauge<?> gauge, String... names)
	{
		String key = this.queue == null ? 
				MetricRegistry.name(MetricRegistry.name(IEventDispatcher.class.getName(),names),"Gauge") :  
				MetricRegistry.name(IQueue.class.getName(),MetricRegistry.name(queue.getQueueId() + ((jobId == null) ? "": "." + jobId), names),"Gauge");
				
		try
		{
			this.metricsWriteLock.lock();
			
			if(! (gauge instanceof Gauge))
			{
				gauge = new SodeacGaugeWrapper<>(gauge);
			}
			
			this.dispatcher.getMetricRegistry().register(key, (Gauge<?>)gauge);
			this.gaugeIndex.put(key, gauge);
			
		}
		finally 
		{
			this.metricsWriteLock.unlock();
		}
		
		return gauge;
		
	}

	@Override
	public Object getQualityValue(String key)
	{
		return this.qualityValues.getProperty(key);
	}

	@Override
	public Object setQualityValue(String key, Object value)
	{
		return this.qualityValues.setProperty(key, value);
	}

	@Override
	public Object removeQualityValue(String key)
	{
		return this.qualityValues.removeProperty(key);
	}
	
	public void dispose()
	{
		try
		{
			this.metricsWriteLock.lock();
		
			MetricRegistry metricRegistry = dispatcher.getMetricRegistry();
			
			for(Entry<String, IMeter> entry : meterIndex.entrySet())
			{
				metricRegistry.remove(entry.getKey());
			}
			meterIndex.clear();
			for(Entry<String, ITimer> entry : timerIndex.entrySet())
			{
				metricRegistry.remove(entry.getKey());
			}
			timerIndex.clear();
			
			meterIndex.clear();
			for(Entry<String, ICounter> entry : counterIndex.entrySet())
			{
				metricRegistry.remove(entry.getKey());
			}
			counterIndex.clear();
			
			for(Entry<String, IHistogram> entry : histogramIndex.entrySet())
			{
				metricRegistry.remove(entry.getKey());
			}
			histogramIndex.clear();
			
			for(Entry<String, IGauge<?>> entry : gaugeIndex.entrySet())
			{
				metricRegistry.remove(entry.getKey());
			}
			gaugeIndex.clear();
			
			this.meterIndex = null;
			this.timerIndex = null;
			this.counterIndex = null;
			this.gaugeIndex = null;
		}
		finally
		{
			this.metricsWriteLock.unlock();
		}
	}

	@Override
	public IMeter meter(String... names)
	{
		String key = this.queue == null ? 
				MetricRegistry.name(MetricRegistry.name(IEventDispatcher.class.getName(),names),"Meters") :  
				MetricRegistry.name(IQueue.class.getName(),MetricRegistry.name(queue.getQueueId() + ((jobId == null) ? "": "." + jobId), names),"Meters");
		
		try
		{
			this.metricsReadLock.lock();
			IMeter meter = this.meterIndex.get(key);
			if(meter != null)
			{
				return meter;
			}
		}
		finally 
		{
			this.metricsReadLock.unlock();
		}
		
		try
		{
			this.metricsWriteLock.lock();
			IMeter meter = this.meterIndex.get(key);
			if(meter != null)
			{
				return meter;
			}
			MetricRegistry registry = this.dispatcher.getMetricRegistry();
			meter = new MeterImpl(registry.meter(key));
			this.meterIndex.put(key,meter);
			return meter;
		}
		finally 
		{
			this.metricsWriteLock.unlock();
		}
	}

	@Override
	public ITimer timer(String... names)
	{
		String key = this.queue == null ? 
				MetricRegistry.name(MetricRegistry.name(IEventDispatcher.class.getName(),names),"Timer") :  
				MetricRegistry.name(IQueue.class.getName(),MetricRegistry.name(queue.getQueueId() + ((jobId == null) ? "": "." + jobId), names),"Timer");
		try
		{
			this.metricsReadLock.lock();
			ITimer timer = this.timerIndex.get(key);
			if(timer != null)
			{
				return timer;
			}
		}
		finally 
		{
			this.metricsReadLock.unlock();
		}
		
		try
		{
			this.metricsWriteLock.lock();
			ITimer timer = this.timerIndex.get(key);
			if(timer != null)
			{
				return timer;
			}
			MetricRegistry registry = this.dispatcher.getMetricRegistry();
			timer = new TimerImpl(registry.timer(key));
			this.timerIndex.put(key,timer);
			return timer;
		}
		finally 
		{
			this.metricsWriteLock.unlock();
		}
	}
	
	@Override
	public ICounter counter(String... names)
	{
		String key = this.queue == null ? 
				MetricRegistry.name(MetricRegistry.name(IEventDispatcher.class.getName(),names),"Counter") :  
				MetricRegistry.name(IQueue.class.getName(),MetricRegistry.name(queue.getQueueId() + ((jobId == null) ? "": "." + jobId), names),"Counter");
		try
		{
			this.metricsReadLock.lock();
			ICounter counter = this.counterIndex.get(key);
			if(counter != null)
			{
				return counter;
			}
		}
		finally 
		{
			this.metricsReadLock.unlock();
		}
		
		try
		{
			this.metricsWriteLock.lock();
			ICounter counter = this.counterIndex.get(key);
			if(counter != null)
			{
				return counter;
			}
			MetricRegistry registry = this.dispatcher.getMetricRegistry();
			counter = new CounterImpl(registry.counter(key));
			this.counterIndex.put(key,counter);
			return counter;
		}
		finally 
		{
			this.metricsWriteLock.unlock();
		}
	}
	
	@Override
	public IHistogram histogram(String... names)
	{
		String key = this.queue == null ? 
				MetricRegistry.name(MetricRegistry.name(IEventDispatcher.class.getName(),names),"Histogram") :  
				MetricRegistry.name(IQueue.class.getName(),MetricRegistry.name(queue.getQueueId() + ((jobId == null) ? "": "." + jobId), names),"Histogram");
		try
		{
			this.metricsReadLock.lock();
			IHistogram histogram = this.histogramIndex.get(key);
			if(histogram != null)
			{
				return histogram;
			}
		}
		finally 
		{
			this.metricsReadLock.unlock();
		}
		
		try
		{
			this.metricsWriteLock.lock();
			IHistogram histogram = this.histogramIndex.get(key);
			if(histogram != null)
			{
				return histogram;
			}
			MetricRegistry registry = this.dispatcher.getMetricRegistry();
			histogram = new HistogramImpl(registry.histogram(key));
			this.histogramIndex.put(key,histogram);
			return histogram;
		}
		finally 
		{
			this.metricsWriteLock.unlock();
		}
	}
	
}
