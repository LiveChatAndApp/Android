/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.favorite.viewholder;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.favorite.FavoriteItem;
import cn.wildfire.chat.kit.mm.MMPreviewActivity;
import cn.wildfire.chat.kit.utils.GlideUtil;

public class FavVideoContentViewHolder extends FavContentViewHolder {
    @BindView(R2.id.favImageContentImageView)
    ImageView imageView;

    public FavVideoContentViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    @Override
    public void bind(Fragment fragment, FavoriteItem item) {
        super.bind(fragment, item);
        GlideUtil.load(itemView, item.getUrl())
                .into(imageView);
    }

    @OnClick(R2.id.favImageContentImageView)
    void showFavImage() {
        MMPreviewActivity.previewVideo(fragment.getActivity(), favoriteItem.getUrl());
    }
}
