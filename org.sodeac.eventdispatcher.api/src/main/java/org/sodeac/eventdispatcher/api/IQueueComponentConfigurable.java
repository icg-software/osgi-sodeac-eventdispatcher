/******
 * *************************************************************************
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

import java.util.List;

/**
 * 
 * @author Sebastian Palarus
 *
 */
public interface IQueueComponentConfigurable extends IQueueComponent
{
	/**
	 * 
	 * 
	 * @return list of configuration to configure queue controller
	 */
	public List<QueueComponentConfiguration> configureQueueController();
	
	/**
	 * 
	 * @return list of configuration to configure queue service
	 */
	public List<QueueComponentConfiguration> configureQueueService();
}
