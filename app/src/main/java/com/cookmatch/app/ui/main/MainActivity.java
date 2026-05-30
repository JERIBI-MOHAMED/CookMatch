package com.cookmatch.app.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.cookmatch.app.R;
import com.cookmatch.app.databinding.ActivityMainBinding;
import com.cookmatch.app.notification.NotificationHelper;
import com.cookmatch.app.ui.auth.AuthActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.material.navigation.NavigationView;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int RC_NOTIFICATIONS = 9811;
    private static final String PREFS = "cookmatch_prefs";
    private static final String PREF_NOTIFICATIONS = "notifications_enabled";
    private static final String PREF_LAST_COMMUNITY_DOC = "last_community_doc";
    private static final String PREF_COMMUNITY_SEEDED = "community_listener_seeded";

    private ActivityMainBinding binding;
    private NavController navController;
    private ListenerRegistration communityListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.main_nav_host);
        if (navHostFragment == null) {
            return;
        }

        navController = navHostFragment.getNavController();
        navController.setGraph(R.navigation.main_nav_graph);

        Set<Integer> topLevelDestinations = new HashSet<>();
        topLevelDestinations.add(R.id.homeFragment);
        topLevelDestinations.add(R.id.searchFragment);
        topLevelDestinations.add(R.id.scannerFragment);
        topLevelDestinations.add(R.id.myListFragment);
        topLevelDestinations.add(R.id.profileFragment);

        AppBarConfiguration appBarConfiguration =
                new AppBarConfiguration.Builder(topLevelDestinations)
                        .setOpenableLayout(binding.drawerLayout)
                        .build();

        NavigationUI.setupWithNavController(binding.bottomNav, navController);
        NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration);

        binding.navigationView.setNavigationItemSelectedListener(this);
        binding.toolbar.setNavigationOnClickListener(v -> {
            DrawerLayout drawerLayout = binding.drawerLayout;
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        ensureNotificationPermission();
        NotificationHelper.ensureChannel(this);
        FirebaseMessaging.getInstance().subscribeToTopic("community");
        startCommunityNotificationListener();
    }

    private void ensureNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                RC_NOTIFICATIONS
        );
    }

    private void startCommunityNotificationListener() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean(PREF_NOTIFICATIONS, true)) {
            return;
        }

        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        communityListener = FirebaseFirestore.getInstance()
                .collection("community_recipes")
                .orderBy("sharedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || snapshot.isEmpty()) {
                        return;
                    }

                    DocumentSnapshot top = snapshot.getDocuments().get(0);
                    String docId = top.getId();

                    boolean seeded = prefs.getBoolean(PREF_COMMUNITY_SEEDED, false);
                    String lastDocId = prefs.getString(PREF_LAST_COMMUNITY_DOC, "");

                    if (!seeded) {
                        prefs.edit()
                                .putBoolean(PREF_COMMUNITY_SEEDED, true)
                                .putString(PREF_LAST_COMMUNITY_DOC, docId)
                                .apply();
                        return;
                    }

                    if (docId.equals(lastDocId)) {
                        return;
                    }

                    String sharedBy = String.valueOf(top.get("sharedBy"));
                    prefs.edit().putString(PREF_LAST_COMMUNITY_DOC, docId).apply();

                    // Avoid notifying users for their own post.
                    if (sharedBy.equals(currentUid)) {
                        return;
                    }

                    String title = String.valueOf(top.get("title"));
                    String author = String.valueOf(top.get("sharedByName"));
                    if (author == null || author.trim().isEmpty() || "null".equalsIgnoreCase(author)) {
                        author = "Someone";
                    }

                    NotificationHelper.showNotification(
                            MainActivity.this,
                            (int) System.currentTimeMillis(),
                            "New community recipe",
                            author + " shared: " + title
                    );
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        if (communityListener != null) {
            communityListener.remove();
            communityListener = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return true;
        }

        navController.navigate(itemId);
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}
