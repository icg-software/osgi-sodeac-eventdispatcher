package org.sodeac.eventdispatcher.impl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.Filter;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.impl.QueueImpl.LinkageDefinitionDispatcherBuilder.AddLinkageDefinition;
import org.sodeac.multichainlist.IListEventHandler;
import org.sodeac.multichainlist.LinkageDefinition;
import org.sodeac.multichainlist.Partition.LinkMode;

public class LinkageDefinitionDispatcherImpl implements IListEventHandler<IQueuedEvent>
{
	private LinkedList<Filter> filterList;
	private LinkedList<AddLinkageDefinition> addLinkageDefinitionList;
	private LinkedList<String> removeLinkageDefinitionList;
	
	public LinkageDefinitionDispatcherImpl(LinkedList<Filter> filterList,LinkedList<AddLinkageDefinition> addLinkageDefinitionList,LinkedList<String> removeLinkageDefinitionList)
	{
		super();
		this.filterList = filterList;
		this.addLinkageDefinitionList = addLinkageDefinitionList;
		this.removeLinkageDefinitionList = removeLinkageDefinitionList;
	}

	@Override
	public List<LinkageDefinition<IQueuedEvent>> onCreateNodeList
	(
		Collection<IQueuedEvent> elements,
		List<LinkageDefinition<IQueuedEvent>> linkageDefinitions, LinkMode linkMode
	)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<LinkageDefinition<IQueuedEvent>> onCreateNode
	(
		IQueuedEvent element,
		List<LinkageDefinition<IQueuedEvent>> linkageDefinitions, LinkMode linkMode
	)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onClearNode(IQueuedEvent element){}

}
