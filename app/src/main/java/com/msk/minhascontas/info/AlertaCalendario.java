package com.msk.minhascontas.info;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;

import java.util.Calendar;
import java.util.TimeZone;

public class AlertaCalendario {

    public static long adicionarEventoNoCalendario(ContentResolver cr,
                                                   String titulo, String descricao, int dia, int mes, int ano, boolean comAlerta) {


        String eventUriStr = "content://com.android.calendar/events";
        if (Build.VERSION.SDK_INT < 8)
            eventUriStr = "content://calendar/events";

        Calendar relogio = Calendar.getInstance();
        relogio.set(ano, mes, dia, 8, 0);
        long data = relogio.getTimeInMillis();

        ContentValues event = new ContentValues();
        event.put("calendar_id", 1);
        event.put("title", titulo);
        event.put("description", descricao);
        event.put("dtstart", data);
        relogio.set(ano, mes, dia, 18, 0);
        data = relogio.getTimeInMillis();
        event.put("dtend", data);
        event.put("eventTimezone", TimeZone.getDefault().getID());
        event.put("hasAlarm", 1);

        Uri eventUri;
        long eventID;

        try {
            eventUri = cr.insert(Uri.parse(eventUriStr), event);
            eventID = Long.parseLong(eventUri.getLastPathSegment());
        } catch (Exception e) {
            eventID = 0;
        }

        if (comAlerta) {
            String reminderUriString = "content://com.android.calendar/reminders";
            if (Build.VERSION.SDK_INT < 8)
                reminderUriString = "content://calendar/reminders";

            ContentValues reminderValues = new ContentValues();
            reminderValues.put("event_id", eventID);
            // Default value of the system. Minutes is a integer
            reminderValues.put("minutes", 15);
            // Alert Methods: Default(0), Alert(1), Email(2), SMS(3)
            reminderValues.put("method", 1);
            try {
                cr.insert(Uri.parse(reminderUriString), reminderValues);
            } catch (Exception e) {
                eventID = 0;
            }
        }
        return eventID;
    }
}
