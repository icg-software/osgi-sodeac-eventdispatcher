package org.sodeac.eventdispatcher.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.osgi.framework.Filter;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueScope;

public class QueueScopeImpl extends QueueImpl implements IQueueScope
{
	private UUID scopeId;
	
	private String scopeName = null;
	private boolean adoptContoller = false;
	private boolean adoptServices = false;
	
	protected QueueScopeImpl(UUID scopeId,QueueImpl parent, String scopeName, boolean adoptContoller, boolean adoptServices, Map<String, Object> configurationProperties, Map<String, Object> stateProperties)
	{
		super(null,(EventDispatcherImpl)parent.getDispatcher(), parent.isMetricsEnabled(), null, null,configurationProperties,stateProperties);
		
		super.parent = parent;
		this.scopeName = scopeName;
		this.adoptContoller = adoptContoller;
		this.adoptServices = adoptServices;
		this.scopeId = scopeId;
		super.queueId = parent.getQueueId() + "." + this.scopeId.toString();
	}

	@Override
	public IQueue getGlobal()
	{
		return parent;
	}

	@Override
	public UUID getScopeId()
	{
		return scopeId;
	}

	@Override
	public String getScopeName()
	{
		return this.scopeName;
	}
	
	public boolean isAdoptContoller()
	{
		return adoptContoller;
	}

	public boolean isAdoptServices()
	{
		return adoptServices;
	}

	@Override
	public void dispose()
	{
		super.dispose();
	}

	public IQueueScope createScope(String scopeName, Map<String, Object> configurationProperties, Map<String, Object> stateProperties)
	{
		return null;
	}

	@Override
	public List<IQueueScope> getScopes()
	{
		return null;
	}


	@Override
	public List<IQueueScope> getScopes(Filter filter)
	{
		return null;
	}

}
