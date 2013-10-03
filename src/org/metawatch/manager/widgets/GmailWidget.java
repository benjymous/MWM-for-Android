package org.metawatch.manager.widgets;

import java.util.ArrayList;

import org.metawatch.manager.Utils;

public class GmailWidget extends SimpleIconWidget {

    @Override
    protected int getCount() {
	return Utils.getUnreadGmailCount(context);
    }

    @Override
    protected String idBase() {
	return "unreadGmail";
    }

    @Override
    protected String descBase() {
	return "Unread Gmail";
    }

    @Override
    protected String icon16x16() {
	return "idle_gmail_10.bmp";
    }

    @Override
    protected String icon24x32() {
	return "idle_gmail.bmp";
    }

    @Override
    protected String icon46x46() {
	return "idle_gmail.bmp";
    }

    @Override
    public void refresh(ArrayList<CharSequence> widgetIds) {
    }

}
