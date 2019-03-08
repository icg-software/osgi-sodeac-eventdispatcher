/*******************************************************************************
 * Copyright (c) 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.common.flow.impl;

import java.util.Arrays;
import java.util.List;

import org.sodeac.eventdispatcher.api.IQueueController;
import org.sodeac.eventdispatcher.api.QueueComponentConfiguration;
import org.sodeac.multichainlist.LinkerBuilder;

public class ServiceChatComponent implements IQueueController
{

	@Override
	public List<QueueComponentConfiguration> configureQueueController()
	{
		/*return Arrays.asList(new QueueComponentConfiguration[] 
		{
			new QueueComponentConfiguration.BoundedByQueueId(QUEUE_ID),
			new QueueComponentConfiguration.ChainDispatcherRuleConfiguration("chain1rule", (event) -> true).setLinksToAdd(LinkerBuilder.newBuilder().linkIntoChain("chain1"))
		});*/
		return null;
	}

}
