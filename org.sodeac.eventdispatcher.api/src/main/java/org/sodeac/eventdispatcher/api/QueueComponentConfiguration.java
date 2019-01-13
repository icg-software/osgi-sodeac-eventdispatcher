/*******************************************************************************
 * Copyright (c) 2018, 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.api;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Predicate;

import org.osgi.service.event.Event;
import org.sodeac.multichainlist.LinkerBuilder;

public abstract class QueueComponentConfiguration implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6854196332026025401L;
	
	/**
	 * specified scopes allowed to apply this configuration type
	 * 
	 * @return array of allowed scope
	 */
	public abstract Class<? extends IQueueComponent>[] getScopes();
	public abstract QueueComponentConfiguration copy(); 
	
	private QueueComponentConfiguration()
	{
		super();
	}
	
	private String dispatcherId = EventDispatcherConstants.DEFAULT_DISPATCHER_ID;
	private String name =  null;
	private String category = null;
	private MetricsRequirement queueMetricsRequirement = MetricsRequirement.PreferMetrics;
	private PrivateQueueWorkerRequirement privateQueueWorkerRequirement = PrivateQueueWorkerRequirement.NoPreferenceOrRequirement;
	
	/**
	 * getter for dispatcherId
	 * 
	 * @return id of dispatcher (NULL for all dispatchers ) managed the queue
	 */
	protected String getDispatcherId()
	{
		return dispatcherId;
	}
	
	/**
	 * setter for dispatcher id
	 * 
	 * @param dispatcherId id of dispatcher (NULL for all dispatchers ) managed the queue
	 * 
	 * @return queue component configuration
	 */
	protected QueueComponentConfiguration setDispatcherId(String dispatcherId)
	{
		this.dispatcherId = dispatcherId;
		return this;
	}
	
	/**
	 * getter for name of queue component
	 * 
	 * @return name of queue component
	 */
	protected String getName()
	{
		return name;
	}
	
	/**
	 * setter for name of queue component
	 * 
	 * @param name name of queue component
	 * @return component configuration
	 */
	protected QueueComponentConfiguration setName(String name)
	{
		this.name = name;
		return this;
	}
	
	/**
	 * getter for category of queue component
	 * 
	 * @return category of queue component
	 */
	protected String getCategory()
	{
		return category;
	}
	
	/**
	 * setter for category of queue component
	 * 
	 * @param category category of queue component
	 * @return component configuration
	 */
	protected QueueComponentConfiguration setCategory(String category)
	{
		this.category = category;
		return this;
	}
	
	/**
	 * getter for queue metrics requirement
	 * 
	 * @return necessity to use queue metrics or not
	 */
	protected MetricsRequirement getQueueMetricsRequirement()
	{
		return queueMetricsRequirement;
	}

	/**
	 * setter for metrics requirement
	 * 
	 * @param metricsRequirement declares necessity to use queue metrics or not
	 * 
	 * @return queue component configuration
	 */
	protected QueueComponentConfiguration setQueueMetricsRequirement(MetricsRequirement queueMetricsRequirement)
	{
		this.queueMetricsRequirement = queueMetricsRequirement;
		return this;
	}
	
	/**
	 * getter for  private queue worker requirement
	 * 
	 * @return necessity to use same queue worker thread for synchronized queue activities at all times
	 */
	protected PrivateQueueWorkerRequirement getPrivateQueueWorkerRequirement()
	{
		return privateQueueWorkerRequirement;
	}

	/**
	 * setter for  private queue worker requirement
	 * 
	 * @param privateQueueWorkerRequirement declares necessity to use same queue worker thread for synchronized queue activities at all times
	 * @return queue component configuration
	 */
	protected QueueComponentConfiguration setPrivateQueueWorkerRequirement(PrivateQueueWorkerRequirement privateQueueWorkerRequirement)
	{
		this.privateQueueWorkerRequirement = privateQueueWorkerRequirement;
		return this;
	}
	
	/**
	 * Configuration to bind a {@link IQueueComponent} (a {@link IQueueController} or a {@link IQueueService}) to the {@link IQueue} 
	 * with specified queueId managed by {@link IEventDispatcher} with specified id. 
	 * 
	 * 
	 * @author Sebastian Palarus
	 *
	 */
	public final static class BoundedByQueueId extends QueueComponentConfiguration
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 6587259399825230943L;
		
		private String queueId;
		private boolean autoCreateQueue = true;
		
		/**
		 * Constructor to  bind a {@link IQueueComponent} to the {@link IQueue} with specified queueId.
		 * 
		 * @param queueId id of queue
		 */
		public BoundedByQueueId(String queueId)
		{
			super();
			this.queueId = queueId;
		}
		
		/**
		 * getter for queue id
		 * 
		 * @return id of queue
		 */
		public String getQueueId()
		{
			return queueId;
		}
		
		@Override
		public String getName()
		{
			return super.getName();
		}

		@Override
		public BoundedByQueueId setName(String name)
		{
			return (BoundedByQueueId)super.setName(name);
		}

		@Override
		public String getCategory()
		{
			return super.getCategory();
		}

		@Override
		public BoundedByQueueId setCategory(String category)
		{
			return (BoundedByQueueId)super.setCategory(category);
		}

		/**
		 * setter for auto create queue mode
		 * 
		 * @param autoCreateQueue if true, dispatcher creates automatically a queue with {@code queueId} if it still does not exist
		 * @return queue component configuration
		 */
		public BoundedByQueueId setAutoCreateQueue(boolean autoCreateQueue)
		{
			this.autoCreateQueue = autoCreateQueue;
			return this;
		}
		

		/**
		 * getter for auto create queue mode
		 * 
		 * @return true, if queue should automatically create, if not exists. return false, if not
		 */
		public boolean isAutoCreateQueue()
		{
			return autoCreateQueue;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Class<? extends IQueueComponent>[] getScopes()
		{
			return new Class[] {IQueueController.class,IQueueService.class};
		}

		@Override
		public BoundedByQueueId setDispatcherId(String dispatcherId)
		{
			return (BoundedByQueueId)super.setDispatcherId(dispatcherId);
		}

		@Override
		public BoundedByQueueId setQueueMetricsRequirement(MetricsRequirement queueMetricsRequirement)
		{
			return (BoundedByQueueId)super.setQueueMetricsRequirement(queueMetricsRequirement);
		}

		@Override
		public BoundedByQueueId setPrivateQueueWorkerRequirement(PrivateQueueWorkerRequirement privateQueueWorkerRequirement)
		{
			return (BoundedByQueueId)super.setPrivateQueueWorkerRequirement(privateQueueWorkerRequirement);
		}

		@Override
		public String getDispatcherId()
		{
			return super.getDispatcherId();
		}

		@Override
		public MetricsRequirement getQueueMetricsRequirement()
		{
			return super.getQueueMetricsRequirement();
		}

		@Override
		public PrivateQueueWorkerRequirement getPrivateQueueWorkerRequirement()
		{
			return super.getPrivateQueueWorkerRequirement();
		}

		@Override
		public BoundedByQueueId copy() 
		{
			return new BoundedByQueueId(this.queueId)
					.setDispatcherId(super.dispatcherId)
					.setName(super.name)
					.setCategory(super.category)
					.setAutoCreateQueue(this.autoCreateQueue)
					.setPrivateQueueWorkerRequirement(super.privateQueueWorkerRequirement)
					.setQueueMetricsRequirement(super.queueMetricsRequirement);
		}
		
		
	}
	
	/**
	 * Configuration to bind a {@link IQueueComponent} (a {@link IQueueController} or a {@link IQueueService}) to existing {@link IQueue}s 
	 * whose properties match a ldapfilter.
	 * 
	 * @author Sebastian Palarus
	 *
	 */
	public final static class BoundedByQueueConfiguration extends QueueComponentConfiguration
	{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 7663354400942715419L;
		
		private String ldapFilter;
		
		/**
		 * Constructor to  bind a {@link IQueueComponent} to existing {@link IQueue}s whose properties match the specified ldap-filter.
		 * 
		 */
		public BoundedByQueueConfiguration(String ldapFilter)
		{
			super();
			this.ldapFilter = ldapFilter;
		}
		
		/**
		 * getter for ldap filter
		 * 
		 * @return ldap filter
		 */
		public String getLdapFilter()
		{
			return ldapFilter;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Class<? extends IQueueComponent>[] getScopes()
		{
			return new Class[] {IQueueController.class,IQueueService.class};
		}
		
		@Override
		public BoundedByQueueConfiguration setDispatcherId(String dispatcherId)
		{
			return (BoundedByQueueConfiguration)super.setDispatcherId(dispatcherId);
		}

		@Override
		public BoundedByQueueConfiguration setQueueMetricsRequirement(MetricsRequirement queueMetricsRequirement)
		{
			return (BoundedByQueueConfiguration)super.setQueueMetricsRequirement(queueMetricsRequirement);
		}

		@Override
		public BoundedByQueueConfiguration setPrivateQueueWorkerRequirement(PrivateQueueWorkerRequirement privateQueueWorkerRequirement)
		{
			return (BoundedByQueueConfiguration)super.setPrivateQueueWorkerRequirement(privateQueueWorkerRequirement);
		}
		
		@Override
		public String getDispatcherId()
		{
			return super.getDispatcherId();
		}
		
		@Override
		public String getName()
		{
			return super.getName();
		}

		@Override
		public BoundedByQueueConfiguration setName(String name)
		{
			return (BoundedByQueueConfiguration)super.setName(name);
		}

		@Override
		public String getCategory()
		{
			return super.getCategory();
		}

		@Override
		public BoundedByQueueConfiguration setCategory(String category)
		{
			return (BoundedByQueueConfiguration)super.setCategory(category);
		}

		@Override
		public MetricsRequirement getQueueMetricsRequirement()
		{
			return super.getQueueMetricsRequirement();
		}

		@Override
		public PrivateQueueWorkerRequirement getPrivateQueueWorkerRequirement()
		{
			return super.getPrivateQueueWorkerRequirement();
		}
		
		@Override
		public BoundedByQueueConfiguration copy() 
		{
			return new BoundedByQueueConfiguration(this.ldapFilter)
					.setDispatcherId(super.dispatcherId)
					.setName(super.name)
					.setCategory(super.category)
					.setPrivateQueueWorkerRequirement(super.privateQueueWorkerRequirement)
					.setQueueMetricsRequirement(super.queueMetricsRequirement);
		}
	}
	
	/**
	 * Subscribe a {@link IQueueComponent} to consume an event. The event will append to queue and  processed by all {@link IQueueController}s implements {@link IOnQueuedEvent}.
	 * 
	 * @author Sebastian Palarus
	 *
	 */	
	public static class SubscribeEvent extends QueueComponentConfiguration
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 5123757398142644193L;
		
		private EventType eventType = EventType.PublishedByEventAdmin;
		private String topic;
		private String filter;
		
		/**
		 * Constructor to subscribe a {@link IQueueComponent} to consume an event. The event will append to queue and  processed by all {@link IQueueController}s implements {@link IOnQueuedEvent}.
		 * <br><br>
		 * Note: A subscription for event type {@link EventType#QueuedByEventDispatcher} is not possible. Queued events will automatically deliver to addressed queues.
		 * 
		 * @param topic event topic
		 * @param filter ldap match filter to event 
		 * @param eventTypes type of event to subscribe. suitable types: {@link EventType#PublishedByEventAdmin}, {@link EventType#PublishedByEventDispatcher} or {@link EventType#PublishedByAny}
		 */
		public SubscribeEvent(String topic, String filter, EventType eventType)
		{
			super();
			this.topic = topic;
			this.filter = filter;
			this.eventType = eventType;
		}

		/**
		 * getter for event type
		 * 
		 * @return event type
		 */
		public EventType getEventType()
		{
			return eventType;
		}
		
		/**
		 * getter for topic
		 * 
		 * @return event topic 
		 */
		public String getTopic()
		{
			return topic;
		}

		/**
		 * getter for ldap match filter
		 * 
		 * @return ldap match filter
		 */
		public String getLdapFilter()
		{
			return filter;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Class<? extends IQueueComponent>[] getScopes()
		{
			return new Class[] {IQueueController.class,IQueueService.class};
		}
		
		@Override
		public SubscribeEvent copy() 
		{
			return new SubscribeEvent(this.topic, this.filter, this.eventType);
		}
	}
	
	/**
	 * Configuration for Queue Services
	 * 
	 * @author Sebastian Palarus
	 *
	 */
	public static class QueueServiceConfiguration extends QueueComponentConfiguration
	{

		public QueueServiceConfiguration(String serviceId)
		{
			super();
			this.serviceId = serviceId;
		}
		
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 7301276962907883651L;
		
		private String serviceId;
		private long timeOutInMS = -1;
		private long heartbeatTimeOutInMS = -1;
		private long startDelayInMS = 0;
		private long periodicRepetitionIntervalMS = -1;
		private MetricsRequirement taskMetricsRequirement = MetricsRequirement.PreferMetrics;
		
		@Override
		public String getDispatcherId()
		{
			return super.getDispatcherId();
		}
		
		@Override
		public String getName()
		{
			return super.getName();
		}

		@Override
		public QueueServiceConfiguration setName(String name)
		{
			return (QueueServiceConfiguration)super.setName(name);
		}

		@Override
		public String getCategory()
		{
			return super.getCategory();
		}
		
		@Override
		public QueueServiceConfiguration setCategory(String category)
		{
			return (QueueServiceConfiguration)super.setCategory(category);
		}
		
		
		/**
		 * getter for service timeout in ms
		 * 
		 * @return service timeout in ms
		 */
		public long getTimeOutInMS()
		{
			return timeOutInMS;
		}
		

		/**
		 * 
		 * setter for service timeout in ms
		 * 
		 * @param timeOutInMS service timeout in ms
		 * @return queue service configuration
		 */
		public QueueServiceConfiguration setTimeOutInMS(long timeOutInMS)
		{
			this.timeOutInMS = timeOutInMS;
			return this;
		}

		/**
		 * getter for service heartbeat timeout in ms
		 * 
		 * @return service heartbeat timeout
		 */
		public long getHeartbeatTimeOutInMS()
		{
			return heartbeatTimeOutInMS;
		}

		/**
		 * setter for service heartbeat timeout in ms
		 * 
		 * @param heartbeatTimeOutInMS service heartbeat timeout in ms
		 * @return queue service configuration
		 */
		public QueueServiceConfiguration setHeartbeatTimeOutInMS(long heartbeatTimeOutInMS)
		{
			this.heartbeatTimeOutInMS = heartbeatTimeOutInMS;
			return this;
		}

		/**
		 * getter for start delay of service in ms
		 * 
		 * @return start delay of service in ms
		 */
		public long getStartDelayInMS()
		{
			return startDelayInMS;
		}

		/**
		 * setter for start delay of service in ms
		 * 
		 * @param startDelayInMS start delay of service in ms
		 * @return queue service configuration
		 */
		public QueueServiceConfiguration setStartDelayInMS(long startDelayInMS)
		{
			this.startDelayInMS = startDelayInMS;
			return this;
		}

		/**
		 * getter for periodic repetition interval of service in ms
		 * @return periodic repetition interval of service in ms
		 */
		public long getPeriodicRepetitionIntervalMS()
		{
			return periodicRepetitionIntervalMS;
		}

		/**
		 * setter for periodic repetition interval of service in ms
		 * @param periodicRepetitionIntervalMS repetition interval of service in ms
		 * @return queue service configuration
		 */
		public QueueServiceConfiguration setPeriodicRepetitionIntervalMS(long periodicRepetitionIntervalMS)
		{
			this.periodicRepetitionIntervalMS = periodicRepetitionIntervalMS;
			return this;
		}
		
		@Override
		public QueueServiceConfiguration setQueueMetricsRequirement(MetricsRequirement queueMetricsRequirement)
		{
			return (QueueServiceConfiguration)super.setQueueMetricsRequirement(queueMetricsRequirement);
		}
		
		public QueueServiceConfiguration setTaskMetricsRequirement(MetricsRequirement taskMetricsRequirement)
		{
			this.taskMetricsRequirement = taskMetricsRequirement;
			return this;
		}

		@Override
		public QueueServiceConfiguration setPrivateQueueWorkerRequirement(PrivateQueueWorkerRequirement privateQueueWorkerRequirement)
		{
			return (QueueServiceConfiguration)super.setPrivateQueueWorkerRequirement(privateQueueWorkerRequirement);
		}

		@Override
		public MetricsRequirement getQueueMetricsRequirement()
		{
			return super.getQueueMetricsRequirement();
		}
		
		public MetricsRequirement getTaskMetricsRequirement()
		{
			return this.taskMetricsRequirement;
		}

		@Override
		public PrivateQueueWorkerRequirement getPrivateQueueWorkerRequirement()
		{
			return super.getPrivateQueueWorkerRequirement();
		}

		/**
		 * getter for service id
		 * 
		 * @return id of service
		 */
		public String getServiceId()
		{
			return serviceId;
		}


		@SuppressWarnings("unchecked")
		@Override
		public Class<? extends IQueueComponent>[] getScopes()
		{
			return new Class[] {IQueueService.class};
		}
		
		@Override
		public QueueServiceConfiguration copy() 
		{
			return new QueueServiceConfiguration(this.serviceId)
					.setName(super.name)
					.setCategory(super.category)
					.setTimeOutInMS(this.timeOutInMS)
					.setHeartbeatTimeOutInMS(this.heartbeatTimeOutInMS)
					.setStartDelayInMS(this.startDelayInMS)
					.setPeriodicRepetitionIntervalMS(this.periodicRepetitionIntervalMS);
		}
	}
	
	/**
	 * 
	 * @author Sebastian Palarus
	 *
	 */
	public static class RunTaskOnTriggerRuleConfiguration extends QueueComponentConfiguration
	{
		/**
		 * run by trigger
		 * :: reset service reference or task ...
		 * all conditions with and-op
		 * metrics trigger (needs adapter rule configuration)
		 * rule group id (to enable /disable set of rules)
		 * 
		 * 
		 */
		private static final long serialVersionUID = 3838680301290863139L;
		
		private String ruleId = null;
		private String chain = null; 
		private IQueueTask task = null;
		private Predicate<IQueuedEvent> eventPredicate = null;

		public RunTaskOnTriggerRuleConfiguration(String ruleId, String chain, IQueueTask task)
		{
			super();
			
			Objects.requireNonNull(task,"task is null");
			Objects.requireNonNull(ruleId,"ruleid is null");
			if(ruleId.isEmpty())
			{
				throw new RuntimeException("ruleid is empty");
			}
			this.ruleId = ruleId;
			this.chain = chain;
			this.task = task;
		}

		public Predicate<IQueuedEvent> getEventPredicate()
		{
			return eventPredicate;
		}

		public RunTaskOnTriggerRuleConfiguration setEventPredicate(Predicate<IQueuedEvent> eventPredicate)
		{
			this.eventPredicate = eventPredicate;
			return this;
		}

		public String getRuleId()
		{
			return ruleId;
		}

		public String getChain()
		{
			return chain;
		}

		public IQueueTask getTask()
		{
			return task;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Class<? extends IQueueComponent>[] getScopes()
		{
			return new Class[] {IQueueController.class,IQueueService.class};
		}

		@Override
		public RunTaskOnTriggerRuleConfiguration copy()
		{
			return new RunTaskOnTriggerRuleConfiguration(this.ruleId, this.chain, this.task).setEventPredicate(this.eventPredicate);
		}
		
	}
	
	public static class AdapterRuleConfiguration // TODO extends QueueComponentConfiguration
	{
		// TODO set adapter in configuration to attach other controller/services
	}
	
	/**
	 * Configuration to define the destination chains for queued events
	 * 
	 * @author Sebastian Palarus
	 *
	 */
	public static class ChainDispatcherRuleConfiguration extends QueueComponentConfiguration
	{	
		/**
		 * 
		 */
		private static final long serialVersionUID = 9166813733404434531L;
		
		private String ruleId = null;
		private Predicate<Event> eventPredicate = null;
		private LinkerBuilder linksToAdd  = null;
		private LinkerBuilder linksToRemove  = null;

		public ChainDispatcherRuleConfiguration(String ruleId, Predicate<Event> eventPredicate)
		{
			super();
			Objects.requireNonNull(ruleId,"ruleid is null");
			Objects.requireNonNull(eventPredicate,"predicate is null");
			if(ruleId.isEmpty())
			{
				throw new RuntimeException("ruleid is empty");
			}
			this.ruleId = ruleId;
			this.eventPredicate = eventPredicate;
		}
		
		public ChainDispatcherRuleConfiguration setLinksToAdd(LinkerBuilder linkerBuilder)
		{
			if(linkerBuilder != null)
			{
				this.linksToAdd = linkerBuilder;
			}
			return this;
		}
		
		public ChainDispatcherRuleConfiguration setLinksToRemove(LinkerBuilder linkerBuilder)
		{
			if(linkerBuilder != null)
			{
				this.linksToRemove = linkerBuilder;
			}
			return this;
		}

		public LinkerBuilder getLinksToAdd()
		{
			return linksToAdd;
		}

		public LinkerBuilder getLinksToRemove()
		{
			return linksToRemove;
		}

		public String getRuleId()
		{
			return ruleId;
		}

		public Predicate<Event> getEventPredicate()
		{
			return eventPredicate;
		}

		@SuppressWarnings("unchecked")
		public Class<? extends IQueueComponent>[] getScopes()
		{
			return new Class[] {IQueueController.class,IQueueService.class};
		}

		@Override
		public ChainDispatcherRuleConfiguration copy()
		{
			return new ChainDispatcherRuleConfiguration(this.ruleId,this.eventPredicate).setLinksToAdd(this.linksToAdd).setLinksToRemove(this.linksToRemove);
		}
	}
}
