package org.metawatch.manager.actions;

import android.content.Context;

public interface ResettableAction extends Action {
	public int performReset(Context context);
}