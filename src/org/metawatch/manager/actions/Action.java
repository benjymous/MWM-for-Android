package org.metawatch.manager.actions;

import org.metawatch.manager.apps.ApplicationBase;

import android.content.Context;

public abstract class Action {
    public static final int SECONDARY_NONE = 0;
    public static final int SECONDARY_RESET = 1;
    public static final int SECONDARY_EXIT = 2;

    public abstract String getName();

    public abstract String bulletIcon();

    public abstract int performAction(Context context);

    public String getId() {
	// Implement this and provide a unique ID to allow the action
	// to be launchable by code
	return null;
    }

    public boolean isHidden() {
	return false;
    }

    public int getSecondaryType() {
	return SECONDARY_NONE;
    }

    public int performSecondary(Context context) {
	return ApplicationBase.BUTTON_NOT_USED;
    }

    /*
     * -1 for Actions that never have a timestamp. 0 for "no timestamp right now". Otherwise a timestamp.
     */
    public long getTimestamp() {
	return -1;
    }

    public ContainerAction parent = null;

    public ContainerAction getParent() {
	return parent;
    }

    public void setParent(ContainerAction action) {
	parent = action;
    }
}