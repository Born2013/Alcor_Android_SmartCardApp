package fm.xiami.aidl;

import android.graphics.Bitmap;

/**
 * Created by jjc on 2014/10/27.
 */
interface IXIAMIMusicCallbackForThird {
 void onPlaySongChanged();
 void onServiceDestory();
 void onPlayStateChanged(long songId,int state);
 void onAlbumCoverChanged(in Bitmap bitmap);
 void onLyricChanged(String lyricPath,String text);
}
