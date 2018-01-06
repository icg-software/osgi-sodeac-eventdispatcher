/*******************************************************************************
 * Copyright (c) 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.api;

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
	 * getter for scope name
	 * 
	 * @return human readable name of this scope (or null if nut defined)
	 */
	public String getScopeName();
	
	/**
	 * dispose this scope and remove it from global scope
	 */
	public void dispose();
}
