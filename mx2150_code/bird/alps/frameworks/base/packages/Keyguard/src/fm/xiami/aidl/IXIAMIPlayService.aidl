package fm.xiami.aidl;

import fm.xiami.aidl.SongInfo;
import fm.xiami.aidl.IXIAMIMusicCallbackForThird;
import android.graphics.Bitmap;

/**
 * Created by jjc on 2014/10/27.
 */
interface IXIAMIPlayService {
 void prev();
 void next();
 void pause();
 void resume();
 int  getPlayState();
 SongInfo getPlaySongInfo();
  void seek(int pos);
  long getCachePercent();
  long getDuration();
  long getCurrTime();
 void requestLrcDownload();
 void registerCallback(IXIAMIMusicCallbackForThird cb);
 void unRegisterCallback (IXIAMIMusicCallbackForThird cb);
 void requestBitmap();
 void playCurrent();
 void stop();
 int getPlayMode();
 void setPlayMode();
}
