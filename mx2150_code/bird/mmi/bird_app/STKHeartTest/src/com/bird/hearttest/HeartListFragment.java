package com.sensortek.stkhealthcare2;

import com.sensortek.stkhealthcare2.provider.Heart;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.app.LoaderManager;
import android.content.Loader;
import com.sensortek.stkhealthcare2.provider.Heart;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.text.ParseException;
import android.animation.ObjectAnimator;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
import android.view.animation.AccelerateDecelerateInterpolator;

public class HeartListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	
    private static final String TAG = "HeartListFragment";
    private ListView mListView;
    private HeartAdapter mAdapter;
    private View mEmptyView;
    private Loader mCursorLoader = null;
    private RelativeLayout mTitleRelativeLayout;
    
	@Override
	public void onAttach(Activity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
       mCursorLoader = getLoaderManager().initLoader(0, null, this);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView");
        final View view = inflater.inflate(R.layout.heart_list_fragment_layout, container, false);
        mListView = (ListView) view.findViewById(R.id.heart_list);
        mEmptyView = view.findViewById(R.id.heart_empty_view);
        mTitleRelativeLayout = (RelativeLayout) view.findViewById(R.id.tab_title);
        mAdapter = new HeartAdapter(getActivity(),mListView);
        mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                final int count = mAdapter.getCount();
                mEmptyView.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
                super.onChanged();
            }
        });
        mListView.setAdapter(mAdapter);
        return view;
	}
	
	private class HeartAdapter extends CursorAdapter{
		
        private String[] currentString= new String[3];
        private String[] oldString= new String[3];
        private ThreadLocal<SimpleDateFormat> DateLocal = new ThreadLocal<SimpleDateFormat>();  
        private SimpleDateFormat mFormatter;
        
        public class ItemHolder {
            TextView bpm;
            TextView date;
            TextView time;
        }

		private final Context mContext;
		private final LayoutInflater mFactory;
		private final ListView mList;
       private String mYear;
       private String mDate;
       private String mTime;
		
		public HeartAdapter(Context context,ListView list) {
                super(context, null, 0);
                mContext = context;
                mFactory = LayoutInflater.from(context);
                mList = list;
                mFormatter = new SimpleDateFormat("yyyy-MMdd-HH:mm");  
		}

        @Override
        public int getCount() {
            return super.getCount();
        }
        
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			 final Heart heart = new Heart(cursor);
            Object tag = view.getTag();
            if (tag == null) {
                // The view was converted but somehow lost its tag.
                tag = setNewHolder(view);
            }
            final ItemHolder itemHolder = (ItemHolder) tag;
            try {
                getDate(heart.dateAndTime);
                if (IsToday(heart.dateAndTime)) {
                    itemHolder.date.setText(R.string.today);
                } else if (IsYesterday(heart.dateAndTime)) {
                    itemHolder.date.setText(R.string.yesterday);
                } else {
                    if (IsThisYear(heart.dateAndTime)) {
                        itemHolder.date.setText(mDate);
                    } else {
                        itemHolder.date.setText(mYear + mDate);
                    }
                }
                itemHolder.time.setText(mTime);
            } catch (Exception e) {
                e.printStackTrace();
            }
            itemHolder.bpm.setText(String.valueOf(heart.bpm));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final View view = mFactory.inflate(R.layout.heart_history_list, parent, false);
            setNewHolder(view);
            return view;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
            if (!getCursor().moveToPosition(position)) {
                // May happen if the last alarm was deleted and the cursor refreshed while the
                // list is updated.
                Log.v(TAG,"couldn't move cursor to position " + position);
                return null;
            }
            View v;
            
			if (convertView == null) {
                v = newView(mContext, getCursor(), parent);
            } else {
                v = convertView;
            }
            if (PulseFragment.isNewInsertData) {
                if (position == 0) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(v, "alpha", 0f, 1f); 
                    animator.setInterpolator(new AccelerateDecelerateInterpolator());
                    animator.setDuration(800);  
                    animator.start();  
                } else {
                    float curTranslationY = v.getTranslationY();  
                    ObjectAnimator animator = ObjectAnimator.ofFloat(v, "translationY", -20, curTranslationY);  
                    animator.setDuration(500);  
                    animator.start();  
                }
                int count = getCount();
                if (count <= 4) {
                    if (position + 1 >= count) {
                        PulseFragment.isNewInsertData = false;
                    }
                } else {
                    if (position + 1 >= 4) {
                        PulseFragment.isNewInsertData = false;
                    }
                }
            }
            bindView(v, mContext, getCursor());
            return v;

		}

        private ItemHolder setNewHolder(View view) {
            // standard view holder optimization
            final ItemHolder holder = new ItemHolder();
            holder.bpm = (TextView) view.findViewById(R.id.bpm);
            holder.date = (TextView) view.findViewById(R.id.date);
            holder.time = (TextView) view.findViewById(R.id.time);
            view.setTag(holder);
            return holder;
        }
    
    public boolean IsToday(String day) throws ParseException {  
        Calendar pre = Calendar.getInstance();  
        Date predate = new Date(System.currentTimeMillis());  
        pre.setTime(predate);  
  
        Calendar cal = Calendar.getInstance();  
        Date date = getDateFormat().parse(day);  
        cal.setTime(date);  
  
        if (cal.get(Calendar.YEAR) == (pre.get(Calendar.YEAR))) {  
            int diffDay = cal.get(Calendar.DAY_OF_YEAR)  
                    - pre.get(Calendar.DAY_OF_YEAR);  
  
            if (diffDay == 0) {  
                return true;  
            }  
        }  
        return false;  
    }  
  
    public boolean IsYesterday(String day) throws ParseException {  
  
        Calendar pre = Calendar.getInstance();  
        Date predate = new Date(System.currentTimeMillis());  
        pre.setTime(predate);  
  
        Calendar cal = Calendar.getInstance();  
        Date date = getDateFormat().parse(day);  
        cal.setTime(date);  
  
        if (cal.get(Calendar.YEAR) == (pre.get(Calendar.YEAR))) {  
            int diffDay = cal.get(Calendar.DAY_OF_YEAR)  
                    - pre.get(Calendar.DAY_OF_YEAR);  
  
            if (diffDay == -1) {  
                return true;  
            }  
        }  
        return false;  
    }  

    public boolean IsThisYear(String day) throws ParseException {  
        Calendar pre = Calendar.getInstance();  
        Date predate = new Date(System.currentTimeMillis());  
        pre.setTime(predate);  
  
        Calendar cal = Calendar.getInstance();  
        Date date = getDateFormat().parse(day);  
        cal.setTime(date);  
  
        if (cal.get(Calendar.YEAR) == (pre.get(Calendar.YEAR))) {   
            return true;  
        }  
        return false;  
    }  
    
        public SimpleDateFormat getDateFormat() {  
            return new SimpleDateFormat("yyyy-MMdd-HH:mm");  
        }  

        public void getDate(String currentTime) {  
            String[] now = currentTime.split("-"); 
            //mYear = now[0];
            if (!now[0].isEmpty() && now[0].length() >= 2) {
                mYear = now[0].substring(now[0].length() - 2, now[0].length());
            } else {
                mYear = "";
            }
            mDate= now[1];
            mTime= now[2];
        }  
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return Heart.getHeartCursorLoader(getActivity());
	}
	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.swapCursor(null);
	}
	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
		mAdapter.swapCursor(data);
	}

    public RelativeLayout getTitleLayout() {
        return mTitleRelativeLayout;
    }
}
