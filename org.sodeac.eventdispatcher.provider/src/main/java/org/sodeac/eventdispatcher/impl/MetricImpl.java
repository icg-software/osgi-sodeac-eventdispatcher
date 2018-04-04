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

import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.ICounter;
import org.sodeac.eventdispatcher.api.IGauge;
import org.sodeac.eventdispatcher.api.IHistogram;
import org.sodeac.eventdispatcher.api.IMeter;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.ITimer;
import org.sodeac.eventdispatcher.extension.api.IEventDispatcherExtension;
import org.sodeac.eventdispatcher.extension.api.IExtensibleGauge;
import org.sodeac.eventdispatcher.extension.api.IExtensibleMetrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

public class MetricImpl implements IMetrics,IExtensibleMetrics
{
	public MetricImpl(QueueImpl queue, IPropertyBlock qualityValues, String jobId, boolean enabled)
	{
		super();
		this.queue = queue;
		this.enabled = enabled;
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
	
	public MetricImpl(EventDispatcherImpl dispatcher, IPropertyBlock qualityValues, boolean enabled)
	{
		super();
		this.queue = null;
		this.enabled = enabled;
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
	
	private String jobId = null;
	private QueueImpl queue = null;
	private boolean enabled = true;
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
	
	private volatile MeterImpl disabledMeter = null;
	private volatile TimerImpl disabledTimer = null;
	private volatile CounterImpl disabledCounter = null;
	private volatile HistogramImpl disabledHistogram = null; 
	
	
	
	public boolean isEnabled()
	{
		return enabled;
	}

	public String getJobId()
	{
		return jobId;
	}

	public QueueImpl getQueue()
	{
		return queue;
	}

	public EventDispatcherImpl getDispatcher()
	{
		return dispatcher;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public <T> IGauge<T> getGauge(Class<T> type, String... names)
	{
		String key = IMetrics.metricName(dispatcher.getId(), this.queue == null ? null : this.queue.getQueueId(), this.jobId, POSTFIX_GAUGE, names);

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
		
		if(! this.enabled)
		{
			return null;
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
			
			this.gaugeIndex.put(key, (IGauge<T>) new CodahaleGaugeWrapper(chgauge,key,names2name(names),this));
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
		String key = IMetrics.metricName(dispatcher.getId(), this.queue == null ? null : this.queue.getQueueId(), this.jobId, POSTFIX_GAUGE, names);
		try
		{
			this.metricsWriteLock.lock();
			
			if(! (gauge instanceof Gauge))
			{
				gauge = new SodeacGaugeWrapper<>(gauge,key,names2name(names),this);
			}
			
			if(this.enabled)
			{
				if(! (gauge instanceof IExtensibleGauge))
				{
					gauge = new SodeacGaugeWrapper<>(gauge,key,names2name(names),this);
				}
				
				try
				{
					this.dispatcher.getMetricRegistry().register(key, (Gauge<?>)gauge);
				}
				catch (Exception e) 
				{
					this.dispatcher.getMetricRegistry().remove(key);
					this.dispatcher.getMetricRegistry().register(key, (Gauge<?>)gauge);
				}
				
				for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
				{
					try
					{
						extension.registerGauge(dispatcher,(IExtensibleGauge<?>)gauge);
					}
					catch (Exception e) 
					{
						this.dispatcher.log(LogService.LOG_ERROR, "Exception while register gauge", e);
					}
				}
			}
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
	
	public void enable()
	{
		try
		{
			this.metricsWriteLock.lock();
			if(this.enabled)
			{
				return;
			}
			
			this.enabled = true;
			
			for(Entry<String,IGauge<?>> entry : this.gaugeIndex.entrySet())
			{
				try
				{
					this.dispatcher.getMetricRegistry().register(entry.getKey(), (Gauge<?>)entry.getValue());
				}
				catch (Exception e) 
				{
					this.dispatcher.getMetricRegistry().remove(entry.getKey());
					this.dispatcher.getMetricRegistry().register(entry.getKey(), (Gauge<?>)entry.getValue());
				}
			}
		}
		finally
		{
			this.metricsWriteLock.unlock();
		}
		
		for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
		{
			try
			{
				registerOnExtension(extension);
			}
			catch(Exception e)
			{
				this.dispatcher.log(LogService.LOG_ERROR, "Exception while register metrics", e);
			}
		}
			
	}
	
	public void disable()
	{
		try
		{
			this.metricsWriteLock.lock();
			if(! this.enabled)
			{
				return;
			}
			
			this.enabled = false;
			
			MetricRegistry metricRegistry = dispatcher.getMetricRegistry();
			
			for(Entry<String, IMeter> entry : meterIndex.entrySet())
			{
				for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
				{
					try
					{
						extension.unregisterMeter(dispatcher, (MeterImpl)entry.getValue());
					}
					catch (Exception e) 
					{
						this.dispatcher.log(LogService.LOG_ERROR, "Exception while unregister meter", e);
					}
				}
				metricRegistry.remove(entry.getKey());
			}
			meterIndex.clear();
			
			for(Entry<String, ITimer> entry : timerIndex.entrySet())
			{
				for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
				{
					try
					{
						extension.unregisterTimer(dispatcher, (TimerImpl)entry.getValue());
					}
					catch (Exception e) 
					{
						this.dispatcher.log(LogService.LOG_ERROR, "Exception while unregister timer", e);
					}
				}
				metricRegistry.remove(entry.getKey());
			}
			timerIndex.clear();
			
			for(Entry<String, ICounter> entry : counterIndex.entrySet())
			{
				for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
				{
					try
					{
						extension.unregisterCounter(dispatcher, (CounterImpl)entry.getValue());
					}
					catch (Exception e) 
					{
						this.dispatcher.log(LogService.LOG_ERROR, "Exception while unregister counter", e);
					}
				}
				metricRegistry.remove(entry.getKey());
			}
			counterIndex.clear();
			
			for(Entry<String, IHistogram> entry : histogramIndex.entrySet())
			{
				for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
				{
					try
					{
						extension.unregisterHistogram(dispatcher, (HistogramImpl)entry.getValue());
					}
					catch (Exception e) 
					{
						this.dispatcher.log(LogService.LOG_ERROR, "Exception while unregister histogram", e);
					}
				}
				metricRegistry.remove(entry.getKey());
			}
			histogramIndex.clear();
			
			for(Entry<String, IGauge<?>> entry : gaugeIndex.entrySet())
			{
				if(entry.getValue() instanceof IExtensibleGauge<?>)
				{
					for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
					{
						try
						{
							extension.unregisterGauge(dispatcher, (IExtensibleGauge<?>)entry.getValue());
						}
						catch (Exception e) 
						{
							this.dispatcher.log(LogService.LOG_ERROR, "Exception while unregister gauge", e);
						}
					}
				}
				metricRegistry.remove(entry.getKey());
			}
			// keep Gauges
		}
		finally
		{
			this.metricsWriteLock.unlock();
		}
	}
	
	public void dispose()
	{
		this.enabled = false;
		try
		{
			this.metricsWriteLock.lock();
		
			MetricRegistry metricRegistry = dispatcher.getMetricRegistry();
			
			if(meterIndex != null)
			{
				for(Entry<String, IMeter> entry : meterIndex.entrySet())
				{
					for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
					{
						try
						{
							extension.unregisterMeter(dispatcher, (MeterImpl)entry.getValue());
						}
						catch (Exception e) 
						{
							this.dispatcher.log(LogService.LOG_ERROR, "Exception while unregister meter", e);
						}
					}
					metricRegistry.remove(entry.getKey());
				}
				meterIndex.clear();
			}
			
			if(timerIndex != null)
			{
				for(Entry<String, ITimer> entry : timerIndex.entrySet())
				{
					for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
					{
						try
						{
							extension.unregisterTimer(dispatcher, (TimerImpl)entry.getValue());
						}
						catch (Exception e) 
						{
							this.dispatcher.log(LogService.LOG_ERROR, "Exception while unregister timer", e);
						}
					}
					metricRegistry.remove(entry.getKey());
				}
				timerIndex.clear();
			}
			
			if(counterIndex != null)
			{
				for(Entry<String, ICounter> entry : counterIndex.entrySet())
				{
					for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
					{
						try
						{
							extension.unregisterCounter(dispatcher, (CounterImpl)entry.getValue());
						}
						catch (Exception e) 
						{
							this.dispatcher.log(LogService.LOG_ERROR, "Exception while unregister counter", e);
						}
					}
					metricRegistry.remove(entry.getKey());
				}
				counterIndex.clear();
			}
			
			if(histogramIndex != null)
			{
				for(Entry<String, IHistogram> entry : histogramIndex.entrySet())
				{
					for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
					{
						try
						{
							extension.unregisterHistogram(dispatcher, (HistogramImpl)entry.getValue());
						}
						catch (Exception e) 
						{
							this.dispatcher.log(LogService.LOG_ERROR, "Exception while unregister histogram", e);
						}
					}
					metricRegistry.remove(entry.getKey());
				}
				histogramIndex.clear();
			}
			
			if(gaugeIndex != null)
			{
				for(Entry<String, IGauge<?>> entry : gaugeIndex.entrySet())
				{
					if(entry.getValue() instanceof IExtensibleGauge<?>)
					{
						for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
						{
							try
							{
								extension.unregisterGauge(dispatcher, (IExtensibleGauge<?>)entry.getValue());
							}
							catch (Exception e) 
							{
								this.dispatcher.log(LogService.LOG_ERROR, "Exception while unregister gauge", e);
							}
						}
					}
					metricRegistry.remove(entry.getKey());
				}
				gaugeIndex.clear();
			}
			
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
	
	public void registerOnExtension(IEventDispatcherExtension extension)
	{
		try
		{
			this.metricsReadLock.lock();
		
			for(Entry<String, IMeter> entry : meterIndex.entrySet())
			{
				try
				{
					extension.registerMeter(dispatcher, (MeterImpl)entry.getValue());
				}
				catch (Exception e) 
				{
					this.dispatcher.log(LogService.LOG_ERROR, "Exception while register meter", e);
				}
			}
			
			for(Entry<String, ITimer> entry : timerIndex.entrySet())
			{
				try
				{
					extension.registerTimer(dispatcher, (TimerImpl)entry.getValue());
				}
				catch (Exception e) 
				{
					this.dispatcher.log(LogService.LOG_ERROR, "Exception while register timer", e);
				}
			}
			
			for(Entry<String, ICounter> entry : counterIndex.entrySet())
			{
				try
				{
					extension.registerCounter(dispatcher, (CounterImpl)entry.getValue());
				}
				catch (Exception e) 
				{
					this.dispatcher.log(LogService.LOG_ERROR, "Exception while register counter", e);
				}
			}
			
			for(Entry<String, IHistogram> entry : histogramIndex.entrySet())
			{
				try
				{
					extension.registerHistogram(dispatcher, (HistogramImpl)entry.getValue());
				}
				catch (Exception e) 
				{
					this.dispatcher.log(LogService.LOG_ERROR, "Exception while register histogram", e);
				}
			}
			
			for(Entry<String, IGauge<?>> entry : gaugeIndex.entrySet())
			{
				if(entry.getValue() instanceof IExtensibleGauge<?>)
				{
					try
					{
						extension.registerGauge(dispatcher, (IExtensibleGauge<?>)entry.getValue());
					}
					catch (Exception e) 
					{
						this.dispatcher.log(LogService.LOG_ERROR, "Exception while register gauge", e);
					}
				}
			}
		}
		finally
		{
			this.metricsReadLock.unlock();
		}
	}

	@Override
	public IMeter meter(String... names)
	{
		String key = IMetrics.metricName(dispatcher.getId(), this.queue == null ? null : this.queue.getQueueId(), this.jobId, POSTFIX_METER, names);
		
		try
		{
			this.metricsReadLock.lock();
			
			if(! this.enabled)
			{
				if(this.disabledMeter == null)
				{
					this.disabledMeter = new MeterImpl(null,key,names2name(names),this);
				}
				return this.disabledMeter;
			}
			
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
			
			if(! this.enabled)
			{
				if(this.disabledMeter == null)
				{
					this.disabledMeter = new MeterImpl(null,key,names2name(names),this);
				}
				return this.disabledMeter;
			}
			
			IMeter meter = this.meterIndex.get(key);
			if(meter != null)
			{
				return meter;
			}
			MetricRegistry registry = this.dispatcher.getMetricRegistry();
			try
			{
				meter = new MeterImpl(registry.meter(key),key,names2name(names),this);
			}
			catch (Exception e) 
			{
				registry.remove(key);
				meter = new MeterImpl(registry.meter(key),key,names2name(names),this);
			}
			this.meterIndex.put(key,meter);
			
			for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
			{
				try
				{
					extension.registerMeter(dispatcher,(MeterImpl)meter);
				}
				catch (Exception e) 
				{
					this.dispatcher.log(LogService.LOG_ERROR, "Exception while register meter", e);
				}
			}
			
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
		String key = IMetrics.metricName(dispatcher.getId(), this.queue == null ? null : this.queue.getQueueId(), this.jobId, POSTFIX_TIMER, names);
		
		try
		{
			this.metricsReadLock.lock();
			
			if(! this.enabled)
			{
				if(this.disabledTimer == null)
				{
					this.disabledTimer = new TimerImpl(null,key,names2name(names),this);
				}
				return this.disabledTimer;
			}
			
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
			
			if(! this.enabled)
			{
				if(this.disabledTimer == null)
				{
					this.disabledTimer = new TimerImpl(null,key,names2name(names),this);
				}
				return this.disabledTimer;
			}
			
			ITimer timer = this.timerIndex.get(key);
			if(timer != null)
			{
				return timer;
			}
			MetricRegistry registry = this.dispatcher.getMetricRegistry();
			try
			{
				timer = new TimerImpl(registry.timer(key),key,names2name(names),this);
			}
			catch (Exception e) 
			{
				registry.remove(key);
				timer = new TimerImpl(registry.timer(key),key,names2name(names),this);
			}
			this.timerIndex.put(key,timer);
			
			for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
			{
				try
				{
					extension.registerTimer(dispatcher,(TimerImpl)timer);
				}
				catch (Exception e) 
				{
					this.dispatcher.log(LogService.LOG_ERROR, "Exception while register timer", e);
				}
			}
			
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
		String key = IMetrics.metricName(dispatcher.getId(), this.queue == null ? null : this.queue.getQueueId(), this.jobId, POSTFIX_COUNTER, names);
		try
		{
			this.metricsReadLock.lock();
			
			if(! this.enabled)
			{
				if(this.disabledCounter == null)
				{
					this.disabledCounter = new CounterImpl(null,key,names2name(names),this);
				}
				return this.disabledCounter;
			}
			
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
			
			if(! this.enabled)
			{
				if(this.disabledCounter == null)
				{
					this.disabledCounter = new CounterImpl(null,key,names2name(names),this);
				}
				return this.disabledCounter;
			}
			
			ICounter counter = this.counterIndex.get(key);
			if(counter != null)
			{
				return counter;
			}
			MetricRegistry registry = this.dispatcher.getMetricRegistry();
			try
			{
				counter = new CounterImpl(registry.counter(key),key,names2name(names),this);
			}
			catch (Exception e) 
			{
				registry.remove(key);
				counter = new CounterImpl(registry.counter(key),key,names2name(names),this);
			}
			this.counterIndex.put(key,counter);
			
			for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
			{
				try
				{
					extension.registerCounter(dispatcher,(CounterImpl)counter);
				}
				catch (Exception e) 
				{
					this.dispatcher.log(LogService.LOG_ERROR, "Exception while register counter", e);
				}
			}
			
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
		String key = IMetrics.metricName(dispatcher.getId(), this.queue == null ? null : this.queue.getQueueId(), this.jobId, POSTFIX_HISTORGRAM, names);
		try
		{
			this.metricsReadLock.lock();
			
			if(! this.enabled)
			{
				if(this.disabledHistogram == null)
				{
					this.disabledHistogram = new HistogramImpl(null,key,names2name(names),this);
				}
				return this.disabledHistogram;
			}
			
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
			
			if(! this.enabled)
			{
				if(this.disabledHistogram == null)
				{
					this.disabledHistogram = new HistogramImpl(null,key,names2name(names),this);
				}
				return this.disabledHistogram;
			}
			
			IHistogram histogram = this.histogramIndex.get(key);
			if(histogram != null)
			{
				return histogram;
			}
			MetricRegistry registry = this.dispatcher.getMetricRegistry();
			try
			{
				histogram = new HistogramImpl(registry.histogram(key),key,names2name(names),this);
			}
			catch (Exception e) 
			{
				registry.remove(key);
				histogram = new HistogramImpl(registry.histogram(key),key,names2name(names),this);
			}
			this.histogramIndex.put(key,histogram);
			
			for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
			{
				try
				{
					extension.registerHistogram(dispatcher,(HistogramImpl)histogram);
				}
				catch (Exception e) 
				{
					this.dispatcher.log(LogService.LOG_ERROR, "Exception while register histogram", e);
				}
			}
			
			return histogram;
		}
		finally 
		{
			this.metricsWriteLock.unlock();
		}
	}
	
	public void updateCounter(CounterImpl counter)
	{
		for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
		{
			try
			{
				extension.updateCounter(dispatcher, counter);
			}
			catch (Exception e) 
			{
				this.dispatcher.log(LogService.LOG_ERROR, "Exception while update counter", e);
			}
		}
	}
	
	public void updateMeter(MeterImpl meter)
	{
		for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
		{
			try
			{
				extension.updateMeter(dispatcher, meter);
			}
			catch (Exception e) 
			{
				this.dispatcher.log(LogService.LOG_ERROR, "Exception while update meter", e);
			}
		}
	}
	
	public void updateHistogram(HistogramImpl histogram)
	{
		for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
		{
			try
			{
				extension.updateHistogram(dispatcher, histogram);
			}
			catch (Exception e) 
			{
				this.dispatcher.log(LogService.LOG_ERROR, "Exception while update histogram", e);
			}
		}
	}
	
	public void updateTimer(TimerImpl timer)
	{
		for(IEventDispatcherExtension extension : this.dispatcher.getEventDispatcherExtensionList())
		{
			try
			{
				extension.updateTimer(dispatcher, timer);
			}
			catch (Exception e) 
			{
				this.dispatcher.log(LogService.LOG_ERROR, "Exception while update timer", e);
			}
		}
	}
	
	private String names2name(String... names)
	{
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		
		for (String name : names) 
		{
			if(!first)
			{
				builder.append(".");
			}
			first = false;
			builder.append(name);
		}
		
		return builder.toString();
	}
	
}
