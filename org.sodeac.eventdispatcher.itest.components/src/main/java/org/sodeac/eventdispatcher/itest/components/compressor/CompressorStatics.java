/*******************************************************************************
 * Copyright (c) 2017, 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.itest.components.compressor;

public class CompressorStatics
{
	public final static String TOPIC_PUBLISH_TRACING_OBJECT 	= "org/sodeac/eventdispatcher/itest/components/compressor/tracingobject/publish";
	public final static String TOPIC_START_TEST 				= "org/sodeac/eventdispatcher/itest/components/compressor/test/start";
	public final static String TOPIC_START_COMPRESSOR			= "org/sodeac/eventdispatcher/itest/components/compressor/compressor/start";
	public final static String TOPIC_STOP_COMPRESSOR			= "org/sodeac/eventdispatcher/itest/components/compressor/compressor/stop";
	public final static String TOPIC_RAW_EVENT 					= "org/sodeac/eventdispatcher/itest/components/compressor/event/raw";
	public final static String TOPIC_COMPRESSED_EVENT 			= "org/sodeac/eventdispatcher/itest/components/compressor/event/compressed";
	public static final String QUEUE_ID 						= "compressortestqueue";
	public static final Long   HEARTBEAT_INTERVAL				= 1000L;
	public static final Long   MINIMAL_INTERVAL					= 500L;
	public static final String SERVICE_REPETITION_INTERVAL		= "500";
	public static final String PROPERTY_COUNT					= "PROPERTY_COUNT";
	public static final String PROPERTY_COUNT_MIN				= "PROPERTY_COUNT_MIN";
	public static final String PROPERTY_COUNT_MAX				= "PROPERTY_COUNT_MAX";
	public static final String PROPERTY_COUNT_SIZE				= "PROPERTY_COUNT_SIZE";
	public static final String COMPRESSOR_SERVICE_ID			= "CompressorService";
}
