package com.cookmatch.app.service;

import androidx.annotation.NonNull;

import com.cookmatch.app.notification.NotificationHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class CookMatchFcmService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // TODO: send token to Firestore for server-side targeting
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "CookMatch";
        String body = "Check out trending recipes!";

        if (remoteMessage.getData() != null) {
            String dataTitle = remoteMessage.getData().get("title");
            String dataBody = remoteMessage.getData().get("body");
            if (dataTitle != null && !dataTitle.trim().isEmpty()) {
                title = dataTitle;
            }
            if (dataBody != null && !dataBody.trim().isEmpty()) {
                body = dataBody;
            }
        }

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        }

        NotificationHelper.showNotification(this, (int) System.currentTimeMillis(), title, body);
    }
}
