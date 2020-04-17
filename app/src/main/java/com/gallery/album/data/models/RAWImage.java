package com.gallery.album.data.models;

import android.content.Context;
import android.os.Parcel;

import com.gallery.album.R;
import com.gallery.album.util.Util;

public class RAWImage extends Photo {

    RAWImage() {

    }

    RAWImage(Parcel parcel) {
        super(parcel);
    }

    @Override
    public int[] retrieveImageDimens(Context context) {
        return Util.getImageDimensions(context, getUri(context));
    }

    @Override
    public String getType(Context context) {
        return context.getString(R.string.raw_image);
    }
}
