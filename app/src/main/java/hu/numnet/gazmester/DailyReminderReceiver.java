package hu.numnet.gazmester;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import android.app.NotificationManager;
import android.app.PendingIntent;

public class DailyReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "gazmester_channel";
    private static final int NOTIFICATION_ID = 2;

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Gázóra leolvasás emlékeztető")
                .setContentText("Ne felejtsd el rögzíteni a gázóra állását!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
