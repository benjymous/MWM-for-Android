/*****************************************************************************
 *  Copyright (c) 2011 Meta Watch Ltd.                                       *
 *  www.MetaWatch.org                                                        *
 *                                                                           *
 =============================================================================
 *                                                                           *
 *  Licensed under the Apache License, Version 2.0 (the "License");          *
 *  you may not use this file except in compliance with the License.         *
 *  You may obtain a copy of the License at                                  *
 *                                                                           *
 *    http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                           *
 *  Unless required by applicable law or agreed to in writing, software      *
 *  distributed under the License is distributed on an "AS IS" BASIS,        *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 *  See the License for the specific language governing permissions and      *
 *  limitations under the License.                                           *
 *                                                                           *
 *****************************************************************************/

/*****************************************************************************
 * CallStateListener.java                                                    *
 * CallStateListener                                                         *
 * Listener waiting for incoming call                                        *
 *                                                                           *
 *                                                                           *
 *****************************************************************************/

package org.metawatch.manager;

import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchService.WatchModes;
import org.metawatch.manager.actions.ActionManager;

import android.content.Context;
import android.media.AudioManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import org.metawatch.manager.Log;

class CallStateListener extends PhoneStateListener {

    Context context;

    public CallStateListener(Context ctx) {
	super();
	context = ctx;
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
	super.onCallStateChanged(state, incomingNumber);

	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "onCallStateChanged " + state + " " + incomingNumber);

	if (!MetaWatchService.Preferences.notifyCall)
	    return;

	if (incomingNumber == null)
	    incomingNumber = "";

	switch (state) {
	case TelephonyManager.CALL_STATE_RINGING:
	    Call.inCall = true;
	    Call.phoneNumber = incomingNumber;
	    MetaWatchService.setWatchMode(WatchModes.CALL);
	    Call.startRinging(context, incomingNumber);
	    break;
	case TelephonyManager.CALL_STATE_IDLE:
	    if (Call.inCall) {
		Call.phoneNumber = null;
		Call.endRinging(context);

		if (Preferences.autoSpeakerphone) {
		    MediaControl.getInstance().setSpeakerphone(context, Call.previousSpeakerphoneState);
		}
		if (Preferences.showActionsInCall) {
		    Idle.getInstance().toPage(context, 0);
		}
		if (Call.previousRingerMode != -1) {
		    AudioManager as = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		    as.setRingerMode(Call.previousRingerMode);
		    Call.previousRingerMode = -1;
		}
		Call.inCall = false;
	    }
	    break;
	case TelephonyManager.CALL_STATE_OFFHOOK:
	    if (Preferences.showActionsInCall) {
		ActionManager.getInstance(context).displayCallActions(context);
	    } else {
		Call.endRinging(context);
		Call.inCall = false;
	    }
	    break;
	}

    }

    @Override
    public void onMessageWaitingIndicatorChanged(boolean messageWaiting) {
	Call.voicemailWaiting = messageWaiting;

	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "onMessageWaitingIndicatorChanged " + messageWaiting);

	if (Preferences.notifyNewVoicemail) {

	    if (messageWaiting) {
		NotificationBuilder.createNewVoicemail(context);
	    }
	    Idle.getInstance().updateIdle(context, true);

	}
    }
}
