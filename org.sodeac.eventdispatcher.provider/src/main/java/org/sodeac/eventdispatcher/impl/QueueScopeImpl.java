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
	private QueueImpl parent;
	private String scopeName;
	private boolean adoptContoller;
	
	protected QueueScopeImpl(QueueImpl parent, String scopeName, boolean adoptContoller)
	{
		super(null,(EventDispatcherImpl)parent.getDispatcher(), parent.isMetricsEnabled(), null, null);
		
		this.parent = parent;
		this.scopeName = scopeName;
		this.adoptContoller = adoptContoller;
		this.scopeId = UUID.randomUUID();
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

	@Override
	public void dispose()
	{
		super.dispose();
		// TODO remove from parentScopeList
		// TODO remove registered Objects (Controller) Services?
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
