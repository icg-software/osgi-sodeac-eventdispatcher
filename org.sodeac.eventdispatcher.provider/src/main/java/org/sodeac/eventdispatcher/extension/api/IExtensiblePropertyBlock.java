package org.sodeac.eventdispatcher.extension.api;

import org.sodeac.eventdispatcher.api.IPropertyBlock;

public interface IExtensiblePropertyBlock extends IPropertyBlock
{
	public void addModifyListener(IPropertyBlockModifyListener listener);
	public void removeModifyListener(IPropertyBlockModifyListener listener);
	public void dispose();
}
