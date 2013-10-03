package org.metawatch.manager;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

public class ThemeContainer extends SherlockFragmentActivity {
    ActionBar mActionBar;
    ActionBar.Tab mGalleryTab;
    ActionBar.Tab mDownloadedTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	setContentView(R.layout.themes_container);
	setProgressBarIndeterminateVisibility(Boolean.FALSE);

	mActionBar = getSupportActionBar();
	mActionBar.setDisplayHomeAsUpEnabled(true);
	mActionBar.setTitle("Themes Manager");
	mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

	mGalleryTab = mActionBar.newTab().setText(R.string.ui_tab_downloaded_themes);
	mDownloadedTab = mActionBar.newTab().setText(R.string.ui_tab_theme_gallery);

	Fragment galleryFragment = ThemeGallery.newInstance();
	Fragment downloadedFragment = ThemePicker.newInstance();

	mGalleryTab.setTabListener(new MyTabsListener(galleryFragment));
	mDownloadedTab.setTabListener(new MyTabsListener(downloadedFragment));

	mActionBar.addTab(mDownloadedTab);
	mActionBar.addTab(mGalleryTab);
    }

    class MyTabsListener implements ActionBar.TabListener {
	public Fragment fragment;

	public MyTabsListener(Fragment fragment) {
	    this.fragment = fragment;
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
	    ft.replace(R.id.themes_container, fragment);
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	    ft.remove(fragment);
	}

    }

    public void setGalleryTabSelected() {
	mActionBar.selectTab(mGalleryTab);
    }

    public void setDownloadedTabSelected() {
	mActionBar.selectTab(mDownloadedTab);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case android.R.id.home:
	    finish();
	    return true;
	default:
	    return false;
	}
    }
}