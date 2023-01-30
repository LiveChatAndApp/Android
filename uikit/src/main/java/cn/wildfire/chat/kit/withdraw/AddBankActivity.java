package cn.wildfire.chat.kit.withdraw;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import java.io.File;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.utils.LogHelper;
import cn.wildfire.chat.kit.utils.TextLengthFilter;
import cn.wildfirechat.remote.UserInfoCallback;

/**
 * 添加银行卡
 */
public class AddBankActivity extends WfcBaseActivity {

    @BindView(R2.id.bankCustomNameEditText)
    EditText bankCustomNameEditText;
    @BindView(R2.id.bankAccountEditText)
    EditText bankAccountEditText;
    @BindView(R2.id.bankNameEditText)
    EditText bankNameEditText;
    @BindView(R2.id.ownerEditText)
    EditText ownerEditText;
    @BindView(R2.id.addButton)
    Button addButton;

    private UserViewModel viewModel;
    private long lastClick = 0;

    @Override
    protected int contentLayout() {
        return R.layout.activity_add_bank;
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        init();
    }

    private void init() {
        bankCustomNameEditText.setFilters(new InputFilter[]{new TextLengthFilter(20, this)});
        bankAccountEditText.setFilters(new InputFilter[]{new TextLengthFilter(30, this)});
        bankNameEditText.setFilters(new InputFilter[]{new TextLengthFilter(20, this)});
        ownerEditText.setFilters(new InputFilter[]{new TextLengthFilter(20, this)});
        viewModel = new ViewModelProvider(this).get(UserViewModel.class);
    }

    @OnTextChanged(value = R2.id.bankCustomNameEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void bankCustomName(Editable editable) {
        addButton.setEnabled(checkText());
    }

    @OnTextChanged(value = R2.id.bankAccountEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void bankAccount(Editable editable) {
        addButton.setEnabled(checkText());
    }

    @OnTextChanged(value = R2.id.bankNameEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void bankName(Editable editable) {
        addButton.setEnabled(checkText());
    }

    @OnTextChanged(value = R2.id.ownerEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void bankOwner(Editable editable) {
        addButton.setEnabled(checkText());
    }

    private boolean checkText() {
        String customName = bankCustomNameEditText.getText().toString();
        String account = bankAccountEditText.getText().toString();
        String bankName = bankNameEditText.getText().toString();
        String owner = ownerEditText.getText().toString();
        if (TextUtils.isEmpty(customName) || TextUtils.isEmpty(account) || TextUtils.isEmpty(bankName) || TextUtils.isEmpty(owner)) {
            return false;
        }
        return true;
    }

    @OnClick(R2.id.addButton)
    void addBankCard() {
        if ((System.currentTimeMillis() - lastClick) < 500) {
            LogHelper.e("click", "addBankCard < 500");
            return;
        } else {
            lastClick = System.currentTimeMillis();
        }
        String customName = bankCustomNameEditText.getText().toString();
        String account = bankAccountEditText.getText().toString();
        String bankName = bankNameEditText.getText().toString();
        String ownerName = ownerEditText.getText().toString();
        addBank(null, customName, account, bankName, ownerName);
    }

    private void addBank(File file, String customName, String account, String bankName, String ownerName) {
        viewModel.addPaymentMethod(1, file, account, bankName, ownerName, customName).observe(this, result -> {
            if ("done".equals(result)) {
                Toast.makeText(AddBankActivity.this, getString(R.string.toast_add_bank_card_success), Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(AddBankActivity.this, result, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
