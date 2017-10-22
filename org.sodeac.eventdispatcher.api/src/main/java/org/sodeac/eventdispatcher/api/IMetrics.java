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

public interface IMetrics
{
	public static final String QUALITY_VALUE_CREATED = "QUALITY_VALUE_CREATED";
	
	public static final String QUALITY_VALUE_STARTED_TIMESTAMP = "QUALITY_VALUE_STARTED_TIMESTAMP";
	public static final String QUALITY_VALUE_FINISHED_TIMESTAMP = "QUALITY_VALUE_FINISHED_TIMESTAMP";
	
	
	public static final String METRICS_RUN_JOB = "Run";
	public static final String METRICS_RUN_JOB_ERROR = "ErrorRun";
	
	public static final String GAUGE_JOB_CREATED = "JobCreated";
	public static final String GAUGE_JOB_STARTED = "JobStarted";
	public static final String GAUGE_JOB_FINISHED = "JobFinished";
	
	public long getLastHeartBeat();
	public void heartBeat();
	
	public Object getQualityValue(String key);
	public Object setQualityValue(String key, Object value);
	public Object removeQualityValue(String key);
	
	public <T> IGauge<T> getGauge(Class<T> type, String... names);
	public IGauge<?> registerGauge(IGauge<?> gauge, String... names);
	
	public IMeter meter(String... names);
	public ITimer timer(String... names);
	public ICounter counter(String... names);
	public IHistogram histogram(String... names);
}
