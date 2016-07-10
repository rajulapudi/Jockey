package com.marverenic.music.data.store;

import android.content.Context;
import android.support.annotation.Nullable;

import com.marverenic.music.R;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

public class LocalPlaylistStore implements PlaylistStore {

    private Context mContext;
    private BehaviorSubject<List<Playlist>> mPlaylists;

    public LocalPlaylistStore(Context context) {
        mContext = context;
    }

    @Override
    public Observable<Boolean> refresh() {
        return MediaStoreUtil.promptPermission(mContext)
                .subscribeOn(Schedulers.io())
                .map(granted -> {
                    if (mPlaylists != null) {
                        mPlaylists.onNext(getAllPlaylists());
                    }
                    return granted;
                });
    }

    @Override
    public Observable<List<Playlist>> getPlaylists() {
        if (mPlaylists == null) {
            mPlaylists = BehaviorSubject.create();

            MediaStoreUtil.getPermission(mContext)
                    .observeOn(Schedulers.io())
                    .subscribe(granted -> {
                        if (granted) {
                            mPlaylists.onNext(getAllPlaylists());
                        } else {
                            mPlaylists.onNext(Collections.emptyList());
                        }
                    });
        }
        return mPlaylists.asObservable().observeOn(AndroidSchedulers.mainThread());
    }

    private List<Playlist> getAllPlaylists() {
        return MediaStoreUtil.getAllPlaylists(mContext);
    }

    @Override
    public Observable<List<Song>> getSongs(Playlist playlist) {
        return Observable.just(MediaStoreUtil.getPlaylistSongs(mContext, playlist));
    }

    @Override
    public Observable<List<Playlist>> searchForPlaylists(String query) {
        return Observable.just(MediaStoreUtil.searchForPlaylists(mContext, query));
    }

    @Override
    public String verifyPlaylistName(String playlistName) {
        if (playlistName == null || playlistName.trim().isEmpty()) {
            return mContext.getString(R.string.error_hint_empty_playlist);
        }

        if (MediaStoreUtil.findPlaylistByName(mContext, playlistName) != null) {
            return mContext.getString(R.string.error_hint_duplicate_playlist);
        }

        return null;
    }

    @Override
    public Playlist makePlaylist(String name) {
        return makePlaylist(name, null);
    }

    @Override
    public AutoPlaylist makePlaylist(AutoPlaylist model) {
        // TODO implement AutoPlaylists
        return null;
    }

    @Override
    public Playlist makePlaylist(String name, @Nullable List<Song> songs) {
        Playlist created = MediaStoreUtil.createPlaylist(mContext, name, songs);

        if (mPlaylists != null && mPlaylists.getValue() != null) {
            List<Playlist> updated = new ArrayList<>(mPlaylists.getValue());
            updated.add(created);
            Collections.sort(updated);

            mPlaylists.onNext(updated);
        }

        return created;
    }

    @Override
    public void removePlaylist(Playlist playlist) {
        MediaStoreUtil.deletePlaylist(mContext, playlist);

        if (mPlaylists != null && mPlaylists.getValue() != null) {
            List<Playlist> updated = new ArrayList<>(mPlaylists.getValue());
            updated.remove(playlist);

            mPlaylists.onNext(updated);
        }
    }

    @Override
    public void editPlaylist(Playlist playlist, List<Song> newSongs) {
        MediaStoreUtil.editPlaylist(mContext, playlist, newSongs);
    }

    @Override
    public void editPlaylist(AutoPlaylist replacementModel) {
        // TODO implement AutoPlaylists
    }

    @Override
    public void addToPlaylist(Playlist playlist, Song song) {
        MediaStoreUtil.appendToPlaylist(mContext, playlist, song);
    }

    @Override
    public void addToPlaylist(Playlist playlist, List<Song> songs) {
        MediaStoreUtil.appendToPlaylist(mContext, playlist, songs);
    }
}
