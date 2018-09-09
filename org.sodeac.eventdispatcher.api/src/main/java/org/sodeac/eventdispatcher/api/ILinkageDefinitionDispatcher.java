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
package org.sodeac.eventdispatcher.api;

import org.osgi.framework.Filter;
import org.sodeac.multichainlist.LinkageDefinition;

/**
 * A LinkageDefinitionDispatcher relinks a new event to non-default chains to provide the possibility to split events
 * by different aspects like responsibility, domains ....
 * 
 * @author Sebastian Palarus
 *
 */
public interface ILinkageDefinitionDispatcher
{
	/**
	 * declare match filter for events should relink to other chains
	 * 
	 * @param filter defines affected events
	 * 
	 * @return linkage definition dispatcher
	 */
	public ILinkageDefinitionDispatcher onMatchFilter(Filter filter);
	
	/**
	 * declare additional chain to link, if chain not already exists in definitions
	 * 
	 * @param chainName declare additional chain to link
	 * @param partitionName declare partition of additional chain to link
	 * 
	 * @return linkage definition dispatcher
	 */
	public ILinkageDefinitionDispatcher addLinkageDefinition(String chainName, String partitionName);
	
	
	/**
	 * remove chain from linkage definition list, if declared chain already exists in definitions 
	 * 
	 * @param chainName to remove
	 * 
	 * @return linkage definition dispatcher
	 */
	public ILinkageDefinitionDispatcher removeLinkageDefinition(String chainName);
}
