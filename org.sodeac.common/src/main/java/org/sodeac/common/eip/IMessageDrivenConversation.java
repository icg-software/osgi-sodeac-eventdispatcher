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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public interface IMessageDrivenConversation
{
	public Set<IChannel.IChannelDescription> getChannelCatalog();
	
	public default <T> IChannel<T> openChannel(Class<T> messageClass)
	{
		return openChannel(messageClass, IChannel.ChannelType.STREAM);
	}
	public <T> IChannel<T> openChannel(Class<T> messageClass,IChannel.ChannelType channelType);
	
	public IMessageDrivenConversation open();
	public IMessageDrivenConversation close();
	public boolean isClosed();
	
	public <A> A getAdapter(Class<A> adapterClass);
	
	public interface IChannel<T>
	{
		public enum ChannelType 
		{
			/**
			 * Stream to consume. If Consumed, the Message is removed
			 */
			STREAM,
			
			/**
			 * A Collection of tags. A tag (identified by equals) can only be one time in collection. 
			 * If a tag is supplied several times, the latest published tag replaces the previous item equals new tag 
			 */
			TAG, 
			
			/**
			 * A Single Object . 
			 */
			STATE
		}
		
		public IChannel<T> close(String reason);
		public boolean isClosed();
		public IChannelDescription getChannelDescription();
		
		public <P extends IChannelPolicy> Optional<P> getChannelPolicy(Class<P> type);
		public <P extends IChannelEventProcessor> Optional<P> getChannelEventProcessor(Class<P> type);
		
		public IChannel<T> onMessageRequested(Function<IMessageRequest<T>,T> messageSupplier);
		public IChannel<T> onMessageSupplied(Consumer<IMessageSupply<T>> messageConsumer);
		
		public IMessageDrivenConversation getConversation();
		
		public interface IChannelDescription
		{
			public ChannelType getChannelType();
		}
		
		public interface IMessageRequest<T>
		{
			public IChannel<T> getChannel();
		}
		
		public interface IMessageSupply<T>
		{
			public T value();
			
			public UUID getId();
			public long getChannelSequence();
			public long getConversationSequence();
			public IChannel<T> getChannel();
		}
		
		public interface IChannelPolicy
		{
		}
		
		public interface IChannelEvent
		{
		}
		
		public interface IChannelEventProcessor
		{
		}
	}
}
