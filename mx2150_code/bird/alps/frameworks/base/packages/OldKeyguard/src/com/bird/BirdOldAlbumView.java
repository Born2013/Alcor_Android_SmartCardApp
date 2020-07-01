package com.android.keyguard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterViewFlipper;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.content.Intent;
import java.io.File;
import java.util.ArrayList;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * Created by root on 17-8-15.
 */

public class BirdOldAlbumView extends FrameLayout {

    private static final String TAG = "BirdOldAlbumView";

    private final static String OLDPHONE_FOLDER_NAME = ".OldAlbum";

    public final static String OLDALBUM_FOLDER_PATH = Environment.getExternalStorageDirectory() + File.separator + OLDPHONE_FOLDER_NAME + File.separator;
    public final static String FROM_INIALBUM_PATH = OLDALBUM_FOLDER_PATH + "Init";
    public final static String FROM_CROP_PATH = OLDALBUM_FOLDER_PATH + "CROP";

    private AdapterViewFlipper mFlipper;
    private MyAdapter myAdapter;
    private int mWidth;
    private int mHeight;
    private ArrayList<File> mFiles = new ArrayList<>();
    private BitmapFactory.Options mOptions;

    private int[] DEFAULTID = new int[]{
            R.drawable.oldphone_photowall_bg1,
            R.drawable.oldphone_photowall_bg2,
            R.drawable.oldphone_photowall_bg3,
            R.drawable.oldphone_photowall_bg4,
            R.drawable.oldphone_photowall_bg5,
            R.drawable.oldphone_photowall_default
    };


    private final static int cache = (int) Runtime.getRuntime().maxMemory() / 1024;//unit KB;

    private static LruCache<String, RoundDrawable> mMemoryCache = new LruCache<String, RoundDrawable>(cache / 8) {

        @Override
        protected int sizeOf(String key, RoundDrawable value) {
            int sum = value.getBitmap().getByteCount() / 1024;
            Log.d(TAG, "sum = " + sum);
            return sum; // unit KB
        }

        @Override
        protected void entryRemoved(boolean evicted, String key, RoundDrawable oldValue, RoundDrawable newValue) {
            super.entryRemoved(evicted, key, oldValue, newValue);
            Log.d(TAG, "key = " + key);
            if (evicted) {
                if (!oldValue.isRecycled()) {
                    oldValue.recycle();
                }
            }
        }
    };

    public BirdOldAlbumView(@NonNull Context context) {
        this(context, null);
    }

    public BirdOldAlbumView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BirdOldAlbumView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public BirdOldAlbumView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
        mOptions = new BitmapFactory.Options();
        mOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), R.drawable.oldphone_panel_mask, mOptions);
        mWidth = mOptions.outWidth;
        mHeight = mOptions.outHeight;

    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mWidth, mHeight);

    }


    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.zzzzz_oldphone_album, this, true);

    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mFlipper =(AdapterViewFlipper) findViewById(R.id.show_album);
        myAdapter = new MyAdapter();
        mFlipper.setAdapter(myAdapter);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        getContext().registerReceiver(receiverNext,filter);
        /*File file = new File(FROM_INIALBUM_PATH);
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].getName().endsWith(".JPEG") || files[i].getName().endsWith(".png")) {
                        mFiles.add(files[i]);
                    }
                }
            } 
        }*/
        
        File file1 = new File(FROM_CROP_PATH);
        if (file1.exists()) {
            File[] files1 = file1.listFiles();
            if (files1 != null) {
                for (int i = 0; i < files1.length; i++) {
                    if (files1[i].getName().endsWith(".JPEG")) {
                        mFiles.add(files1[i]);
                    }
                }
            } 
        }
        
        if (myAdapter != null) {
            myAdapter.notifyDataSetChanged();
        }
    }

    private BroadcastReceiver receiverNext = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
                  if (mFlipper != null) {
                     mFlipper.showNext();
                  }
        }
    };

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }


    protected class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
              int count = mFiles.size()+DEFAULTID.length;
              Log.d(TAG, "count = " + count);
              return count;
        }

        @Override
        public Object getItem(int position) {

            if (!mFiles.isEmpty()) {
                if (position < mFiles.size()) {
                        return mFiles.get(position);
                } else {
                       return DEFAULTID[position-mFiles.size()];
                }
            } else {
                return DEFAULTID[position];
            }
            
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            ViewHolder holder;
            if (view == null) {
                holder = new ViewHolder();
                view = holder.imageView = generImgaView();
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            holder.imageView.setImageDrawable(generBitmap(position));
            return view;
        }

        protected class ViewHolder {
            ImageView imageView;
        }

    }

    private ImageView generImgaView() {
        ImageView imageView = new ImageView(getContext());
        
        AdapterViewFlipper.LayoutParams layoutParams = new AdapterViewFlipper.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, mHeight);
        imageView.setLayoutParams(layoutParams);
        imageView.setElevation(8);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        return imageView;
    }

    private RoundDrawable generBitmap(int position) {
        
        if (!mFiles.isEmpty()) {
            if (position < mFiles.size()) {
                RoundDrawable bitmap = mMemoryCache.get(mFiles.get(position).getAbsolutePath());
                if (bitmap == null) {
                    bitmap = ZoomDrawable (BitmapFactory.decodeFile(mFiles.get(position).getAbsolutePath()),mWidth,mHeight);
                    mMemoryCache.put(mFiles.get(position).getAbsolutePath(), bitmap);
                }
                return  bitmap;
            } else {
                RoundDrawable bitmap = mMemoryCache.get(String.valueOf(DEFAULTID[position-mFiles.size()]));
                if (bitmap == null) {
                    bitmap = ZoomDrawable (BitmapFactory.decodeResource(getResources(),DEFAULTID[position-mFiles.size()]),mWidth,mHeight);
                    mMemoryCache.put(String.valueOf(DEFAULTID[position-mFiles.size()]), bitmap);
                }
                return  bitmap;
            }
        } else {
            RoundDrawable bitmap = mMemoryCache.get(String.valueOf(DEFAULTID[position]));
            if (bitmap == null) {
                bitmap = ZoomDrawable (BitmapFactory.decodeResource(getResources(),DEFAULTID[position-mFiles.size()]),mWidth,mHeight);
                mMemoryCache.put(String.valueOf(DEFAULTID[position]), bitmap);
            }
            return bitmap;
        }
        
    }

    

    private RoundDrawable ZoomDrawable (Bitmap bitmap,int deswidth,int desHeight) {
        Bitmap BitmapOrg = bitmap;
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();        
        int newWidth = deswidth;
        int newHeight = desHeight;       
        
        if (width == newWidth && height ==newHeight) {
             return new RoundDrawable(bitmap);
        }
        
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;        
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);        
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width,height, matrix, true); 
        
        return new RoundDrawable(resizedBitmap);
        
    }


}
