package cn.wildfire.chat.kit.withdraw;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import butterknife.OnTextChanged;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.user.wallet.BankCardAdapter;
import cn.wildfire.chat.kit.utils.LogHelper;
import cn.wildfirechat.model.PaymentInfo;
import cn.wildfirechat.model.WalletInfo;

/**
 * 提现
 */
public class BankWithdrawActivity extends WfcBaseActivity {

    @BindView(R2.id.withdrawAmountEditView)
    EditText withdrawAmountEditView; // 提现金额
    @BindView(R2.id.balanceTextView)
    TextView balanceTextView; // 余额
    @BindView(R2.id.canWithdrawBalance)
    TextView canWithdrawBalance;// 可提现额度
    @BindView(R2.id.selectBankSpinner)
    Spinner selectBankSpinner; // 选择银行

    @BindView(R2.id.bankInfoGroup)
    Group bankInfoGroup;
    @BindView(R2.id.bankAccountNumberTextView)
    TextView bankAccountNumberTextView; // 银行账号
    @BindView(R2.id.bankNameTextView)
    TextView bankNameTextView; // 银行名称
    @BindView(R2.id.bankOwnerNameTextView)
    TextView bankOwnerNameTextView; // 收款人姓名

    @BindView(R2.id.submitButton)
    Button submitButton;

    @BindView(R2.id.empty)
    MaterialTextView empty;

    private UserViewModel viewModel;
    private WalletInfo info;
    private int spinnerIndex = 0;
    private long lastClick = 0;

    @Override
    protected int contentLayout() {
        return R.layout.activity_bank_withdraw;
    }

    @Override
    protected void afterViews() {
        if (!isDarkTheme()) {
            setTitleBackgroundResource(R.color.white, false);
        }
        viewModel = new ViewModelProvider(this).get(UserViewModel.class);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getData();
    }

    // 设定馀额
    private void setBalanceData(WalletInfo info) {
        // 馀额
        balanceTextView.setText(getString(R.string.balance_text)+" : "+info.balance);
        double dCanWithdrawBalance = 0;
        try {
            double balance = Double.parseDouble(info.balance);
            double freeze = Double.parseDouble(info.freeze);
            dCanWithdrawBalance = balance - freeze;
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 可提现额度
        canWithdrawBalance.setText(getString(R.string.could_withdraw_money)+" : " + dCanWithdrawBalance);
    }

    private void initView() {


    }

    private void setSelectBankInfo() {
        BankCardAdapter adapter = (BankCardAdapter) selectBankSpinner.getAdapter();
        PaymentInfo info = (PaymentInfo) adapter.getItem(selectBankSpinner.getSelectedItemPosition());

        bankInfoGroup.setVisibility(View.VISIBLE);
        bankAccountNumberTextView.setText(info.bankCardNumber); // 银行账号
        bankNameTextView.setText(info.bankName); // 银行名称
        bankOwnerNameTextView.setText(info.ownerName); // 收款人姓名
    }

    private void setEmptyView() {
        PaymentInfo info = new PaymentInfo();
        info.name = "选择银行卡";
        List<PaymentInfo> list = new ArrayList<>();
        list.add(0, info);
        BankCardAdapter adapter = new BankCardAdapter(list);
        selectBankSpinner.setAdapter(adapter);

        spinnerIndex = 0;
        bankInfoGroup.setVisibility(View.GONE);
        bankAccountNumberTextView.setText(""); // 银行账号
        bankNameTextView.setText(""); // 银行名称
        bankOwnerNameTextView.setText(""); // 收款人姓名
//        empty.setVisibility(View.VISIBLE);
    }

    // 设定钱包 及 取得提款方式
    private void getData() {
        info = getIntent().getParcelableExtra("wallet");
        setBalanceData(info);
        viewModel.getPaymentMethod(true).observe(this, list -> {
            if (list == null || list.size() == 0) {
                setEmptyView();
                return;
            }
            // 设定银行卡 spinner
            serSpinnerAdapter(list);
        });
    }

    // 设定银行卡 spinner
    private void serSpinnerAdapter(List<PaymentInfo> list) {
        BankCardAdapter adapter = new BankCardAdapter(list);
        selectBankSpinner.setAdapter(adapter);
//        empty.setVisibility(View.GONE);
    }

    // 输入 提现金额
    @OnTextChanged(value = R2.id.withdrawAmountEditView, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void inputWithdrawAmount(Editable editable) {

    }

    @OnItemSelected(R2.id.selectBankSpinner)
    public void selectBankSpinner(Spinner spinner, int position) {
        if (position == 0) {
            spinner.setSelection(spinnerIndex);
            return;
        }
        spinnerIndex = position;
        if (position > 0) {
            setSelectBankInfo();
            submitButton.setEnabled(true);
        } else {
            bankInfoGroup.setVisibility(View.GONE);
            submitButton.setEnabled(false);
        }
    }

    @OnClick(R2.id.submitButton)
    void submitButton() {
        if ((System.currentTimeMillis() - lastClick) < 500) {
            LogHelper.e("click", "submitClick < 500");
            return;
        } else {
            lastClick = System.currentTimeMillis();
        }

        //Todo 不可以超过 可提现额度
        double amount = 0;
        try {
            // 目前使用者提现金额
            amount = Double.parseDouble(withdrawAmountEditView.getText().toString());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(BankWithdrawActivity.this, R.string.toast_withdraw_amount_error2, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            double balance = Double.parseDouble(info.balance);
            double freeze = Double.parseDouble(info.freeze);
            // 可提现额度
            double dCanWithdrawBalance = balance - freeze;

            if (amount > dCanWithdrawBalance) {
                // 不可超过可提现额度
                Toast.makeText(BankWithdrawActivity.this, R.string.toast_withdraw_amount_error, Toast.LENGTH_SHORT).show();
            } else if (amount <= 0) {
                // 不可 0
                Toast.makeText(BankWithdrawActivity.this, R.string.toast_withdraw_amount_error2, Toast.LENGTH_SHORT).show();
            } else if (spinnerIndex == 0) {
                Toast.makeText(BankWithdrawActivity.this, R.string.toast_select_bank_card_error, Toast.LENGTH_SHORT).show();
            } else {
                showTradePasswordDialog();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 输入交易密码 dialog
    private void showTradePasswordDialog() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(R.string.trade_password)
                .inputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
                .input(getString(R.string.please_enter_trade_password), "", new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

                    }
                })
                .positiveText(R.string.submit)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        try {
                            String password = dialog.getInputEditText().getText().toString();
                            if (TextUtils.isEmpty(password)) {
                                Toast.makeText(BankWithdrawActivity.this, getString(R.string.trade_password_no_empty), Toast.LENGTH_SHORT).show();
                                return;
                            }
                            checkTradePassword(password);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        dialog.dismiss();
                    }
                })
                .negativeText(R.string.cancel)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .cancelable(true)
                .build();
        dialog.show();
    }

    // 确认交易密码
    private void checkTradePassword(String tradePwd) {
        submitWithdraw(tradePwd);
    }

    // 送提现订单
    private void submitWithdraw(String tradePwd) {
        BankCardAdapter adapter = (BankCardAdapter) selectBankSpinner.getAdapter();
        PaymentInfo paymentInfo = (PaymentInfo) adapter.getItem(spinnerIndex);
        double amount = Double.parseDouble(withdrawAmountEditView.getText().toString());
        viewModel.withdrawApply(1, amount, info.currency, paymentInfo.id, tradePwd).observe(this, result -> {
            if (result.code == 0) {
                Toast.makeText(BankWithdrawActivity.this, R.string.toast_withdraw_request_success, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(BankWithdrawActivity.this, result.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected int menu() {
        return R.menu.bank_withdraw;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.bankList == item.getItemId()) {
            Intent intent = new Intent(this, BankListActivity.class);
            Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(this,
                    android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
            startActivity(intent, bundle);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
