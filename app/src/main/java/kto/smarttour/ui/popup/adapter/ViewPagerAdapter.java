package kto.smarttour.ui.popup.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {

	private ArrayList<Fragment> fragments;

	public ViewPagerAdapter(FragmentManager fm, ArrayList<Fragment> fragments) {
		super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
		this.fragments = fragments;
	}


	@NonNull
	@Override
	public Fragment getItem(int position) {
		if(position < fragments.size()) {
			return fragments.get(position);
		}
		return null;
	}

	@Override
	public int getCount() {
		return fragments.size();
	}
}
