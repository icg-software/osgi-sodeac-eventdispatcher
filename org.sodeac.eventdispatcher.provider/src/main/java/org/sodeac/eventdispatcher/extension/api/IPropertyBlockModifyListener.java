package org.sodeac.eventdispatcher.extension.api;

public interface IPropertyBlockModifyListener
{
	public static enum ModifyType {INSERT,UPDATE,REMOVE}
	
	public void onModify(ModifyType type,String key, Object valueOld, Object valueNew);
}
