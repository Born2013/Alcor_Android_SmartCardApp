
package fm.xiami.aidl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Administrator on 14-2-26.
 */
public class SongInfo implements Parcelable {

    private String albumName;

    private String artistName;

    private String songName;

    private long songId;

    public SongInfo() {
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public long getSongId() {
        return songId;
    }

    public void setSongId(long songId) {
        this.songId = songId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(albumName);
        out.writeString(artistName);
        out.writeString(songName);
        out.writeLong(songId);
    }

    public static final Creator<SongInfo> CREATOR = new Creator<SongInfo>() {

        @Override
        public SongInfo createFromParcel(Parcel in) {
            return new SongInfo(in);
        }

        @Override
        public SongInfo[] newArray(int size) {
            return new SongInfo[size];
        }
    };

    private SongInfo(Parcel in) {
        albumName = in.readString();
        artistName = in.readString();
        songName = in.readString();
        songId = in.readLong();
    }
}
