package com.sensortek.stkhealthcare2;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;

import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.sensortek.stkhealthcare2.R;
import android.util.Log;
import android.widget.RelativeLayout;

public class HeartFragment extends Fragment implements ViewPager.OnPageChangeListener{

    public static final String TAG = "HeartFragment";
    public static final int TAB_INDEX_LIST = 0;
    public static final int TAB_INDEX_CHART = 1;
    public static final int TAB_COUNT = 2;

	private ViewPager mViewPager;
	private ImageView mIconList,mIconChart;
	private ViewPagerAdapter mViewPagerAdapter;
	private HeartListFragment mHeartListFragment;
	private HeartChartFragment mHeartChartFragment;
	private String[] mTabTitles;
	private ImageView[] mTabIcons;
   ImageView mLeftIcons;
   ImageView mRightIcons;
    private int mTabIndex = TAB_INDEX_LIST;

    public class ViewPagerAdapter extends FragmentPagerAdapter {
        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

		@Override
        public Fragment getItem(int position) {
            switch (position) {
                case TAB_INDEX_LIST:
                	mHeartListFragment = new HeartListFragment();
                    return mHeartListFragment;
                case TAB_INDEX_CHART:
                	mHeartChartFragment = new HeartChartFragment();
                    return mHeartChartFragment;
            }
            throw new IllegalStateException("No fragment at position " + position);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            // On rotation the FragmentManager handles rotation. Therefore getItem() isn't called.
            // Copy the fragments that the FragmentManager finds so that we can store them in
            // instance variables for later.
            final Fragment fragment =
                    (Fragment) super.instantiateItem(container, position);
            if (fragment instanceof HeartListFragment) {
            	mHeartListFragment = (HeartListFragment) fragment;
            } else if (fragment instanceof HeartChartFragment) {
            	mHeartChartFragment = (HeartChartFragment) fragment;
            } 
            return fragment;
        }

        @Override
        public int getCount() {
            return TAB_COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTabTitles[position];
        }
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		 Log.d(TAG,"onCreateView");
        View parentView = inflater.inflate(R.layout.heart_fragment, container, false);
        mViewPager = (ViewPager) parentView.findViewById(R.id.lists_pager);
        mViewPagerAdapter = new ViewPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.setOffscreenPageLimit(TAB_COUNT - 1);
        mViewPager.setOnPageChangeListener(this);
        
        mTabIcons = new ImageView[TAB_COUNT];
        mTabIcons[TAB_INDEX_LIST] = (ImageView)parentView.findViewById(R.id.list_fragment_icon);
        mTabIcons[TAB_INDEX_CHART] = (ImageView)parentView.findViewById(R.id.chart_fragment_icon);
        
        showTab(TAB_INDEX_LIST);
        refreshTabIcon(TAB_INDEX_LIST);
        return parentView;
	}

	private void refreshTabIcon(int index) {
       Log.d(TAG,"refreshTabIcon index = "+index);
		for(int i=0;i<TAB_COUNT;i++){
			if(i == index) {
				mTabIcons[i].setBackgroundResource(R.drawable.ic_page_on);
			} else {
				mTabIcons[i].setBackgroundResource(R.drawable.ic_page_off);
			}
			
		}
	}

	public void showTab(int index) {
		// TODO Auto-generated method stub
		mViewPager.setCurrentItem(index);
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
		// TODO Auto-generated method stub
		Log.d(TAG,"onPageScrollStateChanged arg0 = "+arg0);
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
	    Log.d(TAG,"onPageScrolled position = "+position);
	    mTabIndex = position;
	}

	@Override
	public void onPageSelected(int position) {
	Log.d(TAG,"onPageSelected position = "+position);
	    mTabIndex = position;
       refreshTabIcon(mTabIndex);	
	}

    public RelativeLayout getTitleLayout() {
        if (mTabIndex == TAB_INDEX_LIST) {
            return mHeartListFragment.getTitleLayout();
        } else if (mTabIndex == TAB_INDEX_CHART) {
            return mHeartChartFragment.getTitleLayout();
        } else {
            return null;
        }
    }
}
