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


public interface ILifecycle
{
	public interface ILifecycleEvent
	{
		public ILifecycleState getContemporaryState();
	}
	
	public interface IHealthCheckType{}
	
	public String getId();
	public String getType();
	public ILifecycleState getContemporaryState();
	
	public interface ILifecycleState
	{
		public ILifecycle getLifecycle();
		
		public interface IUninstalledState extends ILifecycleState
		{
			public interface IOnUninstallEvent extends ILifecycleEvent{}
		}
		
		public interface IInstalledState extends IUninstalledState,ILifecycleState
		{
			public interface IOnInstallEvent extends ILifecycleEvent{}
		}
		
		public interface IStartUpState extends IInstalledState,ILifecycleState
		{
			public interface IOnStartUpEvent extends ILifecycleEvent{}
			public interface IConfigureEvent extends ILifecycleEvent{}
			public interface ICheckRequirement extends ILifecycleEvent{}
			public interface IBootstrapEvent extends ILifecycleEvent{}
			public interface ILoadStateEvent extends ILifecycleEvent{}
			public interface IHealthCheckOnStartUpEvent extends ILifecycleEvent, IHealthCheckType{}
		}
		
		public interface IBrokenState extends IStartUpState,ILifecycleState
		{
			public interface IOnBrokenEvent extends ILifecycleEvent{}
			public interface IHealingEvent extends ILifecycleEvent{}
			public interface IHealthCheckOnBrokenEvent extends ILifecycleEvent, IHealthCheckType{}
		}
		
		public interface IReadyState extends IStartUpState,ILifecycleState
		{
			public interface IOnReadyEvent extends ILifecycleEvent{}
			public interface IHealthCheckOnReadyEvent extends ILifecycleEvent, IHealthCheckType{}
			public interface IStartIOEvent extends ILifecycleEvent{}
		}
		
		public interface IActiveState extends IReadyState,ILifecycleState
		{
			public interface IOnActiveEvent extends ILifecycleEvent{}
			public interface IHealthCheckOnActiveEvent extends ILifecycleEvent, IHealthCheckType{}
			public interface IStopIOEvent extends ILifecycleEvent{}
		}
		
		public interface IShutDownState extends IStartUpState,ILifecycleState
		{
			public interface IOnShutDownEvent extends ILifecycleEvent{}
			public interface ISaveStateEvent extends ILifecycleEvent{}
			public interface IDisposeEvent extends ILifecycleEvent{}
		}
	}
}
