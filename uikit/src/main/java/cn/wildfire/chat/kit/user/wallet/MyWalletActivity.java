package cn.wildfire.chat.kit.user.wallet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.recharge.RechargeActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.user.password.ResetTradePasswordActivity;
import cn.wildfire.chat.kit.withdraw.BankWithdrawActivity;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.model.WalletInfo;

/**
 * 我的钱包
 */
public class MyWalletActivity extends WfcBaseActivity {

    private int REQUEST_TRADE = 100;
    private int REQUEST_RECHARGE = 105;

    @BindView(R2.id.balanceTextView)
    MaterialTextView balanceTextView;

    @BindView(R2.id.rechargeButton)
    Button rechargeButton;

    @BindView(R2.id.withdrawButton)
    Button withdrawButton;

    private UserViewModel viewModel;
    private UserInfo userInfo;

    @Override
    protected int contentLayout() {
        return R.layout.activity_my_wallet;
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getData();
    }

    private void init() {
        userInfo = getIntent().getParcelableExtra("userInfo");
        viewModel = new ViewModelProvider(this).get(UserViewModel.class);
        subMyWallet();
    }

    private void subMyWallet() {
        viewModel.subMyWallet().observe(this, info -> {
            if (info.error) {
                if (TextUtils.isEmpty(info.balance)) {
                    Toast.makeText(MyWalletActivity.this, R.string.toast_connect_error, Toast.LENGTH_SHORT).show();
                } else {
                    balanceTextView.setText(info.balance);
                }
                rechargeButton.setEnabled(true);
                withdrawButton.setEnabled(false);
                return;
            }
            balanceTextView.setText(info.balance);
            // 暂时不开启
//            rechargeButton.setEnabled(info.canRecharge);
//            withdrawButton.setEnabled(info.canWithdraw);
        });
    }

    private void getData() {
        viewModel.getMyWallet();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_TRADE == requestCode && RESULT_OK == resultCode) {
            userInfo.hasTradePwd = 1;
        } else if (REQUEST_RECHARGE == requestCode && RESULT_OK == resultCode) {
            WalletInfo info = viewModel.getWalletInfo();
            if (info == null) {
                Toast.makeText(this, getString(R.string.toast_no_wallet_data), Toast.LENGTH_SHORT).show();
                return;
            }
            changeToWalletDetail(info);
        }
    }

    @OnClick(R2.id.rechargeButton)
    void rechargeClick() {
        Intent intent = new Intent(this, RechargeActivity.class);
        Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(this,
                android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
        WalletInfo info = viewModel.getWalletInfo();
        if (info == null) {
            Toast.makeText(this, getString(R.string.toast_no_wallet_data), Toast.LENGTH_SHORT).show();
            return;
        }
        intent.putExtra("wallet", info);
        startActivityForResult(intent, REQUEST_RECHARGE, bundle);
    }

    @OnClick(R2.id.withdrawButton)
    void withdrawClick() {
        if (userInfo == null || userInfo.hasTradePwd != 1) {
            Toast.makeText(this, R.string.trade_password_null, Toast.LENGTH_SHORT).show();
            change2TradePassword();
            return;
        }
        Intent intent = new Intent(this, BankWithdrawActivity.class);
        Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(this,
                android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
        WalletInfo info = viewModel.getWalletInfo();
        if (info == null) {
            Toast.makeText(this, getString(R.string.toast_no_wallet_data), Toast.LENGTH_SHORT).show();
            return;
        }
        intent.putExtra("wallet", info);
        startActivity(intent, bundle);
    }

    // 切换到 交易密码设定
    private void change2TradePassword() {
        // 有区分 已设置或 未设置 , 待开发
        String phone = getSharedPreferences(Config.SP_INIT_FILE_NAME, Context.MODE_PRIVATE).getString("phone", "");

        Intent intent = new Intent(this, ResetTradePasswordActivity.class);
        intent.putExtra("phone", phone);
        intent.putExtra("userInfo", userInfo);
        Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(this,
                android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
        startActivityForResult(intent, REQUEST_TRADE, bundle);
    }

    @Override
    protected int menu() {
        return R.menu.my_wallect;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.detail) {
            WalletInfo info = viewModel.getWalletInfo();
            if (info == null) {
                Toast.makeText(this, getString(R.string.toast_no_wallet_data), Toast.LENGTH_SHORT).show();
                return super.onOptionsItemSelected(item);
            }
            changeToWalletDetail(info);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void changeToWalletDetail(WalletInfo info) {
        Intent intent = new Intent(this, WalletDetailActivity.class);
        Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(this,
                android.R.anim.fade_in, android.R.anim.fade_out).toBundle();

        intent.putExtra("wallet", info);
        startActivity(intent, bundle);
    }
}
