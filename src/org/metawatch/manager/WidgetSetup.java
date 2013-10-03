package org.metawatch.manager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.widgets.InternalWidget.WidgetData;
import org.metawatch.manager.widgets.WidgetManager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.internal.nineoldandroids.animation.Animator;
import com.actionbarsherlock.internal.nineoldandroids.animation.Animator.AnimatorListener;
import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class WidgetSetup extends SherlockFragment {
    private LinearLayout mIdlePreviews;
    private ExpandableListView widgetList;
    private WidgetListAdaptor adapter;
    private SherlockFragmentActivity mActivity;
    private int mCurrentNumberOfPages = 0;
    private View mMainView = null;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private class WidgetListAdaptor extends BaseExpandableListAdapter {

	private Map<String, WidgetData> widgetMap = new HashMap<String, WidgetData>();
	private List<List<String>> groups = new ArrayList<List<String>>();
	private LayoutInflater mInflater = LayoutInflater.from(getActivity());

	public void init(Context context) {

	    widgetMap = WidgetManager.getInstance(context).getCachedWidgets(context, null);

	    ArrayList<String> rows = new ArrayList<String>(Arrays.asList(MetaWatchService.getWidgets(context).split("\\|")));
	    final ArrayList<List<String>> pGroups = new ArrayList<List<String>>();

	    for (String line : rows) {
		String[] widgets = (line).split(",");
		List<String> list = new ArrayList<String>(Arrays.asList(widgets));
		pGroups.add(list);
	    }
	    groups = pGroups;
	    notifyDataSetChanged();
	}

	public void set(int groupPosition, int childPosition, String value) {
	    while (groupPosition >= groups.size()) {
		groups.add(new ArrayList<String>());
	    }

	    while (childPosition >= groups.get(groupPosition).size()) {
		groups.get(groupPosition).add("");
	    }

	    groups.get(groupPosition).set(childPosition, value);

	    tidy();
	}

	private void tidy() {
	    // Tidy up any empty groups
	    for (List<String> row : groups) {
		boolean isEmpty = true;
		for (String entry : row) {
		    if (!Utils.stringIsEmpty(entry)) {
			isEmpty = false;
		    }
		}
		if (isEmpty) {
		    row.clear();
		}
	    }
	}

	public String get() {
	    StringBuilder out = new StringBuilder();
	    for (List<String> row : groups) {
		if (out.length() > 0)
		    out.append("|");

		StringBuilder line = new StringBuilder();
		for (String id : row) {
		    if (id != "") {
			if (line.length() > 0)
			    line.append(",");

			line.append(id);
		    }
		}

		out.append(line);
	    }

	    return out.toString();
	}

	public Object getChild(int groupPosition, int childPosition) {
	    if (groupPosition >= groups.size()) {
		return "";
	    } else if (childPosition >= groups.get(groupPosition).size()) {
		return "";
	    }
	    return groups.get(groupPosition).get(childPosition);
	}

	public long getChildId(int groupPosition, int childPosition) {
	    return childPosition;
	}

	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

	    if (convertView == null) {
		convertView = mInflater.inflate(R.layout.list_item_icon_text, null);
	    }

	    TextView label = (TextView) convertView.findViewById(R.id.text);
	    ImageView icon = (ImageView) convertView.findViewById(R.id.icon);

	    String id = (String) getChild(groupPosition, childPosition);
	    String name = id;

	    Bitmap bmp = null;
	    if (Utils.stringIsEmpty(id)) {
		id = "";
		name = "<Add Widget>";
		icon.setVisibility(View.GONE);
	    } else {

		icon.setVisibility(View.VISIBLE);
		if (widgetMap.containsKey(id)) {
		    name = widgetMap.get(id).description;
		    bmp = widgetMap.get(id).bitmap;
		} else {
		    bmp = Bitmap.createBitmap(1, 1, Config.ALPHA_8);
		}

	    }

	    label.setText(name);
	    if (bmp != null) {
		icon.setImageBitmap(Bitmap.createScaledBitmap(bmp, bmp.getWidth() * 2, bmp.getHeight() * 2, false));
	    }
	    if (Preferences.animations && (convertView.getTag() == null || !((Boolean) convertView.getTag()))) {
		ObjectAnimator fadeIn = ObjectAnimator.ofFloat(convertView, "alpha", 0, 1);
		fadeIn.setDuration(750);
		fadeIn.start();
		convertView.setTag(true);
	    }
	    return convertView;
	}

	public int getChildrenCount(int groupPosition) {
	    return groupPosition >= groups.size() ? 1 : groups.get(groupPosition).size() + 1;
	}

	public Object getGroup(int groupPosition) {
	    return groupPosition >= groups.size() ? null : groups.get(groupPosition);
	}

	public int getGroupCount() {
	    return groups.size() + 1;
	}

	public long getGroupId(int groupPosition) {
	    return groupPosition;
	}

	private String getGroupName(int group) {
	    StringBuilder nameSb = new StringBuilder();
	    nameSb.append("Row ");
	    nameSb.append(group + 1);
	    return nameSb.toString();
	}

	private String getGroupLabel(int group) {

	    int widgetCount = 0;
	    if (group < groups.size()) {
		for (String entry : groups.get(group)) {
		    if (!Utils.stringIsEmpty(entry))
			widgetCount++;
		}
	    }

	    StringBuilder nameSb = new StringBuilder();
	    if (widgetCount == 0) {
		nameSb.append("empty");
	    } else if (widgetCount == 1) {
		nameSb.append("1 widget");
	    } else {
		nameSb.append(widgetCount);
		nameSb.append(" widgets");
	    }

	    return nameSb.toString();
	}

	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
	    if (convertView == null)
		convertView = mInflater.inflate(R.layout.expandable_text_item, null);
	    ((TextView) convertView).setText(getGroupName(groupPosition) + " - " + getGroupLabel(groupPosition));
	    return convertView;
	}

	public boolean hasStableIds() {
	    return true;
	}

	public boolean isChildSelectable(int groupPosition, int childPosition) {
	    return true;
	}

    }

    @Override
    public void onCreate(Bundle bundle) {
	super.onCreate(bundle);
	mActivity = (SherlockFragmentActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	mMainView = inflater.inflate(R.layout.widget_setup, null);
	widgetList = (ExpandableListView) mMainView.findViewById(R.id.widgetList);
	widgetList.setGroupIndicator(null);
	mIdlePreviews = (LinearLayout) mMainView.findViewById(R.id.idlePreviews);
	return mMainView;
    }

    @Override
    public void onStart() {
	super.onStart();
	widgetList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
	    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		Intent i = new Intent(mActivity, WidgetPicker.class);
		i.putExtra("groupPosition", groupPosition);
		i.putExtra("childPosition", childPosition);
		startActivityForResult(i, 1);
		return false;
	    }
	});

	adapter = new WidgetListAdaptor();
	widgetList.setAdapter(adapter);
	mHandler.post(mPreviewUpdate);
    }

    private Runnable mPreviewUpdate = new Runnable() {

	@Override
	public void run() {
	    refreshPreview();
	    adapter.init(mActivity);
	    mHandler.postDelayed(mPreviewUpdate, 2500);
	}
    };

    @Override
    public void onPause() {
	super.onPause();
	mHandler.removeCallbacks(mPreviewUpdate);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
	super.onActivityResult(requestCode, resultCode, data);

	if (resultCode == Activity.RESULT_OK) {
	    String id = data.getStringExtra("selectedWidget");
	    int groupPosition = data.getIntExtra("groupPosition", -1);
	    int childPosition = data.getIntExtra("childPosition", -1);

	    if (groupPosition > -1 && childPosition > -1) {

		adapter.set(groupPosition, childPosition, id);

		adapter.notifyDataSetChanged();
		storeWidgetLayout();
		refreshPreview();
		Idle.getInstance().updateIdle(mActivity, true);
		if (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG) {
		    Idle.getInstance().sendOledIdle(mActivity);
		}

	    }
	}
    }

    private void refreshPreview() {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "WidgetSetup.refreshPreview() start");
	Idle.getInstance().updateIdlePages(mActivity);
	int pages = Idle.getInstance().numPages();
	if (mCurrentNumberOfPages != pages)
	    mIdlePreviews.removeAllViews();
	mCurrentNumberOfPages = pages;
	for (int i = 0; i < pages; ++i) {
	    Bitmap bmp = Idle.getInstance().createIdle(mActivity, true, i);
	    if (bmp != null) {
		int backCol = Color.LTGRAY;
		int viewId = (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG) ? R.layout.idle_screen_preview_oled : R.layout.idle_screen_preview;

		if (Preferences.invertLCD || MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG) {
		    bmp = Utils.invertBitmap(bmp);
		    backCol = 0xff111111;
		}
		bmp = Bitmap.createScaledBitmap(bmp, bmp.getWidth() * 4, bmp.getHeight() * 4, false);
		LayoutInflater factory = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		ImageView imageView;
		View view = mIdlePreviews.findViewWithTag(i);
		if (view == null) {
		    view = factory.inflate(viewId, null);
		    imageView = (ImageView) view.findViewById(R.id.image);
		    imageView.setClickable(true);
		    imageView.setBackgroundColor(backCol);
		    view.setTag(i);
		    imageView.setTag(i);
		    imageView.setOnClickListener(new OnClickListener() {
			// @Override
			public void onClick(View v) {
			    MetaWatchStatus.wiggleButton(v);
			    Integer page = (Integer) v.getTag();
			    Idle.getInstance().toPage(mActivity, page);
			    Idle.getInstance().toIdle(mActivity);
			    Idle.getInstance().updateIdle(mActivity, true);
			    if (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG) {
				Idle.getInstance().sendOledIdle(v.getContext());
			    }
			}
		    });
		    mIdlePreviews.addView(view);
		    fadeIn(view).start();
		} else {
		    imageView = (ImageView) view.findViewById(R.id.image);
		}

		// Add glow effect
		bmp = createGlow(bmp);

		Drawable tmpDrawable = imageView.getDrawable();
		Bitmap currentBitmap = null;
		if (tmpDrawable != null)
		    currentBitmap = ((BitmapDrawable) tmpDrawable).getBitmap();
		if (Preferences.animations && currentBitmap != null && !bitmapCompare(currentBitmap, bmp)) {
		    ObjectAnimator upToTop = ObjectAnimator.ofFloat(imageView, "alpha", 1, 0);
		    upToTop.setDuration(750);
		    upToTop.addListener(new MyAnimatorListener(imageView, bmp));
		    upToTop.start();
		} else {
		    imageView.setImageBitmap(bmp);
		}
	    }
	}
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "WidgetSetup.refreshPreview() end");
    }

    private ObjectAnimator fadeIn(final View view) {
	ObjectAnimator downFromTop = ObjectAnimator.ofFloat(view, "alpha", 0, 1);
	downFromTop.setDuration(750);
	return downFromTop;
    }

    private Bitmap createGlow(Bitmap src) {
	int margin = 12;
	int halfMargin = margin / 2;

	// the glow radius
	int glowRadius = 16;

	// the glow color
	int glowColor = Color.parseColor("#33b5e5");

	// extract the alpha from the source image
	Bitmap alpha = src.extractAlpha();

	// The output bitmap (with the icon + glow)
	Bitmap bmp = Bitmap.createBitmap(src.getWidth() + margin, src.getHeight() + margin, Bitmap.Config.ARGB_8888);

	// The canvas to paint on the image
	Canvas canvas = new Canvas(bmp);

	Paint paint = new Paint();
	paint.setColor(glowColor);

	// outer glow
	paint.setMaskFilter(new BlurMaskFilter(glowRadius, Blur.OUTER));
	canvas.drawBitmap(alpha, halfMargin, halfMargin, paint);

	// original icon
	canvas.drawBitmap(src, halfMargin, halfMargin, null);

	return bmp;
    }

    private class MyAnimatorListener implements AnimatorListener {

	private ImageView iv;
	private Bitmap bmp;

	public MyAnimatorListener(ImageView iv, Bitmap bmp) {
	    this.iv = iv;
	    this.bmp = bmp;
	}

	@Override
	public void onAnimationStart(Animator animation) {
	}

	@Override
	public void onAnimationEnd(Animator animation) {
	    iv.setImageBitmap(bmp);
	    iv.requestLayout();
	    iv.invalidate();
	    fadeIn(iv).start();
	}

	@Override
	public void onAnimationCancel(Animator animation) {
	}

	@Override
	public void onAnimationRepeat(Animator animation) {
	}
    }

    private void storeWidgetLayout() {
	MetaWatchService.saveWidgets(mActivity, adapter.get());
    }

    public boolean bitmapCompare(Bitmap bitmap1, Bitmap bitmap2) {
	ByteBuffer buffer1 = ByteBuffer.allocate(bitmap1.getHeight() * bitmap1.getRowBytes());
	bitmap1.copyPixelsToBuffer(buffer1);

	ByteBuffer buffer2 = ByteBuffer.allocate(bitmap2.getHeight() * bitmap2.getRowBytes());
	bitmap2.copyPixelsToBuffer(buffer2);

	return Arrays.equals(buffer1.array(), buffer2.array());
    }
}
