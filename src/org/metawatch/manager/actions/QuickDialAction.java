package org.metawatch.manager.actions;

import org.metawatch.manager.Utils;
import org.metawatch.manager.apps.ApplicationBase;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;

public class QuickDialAction extends ContainerAction {

    public String id = "quickdial";

    public String getId() {
	return id;
    }

    public String getName() {
	return "Quickdial";
    }

    public void refreshSubActions(Context context) {
	subActions.clear();

	subActions.add(ActionManager.getInstance(context).getAction(InternalActions.SpeakerphoneAction.id));

	subActions.add(new Action() {

	    @Override
	    public String getName() {
		return "Voicemail";
	    }

	    @Override
	    public String bulletIcon() {
		return "bullet_circle.bmp";
	    }

	    @Override
	    public int performAction(Context context) {
		Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("voicemail:"));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
		return ApplicationBase.BUTTON_USED;
	    }

	});

	final String[] projection = new String[] { ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.HAS_PHONE_NUMBER, };

	Utils.CursorHandler ch = new Utils.CursorHandler();
	try {

	    Cursor people = ch.add(context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, projection, "starred=?", new String[] { "1" }, ContactsContract.Contacts.TIMES_CONTACTED + " DESC"));

	    if (people == null)
		return;

	    while (people.moveToNext()) {
		int idFieldIndex = people.getColumnIndex(ContactsContract.Contacts._ID);
		final String id = people.getString(idFieldIndex);

		int nameFieldIndex = people.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
		final String contact = people.getString(nameFieldIndex);

		int hasNumberFieldIndex = people.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
		if (Integer.parseInt(people.getString(hasNumberFieldIndex)) > 0) {

		    Cursor personNumbers = context.getContentResolver().query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + "=?", new String[] { id }, null);
		    if (personNumbers == null)
			continue;

		    ContainerAction personContainer = null;
		    String prefix = contact + " ";
		    if (personNumbers.getCount() > 1) {
			personContainer = new ContainerAction() {
			    public String getName() {
				return contact;
			    }
			};
			subActions.add(personContainer);
			prefix = "";
		    }
		    final String namePrefix = prefix;

		    while (personNumbers.moveToNext()) {

			int phoneFieldIndex = personNumbers.getColumnIndex(Phone.DATA);
			final String number = personNumbers.getString(phoneFieldIndex);

			int typeFieldIndex = personNumbers.getColumnIndex(Phone.TYPE);
			final String type = (String) Phone.getTypeLabel(context.getResources(), personNumbers.getInt(typeFieldIndex), "");

			Action numberEntry = new Action() {

			    String title = namePrefix + type + "\n" + number;
			    String uri = "tel:" + number;

			    @Override
			    public String getName() {
				return title;
			    }

			    @Override
			    public String bulletIcon() {
				return "bullet_circle.bmp";
			    }

			    @Override
			    public int performAction(Context context) {
				Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(uri));
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent);
				return ApplicationBase.BUTTON_USED;
			    }

			};

			if (personContainer != null) {
			    personContainer.addSubAction(numberEntry);
			} else {
			    subActions.add(numberEntry);
			}
		    }

		}

	    }
	} finally {
	    ch.closeAll();
	}
    }

}
