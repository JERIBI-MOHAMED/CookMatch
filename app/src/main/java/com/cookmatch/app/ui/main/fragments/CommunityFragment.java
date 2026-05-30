package com.cookmatch.app.ui.main.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cookmatch.app.databinding.FragmentCommunityBinding;
import com.cookmatch.app.notification.NotificationHelper;
import com.cookmatch.app.presentation.state.UiState;
import com.cookmatch.app.ui.main.community.CommunityAdapter;
import com.cookmatch.app.ui.main.community.CommunityViewModel;

public class CommunityFragment extends Fragment {

    private FragmentCommunityBinding binding;
    private CommunityViewModel viewModel;
    private CommunityAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCommunityBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CommunityViewModel.class);

        adapter = new CommunityAdapter();
        binding.communityRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.communityRecyclerView.setAdapter(adapter);

        binding.shareButton.setOnClickListener(v -> {
            String title = String.valueOf(binding.shareInput.getText()).trim();
            if (TextUtils.isEmpty(title)) { binding.shareInput.setError("Required"); return; }
            viewModel.shareRecipe(title);
        });

        viewModel.getPostsState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            switch (state.getStatus()) {
                case LOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.emptyText.setVisibility(View.GONE);
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    adapter.submitList(state.getData());
                    boolean hasItems = state.getData() != null && !state.getData().isEmpty();
                    binding.emptyText.setVisibility(hasItems ? View.GONE : View.VISIBLE);
                    break;
                case EMPTY:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.emptyText.setVisibility(View.VISIBLE);
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), state.getMessage(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        viewModel.getShareState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            if (state.getStatus() == UiState.Status.SUCCESS) {
                binding.shareInput.setText("");
                Toast.makeText(requireContext(), "Shared!", Toast.LENGTH_SHORT).show();
                NotificationHelper.showNotification(
                        requireContext(),
                        (int) System.currentTimeMillis(),
                        "Recipe shared successfully",
                        "Your community post is now live."
                );
            } else if (state.getStatus() == UiState.Status.ERROR) {
                Toast.makeText(requireContext(), state.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.loadCommunityRecipes();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
