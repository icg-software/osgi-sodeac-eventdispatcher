package org.sodeac.eventdispatcher.impl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.Filter;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.impl.QueueImpl.LinkageDefinitionDispatcherBuilder.AddLinkageDefinition;
import org.sodeac.multichainlist.IListEventHandler;
import org.sodeac.multichainlist.LinkageDefinition;
import org.sodeac.multichainlist.Linker;
import org.sodeac.multichainlist.MultiChainList;
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
	public Linker<IQueuedEvent>.LinkageDefinitionContainer onCreateNodes(MultiChainList<IQueuedEvent> multiChainList, Collection<IQueuedEvent> elements, Linker<IQueuedEvent>.LinkageDefinitionContainer linkageDefinitionContainer, LinkMode linkMode)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onDisposeNode(MultiChainList<IQueuedEvent> multiChainList, IQueuedEvent element)
	{
		// TODO Auto-generated method stub
		
	}

	

}
