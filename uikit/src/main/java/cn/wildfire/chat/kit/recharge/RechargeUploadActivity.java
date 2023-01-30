package cn.wildfire.chat.kit.recharge;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.king.zxing.util.CodeUtils;
import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.bean.ImageItem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.third.utils.ImageUtils;
import cn.wildfire.chat.kit.utils.GlideUtil;
import cn.wildfire.chat.kit.utils.LogHelper;
import cn.wildfirechat.model.RechargeChannel;
import cn.wildfirechat.model.RechargeResultInfo;

/**
 * 上传充值截图
 */
public class RechargeUploadActivity extends WfcBaseActivity {
    @BindView(R2.id.bankOwnerNameLabel)
    TextView bankOwnerNameLabel; // 收款人姓名 标题
    @BindView(R2.id.rechargeQrCodeLabel)
    TextView rechargeQrCodeLabel; // 收款码
    @BindView(R2.id.bankNameLabel)
    TextView bankNameLabel; // 银行名称 标题
    @BindView(R2.id.bankAccountNumberLabel)
    TextView bankAccountNumberLabel; // 银行帐号 标题
    @BindView(R2.id.bankOwnerNameTextView)
    TextView bankOwnerNameTextView; // 收款人姓名
    @BindView(R2.id.bankNameTextView)
    TextView bankNameTextView; // 银行名称
    @BindView(R2.id.bankAccountNumberTextView)
    TextView bankAccountNumberTextView; // 银行帐号

    @BindView(R2.id.copyRechargeNameImageView)
    ImageView copyRechargeNameImageView; // 复制姓名
    @BindView(R2.id.copyRechargeAccountImageView)
    ImageView copyRechargeAccountImageView; // 复制帐号

    @BindView(R2.id.rechargeQrCodeImageView)
    ImageView rechargeQrCodeImageView; // QR code
    @BindView(R2.id.downloadQrCodeTextView)
    TextView downloadQrCodeTextView;  // 下载 QR code

    @BindView(R2.id.uploadScreenShotImageView)
    ImageView uploadScreenShotImageView; // 上传截图

    @BindView(R2.id.submitButton)
    Button submitButton;

    private static final int REQUEST_CODE_PICK_IMAGE = 100;

    private RechargeViewModel rechargeViewModel;
    private RechargeChannel channel;
    private RechargeResultInfo rechargeResultInfo;
    private File thumbImgFile;
    private long lastClick = 0;

    @Override
    protected int contentLayout() {
        return R.layout.activity_recharge_upload;
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        rechargeViewModel = new ViewModelProvider(this).get(RechargeViewModel.class);
        init();
    }

    private void init() {
        channel = getIntent().getParcelableExtra("RechargeChannel");
        rechargeResultInfo = getIntent().getParcelableExtra("RechargeResultInfo");
        initView();
        initData();
    }

    private void initView() {
        if (rechargeResultInfo == null) {
            finish();
            return;
        }
        changeLayout(rechargeResultInfo.getMethod());
        // bank
        if (rechargeResultInfo.getMethod() == 1) {
            String nameLabel = getString(R.string.bank_owner_name) + " : ";
            String bankName = getString(R.string.recharge_account_name) + " : ";
            String account = getString(R.string.recharge_account_number) + " : ";
            bankOwnerNameLabel.setText(nameLabel);// 收款人姓名 标题
            bankNameLabel.setText(bankName);// 银行名称 标题
            bankAccountNumberLabel.setText(account);// 银行帐号 标题

            bankOwnerNameTextView.setText(channel.info.realName);// 收款人姓名
            bankNameTextView.setText(channel.info.bankName); // 银行名称
            bankAccountNumberTextView.setText(channel.info.bankAccount); // 银行帐号
        } else if (rechargeResultInfo.getMethod() == 2) {
            // 微信
            String nameLabel = getString(R.string.bank_owner_name2) + " : ";
            String account = getString(R.string.recharge_account_qrCode) + " : ";

            bankOwnerNameLabel.setText(nameLabel);// 收款人姓名 标题
            rechargeQrCodeLabel.setText(account); // 收款码

            bankOwnerNameTextView.setText(channel.info.realName);// 收款人姓名
            showQRCode(channel.info.qrCodeImage);
        } else if (rechargeResultInfo.getMethod() == 3) {
            // 支付宝
            String nameLabel = getString(R.string.bank_owner_name2) + " : ";
            String account = getString(R.string.recharge_account) + " : ";
            String account2 = getString(R.string.recharge_account_qrCode) + " : ";

            bankOwnerNameLabel.setText(nameLabel);// 收款人姓名 标题
            bankAccountNumberLabel.setText(account); // 银行帐号 标题
            rechargeQrCodeLabel.setText(account2); // 收款码

            bankOwnerNameTextView.setText(channel.info.realName);// 收款人姓名
            bankAccountNumberTextView.setText(channel.info.bankAccount); // 银行帐号
            showQRCode(channel.info.qrCodeImage);
        }
    }

    private void changeLayout(int type) {
        // 银行名称
        bankNameLabel.setVisibility(type == 1 ? View.VISIBLE : View.GONE);
        bankNameTextView.setVisibility(type == 1 ? View.VISIBLE : View.GONE);
        // 银行帐号
        bankAccountNumberLabel.setVisibility(type != 2 ? View.VISIBLE : View.GONE);
        bankAccountNumberTextView.setVisibility(type != 2 ? View.VISIBLE : View.GONE);
        copyRechargeAccountImageView.setVisibility(type != 2 ? View.VISIBLE : View.GONE);
        // 收款码
        rechargeQrCodeLabel.setVisibility(type != 1 ? View.VISIBLE : View.GONE);
        rechargeQrCodeImageView.setVisibility(type != 1 ? View.VISIBLE : View.GONE);
        downloadQrCodeTextView.setVisibility(type != 1 ? View.VISIBLE : View.GONE);
    }

    // 显示 qr code
    private void showQRCode(String value) {
        GlideUtil.load(this, value)
                .into(rechargeQrCodeImageView);
    }

    private void initData() {

    }

    // 复制名字
    @OnClick(R2.id.copyRechargeNameImageView)
    public void copyRechargeName() {
        copyText("name", bankOwnerNameTextView.getText().toString());
    }

    // 复制帐号
    @OnClick(R2.id.copyRechargeAccountImageView)
    public void copyRechargeAccount() {
        copyText("account", bankAccountNumberTextView.getText().toString());
    }

    // 上传截图
    @OnClick(R2.id.uploadScreenShotImageView)
    public void uploadScreenShot() {
        imagePicker();
    }

    // 相册
    private void imagePicker() {
        ImagePicker.picker().pick(this, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
            if (images == null || images.isEmpty()) {
                Toast.makeText(this, "选择截图失败", Toast.LENGTH_SHORT).show();
                return;
            }
            thumbImgFile = ImageUtils.compressImage(images.get(0).path);
            if (thumbImgFile == null) {
                Toast.makeText(this, "更新截图失败: 生成缩略图失败", Toast.LENGTH_SHORT).show();
                return;
            }

            GlideUtil.loadAsBitmap(this, images.get(0).path)
                    .into(uploadScreenShotImageView);
        }
    }

    // 下载QR code
    @OnClick(R2.id.downloadQrCodeTextView)
    public void downloadQrCode() {
        try {
            // 取得外部儲存裝置路徑
            File appDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "qrCode");
            if (!appDir.exists()) {
                appDir.mkdir();
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA);
            String fileName = dateFormat.format(new Date(System.currentTimeMillis())) + ".png";
            File file = new File(appDir, fileName);
            // 開啟檔案串流
            FileOutputStream out = new FileOutputStream(file);
            // 將 Bitmap壓縮成指定格式的圖片並寫入檔案串流
            Bitmap qrBitmap = ((BitmapDrawable)rechargeQrCodeImageView.getDrawable()).getBitmap();
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            // 刷新並關閉檔案串流
            out.flush();
            out.close();
            Toast.makeText(this, "图片已下载完成", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // 提交
    @OnClick(R2.id.submitButton)
    public void submitClick() {
        if ((System.currentTimeMillis() - lastClick) < 500) {
            LogHelper.e("click", "submitClick < 500");
            return;
        } else {
            lastClick = System.currentTimeMillis();
        }

        if (thumbImgFile == null) {
            Toast.makeText(this, R.string.upload_error, Toast.LENGTH_SHORT).show();
            return;
        }
        rechargeViewModel.uploadRechargeScreenshot(rechargeResultInfo.getId(), thumbImgFile).observe(this, response -> {
            if (response.code != 0) {
                Toast.makeText(RechargeUploadActivity.this, response.message, Toast.LENGTH_SHORT).show();
                return;
            }
            setResult(RESULT_OK);
            finish();
            Toast.makeText(RechargeUploadActivity.this, R.string.upload_success, Toast.LENGTH_SHORT).show();
        });
    }

    private void copyText(String label, String text) {
        Object clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            return;
        }
        ClipData clipData = ClipData.newPlainText(label, text);
        ((ClipboardManager) clipboardManager).setPrimaryClip(clipData);
        Toast.makeText(this, "复制成功", Toast.LENGTH_SHORT).show();
    }
}
