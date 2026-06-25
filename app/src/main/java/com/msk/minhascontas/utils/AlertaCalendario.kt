package com.msk.minhascontas.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import java.util.Calendar
import java.util.TimeZone

object AlertaCalendario {
    fun adicionarEventoNoCalendario(
        cr: ContentResolver,
        titulo: String?,
        descricao: String?,
        dia: Int,
        mes: Int,
        ano: Int,
        comAlerta: Boolean,
        qtRepete: Int,
        intervalo: Int
    ): Long {
        var eventUriStr = "content://com.android.calendar/events"
        if (Build.VERSION.SDK_INT < 8) eventUriStr = "content://calendar/events"

        val relogio = Calendar.getInstance()
        relogio.set(ano, mes, dia, 8, 0)
        var data = relogio.getTimeInMillis()

        val event = ContentValues()
        event.put("calendar_id", 1)
        event.put("title", titulo)
        event.put("description", descricao)
        event.put("dtstart", data)
        relogio.set(ano, mes, dia, 18, 0)
        data = relogio.getTimeInMillis()
        event.put("dtend", data)
        event.put("eventTimezone", TimeZone.getDefault().getID())
        if (qtRepete > 1) {
            if (intervalo == 300) event.put("rrule", "FREQ=DAILY;INTERVAL=30;COUNT=" + qtRepete)
            else if (intervalo == 3650) event.put("rrule", "FREQ=YEARLY;COUNT=" + qtRepete)
            else if (intervalo == 107) event.put("rrule", "FREQ=DAILY;INTERVAL=7;COUNT=" + qtRepete)
            else event.put("rrule", "FREQ=DAILY;COUNT=" + qtRepete)
        }
        event.put("hasAlarm", 1)

        val eventUri: Uri?
        var eventID: Long

        try {
            eventUri = cr.insert(Uri.parse(eventUriStr), event)
            eventID = eventUri!!.getLastPathSegment()!!.toLong()
        } catch (e: Exception) {
            eventID = 0
        }

        if (comAlerta) {
            var reminderUriString = "content://com.android.calendar/reminders"
            if (Build.VERSION.SDK_INT < 8) reminderUriString = "content://calendar/reminders"

            val reminderValues = ContentValues()
            reminderValues.put("event_id", eventID)
            // Default value of the system. Minutes is a integer
            reminderValues.put("minutes", 15)
            // Alert Methods: Default(0), Alert(1), Email(2), SMS(3)
            reminderValues.put("method", 1)
            try {
                cr.insert(Uri.parse(reminderUriString), reminderValues)
            } catch (e: Exception) {
                eventID = 0
            }
        }
        return eventID
    }
}