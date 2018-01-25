package org.sodeac.eventdispatcher.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.osgi.framework.Filter;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueSessionScope;

public class QueueSessionScopeImpl extends QueueImpl implements IQueueSessionScope
{
	private UUID scopeId;
	
	private String scopeName = null;
	private boolean adoptContoller = false;
	private boolean adoptServices = false;
	
	protected QueueSessionScopeImpl(UUID scopeId,QueueImpl parent, String scopeName, boolean adoptContoller, boolean adoptServices, Map<String, Object> configurationProperties, Map<String, Object> stateProperties)
	{
		super(parent.getQueueId() + "." + scopeId.toString(),(EventDispatcherImpl)parent.getDispatcher(), parent.isMetricsEnabled(), null, null,configurationProperties,stateProperties);
		
		super.parent = parent;
		this.scopeName = scopeName;
		this.adoptContoller = adoptContoller;
		this.adoptServices = adoptServices;
		this.scopeId = scopeId;
		super.queueId = parent.getQueueId() + "." + this.scopeId.toString();
	}

	@Override
	public IQueue getGlobalScope()
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

	public IQueueSessionScope createScope(String scopeName, Map<String, Object> configurationProperties, Map<String, Object> stateProperties)
	{
		return null;
	}

	@Override
	public List<IQueueSessionScope> getSessionScopes()
	{
		return null;
	}


	@Override
	public List<IQueueSessionScope> getSessionScopes(Filter filter)
	{
		return null;
	}

}
