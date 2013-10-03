package org.metawatch.manager.widgets;

import java.util.ArrayList;

import org.metawatch.manager.Utils;

public class SmsWidget extends SimpleIconWidget {

    @Override
    protected int getCount() {
	return Utils.getUnreadSmsCount(context);
    }

    @Override
    protected String idBase() {
	return "unreadSms";
    }

    @Override
    protected String descBase() {
	return "Unread SMS/MMS";
    }

    @Override
    protected String icon16x16() {
	return "idle_sms_10.bmp";
    }

    @Override
    protected String icon24x32() {
	return "idle_sms.bmp";
    }

    @Override
    protected String icon46x46() {
	return "idle_sms.bmp";
    }

    @Override
    public void refresh(ArrayList<CharSequence> widgetIds) {
    }

}
