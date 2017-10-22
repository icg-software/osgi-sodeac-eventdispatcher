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
package org.sodeac.eventdispatcher.itest.components.compressor;

public class CompressorStatics
{
	public final static String TOPIC_PUBLISH_TRACING_OBJECT 	= "org/sodeac/eventdispatcher/itest/components/compressor/tracingobject/publish";
	public final static String TOPIC_START_TEST 				= "org/sodeac/eventdispatcher/itest/components/compressor/test/start";
	public final static String TOPIC_RAW_EVENT 					= "org/sodeac/eventdispatcher/itest/components/compressor/event/raw";
	public final static String TOPIC_COMPRESSED_EVENT 			= "org/sodeac/eventdispatcher/itest/components/compressor/event/compressed";
	public static final String QUEUE_ID 						= "compressortestqueue";
	public static final String HEARTBEAT						= "1000";
	public static final String COMPRESSOR_SERVICE_ID			= "CompressorService";
}
