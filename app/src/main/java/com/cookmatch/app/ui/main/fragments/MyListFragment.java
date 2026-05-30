package com.cookmatch.app.ui.main.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cookmatch.app.R;
import com.cookmatch.app.databinding.FragmentMyListBinding;
import com.cookmatch.app.ui.main.home.HomeViewModel;
import com.cookmatch.app.ui.main.mylist.IngredientsAdapter;

public class MyListFragment extends Fragment {

    private FragmentMyListBinding binding;
    private IngredientsAdapter adapter;
    private HomeViewModel searchViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMyListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        adapter = new IngredientsAdapter(ingredient -> {
            adapter.remove(ingredient);
        });

        binding.ingredientsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.ingredientsRecyclerView.setAdapter(adapter);

        binding.addIngredientButton.setOnClickListener(v -> addIngredient());
        binding.ingredientInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) { addIngredient(); return true; }
            return false;
        });

        binding.findRecipesButton.setOnClickListener(v -> {
            String query = String.join(",", adapter.getItems());
            if (TextUtils.isEmpty(query)) {
                Toast.makeText(requireContext(), "Add some ingredients first", Toast.LENGTH_SHORT).show();
                return;
            }
            // Navigate to Search with first ingredient as query
            Bundle args = new Bundle();
            NavHostFragment.findNavController(this).navigate(R.id.searchFragment, args);
        });

        String prefill = getArguments() != null ? getArguments().getString("prefillIngredients", "") : "";
        if (!prefill.isEmpty()) {
            String[] items = prefill.split(",");
            for (String item : items) {
                String ingredient = item.trim();
                if (!ingredient.isEmpty()) {
                    adapter.add(ingredient);
                }
            }
        }
    }

    private void addIngredient() {
        String ing = String.valueOf(binding.ingredientInput.getText()).trim();
        if (!TextUtils.isEmpty(ing)) {
            adapter.add(ing);
            binding.ingredientInput.setText("");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
