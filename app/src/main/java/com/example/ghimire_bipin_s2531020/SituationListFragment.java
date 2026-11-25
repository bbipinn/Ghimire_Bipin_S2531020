package com.example.ghimire_bipin_s2531020;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SituationListFragment extends Fragment {

    public SituationListFragment() { super(R.layout.fragment_list); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        RecyclerView rv = view.findViewById(R.id.situationList);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        SituationViewModel vm = new ViewModelProvider(requireActivity()).get(SituationViewModel.class);

        vm.getFiltered().observe(getViewLifecycleOwner(), list ->
                rv.setAdapter(new RecViewCustomAdapter(list, s -> {
                    vm.select(s);
                    // Portrait  no detail pane on screen, so swap fragments
                    if (requireActivity().findViewById(R.id.detailContainer) == null) {
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.listContainer, new SituationDetailFragment())
                                .addToBackStack(null)
                                .commit();
                    }
                })));
    }
}