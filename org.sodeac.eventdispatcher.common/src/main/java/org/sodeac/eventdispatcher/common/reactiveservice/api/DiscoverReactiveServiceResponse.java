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
package org.sodeac.eventdispatcher.common.reactiveservice.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiscoverReactiveServiceResponse
{
	public DiscoverReactiveServiceResponse(List<IReactiveServiceReference> serviceReferenceList )
	{
		super();
		if(serviceReferenceList == null)
		{
			serviceReferenceList = new ArrayList<IReactiveServiceReference>();
		}
		this.serviceReferenceList = Collections.unmodifiableList(serviceReferenceList);
	}
	
	private List<IReactiveServiceReference> serviceReferenceList = null;

	public List<IReactiveServiceReference> getServiceReferenceList()
	{
		return serviceReferenceList;
	}
}
