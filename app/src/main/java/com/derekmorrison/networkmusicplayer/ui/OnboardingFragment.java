package com.derekmorrison.networkmusicplayer.ui;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.derekmorrison.networkmusicplayer.R;
import com.derekmorrison.networkmusicplayer.data.NMPDbHelper;
import com.derekmorrison.networkmusicplayer.sync.NetworkQueryService;

/**
 * A simple {@link Fragment} subclass.
 */
public class OnboardingFragment extends Fragment {

    public static final int ONBOARDING_SCAN_DEPTH = 4;

    public OnboardingFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View onboarding =  inflater.inflate(R.layout.fragment_onboarding, container, false);

        Button startButton = (Button)onboarding.findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // send message to NetworkQueryService
                NetworkQueryService.startActionScanNode(getContext(),
                        0, "smb://", ONBOARDING_SCAN_DEPTH, NMPDbHelper.NODE_TYPE_START);





                // switch to the Initial Network Scan fragment
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                InitialScanFragment fragment = new InitialScanFragment();
                transaction.replace(R.id.sample_content_fragment, fragment);
                transaction.commit();

            }
        });

        return onboarding;
    }

}
