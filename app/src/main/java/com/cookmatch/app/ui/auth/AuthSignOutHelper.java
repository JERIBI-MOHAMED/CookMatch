package com.cookmatch.app.ui.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;

import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.Executors;

public final class AuthSignOutHelper {

    private AuthSignOutHelper() {
    }

    public static void signOut(Activity activity) {
        FirebaseAuth.getInstance().signOut();

        CredentialManager credentialManager = CredentialManager.create(activity.getApplicationContext());
        credentialManager.clearCredentialStateAsync(
                new ClearCredentialStateRequest(),
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(Void result) {
                        goToAuth(activity);
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        goToAuth(activity);
                    }
                }
        );
    }

    private static void goToAuth(Activity activity) {
        activity.runOnUiThread(() -> {
            Intent intent = new Intent(activity, AuthActivity.class);
            activity.startActivity(intent);
            activity.finish();
        });
    }
}
