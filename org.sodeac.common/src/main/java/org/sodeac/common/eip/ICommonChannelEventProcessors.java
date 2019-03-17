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
package org.sodeac.common.eip;


import java.util.function.Consumer;

import org.sodeac.common.eip.IMessageDrivenConversation.IChannel;
import org.sodeac.common.eip.IMessageDrivenConversation.IChannel.IChannelEvent;
import org.sodeac.common.eip.IMessageDrivenConversation.IChannel.IChannelEventProcessor;

public interface ICommonChannelEventProcessors
{
	public interface IChannelErrorProcessor  extends IChannelEventProcessor
	{
		public IChannelEventProcessor onChannelEvent(Consumer<IChannelError> consumer);
	}
	
	public interface IChannelError extends IChannelEvent
	{
		public enum ErrorType {ON_TRANSPORT, ON_SUPPLY, ON_CONSUME, ON_TIMEOUT}
		
		public Throwable getThrowable();
		public ErrorType getType();
		
		public IChannel<?> getChannel();
	}

	public interface IChannelCloseProcessor  extends IChannelEventProcessor
	{
		public <T> IChannelEventProcessor onChannelEvent(Consumer<IChannelClose> consumer);
	}
	
	public interface IChannelClose extends IChannelEvent
	{
		public enum ErrorType {SUPPLIER,CONSUMNER}
		
		public int getCountSupplier();
		public int getCountConsumer();
		
		public IChannel<?> getChannel();
	}
}
