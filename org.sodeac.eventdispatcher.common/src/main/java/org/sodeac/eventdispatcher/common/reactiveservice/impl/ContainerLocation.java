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
package org.sodeac.eventdispatcher.common.reactiveservice.impl;

import java.util.Map;
import java.util.UUID;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.common.reactiveservice.api.IContainerLocation;

@Component(service=IContainerLocation.class, immediate=true)
public class ContainerLocation implements IContainerLocation
{
	private volatile ComponentContext context = null;
	
	private UUID clusterId = IContainerLocation.LOCAL_ID;
	private String clusterName = IContainerLocation.LOCAL_NAME;
	private UUID dataCenterId = IContainerLocation.LOCAL_ID;
	private String dataCenterName = IContainerLocation.LOCAL_NAME;
	private UUID machineId = IContainerLocation.LOCAL_ID;
	private String machineName = IContainerLocation.LOCAL_NAME;
	private UUID containerId = IContainerLocation.LOCAL_ID;
	private String containerName = IContainerLocation.LOCAL_NAME;
	
	@Activate
	private void activate(ComponentContext context, Map<String, ?> properties)
	{
		this.context = context;
	}
	
	@Deactivate
	private void deactivate(ComponentContext context, Map<String, ?> properties)
	{
		this.context = null;
	}
	
	@Override
	public UUID getClusterId()
	{
		return this.clusterId;
	}

	@Override
	public String getClusterName()
	{
		return this.clusterName;
	}

	@Override
	public UUID getDataCenterId()
	{
		return this.dataCenterId;
	}

	@Override
	public String getDataCenterName()
	{
		return this.dataCenterName;
	}

	@Override
	public UUID getMachineId()
	{
		return this.machineId;
	}

	@Override
	public String getMachineName()
	{
		return this.machineName;
	}

	@Override
	public UUID getContainerId()
	{
		return containerId;
	}

	@Override
	public String getContainerName()
	{
		return containerName;
	}

	@Reference(cardinality=ReferenceCardinality.MULTIPLE,policy=ReferencePolicy.DYNAMIC)
	public void bindEventDispatcher(IEventDispatcher eventDispatcher)
	{
		IContainerLocation clusterLocation = eventDispatcher.getPropertyBlock().getAdapter(IContainerLocation.class);
		if(clusterLocation == null)
		{
			// TODO lock
			eventDispatcher.getPropertyBlock().setAdapter(IContainerLocation.class, this);
			return;
		}
		if(clusterLocation != this)
		{
			// TODO warn
		}
	}
	
	public void unbindEventDispatcher(IEventDispatcher eventDispatcher)
	{
		IContainerLocation clusterLocation = eventDispatcher.getPropertyBlock().getAdapter(IContainerLocation.class);
		if(clusterLocation == null)
		{
			return;
		}
		if(clusterLocation == this)
		{
			// TODO unlock
			eventDispatcher.getPropertyBlock().removeAdapter(IContainerLocation.class);
			return ;
		}
		// TODO lock
	}
}
