package org.sodeac.common.eip;

import org.sodeac.common.eip.IMessageDrivenConversation.IChannel.IChannelPolicy;

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
