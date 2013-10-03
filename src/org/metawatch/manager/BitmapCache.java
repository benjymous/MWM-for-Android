package org.metawatch.manager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.metawatch.manager.MetaWatchService.Preferences;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

public class BitmapCache {

    private static BitmapCache mInstance;
    
    private BitmapCache(){}
    
    public static BitmapCache getInstance() {
	if (mInstance == null)
	    mInstance = new BitmapCache();
	return mInstance;
    }
    
    public void destroy() {
	mInstance = null;
    }
    
    public class ThemeData {
	public ThemeData(String name) {
	    themeName = name;
	}

	public String themeName = "";
	public long timeStamp = 0;

	protected HashMap<String, Object> data = new HashMap<String, Object>();

	public Object get(String key) {
	    return data.containsKey(key) ? data.get(key) : null;
	}

	public Bitmap getBitmap(String key) {
	    Object obj = data.get(key);
	    return obj instanceof Bitmap ? (Bitmap) obj : null;
	}

	public Properties getProperties(String key) {
	    Object obj = data.get(key);
	    return obj instanceof Properties ? (Properties) obj : null;
	}

	public void readTheme(File themeFile) {

	    HashMap<String, Object> newCache = new HashMap<String, Object>();

	    FileInputStream fis = null;
	    ZipInputStream zis = null;

	    timeStamp = themeFile.lastModified();

	    try {

		fis = new FileInputStream(themeFile);
		zis = new ZipInputStream(fis);

		ZipEntry ze = zis.getNextEntry();
		while (ze != null) {
		    String entryName = ze.getName();

		    final int size = (int) ze.getSize();

		    // Need to copy into a buffer rather than decoding directly
		    // from zis
		    // as BitmapFactory seems unable to read a .bmp file from a
		    // ZipInputStream :-\
		    byte[] buffer = new byte[size];
		    int offset = 0;
		    int read = 0;
		    do {
			read = zis.read(buffer, offset, size - offset);
			offset += read;
		    } while (read > 0);

		    if (entryName.toLowerCase().endsWith(".bmp") || entryName.toLowerCase().endsWith(".png")) {
			Bitmap bitmap = BitmapFactory.decodeByteArray(buffer, 0, size);
			if (bitmap != null) {
			    // if (Preferences.logging)
			    // Log.d(MetaWatchStatus.TAG,
			    // "Loaded "+ze.getName());
			    newCache.put(entryName, bitmap);
			} else {
			    if (Preferences.logging)
				Log.d(MetaWatchStatus.TAG, "Failed to load " + ze.getName());
			}
		    } else if (entryName.toLowerCase().endsWith(".xml")) {
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
			Properties properties = new Properties();
			properties.loadFromXML(byteArrayInputStream);

			newCache.put(entryName, properties);
		    }

		    zis.closeEntry();
		    ze = zis.getNextEntry();

		}

	    } catch (FileNotFoundException e) {
	    } catch (IOException e) {
	    } finally {
		try {
		    if (zis != null)
			zis.close();
		    if (fis != null)
			fis.close();
		} catch (IOException e) {
		}
	    }

	    data = newCache;

	}

	public Bitmap getBanner() {
	    return getBitmap("theme_banner.png");
	}

	public Properties getThemeProperties() {
	    return getProperties("theme.xml");
	}

    }

    private class DefaultTheme extends ThemeData {
	public DefaultTheme(Context context) {
	    super("");
	    this.context = context;
	}

	Context context;

	@Override
	public void readTheme(File themeFile) {
	}

	@Override
	public Bitmap getBitmap(String key) {
	    Bitmap bitmap = super.getBitmap(key);
	    if (bitmap != null)
		return bitmap;

	    bitmap = loadBitmapFromAssets(context, key);

	    if (bitmap != null) {
		data.put(key, bitmap);
		return bitmap;
	    }
	    return null;
	}

	@Override
	public Properties getProperties(String key) {
	    Properties properties = super.getProperties(key);
	    if (properties != null)
		return properties;

	    properties = loadPropertiesFromAssets(context, key);

	    if (properties != null) {
		data.put(key, properties);
		return properties;
	    }
	    return null;
	}

	private Bitmap loadBitmapFromAssets(Context context, String path) {

	    try {
		InputStream inputStream = context.getAssets().open(path);
		Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
		inputStream.close();
		return bitmap;
	    } catch (IOException e) {
		return null;
	    }
	}

	private Properties loadPropertiesFromAssets(Context context, String path) {

	    try {
		InputStream inputStream = context.getAssets().open(path);
		Properties properties = new Properties();
		properties.loadFromXML(inputStream);
		inputStream.close();
		return properties;
	    } catch (IOException e) {
		return null;
	    }
	}
    }

    private DefaultTheme internalTheme = null;
    private ThemeData currentTheme = null;

    private static File getThemeFile(Context context, String themeName) {
	return new File(Utils.getExternalFilesDir(context, "Themes"), themeName + ".zip");
    }

    public synchronized Bitmap getBitmap(Context context, String path) {

	updateCache(context);

	Bitmap bitmap = currentTheme.getBitmap(path);
	if (bitmap != null)
	    return bitmap;

	bitmap = internalTheme.getBitmap(path);
	if (bitmap != null)
	    return bitmap;

	return null;

    }

    public synchronized Properties getProperties(Context context, String path) {

	updateCache(context);

	Properties properties = currentTheme.getProperties(path);
	if (properties != null)
	    return properties;

	properties = internalTheme.getProperties(path);
	if (properties != null)
	    return properties;

	return new Properties();
    }

    public Bitmap getDefaultThemeBanner(Context context) {
	return internalTheme.getBanner();
    }

    private void updateCache(Context context) {

	if (internalTheme == null) {
	    internalTheme = new DefaultTheme(context);
	}

	File themeFile = getThemeFile(context, Preferences.themeName);

	if (currentTheme == null || Preferences.themeName != currentTheme.themeName || (themeFile.lastModified() != currentTheme.timeStamp)) {
	    currentTheme = loadTheme(context, Preferences.themeName, themeFile);
	}
    }

    public ThemeData getInternalTheme(Context context) {
	updateCache(context);
	return internalTheme;
    }

    public ThemeData loadTheme(Context context, String themeName) {
	File themeFile = getThemeFile(context, themeName);

	return loadTheme(context, themeName, themeFile);
    }

    private ThemeData loadTheme(Context context, String themeName, File themeFile) {

	ThemeData theme = new ThemeData(themeName);

	if (themeFile.exists()) {
	    theme.readTheme(themeFile);
	}

	return theme;
    }

    private String getThemeName(String uri) {
	String filename = uri.substring(uri.lastIndexOf('/') + 1, uri.length());
	return filename.substring(0, filename.lastIndexOf('.'));
    }

    public void downloadAndInstallTheme(final ThemeContainer themeContainer, final String uri) {

	Thread thread = new Thread("ThemeInstaller") {

	    @Override
	    public void run() {

		OutputStream fOut = null;
		try {
		    themeContainer.runOnUiThread(new Runnable() {
			public void run() {
			    Toast.makeText(themeContainer, themeContainer.getString(R.string.downloading_theme), Toast.LENGTH_SHORT).show();
			    themeContainer.setProgressBarIndeterminateVisibility(Boolean.TRUE);
			}
		    });

		    // Create a URL for the desired page
		    URL url = new URL(uri);

		    String themeName = getThemeName(uri);
		    File themeFile = getThemeFile(themeContainer, themeName);

		    URLConnection uc = url.openConnection();
		    int contentLength = uc.getContentLength();

		    InputStream raw = uc.getInputStream();
		    InputStream in = new BufferedInputStream(raw);
		    byte[] data = new byte[contentLength];
		    int bytesRead = 0;
		    int offset = 0;

		    while (offset < contentLength) {
			bytesRead = in.read(data, offset, data.length - offset);
			if (bytesRead == -1)
			    break;
			offset += bytesRead;
		    }

		    in.close();

		    if (offset != contentLength)
			throw new IOException();

		    fOut = new BufferedOutputStream(new FileOutputStream(themeFile));
		    fOut.write(data);
		    fOut.flush();
		    fOut.close();

		    Preferences.themeName = themeName;
		    MetaWatchService.saveTheme(themeContainer, Preferences.themeName);

		    Idle.getInstance().updateIdle(themeContainer, true);
		} catch (MalformedURLException e) {
		} catch (IOException e) {
		} finally {
		    themeContainer.runOnUiThread(new Runnable() {
			public void run() {
			    if (themeContainer != null && !themeContainer.isFinishing()) {
				themeContainer.setProgressBarIndeterminateVisibility(Boolean.FALSE);
				Toast.makeText(themeContainer, R.string.downloaded_and_applied, Toast.LENGTH_SHORT).show();
				themeContainer.setDownloadedTabSelected();
			    }
			}
		    });
		}
	    }
	};
	thread.start();
    }
}