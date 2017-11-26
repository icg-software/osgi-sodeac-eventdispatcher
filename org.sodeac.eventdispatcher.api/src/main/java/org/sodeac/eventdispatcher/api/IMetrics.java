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
package org.sodeac.eventdispatcher.api;

/**
 * metric-handler to read or feed metrics for {@link IEventDispatcher}, {@link IQueue} and {@link IQueueJob}
 * 
 * @author Sebastian Palarus
 *
 */
public interface IMetrics
{
	public static final String QUALITY_VALUE_CREATED = "QUALITY_VALUE_CREATED";
	
	public static final String QUALITY_VALUE_STARTED_TIMESTAMP = "QUALITY_VALUE_STARTED_TIMESTAMP";
	public static final String QUALITY_VALUE_FINISHED_TIMESTAMP = "QUALITY_VALUE_FINISHED_TIMESTAMP";
	public static final String QUALITY_VALUE_LAST_HEARTBEAT = "QUALITY_VALUE_LAST_HEARTBEAT";
	public static final String QUALITY_VALUE_LAST_SEND_EVENT = "QUALITY_VALUE_LAST_SEND_EVENT";
	public static final String QUALITY_VALUE_LAST_POST_EVENT = "QUALITY_VALUE_LAST_POST_EVENT";
	
	public static final String POSTFIX_COUNTER = "Counter";
	public static final String POSTFIX_GAUGE = "Gauge";
	public static final String POSTFIX_HISTORGRAM = "Histogram";
	public static final String POSTFIX_METER = "Meter";
	public static final String POSTFIX_TIMER = "Timer";
	
	public static final String METRICS_QUEUE = "Queue";
	public static final String METRICS_EVENT_CONTROLLER = "EventController";
	
	public static final String METRICS_RUN_JOB = "Run";
	public static final String METRICS_RUN_JOB_ERROR = "ErrorRun";
	
	public static final String GAUGE_JOB_CREATED = "JobCreated";
	public static final String GAUGE_JOB_STARTED = "JobStarted";
	public static final String GAUGE_JOB_FINISHED = "JobFinished";
	public static final String GAUGE_JOB_LAST_HEARTBEAT = "LastHeartbeat";
	public static final String GAUGE_LAST_SEND_EVENT = "LastSendEvent";
	public static final String GAUGE_LAST_POST_EVENT = "LastPostEvent";
	
	public static final String METRICS_SEND_EVENT = "SendEvent";
	public static final String METRICS_POST_EVENT = "PostEvent";
	
	/**
	 * getter for registered qualityvalue with associated {@code key}. Qualityvalues acts as database for gauges.
	 * 
	 * @param key the key whose associated qualityvalue is to be returned
	 * 
	 * @return the quality with specified key, or null if property does not exists
	 */
	public Object getQualityValue(String key);
	
	/**
	 * register a qualityvalue {@code value} with associated {@code key}.Qualityvalues acts as database for gauges.
	 * 
	 * @param key key with which the specified quality is to be associated
	 * @param value quality to be associated with the specified key 
	 * 
	 * @return previews qualityvalue registered with {@code key}, or null
	 */
	public Object setQualityValue(String key, Object value);
	
	/**
	 * remove qualityvalue with associated {@code key}
	 * 
	 * @param key key the key whose associated qualityvalue is to be removed
	 * 
	 * @return the removed qualityvalue with specified key, or null if qualityvalue does not exists
	 */
	public Object removeQualityValue(String key);
	
	/**
	 * Returns the {@link IGauge} of this metric-object registered under {@code names}
	 * 
	 * @param type the type of {@link IGauge}
	 * @param names registration names of {@link IGauge}
	 * @return {@link IGauge} with type {@code type} registered under {@code names}, or null
	 */
	public <T> IGauge<T> getGauge(Class<T> type, String... names);
	
	/**
	 * register {@link IGauge} under {@code names}
	 * 
	 * @param gauge implementation of {@link IGauge} to register
	 * @param names registration names
	 * @return registered {@link IGauge}
	 */
	public IGauge<?> registerGauge(IGauge<?> gauge, String... names);
	
	/**
	 * return the {@link IMeter} registered under {@code names}. If {@link IMeter} not registered 
     * a new {@link IMeter} is created and registered before returns
	 *  
	 * @param names registration names of {@link IMeter}
	 * @return {@link IMeter} registered under {@code names}
	 */
	public IMeter meter(String... names);
	
	/**
	 * return the {@link ITimer} registered under {@code names}. If {@link ITimer} not registered 
     * a new {@link ITimer} is created and registered before returns
	 *  
	 * @param names registration names of {@link ITimer}
	 * @return {@link ITimer} registered under {@code names}
	 */
	public ITimer timer(String... names);
	
	/**
	 * return the {@link ICounter} registered under {@code names}. If {@link ICounter} not registered 
     * a new {@link ICounter} is created and registered before returns
	 *  
	 * @param names registration names of {@link ICounter}
	 * @return {@link ICounter} registered under {@code names}
	 */
	public ICounter counter(String... names);
	
	/**
	 * return the {@link IHistogram} registered under {@code names}. If {@link IHistogram} not registered 
     * a new {@link IHistogram} is created and registered before returns
	 *  
	 * @param names registration names of {@link IHistogram}
	 * @return {@link IHistogram} registered under {@code names}
	 */
	public IHistogram histogram(String... names);
	
	/**
	 * return common metric key 
	 * 
	 * @param queueId id of queue or null
	 * @param jobId id of job or null
	 * @param postfix Counter/Gauge/Histogram/Meter/Timer
	 * @param names registration name of metric object
	 * 
	 * @return common metric key 
	 */
	public static String metricName(String queueId, String jobId, String postfix, String... names)
	{
		StringBuilder builder = new StringBuilder();
		if((queueId != null) && (!queueId.isEmpty()))
		{
			builder.append(IQueue.class.getName());
			builder.append("." + queueId);
			
			if((jobId != null) && (!jobId.isEmpty()))
			{
				builder.append("." + jobId);
			}
		}
		else
		{
			builder.append(IEventDispatcher.class.getName());
		}
		
		if(names != null)
		{
			for (String name : names) 
			{
				if(name == null)
				{
					continue;
				}
				if(name.isEmpty())
				{
					continue;
				}
				builder.append("." + name);
            }
		}
		
		if((postfix != null) && (! postfix.isEmpty()))
		{
			builder.append("." + postfix);
		}
		
		return builder.toString();
	}
}
