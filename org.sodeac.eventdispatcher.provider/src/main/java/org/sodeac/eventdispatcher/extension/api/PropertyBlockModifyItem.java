package org.sodeac.eventdispatcher.extension.api;

import org.sodeac.eventdispatcher.extension.api.IPropertyBlockModifyListener.ModifyType;

public class PropertyBlockModifyItem
{
	public PropertyBlockModifyItem(ModifyType type,String key, Object valueOld, Object valueNew)
	{
		super();
		this.type = type;
		this.key = key;
		this.valueOld = valueOld;
		this.valueNew = valueNew;
	}
	
	private ModifyType type;
	private String key;
	private Object valueOld; 
	private Object valueNew;
	
	public ModifyType getType()
	{
		return type;
	}
	public void setType(ModifyType type)
	{
		this.type = type;
	}
	public String getKey()
	{
		return key;
	}
	public void setKey(String key)
	{
		this.key = key;
	}
	public Object getValueOld()
	{
		return valueOld;
	}
	public void setValueOld(Object valueOld)
	{
		this.valueOld = valueOld;
	}
	public Object getValueNew()
	{
		return valueNew;
	}
	public void setValueNew(Object valueNew)
	{
		this.valueNew = valueNew;
	}
}
