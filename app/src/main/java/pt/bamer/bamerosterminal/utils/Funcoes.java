package pt.bamer.bamerosterminal.utils;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import pt.bamer.bamerosterminal.R;

@SuppressWarnings("unused")
public class Funcoes {

    public static String localDateTimeToStrFull(LocalDateTime data) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
        return formatter.print(data);
    }

    public static String localDateTimeToStrFullaZeroHour(LocalDateTime data) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd 00:00:00");
        return formatter.print(data);
    }

    public static String dToC(LocalDateTime data) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd-MM-yyyy");
        return formatter.print(data);
    }

    public static String currentTimeStringStamp() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        Calendar calendar = GregorianCalendar.getInstance();
        return dateFormatter.format(calendar.getTime());
    }

    public static LocalDateTime cToT(String datastr) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
        return formatter.parseLocalDateTime(datastr);
    }

    public static String dataBonita(String data) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.yyyy");
        LocalDateTime local = cToT(data);
        return formatter.print(local);
    }

    public static String milisegundos_em_HH_MM_SS(long milisegundos) {
        PeriodFormatter fmt = new PeriodFormatterBuilder()
                .printZeroAlways()
                .minimumPrintedDigits(2)
                .appendHours()
                .appendSeparator(":")
                .printZeroAlways()
                .minimumPrintedDigits(2)
                .appendMinutes()
                .appendSeparator(":")
                .printZeroAlways()
                .minimumPrintedDigits(2)
                .appendSeconds()
                .toFormatter();
        Period period = new Period(milisegundos);
        return fmt.print(period);
    }

    public static void alerta(Context context, String titulo, String mensagem) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.myDialog));
        alertBuilder.setMessage(mensagem);
        alertBuilder.setTitle(titulo);
        alertBuilder.setPositiveButton("OK", null);
        alertBuilder.create();
        alertBuilder.show();
    }

    public static long adicionarSecsToUnix(long valor, int sec) {
        long retryDate = valor * 1000L;
        Timestamp original = new Timestamp(retryDate);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(original.getTime());
        cal.add(Calendar.SECOND, sec);
        Timestamp later = new Timestamp(cal.getTime().getTime());
        return later.getTime()/1000;
    }

    public static String hojeMeiaNoite(String formato) {
        SimpleDateFormat sdf = new SimpleDateFormat(formato, Locale.getDefault());
        return sdf.format(new Date());
    }
}

