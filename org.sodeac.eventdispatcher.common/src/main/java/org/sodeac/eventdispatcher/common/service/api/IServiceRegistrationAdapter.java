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
package org.sodeac.eventdispatcher.common.service.api;

import java.util.Dictionary;

import org.osgi.framework.Bundle;
public interface IServiceRegistrationAdapter
{
	public static final String SIGNAL_REGISTRATION_UPDATE = "SIGNAL_REGISTRATION_UPDATE";
	
	public void register(IEventDrivenService service, Dictionary<String, ?> properties, Bundle bundle);
	public void unregister(IEventDrivenService service, Dictionary<String, ?> properties, Bundle bundle);
	
	public void updateRegistrations();
	public boolean isEmpty();
}
