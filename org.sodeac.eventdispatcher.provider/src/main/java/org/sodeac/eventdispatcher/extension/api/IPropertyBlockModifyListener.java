package org.sodeac.eventdispatcher.extension.api;

import java.util.List;

public interface IPropertyBlockModifyListener
{
	public static enum ModifyType {INSERT,UPDATE,REMOVE}
	
	public void onModify(ModifyType type,String key, Object valueOld, Object valueNew);
	public void onModifySet(List<PropertyBlockModifyItem> modifySet);
}
