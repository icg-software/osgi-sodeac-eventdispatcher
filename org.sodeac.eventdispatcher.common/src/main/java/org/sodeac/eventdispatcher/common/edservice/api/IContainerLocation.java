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

import java.util.UUID;

public interface IContainerLocation
{
	public static final UUID LOCAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
	public static final String LOCAL_NAME = "local";
	
	public UUID getClusterId();
	public String getClusterName();
	public UUID getDataCenterId();
	public String getDataCenterName();
	public UUID getMachineId();
	public String getMachineName();
	public UUID getContainerId();
	public String getContainerName();
}
