package org.metawatch.manager;

public interface GmailMonitor {
    public void startMonitor();

    public int getUnreadCount();
}