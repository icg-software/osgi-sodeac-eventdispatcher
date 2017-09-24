package org.sodeac.eventdispatcher.api;

public interface IQueueService extends IQueueJob
{
	public static final String PROPERTY_QUEUE_ID = IEventDispatcher.PROPERTY_QUEUE_ID;
	
	public static final String PROPERTY_SERVICE_ID 						= "serviceid";
	public static final String PROPERTY_TIMEOUT_MS 						= "servicetimeout";
	public static final String PROPERTY_HB_TIMEOUT_MS 					= "serviceheartbeattimeout";
	public static final String PROPERTY_START_DELAY_MS 					= "servicestartdelay";
	public static final String PROPERTY_PERIODIC_REPETITION_INTERVAL 	= "serviceperiodicrepetitioninterval"; 
}
