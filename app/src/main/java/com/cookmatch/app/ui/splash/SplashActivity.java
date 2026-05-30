package com.cookmatch.app.ui.splash;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.cookmatch.app.R;
import com.cookmatch.app.ui.auth.AuthActivity;
import com.cookmatch.app.ui.main.MainActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        LottieAnimationView lottie = findViewById(R.id.lottieAnimation);
        lottie.addAnimatorListener(new android.animation.Animator.AnimatorListener() {
            @Override public void onAnimationStart(android.animation.Animator a) {}
            @Override public void onAnimationCancel(android.animation.Animator a) {}
            @Override public void onAnimationRepeat(android.animation.Animator a) {}

            @Override
            public void onAnimationEnd(android.animation.Animator a) {
                navigate();
            }
        });
    }

    private void navigate() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, AuthActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }
}
