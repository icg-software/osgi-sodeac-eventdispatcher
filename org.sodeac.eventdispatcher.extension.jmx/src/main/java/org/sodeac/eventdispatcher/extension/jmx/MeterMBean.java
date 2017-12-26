package org.sodeac.eventdispatcher.extension.jmx;

public interface MeterMBean
{
	public String getName();
	public String getKey();
	
	public long getCount();
	public double getMeanRate();
	public double getOneMinuteRate();
	public double getFiveMinuteRate();
	public double getFifteenMinuteRate();
}
