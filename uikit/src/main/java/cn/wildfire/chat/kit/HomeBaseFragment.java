package cn.wildfire.chat.kit;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.contact.ContactListActivity;
import cn.wildfire.chat.kit.contact.newfriend.SearchUserActivity;
import cn.wildfire.chat.kit.conversation.CreateConversationActivity;
import cn.wildfire.chat.kit.qrcode.ScanQRCodeActivity;
import cn.wildfire.chat.kit.widget.ProgressFragment;
import cn.wildfirechat.remote.ChatManager;

/**
 * 首页基础fragment 统一管理 menu
 */
public class HomeBaseFragment extends Fragment {
    private static final int REQUEST_CODE_SCAN_QR_CODE = 102;
    private static final int REQUEST_CODE_PICK_CONTACT = 101;
    private static final int REQUEST_CREATE_GROUP = 100;
    protected MenuItem chatItem;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.main, menu);
        afterMenus(menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    protected void afterMenus(Menu menu) {
        boolean isEnableSecretChat = ChatManager.Instance().isEnableSecretChat();
        if (!isEnableSecretChat) {
            MenuItem menuItem = menu.findItem(R.id.secretChat);
            menuItem.setEnabled(false);
        }
        chatItem = menu.findItem(R.id.chat);
        chatItem.setEnabled(checkCreateGroupPermission());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.more) {
            if (chatItem != null) {
                chatItem.setEnabled(checkCreateGroupPermission());
            }
        } else if (itemId == R.id.chat) {
            createConversation();
        } else if (itemId == R.id.secretChat) {
            pickContactToCreateSecretConversation();
        } else if (itemId == R.id.add_contact) {
            searchUser();
        } else if (itemId == R.id.scan_qrcode) {
            String[] permissions = new String[]{Manifest.permission.CAMERA};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!checkPermission(permissions)) {
                    requestPermissions(permissions, 100);
                    return true;
                }
            }
            getActivity().startActivityForResult(new Intent(getActivity(), ScanQRCodeActivity.class), REQUEST_CODE_SCAN_QR_CODE);
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkCreateGroupPermission() {
        SharedPreferences sp2 = getContext().getSharedPreferences(Config.SP_INIT_FILE_NAME, Context.MODE_PRIVATE);
        return sp2.getBoolean("createGroupEnable", true);
    }

    private void createConversation() {
        Intent intent = new Intent(getActivity(), CreateConversationActivity.class);
        getActivity().startActivityForResult(intent, REQUEST_CREATE_GROUP);
    }

    private void pickContactToCreateSecretConversation() {
        Intent intent = new Intent(getActivity(), ContactListActivity.class);
        intent.putExtra("showChannel", false);
        getActivity().startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT);
    }

    private void searchUser() {
        Intent intent = new Intent(getActivity(), SearchUserActivity.class);
        getActivity().startActivity(intent);
    }

    public boolean checkPermission(String permission) {
        return checkPermission(new String[]{permission});
    }

    public boolean checkPermission(String[] permissions) {
        boolean granted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissions) {
                granted = getContext().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
                if (!granted) {
                    break;
                }
            }
        }
        return granted;
    }
}
