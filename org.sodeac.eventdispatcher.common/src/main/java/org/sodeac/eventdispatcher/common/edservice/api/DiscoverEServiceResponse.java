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
package org.sodeac.eventdispatcher.common.edservice.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiscoverEServiceResponse
{
	public DiscoverEServiceResponse(List<IEServiceReference> serviceReferenceList )
	{
		super();
		if(serviceReferenceList == null)
		{
			serviceReferenceList = new ArrayList<IEServiceReference>();
		}
		this.serviceReferenceList = Collections.unmodifiableList(serviceReferenceList);
	}
	
	private List<IEServiceReference> serviceReferenceList = null;

	public List<IEServiceReference> getServiceReferenceList()
	{
		return serviceReferenceList;
	}
}
