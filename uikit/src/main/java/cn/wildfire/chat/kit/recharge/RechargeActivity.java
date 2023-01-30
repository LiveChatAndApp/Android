package cn.wildfire.chat.kit.recharge;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.utils.LogHelper;
import cn.wildfirechat.model.RechargeChannel;
import cn.wildfirechat.model.RechargeResultInfo;
import cn.wildfirechat.model.WalletInfo;
import cn.wildfirechat.model.WalletOrderInfo;
import cn.wildfirechat.model.WebResponse;

/**
 * 充值
 */
public class RechargeActivity extends WfcBaseActivity {

    @BindView(R2.id.radioGroup)
    RadioGroup radioGroup;
    @BindView(R2.id.selectChannelSpinner)
    Spinner selectChannelSpinner;
    @BindView(R2.id.submitButton)
    Button submitButton;
    @BindView(R2.id.rechargeAmountEditView)
    EditText rechargeAmountEditView;

    private static final int REQUEST_CODE_PICK_IMAGE = 100;

    private UserViewModel userViewModel;
    private RechargeViewModel rechargeViewModel;
    RechargeChannelAdapter adapter;
    private WalletInfo info;
    private int spinnerIndex = 0;
    private int method = 0;
    private long lastClick = 0;

    @Override
    protected int contentLayout() {
        return R.layout.activity_recharge;
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        rechargeViewModel = new ViewModelProvider(this).get(RechargeViewModel.class);
        init();
    }

    private void init() {
        info = getIntent().getParcelableExtra("wallet");
        initView();
        initData();
    }

    private void initView() {
        setAdapter();
    }

    private void initData() {
        getRechargeData("1");
    }

    private void setAdapter() {
        adapter = new RechargeChannelAdapter();
        selectChannelSpinner.setAdapter(adapter);
    }

    // 取得充值資料
    private void getRechargeData(String type) {
        rechargeViewModel.getWalletOrderDetail(type).observe(this, response -> {
            if (response.code != 0) {
                Toast.makeText(RechargeActivity.this, response.message, Toast.LENGTH_SHORT).show();
                return;
            }
            adapter.setData(response.result);
        });
    }

    @SuppressLint("InvalidR2Usage")
    @OnCheckedChanged({R2.id.bankRadioButton, R2.id.weChatRadioButton, R2.id.aliRadioButton})
    void checkChange(CompoundButton button, boolean checked) {
        if (checked) {
            int id = button.getId();
            if (id == R.id.bankRadioButton) {
                getRechargeData("1");
                method = 1;
            } else if (id == R.id.weChatRadioButton) {
                getRechargeData("2");
                method = 2;
            } else if (id == R.id.aliRadioButton) {
                getRechargeData("3");
                method = 3;
            }
        }
    }

    @OnClick(R2.id.submitButton)
    void submitClick() {
        if ((System.currentTimeMillis() - lastClick) < 500) {
            LogHelper.e("click", "submitClick < 500");
            return;
        } else {
            lastClick = System.currentTimeMillis();
        }

        long amount = 0;
        try {
            amount = Long.parseLong(rechargeAmountEditView.getText().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (amount == 0) {
            Toast.makeText(this, R.string.please_enter_recharge_money, Toast.LENGTH_SHORT).show();
            return;
        }
        int index = selectChannelSpinner.getSelectedItemPosition();
        if (index < 0) {
            return;
        }
        RechargeChannel channel = (RechargeChannel) adapter.getItem(index);
        sendOrder(amount, info.currency, channel);
    }

    // 送单
    private void sendOrder(long amount, String currency, RechargeChannel channel) {
        rechargeViewModel.sendOrder(amount, channel.id, currency, channel.paymentMethod).observe(this, new Observer<WebResponse<RechargeResultInfo>>() {
            @Override
            public void onChanged(WebResponse<RechargeResultInfo> response) {
                if (response.code != 0) {
                    Toast.makeText(RechargeActivity.this, response.message, Toast.LENGTH_SHORT).show();
                    return;
                }
                changeToUploadActivity(channel, response.result);
            }
        });
    }

    // 上传充值截图
    private void changeToUploadActivity(RechargeChannel channel, RechargeResultInfo resultInfo) {
        Intent intent = new Intent(this, RechargeUploadActivity.class);
        Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(this,
                android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
        intent.putExtra("RechargeChannel", channel);
        intent.putExtra("RechargeResultInfo", resultInfo);
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE, bundle);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            finish();
        }
    }
}
