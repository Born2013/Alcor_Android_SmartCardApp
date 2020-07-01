package com.sensortek.stkhealthcare2;

import android.app.Fragment;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.sensortek.stkhealthcare2.provider.Heart;
import com.sensortek.stkhealthcare2.provider.HeartContract;
import android.content.ContentResolver;
import android.content.Context;
import java.util.List;
import android.os.AsyncTask;
import android.database.ContentObserver;
import android.os.Handler;
import android.widget.TextView;
import android.widget.RelativeLayout;

public class HeartChartFragment extends Fragment {

    public static final String TAG = "HeartChartFragment";
    private MyAsync mTask;
    private Context mContext;
    private HistogramView mHistogramView;
    private TextView mAverage;
    private final Handler mHandler = new Handler();
    private View mEmptyView;
    private RelativeLayout mTitleRelativeLayout;

    private final ContentObserver mHeartObserver =  new ContentObserver(mHandler) {
        
        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG,"onChange");
            final ContentResolver cr = mContext.getContentResolver();
            final List<Heart> hearts = Heart.getLastHearts(cr,null,null);
            mHistogramView.refreshData(hearts);
            refreshAverage();
        }
    };
            
	@Override
	public void onAttach(Activity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContext = getActivity();
        mTask = new MyAsync(mContext);
        mTask.execute();
        mContext.getContentResolver().registerContentObserver(
            HeartContract.CONTENT_URI,
            true,
            mHeartObserver);
        Log.d(TAG,"onActivityCreated");
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        Log.d(TAG,"onResume");
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView");
        final View view = inflater.inflate(R.layout.heart_chart_fragment_layout, container, false);
        mTitleRelativeLayout = (RelativeLayout) view.findViewById(R.id.chart_tab_title);
        mHistogramView = (HistogramView)view.findViewById(R.id.histogram);
        mAverage =  (TextView)view.findViewById(R.id.average_value);
        mEmptyView = view.findViewById(R.id.heart_chart_empty_view);
        return view;
	}

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        Log.d(TAG,"onPause");
        
    }

	@Override
	public void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
        Log.d(TAG,"onStop");
	}

	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		super.onDestroyView();
        mContext.getContentResolver().unregisterContentObserver(mHeartObserver);
        Log.d(TAG,"onDestroyView");
	}
    
    private class MyAsync extends AsyncTask<Void, Void, List<Heart> > {

        private final Context mContext;
        
        public MyAsync(Context context) {
            mContext = context;
        }

        @Override
        protected List<Heart> doInBackground(Void... parameters) {
            final ContentResolver cr = mContext.getContentResolver();
            final List<Heart> hearts = Heart.getLastHearts(cr,null,null);
            Log.d(TAG,"doInBackground hearts = "+hearts);
            if (hearts.isEmpty()) {
                return null;
            }
            return hearts;
        }

        @Override
        protected void onPostExecute(List<Heart> result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            Log.d(TAG,"onPostExecute");
            mHistogramView.refreshData(result); 
            refreshAverage();
        }
    }

    private void refreshAverage() {
        if (mHistogramView.getAverage() == 0) {
            mAverage.setText("");
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mAverage.setText(String.valueOf(mHistogramView.getAverage()));
            mEmptyView.setVisibility(View.GONE);
        }
    }

    public RelativeLayout getTitleLayout() {
        return mTitleRelativeLayout;
    }
}
