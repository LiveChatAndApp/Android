package cn.wildfire.chat.kit.withdraw;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

import butterknife.BindView;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.PaymentInfo;
import cn.wildfirechat.remote.UserInfoCallback;

/**
 * 银行卡列表
 */
public class BankListActivity extends WfcBaseActivity {

    @BindView(R2.id.bankListRecyclerView)
    RecyclerView bankListRecyclerView;
    @BindView(R2.id.empty)
    MaterialTextView empty;

    private UserViewModel viewModel;
    private BankListAdapter adapter;

    @Override
    protected int contentLayout() {
        return R.layout.activity_bank_list;
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        init();
    }

    private void init() {
        viewModel = new ViewModelProvider(this).get(UserViewModel.class);
        bankListRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        if (adapter == null) {
            adapter = new BankListAdapter();
            adapter.setListener(new BankListAdapter.BankCardDeleteListener() {
                @Override
                public void onBankCardDeleteClick(PaymentInfo info) {
                    deletePaymentDialog(info);
                }
            });
            bankListRecyclerView.setAdapter(adapter);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        getPaymentList();
    }

    private void deletePaymentDialog(PaymentInfo info) {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(R.string.delete_payment_title)
                .content(R.string.delete_payment_content)
                .positiveText(R.string.submit3)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        deletePayment(info);
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

    private void getPaymentList() {
        viewModel.getPaymentMethod(false).observe(this, list -> {
            if (list == null || list.size() == 0) {
                empty.setVisibility(View.VISIBLE);
                return;
            }
            adapter.setData(list);
            empty.setVisibility(View.GONE);
        });
    }

    private void deletePayment(PaymentInfo info) {
        viewModel.deletePaymentMethod(info.id).observe(this, result -> {
            if (!"done".equals(result)) {
                Toast.makeText(BankListActivity.this, result, Toast.LENGTH_SHORT).show();
                return;
            }
            adapter.deleteData(info);
        });
    }

    @Override
    protected int menu() {
        return R.menu.bank_list;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.addBank == item.getItemId()) {
            Intent intent = new Intent(this, AddBankActivity.class);
            Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(this,
                    android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
            startActivity(intent, bundle);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
