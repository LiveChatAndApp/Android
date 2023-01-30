package cn.wildfire.chat.kit.user.wallet;

import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.WalletInfo;
import cn.wildfirechat.model.WalletOrderInfo;

/**
 * 錢包明細
 */
public class WalletDetailActivity extends WfcBaseActivity {

    @BindView(R2.id.recyclerView)
    RecyclerView recyclerView;

    private UserViewModel userViewModel;
    private WalletInfo walletInfo;
    private WalletDetailAdapter adapter;

    @Override
    protected int contentLayout() {
        return R.layout.activity_wallet_detail;
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        init();
    }

    private void init() {
        initView();
        initData();
    }

    private void initView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        setAdapter();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getWalletDetailData();
    }

    private void initData() {
        walletInfo = getIntent().getParcelableExtra("wallet");
    }

    private void setAdapter() {
        if (adapter == null) {
            adapter = new WalletDetailAdapter();
        }
        recyclerView.setAdapter(adapter);
    }

    // 取得明細資料
    private void getWalletDetailData() {
        userViewModel.getWalletDetail().observe(this, response -> {
            if (response.code != 0) {
                Toast.makeText(WalletDetailActivity.this, response.message, Toast.LENGTH_SHORT).show();
                return;
            }
            adapter.setDataList(response.result);
        });
    }
}
