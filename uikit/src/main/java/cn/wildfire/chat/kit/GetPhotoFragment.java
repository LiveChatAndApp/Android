/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.lqr.imagepicker.ImagePicker;

import java.io.File;
import java.util.UUID;

import cn.wildfire.chat.kit.utils.LogHelper;

public class GetPhotoFragment extends Fragment {
    protected static final int REQUEST_CODE_PICK_IMAGE = 100;
    /**
     * 调用相机的requestCode
     */
    protected final int OPEN_CAMERA_REQUEST_CODE = 200;

    protected String[] mandatoryPermissions;

    // 选择开启相册 或 相机 dialog
    protected void showSelectPhotoDialog() {
        new MaterialDialog.Builder(getContext()).items(R.array.head_icon_select).itemsCallback(new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog dialog, View v, int position, CharSequence text) {
                if (position == 0) {
                    // 开启相机
                    if (!checkPermission()) {
                        // 请求权限
                        requestPermissions(mandatoryPermissions, 100);
                        return;
                    }
                    openCamera();
                } else if (position == 1) {
                    // 相册
                    updatePortrait();
                }
            }
        }).show();
    }

    // 相册
    protected void updatePortrait() {
        ImagePicker.picker().pick(this, REQUEST_CODE_PICK_IMAGE);
    }

    protected File tempFile;
    protected Uri imageUri;

    // 开启相机
    protected void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        String fileName = UUID.randomUUID().toString();
        File dir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File folder = new File(dir, "im");
        if (!folder.exists()) {
            folder.mkdir();
        }
        tempFile = new File(folder, fileName + ".jpg");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            imageUri = Uri.fromFile(tempFile);
        } else {
            imageUri = FileProvider.getUriForFile(getActivity(), getActivity().getPackageName() + ".fileprovider", tempFile);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        LogHelper.i("拍照", "imageUri = " + imageUri);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, OPEN_CAMERA_REQUEST_CODE);

        //为拍摄的图片指定一个存储的路径
//        Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, "photoUri");
//        openCameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
//                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
//        startActivityForResult(openCameraIntent, OPEN_CAMERA_REQUEST_CODE);
    }

    /**
     * 权限检查
     */
    public boolean checkPermission() {
        if (mandatoryPermissions == null) {
            mandatoryPermissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }

        boolean granted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : mandatoryPermissions) {
                granted = getActivity().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
                if (!granted) {
                    break;
                }
            }
        }
        return granted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getActivity(), "需要相关权限才能正常使用", Toast.LENGTH_LONG).show();
                getActivity().finish();
                return;
            }
        }
        openCamera();
    }
}
