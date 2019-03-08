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
package org.sodeac.common;

import java.util.Optional;

public interface ILifecycle
{
	public interface ILifecycleEvent
	{
		public ILifecycleState getPreviousState();
		public ILifecycleState getNextState();
	}
	
	public String getId();
	public String getType();
	public ILifecycleState getContemporaryState();
	public <T> Optional<T> getProperty(String key, Class<T> clazz);
	
	public interface ILifecycleState
	{
		public interface IInstalledState extends ILifecycleState
		{
			public interface IInstallEvent extends ILifecycleEvent{}
			public interface IUninstallEvent extends ILifecycleEvent{}
		}
		
		public interface IStartUpState extends IInstalledState
		{
			public interface IConfigureEvent extends ILifecycleEvent{}
			public interface ICheckRequirement extends ILifecycleEvent{}
			public interface IBootstrapEvent extends ILifecycleEvent{}
			public interface ILoadStateEvent extends ILifecycleEvent{}
			public interface IHealthCheckEvent extends ILifecycleEvent{}
		}
		
		public interface IReadyState extends IStartUpState
		{
			public interface IStartIOEvent extends ILifecycleEvent{}
		}
		
		public interface IActiveState extends IReadyState
		{
			public interface IStopIOEvent extends ILifecycleEvent{}
		}
		
		public interface IShutdownState extends IInstalledState
		{
			public interface ISaveStateEvent extends ILifecycleEvent{}
			public interface IShutdownEvent extends ILifecycleEvent{}
		}
		
		public interface IBrokenState extends IInstalledState
		{
			public interface IHealingEvent extends ILifecycleEvent{}
		}
		
		public interface IUninstalledState extends ILifecycleState{}
	}
}
