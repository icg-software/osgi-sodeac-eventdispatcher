package org.sodeac.eventdispatcher.impl;

public class QueueBindingModifyFlags
{
	private boolean globalSet = false;
	private boolean globalAdd = false;
	private boolean globalRemove = false;
	private boolean scopeSet = false;
	private boolean scopeAdd = false;
	private boolean scopeRemove = false;
	
	public void reset()
	{
		globalSet = false;
		globalAdd = false;
		globalRemove = false;
		scopeSet = false;
		scopeAdd = false;
		scopeRemove = false;
	}

	public boolean isGlobalSet()
	{
		return globalSet;
	}

	public void setGlobalSet(boolean globalSet)
	{
		this.globalSet = globalSet;
	}

	public boolean isGlobalAdd()
	{
		return globalAdd;
	}

	public void setGlobalAdd(boolean globalAdd)
	{
		this.globalAdd = globalAdd;
	}

	public boolean isGlobalRemove()
	{
		return globalRemove;
	}

	public void setGlobalRemove(boolean globalRemove)
	{
		this.globalRemove = globalRemove;
	}

	public boolean isScopeSet()
	{
		return scopeSet;
	}

	public void setScopeSet(boolean scopeSet)
	{
		this.scopeSet = scopeSet;
	}

	public boolean isScopeAdd()
	{
		return scopeAdd;
	}

	public void setScopeAdd(boolean scopeAdd)
	{
		this.scopeAdd = scopeAdd;
	}

	public boolean isScopeRemove()
	{
		return scopeRemove;
	}

	public void setScopeRemove(boolean scopeRemove)
	{
		this.scopeRemove = scopeRemove;
	}
}
