package cn.wildfire.chat.kit.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;

import cn.wildfire.chat.kit.ImplementUserSource;

public class GlideUtil {
    private static String imageHost = "";

    public static RequestBuilder<Drawable> load(Context context, String imageUrl) {
        return Glide.with(context).load(checkHost(imageUrl));
    }

    public static RequestBuilder<Drawable> load(Context context, int imageUrl) {
        return Glide.with(context).load(imageUrl);
    }

    public static RequestBuilder<Drawable> load(Activity activity, String imageUrl) {
        return Glide.with(activity).load(checkHost(imageUrl));
    }

    public static RequestBuilder<Drawable> load(Fragment fragment, String imageUrl) {
        return Glide.with(fragment).load(checkHost(imageUrl));
    }

    public static RequestBuilder<Drawable> load(Fragment fragment, int imageUrl) {
        return Glide.with(fragment).load(imageUrl);
    }

    public static RequestBuilder<Drawable> load(Fragment fragment, Bitmap bitmap) {
        return Glide.with(fragment).load(bitmap);
    }

    public static RequestBuilder<Drawable> load(View view, String imageUrl) {
        return Glide.with(view).load(checkHost(imageUrl));
    }

    public static RequestBuilder<Drawable> load(View view, Bitmap bitmap) {
        return Glide.with(view).load(bitmap);
    }

    public static RequestBuilder<Bitmap> loadAsBitmap(Activity activity, String imageUrl) {
        return Glide.with(activity).asBitmap().load(checkHost(imageUrl));
    }

    public static String checkHost(String url) {
        String replace = "{{domain}}";

        if (!TextUtils.isEmpty(url) && url.contains(replace)) {
            return url.replace(replace, ImplementUserSource.Instance().getImageHost());
        } else {
            return url;
        }
    }
}
