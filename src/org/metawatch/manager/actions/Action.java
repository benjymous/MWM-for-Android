package org.metawatch.manager.actions;

import android.content.Context;

public interface Action {
	public String getName();
	public String bulletIcon();
	public int performAction(Context context);
}