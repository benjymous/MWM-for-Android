package org.metawatch.manager.widgets;

import java.util.ArrayList;

import org.metawatch.manager.Utils;

public class MissedCallsWidget extends SimpleIconWidget {

    @Override
    protected int getCount() {
	return Utils.getMissedCallsCount(context);
    }

    @Override
    protected String idBase() {
	return "missedCalls";
    }

    @Override
    protected String descBase() {
	return "Missed Calls";
    }

    @Override
    protected String icon16x16() {
	return "idle_call_10.bmp";
    }

    @Override
    protected String icon24x32() {
	return "idle_call.bmp";
    }

    @Override
    protected String icon46x46() {
	return "idle_call.bmp";
    }

    @Override
    public void refresh(ArrayList<CharSequence> widgetIds) {
    }

}
