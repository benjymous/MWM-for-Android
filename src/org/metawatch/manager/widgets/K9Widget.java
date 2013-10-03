package org.metawatch.manager.widgets;

import java.util.ArrayList;

import org.metawatch.manager.Utils;

public class K9Widget extends SimpleIconWidget {

    @Override
    protected int getCount() {
	return Utils.getUnreadK9Count(context);
    }

    @Override
    protected String idBase() {
	return "unreadK9";
    }

    @Override
    protected String descBase() {
	return "Unread K-9 email";
    }

    @Override
    protected String icon16x16() {
	return "idle_k9mail_10.bmp";
    }

    @Override
    protected String icon24x32() {
	return "idle_k9mail.bmp";
    }

    @Override
    protected String icon46x46() {
	return "idle_k9mail.bmp";
    }

    @Override
    public void refresh(ArrayList<CharSequence> widgetIds) {
    }
}
