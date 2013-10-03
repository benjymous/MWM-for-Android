package org.metawatch.manager.actions;

import org.metawatch.manager.Notification;
import org.metawatch.manager.Notification.NotificationType;
import org.metawatch.manager.apps.ApplicationBase;

import android.content.Context;

public class NotificationsAction extends ContainerAction {

    public String id = "notifications";

    public String getId() {
	return id;
    }

    public String getName() {
	return "Recent Notifications";
    }

    public String getTitle() {
	return "Notifications";
    }

    public long getTimestamp() {
	if (subActions.size() > 0) {
	    return subActions.get(0).getTimestamp();
	} else {
	    return 0;
	}
    }

    public void refreshSubActions(Context context) {
	subActions.clear();

	for (final NotificationType n : Notification.getInstance().history()) {
	    subActions.add(new Action() {

		public String getName() {
		    return n.description;
		}

		public long getTimestamp() {
		    return n.timestamp;
		}

		public String bulletIcon() {
		    return n.viewed ? "bullet_triangle.bmp" : "bullet_star.bmp";
		}

		public int performAction(Context context) {
		    Notification.getInstance().replay(context, n);
		    // DONT_UPDATE since the idle screen overwrites the
		    // notification otherwise.
		    return ApplicationBase.BUTTON_USED_DONT_UPDATE;
		}
	    });
	}
    }

    public int getSecondaryType() {
	return SECONDARY_EXIT;
    }

    public int performSecondary(Context context) {
	Notification.getInstance().clearHistory();
	return ApplicationBase.BUTTON_USED;
    }
}