/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.qrcode;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.king.zxing.util.CodeUtils;

import butterknife.BindView;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.utils.GlideUtil;
import cn.wildfire.chat.kit.utils.LogHelper;
import cn.wildfire.chat.kit.utils.QrCodeUtil;

/**
 * 显示 QR Code
 * WfcScheme.QR_CODE_PREFIX_USER
 * WfcScheme.QR_CODE_PREFIX_CHANNEL
 * WfcScheme.QR_CODE_PREFIX_GROUP
 */
public class QRCodeActivity extends WfcBaseActivity {
    private String TAG = QRCodeActivity.class.getSimpleName();
    public static final int TYPE_PERSON = 1;
    public static final int TYPE_GROUP = 2;
    public static final int TYPE_CHANNEL = 3;

    private int qrCodeType = TYPE_PERSON;
    private String title;
    private String id;
    private String qrCodeValue;

    @BindView(R2.id.qrCodeImageView)
    ImageView qrCodeImageView;

    private QrCodeViewModel viewModel;

    public static Intent buildQRCodeIntent(Context context, String title, int type, String id) {
        Intent intent = new Intent(context, QRCodeActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("qrCodeType", type);
        intent.putExtra("id", QrCodeUtil.getId(type, id));
        return intent;
    }

    @Override
    protected void beforeViews() {
        super.beforeViews();
        Intent intent = getIntent();
        title = intent.getStringExtra("title");
        qrCodeType = intent.getIntExtra("qrCodeType", TYPE_PERSON);
        id = intent.getStringExtra("id");
    }

    @Override
    protected int contentLayout() {
        return R.layout.qrcode_activity;
    }

    @Override
    protected void afterViews() {
        viewModel = new ViewModelProvider(this).get(QrCodeViewModel.class);
        setTitle(title);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogHelper.e(TAG, "title =" + title + ", qrCodeType =" + qrCodeType + ", id =" + id);
        showQRCode(id);
    }

    private void getQrCode() {
        viewModel.getQrCode(new SimpleCallback<GetStringResult>() {
            @Override
            public void onUiSuccess(GetStringResult result) {
                String token = result.getResult();
                LogHelper.i(TAG, "QR code token = " + token);
                showQRCode(token);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Toast.makeText(QRCodeActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showQRCode(String value) {
        CustomViewTarget<ImageView, Bitmap> customViewTarget = new CustomViewTarget<ImageView, Bitmap>(qrCodeImageView) {
            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                // the errorDrawable will always be bitmapDrawable here
                if (errorDrawable instanceof BitmapDrawable) {
                    Bitmap bitmap = ((BitmapDrawable) errorDrawable).getBitmap();
                    Bitmap qrBitmap = CodeUtils.createQRCode(value, 400, bitmap);
                    qrCodeImageView.setImageBitmap(qrBitmap);
                }
            }

            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition transition) {
                Bitmap bitmap = CodeUtils.createQRCode(value, 400, resource);
                qrCodeImageView.setImageBitmap(bitmap);
            }

            @Override
            protected void onResourceCleared(@Nullable Drawable placeholder) {

            }
        };

        GlideUtil.loadAsBitmap(this, "")
                .placeholder(R.mipmap.ic_launcher)
                .into(customViewTarget);
    }
}
