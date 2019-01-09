/*******************************************************************************
 * Copyright (c) 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.osgi.framework.Filter;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueChildScope;

public class QueueSessionScopeImpl extends QueueImpl implements IQueueChildScope
{
	private UUID scopeId;
	private UUID parentScopeId;
	private String scopeName = null;
	private boolean adoptContoller = false;
	private boolean adoptServices = false;
	
	protected QueueSessionScopeImpl(UUID scopeId,UUID parentScopeId,QueueImpl parent, String scopeName, boolean adoptContoller, boolean adoptServices, Map<String, Object> configurationProperties, Map<String, Object> stateProperties)
	{
		super(parent.getId() + "." + scopeId.toString(),(EventDispatcherImpl)parent.getDispatcher(), parent.isMetricsEnabled(), null, null,configurationProperties,stateProperties);
		
		super.parent = parent;
		this.scopeName = scopeName;
		this.adoptContoller = adoptContoller;
		this.adoptServices = adoptServices;
		this.scopeId = scopeId;
		this.parentScopeId = parentScopeId;
		super.queueId = parent.getId() + "." + this.scopeId.toString();
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
	
	protected UUID getParentScopeId()
	{
		return this.parentScopeId;
	}
	
	protected void unlinkFromParent()
	{
		this.parentScopeId = null;
	}
	
	@Override
	public IQueueChildScope getParentScope()
	{
		return this.parentScopeId == null ? null : this.parent.getChildScope(this.parentScopeId);
	}

	@Override
	public List<IQueueChildScope> getChildScopes()
	{
		if(this.parentScopeId == null)
		{
			return Collections.unmodifiableList(new ArrayList<IQueueChildScope>());
		}
		return parent.getChildSessionScopes(this.parentScopeId);
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

	public IQueueChildScope createScope(String scopeName, Map<String, Object> configurationProperties, Map<String, Object> stateProperties)
	{
		return null; // TODO
	}


	@Override
	public List<IQueueChildScope> getChildScopes(Filter filter)
	{
		return null; // TODO
	}

}
