/*
 * Copyright (c) 2015.  James Morris Studios
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jamesmorrisstudios.com.randremind.reminder;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.jamesmorrisstudios.materialuilibrary.listAdapters.BaseRecycleItem;
import com.jamesmorrisstudios.materialuilibrary.utils.Utils;
import com.jamesmorrisstudios.utilitieslibrary.Bus;
import com.jamesmorrisstudios.utilitieslibrary.FileWriter;
import com.jamesmorrisstudios.utilitieslibrary.Serializer;
import com.jamesmorrisstudios.utilitieslibrary.app.AppUtil;
import com.jamesmorrisstudios.utilitieslibrary.notification.NotificationAction;
import com.jamesmorrisstudios.utilitieslibrary.notification.NotificationContent;
import com.jamesmorrisstudios.utilitieslibrary.preferences.Preferences;
import com.jamesmorrisstudios.utilitieslibrary.time.DateTimeItem;
import com.jamesmorrisstudios.utilitieslibrary.time.TimeItem;
import com.jamesmorrisstudios.utilitieslibrary.time.UtilsTime;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import jamesmorrisstudios.com.randremind.R;
import jamesmorrisstudios.com.randremind.activities.MainActivity;
import jamesmorrisstudios.com.randremind.receiver.AlarmReceiver;
import jamesmorrisstudios.com.randremind.receiver.NotificationReceiver;

/**
 * Individual reminder reminder that contains all needed items to be a reminder
 * <p/>
 * Created by James on 4/20/2015.
 */
public final class ReminderItem extends BaseRecycleItem {
    //Unique data
    @SerializedName("uniqueName")
    public String uniqueName;
    //Title
    @SerializedName("title")
    public String title;
    @SerializedName("enabled")
    public boolean enabled;
    //Content
    @SerializedName("content")
    public String content;
    //Timing
    @SerializedName("startTime")
    public TimeItem startTime;
    @SerializedName("endTime")
    public TimeItem endTime;
    @SerializedName("singleTime")
    public TimeItem singleTime;
    @SerializedName("numberPerDay")
    public int numberPerDay;
    @SerializedName("randomDistribution")
    public boolean randomDistribution;
    @SerializedName("rangeTiming")
    public boolean rangeTiming = true;
    //Repeat
    @SerializedName("repeat")
    public boolean repeat = true;
    @SerializedName("daysToRun")
    public boolean[] daysToRun; //Sunday -> Saturday
    //Notifications
    @SerializedName("notificationToneString")
    public String notificationTone;
    @SerializedName("notificationToneName")
    public String notificationToneName;
    @SerializedName("notificationVibrate")
    public boolean notificationVibrate;
    @SerializedName("notificationLED")
    public boolean notificationLED = true;
    @SerializedName("notificationLEDColorInt")
    public int notificationLEDColor = Color.BLUE;
    @SerializedName("notificationHighPriority")
    public boolean notificationHighPriority = false;
    //Do Not Serialize This
    public transient ReminderLog reminderLog = null;
    private transient AsyncTask<Void, Void, Boolean> taskLoad = null;

    /**
     * Events
     */
    public enum ReminderItemEvent {
        DATA_LOAD_PASS,
        DATA_LOAD_FAIL,
    }

    /**
     * Creates a new reminder reminder with all the default values set
     */
    public ReminderItem() {
        //Unique name
        this.uniqueName = getUniqueName();
        //Title
        this.title = "";
        this.enabled = true;
        //Content
        this.content = "";
        //Timing
        this.startTime = new TimeItem(9, 0);
        this.endTime = new TimeItem(20, 0);
        this.singleTime = new TimeItem(13, 0);
        this.numberPerDay = 6;
        this.randomDistribution = true;
        this.rangeTiming = true;
        //Repeat
        this.repeat = true; //unused
        this.daysToRun = new boolean[]{true, true, true, true, true, true, true};
        //Notifications
        this.notificationTone = null;
        this.notificationToneName = AppUtil.getContext().getString(R.string.sound_none);
        this.notificationVibrate = false;
        this.notificationLED = true;
        this.notificationLEDColor = Color.BLUE;
        this.notificationHighPriority = false;
    }

    /**
     * @param title                Title
     * @param enabled              True to enable this reminder
     * @param startTime            Start time object
     * @param endTime              End time object
     * @param numberPerDay         Number per day
     * @param randomDistribution         Distribution
     * @param daysToRun            Days to run
     * @param notificationTone     The uri of the desired notification tone
     * @param notificationToneName The readable name of the notification tone
     * @param notificationVibrate  True to enable vibrate with the notification
     */
    public ReminderItem(@NonNull String uniqueName, @NonNull String title, @NonNull String content,
                        boolean enabled, @NonNull TimeItem startTime, @NonNull TimeItem endTime, @NonNull TimeItem singleTime,
                        int numberPerDay, @NonNull boolean randomDistribution, boolean rangeTiming, boolean repeat,
                        @NonNull boolean[] daysToRun, String notificationTone, String notificationToneName,
                        boolean notificationVibrate, boolean notificationLED, int notificationLEDColor, boolean notificationHighPriority) {
        this.uniqueName = uniqueName;
        this.title = title;
        this.content = content;
        this.enabled = enabled;
        this.startTime = startTime.copy();
        this.endTime = endTime;
        this.singleTime = singleTime;
        this.numberPerDay = numberPerDay;
        this.randomDistribution = randomDistribution;
        this.rangeTiming = rangeTiming;
        this.repeat = repeat;
        this.daysToRun = daysToRun.clone();
        this.notificationTone = notificationTone;
        this.notificationToneName = notificationToneName;
        this.notificationVibrate = notificationVibrate;
        this.notificationLED = notificationLED;
        this.notificationLEDColor = notificationLEDColor;
        this.notificationHighPriority = notificationHighPriority;
    }

    /**
     * Truncates the UUID for a unique id
     *
     * @return A unique notification id
     */
    public final int getNotificationId() {
        return uniqueName.hashCode();
    }

    /**
     * Generates a unique name for the reminder
     *
     * @return Unique name
     */
    @NonNull
    public static String getUniqueName() {
        return UUID.randomUUID().toString();
    }

    /**
     * @return A deep copy of this reminder
     */
    @NonNull
    public final ReminderItem copy() {
        return new ReminderItem(uniqueName, title, content, enabled, startTime, endTime, singleTime, numberPerDay,
                randomDistribution, rangeTiming, repeat, daysToRun, notificationTone, notificationToneName,
                notificationVibrate, notificationLED, notificationLEDColor, notificationHighPriority);
    }

    /**
     * @return A deep copy of this reminder but with a new unique name
     */
    @NonNull
    public final ReminderItem duplicate() {
        return new ReminderItem(getUniqueName(), title, content, enabled, startTime, endTime, singleTime, numberPerDay,
                randomDistribution, rangeTiming, repeat, daysToRun, notificationTone, notificationToneName,
                notificationVibrate, notificationLED, notificationLEDColor, notificationHighPriority);
    }

    /**
     * @return The notification tone as a Uri
     */
    public final Uri getNotificationTone() {
        if (notificationTone == null) {
            return null;
        }
        return Uri.parse(notificationTone);
    }

    /**
     * @param obj Object to compare to
     * @return True if equal based on unique id.
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj != null && obj instanceof ReminderItem) {
            ReminderItem item = (ReminderItem) obj;
            return this.uniqueName.equals(item.uniqueName);
        } else {
            return false;
        }
    }

    /**
     * Generate new alert times given the current parameters
     */
    public final void updateAlertTimes() {
        if (!rangeTiming) {
            ArrayList<TimeItem> alertTimes = new ArrayList<>();
            alertTimes.add(singleTime);
            ReminderItem.setAlertTimes(alertTimes, this.uniqueName);
            return;
        }
        int diff = getDiffMinutes();
        int startOffset = timeToMinutes(startTime);

        if(randomDistribution) {
            generateEvenishSplit(diff, startOffset, 0.5f, numberPerDay);
        } else {
            generateEvenishSplit(diff, startOffset, 0, numberPerDay);
        }
    }

    /**
     * Creates a distribution of values from offset to diff+offset
     *
     * @param diff        Total range of values
     * @param offset      Offset amount to shift the entire range
     * @param wiggle      Percent of space between items to allow randomization
     * @param numberItems Number of items to generate
     */
    private void generateEvenishSplit(int diff, int offset, float wiggle, int numberItems) {
        ArrayList<TimeItem> alertTimes = new ArrayList<>();
        Random rand = new Random();
        int itemSplit = Math.round((diff * 1.0f) / numberItems);
        int[] values = new int[numberItems];
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.round((i * 1.0f) / (numberItems) * diff) + itemSplit / 2 + Math.round((itemSplit * wiggle) * (rand.nextFloat() - 0.5f));
            if (i > 0) {
                values[i] = Math.min(Math.max(values[i], values[i - 1] + 5), diff);
            }
        }
        for (int value : values) {
            alertTimes.add(minutesToTimeItem(value + offset));
            Log.v(title, alertTimes.get(alertTimes.size() - 1).getHourInTimeFormatString() + ":" + alertTimes.get(alertTimes.size() - 1).getMinuteString());
        }
        ReminderItem.setAlertTimes(alertTimes, this.uniqueName);
    }

    /**
     * @return The difference in minutes
     */
    private int getDiffMinutes() {
        return timeToMinutes(endTime) - timeToMinutes(startTime);
    }

    /**
     * Converts a time reminder to minutes value
     *
     * @param time Time reminder
     * @return Value in minutes
     */
    private int timeToMinutes(@NonNull TimeItem time) {
        return time.hour * 60 + time.minute;
    }

    /**
     * Converts minutes to time reminder
     *
     * @param totalMinutes Minutes
     * @return Time reminder
     */
    private TimeItem minutesToTimeItem(int totalMinutes) {
        int hour = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return new TimeItem(hour, minutes);
    }

    public final void scheduleNextWake(TimeItem timeNow) {
        if(!enabled) {
            return;
        }
        ArrayList<TimeItem> alertTimes = ReminderItem.getAlertTimes(uniqueName);
        if(alertTimes.isEmpty()) {
            return;
        }
        for(TimeItem alertTime : alertTimes) {
            //alert time is after the current time
            if(!UtilsTime.timeBeforeOrEqual(alertTime, timeNow) || (timeNow.minute == 0 && timeNow.hour == 0)) {
                Scheduler.getInstance().scheduleWake(alertTime, uniqueName);
                return;
            }
        }
    }

    public final void deleteNextWake() {
        Scheduler.getInstance().cancelWake(uniqueName);
    }

    public final void rescheduleNextWake(TimeItem time) {
        deleteNextWake();
        scheduleNextWake(time);
    }

    public final NotificationContent getNotification(boolean preview, DateTimeItem dateTime) {
        String title = this.title;
        if(title == null || title.isEmpty()) {
            title = AppUtil.getContext().getString(R.string.default_title);
        }

        String content = this.content;
        if(content == null || content.isEmpty()) {
            content = AppUtil.getContext().getString(R.string.default_content);
        }

        NotificationContent notif = new NotificationContent(title, content, this.getNotificationTone(), R.drawable.notification_icon,
                AppUtil.getContext().getResources().getColor(R.color.accent), getNotificationId());

        if(this.notificationVibrate) {
            notif.enableVibrate();
        }
        if(this.notificationHighPriority) {
            notif.enableHighPriority();
        }
        if(this.notificationLED) {
            notif.enableLed(this.notificationLEDColor);
        }

        Intent intentClicked = new Intent(AppUtil.getContext(), NotificationReceiver.class);
        intentClicked.setAction("jamesmorrisstudios.com.randremind.NOTIFICATION_CLICKED");
        intentClicked.setType(this.uniqueName);
        intentClicked.putExtra("NAME", this.uniqueName);
        intentClicked.putExtra("DATETIME", DateTimeItem.encodeToString(dateTime));
        intentClicked.putExtra("NOTIFICATION_ID", getNotificationId());

        Intent intentCancel = new Intent(AppUtil.getContext(), NotificationReceiver.class);
        intentCancel.setAction("jamesmorrisstudios.com.randremind.NOTIFICATION_DELETED");
        intentCancel.setType(this.uniqueName);
        intentCancel.putExtra("NAME", this.uniqueName);
        intentCancel.putExtra("DATETIME", DateTimeItem.encodeToString(dateTime));
        intentCancel.putExtra("NOTIFICATION_ID", getNotificationId());

        Intent intentDismiss = new Intent(AppUtil.getContext(), NotificationReceiver.class);
        intentDismiss.setAction("jamesmorrisstudios.com.randremind.NOTIFICATION_DISMISS");
        intentDismiss.setType(this.uniqueName);
        intentDismiss.putExtra("NAME", this.uniqueName);
        intentDismiss.putExtra("DATETIME", DateTimeItem.encodeToString(dateTime));
        intentDismiss.putExtra("NOTIFICATION_ID", getNotificationId());

        Intent intentAck = new Intent(AppUtil.getContext(), NotificationReceiver.class);
        intentAck.setAction("jamesmorrisstudios.com.randremind.NOTIFICATION_ACKNOWLEDGE");
        intentAck.setType(this.uniqueName);
        intentAck.putExtra("NAME", this.uniqueName);
        intentAck.putExtra("DATETIME", DateTimeItem.encodeToString(dateTime));
        intentAck.putExtra("NOTIFICATION_ID", getNotificationId());

        if(preview) {
            intentClicked.putExtra("PREVIEW", true);
            intentCancel.putExtra("PREVIEW", true);
            intentDismiss.putExtra("PREVIEW", true);
            intentAck.putExtra("PREVIEW", true);
        }

        String pref = AppUtil.getContext().getString(R.string.settings_pref);
        String key = AppUtil.getContext().getString(R.string.pref_notification_click_ack);

        if(!Preferences.getBoolean(pref, key, true)) {
            intentClicked.setAction("jamesmorrisstudios.com.randremind.NOTIFICATION_CLICKED_SILENT");
        }

        PendingIntent pClicked = PendingIntent.getBroadcast(AppUtil.getContext(), 0, intentClicked, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pCanceled = PendingIntent.getBroadcast(AppUtil.getContext(), 0, intentCancel, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pDismiss = PendingIntent.getBroadcast(AppUtil.getContext(), 0, intentDismiss, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pAck = PendingIntent.getBroadcast(AppUtil.getContext(), 0, intentAck, PendingIntent.FLAG_CANCEL_CURRENT);

        notif.addContentIntent(pClicked);
        notif.addDeleteIntent(pCanceled);
        notif.addAction(new NotificationAction(0, "Dismiss", pDismiss));
        notif.addAction(new NotificationAction(0, "Acknowledge", pAck));

        return notif;
    }

    public final void deleteReminderLog() {
        FileWriter.deleteFile("LOG" + uniqueName, false);
    }

    public final void deleteAlertTimes() {
        Preferences.deleteStringArrayList(AppUtil.getContext().getString(R.string.pref_reminder_alerts), "ALERTS"+uniqueName);
    }

    public static ArrayList<TimeItem> getAlertTimes(String uniqueName) {
        ArrayList<String> items = Preferences.getStringArrayList(AppUtil.getContext().getString(R.string.pref_reminder_alerts), "ALERTS"+uniqueName);
        ArrayList<TimeItem> timeItems = new ArrayList<>();
        for(String item : items) {
            timeItems.add(TimeItem.decodeFromString(item));
        }
        return timeItems;
    }

    public static void setAlertTimes(ArrayList<TimeItem> alertTimes, String uniqueName) {
        ArrayList<String> items = new ArrayList<>();
        for(TimeItem timeItem : alertTimes) {
            items.add(TimeItem.encodeToString(timeItem));
        }
        Preferences.putStringArrayList(AppUtil.getContext().getString(R.string.pref_reminder_alerts), "ALERTS" + uniqueName, items);
    }

    public static boolean logReminderShown(String uniqueName, DateTimeItem dateTime) {
        Log.v("REMINDER ITEM", "STATIC Log Show Reminder");
        ReminderLog reminderLog = loadFromFile(uniqueName);
        if(reminderLog == null) {
            Log.v("REMINDER ITEM", "No save, creating new one");
            reminderLog = new ReminderLog();
        }
        reminderLog.logShown(dateTime);
        return saveToFile(reminderLog, uniqueName);
    }

    public static boolean logReminderClicked(String uniqueName, DateTimeItem dateTime) {
        Log.v("REMINDER ITEM", "Log Clicked Reminder");
        ReminderLog reminderLog = loadFromFile(uniqueName);
        if(reminderLog == null) {
            Log.v("REMINDER ITEM", "No save, creating new one");
            reminderLog = new ReminderLog();
        }
        reminderLog.logClicked(dateTime);
        return saveToFile(reminderLog, uniqueName);
    }

    /**
     * Events to post
     *
     * @param event Enum to post
     */
    private static void postReminderItemEvent(@NonNull ReminderItemEvent event) {
        Bus.postEnum(event);
    }

    public final boolean hasReminderLog() {
        return reminderLog != null && reminderLog.days != null;
    }

    /**
     * Loads the reminder list from disk. If already loaded it posts instantly
     * subscribe to Event.DATA_LOAD_PASS and Event.DATA_LOAD_FAIL for callbacks
     * @param forceRefresh True to force reload from disk
     */
    public final void loadData(boolean forceRefresh) {
        if(!forceRefresh && hasReminderLog()) {
            postReminderItemEvent(ReminderItemEvent.DATA_LOAD_PASS);
        } else {
            taskLoad = new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... params) {
                    ReminderLog log = loadFromFile(uniqueName);
                    if(log != null) {
                        reminderLog = log;
                        return true;
                    }
                    return false;
                }

                @Override
                protected void onPostExecute(Boolean value) {
                    if(value) {
                        postReminderItemEvent(ReminderItemEvent.DATA_LOAD_PASS);
                    } else {
                        postReminderItemEvent(ReminderItemEvent.DATA_LOAD_FAIL);
                    }
                    taskLoad = null;
                }
            };
            taskLoad.execute();
        }
    }

    /**
     * Saves the reminder log to file
     *
     * @return True if successful
     */
    private static boolean saveToFile(ReminderLog reminderLog, String uniqueName) {
        byte[] bytes = Serializer.serializeClass(reminderLog);
        return bytes != null && FileWriter.writeFile("LOG" + uniqueName, bytes, false);
    }

    /**
     * Loads the reminder log from file
     *
     * @return True if successful
     */
    private static ReminderLog loadFromFile(String uniqueName) {
        if (!FileWriter.doesFileExist("LOG" + uniqueName, false)) {
            return new ReminderLog();
        }
        byte[] bytes = FileWriter.readFile("LOG" + uniqueName, false);
        if (bytes == null) {
            return new ReminderLog();
        }
        return Serializer.deserializeClass(bytes, ReminderLog.class);
    }

}
