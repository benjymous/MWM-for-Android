package org.metawatch.manager.actions;

import java.util.ArrayList;
import java.util.List;

import org.metawatch.manager.apps.ApplicationBase;

import android.content.Context;

public abstract class ContainerAction extends Action {
    protected List<Action> subActions = new ArrayList<Action>();

    public void refreshSubActions(Context context) {
    }

    public List<Action> getSubActions() {
	return subActions;
    }

    public void addSubAction(Action action) {
	if (action.getParent() != null) {
	    action.getParent().removeSubAction(action);
	}
	subActions.add(action);
	action.setParent(this);
    }

    public void removeSubAction(Action action) {
	subActions.remove(action);
	action.setParent(null);
    }

    public boolean isHidden() {
	return size() == 0;
    }

    public int size() {
	int visible = 0;
	for (Action action : subActions) {
	    if (!action.isHidden())
		visible++;
	}
	return visible;
    }

    // Overridable by subclasses, but default to the name.
    public String getTitle() {
	return getName();
    }

    public String getHeaderText(Context context) {
	// Implement this to display additional text at the top of the
	// action container
	return null;
    }

    public String bulletIcon() {
	return "bullet_plus.bmp"; // plus
    }

    // Override to do something after opening the container
    public int performAction(Context context) {
	return ApplicationBase.BUTTON_USED;
    }

    // Override to provide a custom back action
    public Action getBackAction() {
	return null;
    }
}