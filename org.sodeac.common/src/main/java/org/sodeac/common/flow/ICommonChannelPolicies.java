package org.sodeac.common.flow;

import org.sodeac.common.flow.IMessageDrivenConversation.IChannel.IChannelPolicy;

public interface ICommonChannelPolicies
{
	public interface IPreMessageRequest extends IChannelPolicy
	{
		public IPreMessageRequest ifChannelMessageSizeLessThen(int value);
		public IPreMessageRequest thenPreRequestForNext(int value);
		
		/**
		 * dummy method for nicer fluent api
		 */
		public void messages();
	}
}
