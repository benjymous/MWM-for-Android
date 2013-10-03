package org.metawatch.manager.widgets;

import java.util.ArrayList;

import org.metawatch.manager.Monitors;

public class TouchDownWidget extends SimpleIconWidget {

    @Override
    protected int getCount() {
	return Monitors.getInstance().mTouchDownData.unreadMailCount;
    }

    @Override
    protected String idBase() {
	return "unreadTouchDown";
    }

    @Override
    protected String descBase() {
	return "Unread TouchDown email";
    }

    @Override
    protected String icon16x16() {
	return "idle_touchdown_10.bmp";
    }

    @Override
    protected String icon24x32() {
	return "idle_touchdown.bmp";
    }

    @Override
    protected String icon46x46() {
	return "idle_touchdown.bmp";
    }

    @Override
    public void refresh(ArrayList<CharSequence> widgetIds) {
    }
}
