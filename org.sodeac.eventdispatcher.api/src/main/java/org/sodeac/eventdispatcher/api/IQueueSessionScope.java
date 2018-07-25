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
package org.sodeac.eventdispatcher.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
/**
 *  API for a special scope in sessions event-queues. Session Scopes has a private event queue, job queue and propertyblock
 *  
 * @author Sebastian Palarus
 *
 */
public interface IQueueSessionScope extends IQueue
{
	/**
	 * returns global scope. global scope is a session creates this scope
	 *
	 * @return global scope
	 */
	public IQueue getGlobalScope();
	
	/**
	 * getter for scope id. ScopeId is unique key (by global session) addressed this scope
	 * 
	 * @return scope id
	 */
	public UUID getScopeId();
	
	/**
	 * getter for parent scope, if exists. The parent scope is defined by virtual tree structure
	 * 
	 * @return parentScope or null
	 */
	public IQueueSessionScope getParentScope();
	
	/**
	 * getter for child scope list. The child scopes list is defined by virtual tree structure.
	 * 
	 * @return immutable list of child scopes
	 */
	public List<IQueueSessionScope> getChildScopes();
	/**
	 * getter for scope name
	 * 
	 * @return human readable name of this scope (or null if nut defined)
	 */
	public String getScopeName();
	
	/**
	 * dispose this scope and remove it from global scope
	 */
	public void dispose();
	
	/**
	 * creates a {@link IQueueSessionScope} in global scope ({@link IQueue}) with this scope as parent scope.
	 * 
	 * @param scopeId unique id of scope (unique by queue) or null for auto-generation
	 * @param scopeName human readable name of scope (nullable)
	 * @param configurationProperties blue print for configuration propertyblock of new scope (nullable)
	 * @param stateProperties blue print for state propertyblock of new scope (nullable)
	 * 
	 * @return new scope, or null, if scope already exists
	 */
	public default IQueueSessionScope createSessionScope(UUID scopeId,String scopeName, Map<String,Object> configurationProperties, Map<String,Object> stateProperties)
	{
		return getGlobalScope().createSessionScope(scopeId, scopeName, this, configurationProperties, stateProperties, false, false);
	}
	
}
