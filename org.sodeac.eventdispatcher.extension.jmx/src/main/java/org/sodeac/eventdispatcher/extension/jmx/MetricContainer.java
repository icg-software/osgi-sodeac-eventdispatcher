package org.sodeac.eventdispatcher.extension.jmx;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.management.ObjectName;

import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.extension.api.IExtensibleCounter;
import org.sodeac.eventdispatcher.extension.api.IExtensibleGauge;
import org.sodeac.eventdispatcher.extension.api.IExtensibleHistogram;
import org.sodeac.eventdispatcher.extension.api.IExtensibleMeter;
import org.sodeac.eventdispatcher.extension.api.IExtensibleTimer;

public class MetricContainer
{
	private String objectNamePrefix ;
	private EventDispatcherJavaManagementExtension extension = null;
	private Map<IExtensibleCounter,Counter> counterIndex = null;
	private Map<IExtensibleMeter,Meter> meterIndex = null;
	private Map<IExtensibleHistogram,Histogram> histogramIndex = null;
	private Map<IExtensibleTimer,Timer> timerIndex = null;
	private Map<IExtensibleGauge<?>,Gauge> gaugeIndex = null;
	
	private ReentrantReadWriteLock lock = null;
	private ReadLock readLock = null;
	private WriteLock writeLock = null;
	
	
	public MetricContainer(String objectNamePrefix,EventDispatcherJavaManagementExtension extension)
	{
		super();
		this.objectNamePrefix = objectNamePrefix;
		this.extension = extension;
		
		this.lock = new ReentrantReadWriteLock(true);
		this.readLock = this.lock.readLock();
		this.writeLock = this.lock.writeLock();
		
		this.counterIndex = new HashMap<IExtensibleCounter,Counter>();
		this.meterIndex = new HashMap<IExtensibleMeter,Meter>();
		this.histogramIndex = new HashMap<IExtensibleHistogram,Histogram>();
		this.timerIndex = new HashMap<IExtensibleTimer,Timer>();
		this.gaugeIndex = new HashMap<IExtensibleGauge<?>,Gauge>();
	}
	
	public void dispose()
	{
		writeLock.lock();
		try
		{
			for(Entry<IExtensibleCounter,Counter> entry : this.counterIndex.entrySet())
			{
				try
				{
					ManagementFactory.getPlatformMBeanServer().unregisterMBean(entry.getValue().counterObjectName);
				}
				catch (Exception e) 
				{
					extension.log(LogService.LOG_ERROR,"clean counter",e);
				}
			}
			this.counterIndex.clear();
			
			for(Entry<IExtensibleMeter,Meter> entry : this.meterIndex.entrySet())
			{
				try
				{
					ManagementFactory.getPlatformMBeanServer().unregisterMBean(entry.getValue().meterObjectName);
				}
				catch (Exception e) 
				{
					extension.log(LogService.LOG_ERROR,"clean meter",e);
				}
			}
			this.meterIndex.clear();
			
			for(Entry<IExtensibleHistogram,Histogram> entry : this.histogramIndex.entrySet())
			{
				try
				{
					ManagementFactory.getPlatformMBeanServer().unregisterMBean(entry.getValue().histogramObjectName);
				}
				catch (Exception e) 
				{
					extension.log(LogService.LOG_ERROR,"clean histogram",e);
				}
			}
			this.histogramIndex.clear();
			
			for(Entry<IExtensibleTimer,Timer> entry : this.timerIndex.entrySet())
			{
				try
				{
					ManagementFactory.getPlatformMBeanServer().unregisterMBean(entry.getValue().timerObjectName);
				}
				catch (Exception e) 
				{
					extension.log(LogService.LOG_ERROR,"clean timer",e);
				}
			}
			this.timerIndex.clear();
			
			for(Entry<IExtensibleGauge<?>,Gauge> entry : this.gaugeIndex.entrySet())
			{
				try
				{
					ManagementFactory.getPlatformMBeanServer().unregisterMBean(entry.getValue().gaugeObjectName);
				}
				catch (Exception e) 
				{
					extension.log(LogService.LOG_ERROR,"clean gauge",e);
				}
			}
			this.timerIndex.clear();
		}
		finally 
		{
			writeLock.unlock();
		}
	}
	
	public void registerCounter(IExtensibleCounter counter)
	{
		Counter counterBean = null;
		readLock.lock();
		try
		{
			counterBean = this.counterIndex.get(counter);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(counterBean == null)
		{
			writeLock.lock();
			try
			{
				counterBean = this.counterIndex.get(counter);
				if(counterBean == null)
				{
					try
					{
						ObjectName objectName = new ObjectName(this.objectNamePrefix + ",metric=counter,name=" + counter.getName());
						counterBean = new Counter(objectName,counter);
						this.counterIndex.put(counter,counterBean);
						ManagementFactory.getPlatformMBeanServer().registerMBean(counterBean,objectName);
					}
					catch (Exception e) 
					{
						extension.log(LogService.LOG_ERROR,"register counter",e);
					}
				}
			}
			finally 
			{
				writeLock.unlock();
			}
		}
	}
	
	public void unregisterCounter(IExtensibleCounter counter)
	{
		Counter counterBean = null;
		readLock.lock();
		try
		{
			counterBean = this.counterIndex.get(counter);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(counterBean == null)
		{
			writeLock.lock();
			try
			{
				counterBean = this.counterIndex.remove(counter);
				if(counterBean != null)
				{
					try
					{
						ManagementFactory.getPlatformMBeanServer().unregisterMBean(counterBean.counterObjectName);
					}
					catch (Exception e) 
					{
						extension.log(LogService.LOG_ERROR,"unregister counter",e);
					}
				}
			}
			finally 
			{
				writeLock.unlock();
			}
		}
	}
	
	public void registerMeter(IExtensibleMeter meter)
	{
		Meter meterBean = null;
		readLock.lock();
		try
		{
			meterBean = this.meterIndex.get(meter);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(meterBean == null)
		{
			writeLock.lock();
			try
			{
				meterBean = this.meterIndex.get(meter);
				if(meterBean == null)
				{
					try
					{
						ObjectName objectName = new ObjectName(this.objectNamePrefix + ",metric=meter,name=" + meter.getName());
						meterBean = new Meter(objectName,meter);
						this.meterIndex.put(meter,meterBean);
						ManagementFactory.getPlatformMBeanServer().registerMBean(meterBean,objectName);
					}
					catch (Exception e) 
					{
						extension.log(LogService.LOG_ERROR,"register meter",e);
					}
				}
			}
			finally 
			{
				writeLock.unlock();
			}
		}
	}
	
	public void unregisterMeter(IExtensibleMeter meter)
	{
		Meter meterBean = null;
		readLock.lock();
		try
		{
			meterBean = this.meterIndex.get(meter);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(meterBean == null)
		{
			writeLock.lock();
			try
			{
				meterBean = this.meterIndex.remove(meter);
				if(meterBean != null)
				{
					try
					{
						ManagementFactory.getPlatformMBeanServer().unregisterMBean(meterBean.meterObjectName);
					}
					catch (Exception e) 
					{
						extension.log(LogService.LOG_ERROR,"unregister meter",e);
					}
				}
			}
			finally 
			{
				writeLock.unlock();
			}
		}
	}
	
	public void registerHistogram(IExtensibleHistogram histogram)
	{
		Histogram histogramBean = null;
		readLock.lock();
		try
		{
			histogramBean = this.histogramIndex.get(histogram);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(histogramBean == null)
		{
			writeLock.lock();
			try
			{
				histogramBean = this.histogramIndex.get(histogram);
				if(histogramBean == null)
				{
					try
					{
						ObjectName objectName = new ObjectName(this.objectNamePrefix + ",metric=histogram,name=" + histogram.getName());
						histogramBean = new Histogram(objectName,histogram);
						this.histogramIndex.put(histogram,histogramBean);
						ManagementFactory.getPlatformMBeanServer().registerMBean(histogramBean,objectName);
					}
					catch (Exception e) 
					{
						extension.log(LogService.LOG_ERROR,"register histogram",e);
					}
				}
			}
			finally 
			{
				writeLock.unlock();
			}
		}
	}
	
	public void unregisterHistogram(IExtensibleHistogram histogram)
	{
		Histogram histogramBean = null;
		readLock.lock();
		try
		{
			histogramBean = this.histogramIndex.get(histogram);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(histogramBean == null)
		{
			writeLock.lock();
			try
			{
				histogramBean = this.histogramIndex.remove(histogram);
				if(histogramBean != null)
				{
					try
					{
						ManagementFactory.getPlatformMBeanServer().unregisterMBean(histogramBean.histogramObjectName);
					}
					catch (Exception e) 
					{
						extension.log(LogService.LOG_ERROR,"unregister histogram",e);
					}
				}
			}
			finally 
			{
				writeLock.unlock();
			}
		}
	}
	
	public void registerTimer(IExtensibleTimer timer)
	{
		Timer timerBean = null;
		readLock.lock();
		try
		{
			timerBean = this.timerIndex.get(timer);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(timerBean == null)
		{
			writeLock.lock();
			try
			{
				timerBean = this.timerIndex.get(timer);
				if(timerBean == null)
				{
					try
					{
						ObjectName objectName = new ObjectName(this.objectNamePrefix + ",metric=timer,name=" + timer.getName());
						timerBean = new Timer(objectName,timer);
						this.timerIndex.put(timer,timerBean);
						ManagementFactory.getPlatformMBeanServer().registerMBean(timerBean,objectName);
					}
					catch (Exception e) 
					{
						extension.log(LogService.LOG_ERROR,"register timer",e);
					}
				}
			}
			finally 
			{
				writeLock.unlock();
			}
		}
	}
	
	public void unregisterTimer(IExtensibleTimer timer)
	{
		Timer timerBean = null;
		readLock.lock();
		try
		{
			timerBean = this.timerIndex.get(timer);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(timerBean == null)
		{
			writeLock.lock();
			try
			{
				timerBean = this.timerIndex.remove(timer);
				if(timerBean != null)
				{
					try
					{
						ManagementFactory.getPlatformMBeanServer().unregisterMBean(timerBean.timerObjectName);
					}
					catch (Exception e) 
					{
						extension.log(LogService.LOG_ERROR,"unregister timer",e);
					}
				}
			}
			finally 
			{
				writeLock.unlock();
			}
		}
	}
	
	
	public void registerGauge(IExtensibleGauge<?> gauge)
	{
		Gauge gaugeBean = null;
		readLock.lock();
		try
		{
			gaugeBean = this.gaugeIndex.get(gauge);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(gaugeBean == null)
		{
			writeLock.lock();
			try
			{
				gaugeBean = this.gaugeIndex.get(gauge);
				if(gaugeBean == null)
				{
					try
					{
						ObjectName objectName = new ObjectName(this.objectNamePrefix + ",metric=gauge,name=" + gauge.getName());
						gaugeBean = new Gauge(objectName,gauge);
						this.gaugeIndex.put(gauge,gaugeBean);
						ManagementFactory.getPlatformMBeanServer().registerMBean(gaugeBean,objectName);
					}
					catch (Exception e) 
					{
						extension.log(LogService.LOG_ERROR,"register gauge",e);
					}
				}
			}
			finally 
			{
				writeLock.unlock();
			}
		}
	}
	
	public void unregisterGauge(IExtensibleGauge<?> gauge)
	{
		Gauge gaugeBean = null;
		readLock.lock();
		try
		{
			gaugeBean = this.gaugeIndex.get(gauge);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(gaugeBean == null)
		{
			writeLock.lock();
			try
			{
				gaugeBean = this.gaugeIndex.remove(gauge);
				if(gaugeBean != null)
				{
					try
					{
						ManagementFactory.getPlatformMBeanServer().unregisterMBean(gaugeBean.gaugeObjectName);
					}
					catch (Exception e) 
					{
						extension.log(LogService.LOG_ERROR,"unregister gauge",e);
					}
				}
			}
			finally 
			{
				writeLock.unlock();
			}
		}
	}
}
